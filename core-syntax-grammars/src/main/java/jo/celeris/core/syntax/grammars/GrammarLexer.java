package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jo.celeris.core.syntax.scope.ScopeStacks;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;

import jo.celeris.core.syntax.Token;
import org.jspecify.annotations.Nullable;

/**
 * Lexeur d'une grammaire TextMate (ADR-004, ADR-014) : lexicographie
 * d'UNE ligne à la fois, pilotée par la pile des blocs ouverts
 * transportée d'une ligne à la suivante (PIEGE-21). À chaque position,
 * le candidat le plus à gauche gagne — les ex æquo se départagent par
 * l'ordre des règles, la fermeture du bloc ouvert prime. Les portées se
 * posent en pile : le contenu d'un bloc porte sa portée de contenu,
 * sinon la portée de sa construction, sinon celle du bloc englobant —
 * la portée racine sert de plancher. Les injections (H-05) s'appliquent
 * partout où leur sélecteur matche la pile courante, à gauche ou à
 * droite des règles du contexte selon leur priorité. Le budget par
 * ligne borne le travail (ADR-014) : au régime livré il est
 * proportionnel à la longueur de ligne (plancher 20 000 essais,
 * plafond deux millions — la ligne dense ordinaire se colore entière,
 * la ligne monstre reste bornée) ; au dépassement, la fin de ligne
 * garde la portée de son contexte, l'incident se journalise — jamais
 * un gel de l'interface.
 */
final class GrammarLexer implements LanguageLexer {

    // L'API de journalisation du JDK, pas celle de System.Logger :
    // la seconde manque sur appareil avant le niveau 33 (R8 l'a
    // révélé à la réduction, M7.6 étape 7) — la première vit sur le
    // bureau comme sur Android depuis toujours.
    private static final Logger JOURNAL =
            Logger.getLogger(GrammarLexer.class.getName());

    /** Le plancher du budget par défaut : essais d'expression par
     *  ligne — les lignes ordinaires (une construction par mot) ne
     *  l'approchent jamais. */
    private static final int BUDGET_ETAPES_DEFAUT = 20_000;

    /** Essais par caractère d'une ligne dense : la mesure de l'audit
     *  coloration (H9) donne ~168 essais par jeton sur la grammaire
     *  javascript livrée — 100 par caractère couvre la densité réelle
     *  (une ligne minifiée de 310 caractères débordait l'ancien
     *  plafond fixe) en bornant toujours le travail. */
    private static final long ESSAIS_PAR_CARACTERE = 100L;

    /** Le plafond du budget proportionnel : au-delà, la ligne reste
     *  bornée (une ligne monstre d'un méga-octet ne paie jamais plus
     *  que deux millions d'essais — le gel reste impossible). */
    private static final long PLAFOND_ETAPES = 2_000_000L;

    private final Grammar grammaire;
    /** Le budget des lignes : un nombre exact (banc de bornage), ou
     *  {@link #PROPORTIONNEL} pour le régime livré — proportionnel à
     *  la longueur de ligne, plancher et plafond compris. */
    private final int budgetEtapes;

    /** Le marqueur du régime proportionnel (le constructeur public). */
    private static final int PROPORTIONNEL = -1;

    /** Les essais de la ligne en cours — le budget est par ligne. */
    private long etapes;

    /** Le budget effectif de la ligne en cours. */
    private long budgetLigne;

    /** Vrai dès que la ligne en cours a dépassé son budget. */
    private boolean deborde;

    /** L'ancre de document (chantier C2) : vrai sur la PREMIÈRE ligne
     *  de la vie du lexeur — le service bâtit un lexeur neuf par
     *  analyse et la passe part de la ligne 0, le début du texte y
     *  coïncide avec le début du document (modèle tm4e : allowA). */
    private boolean premiereLigne;

    /** Faux dès le premier lex : plus AUCUNE ligne suivante n'est la
     *  première du document. */
    private boolean jamaisLexe = true;

    /** La dernière position d'un tour sans progression — la garde
     *  anti-boucle des correspondances de largeur nulle (audit H8) :
     *  un seul tour sans progression par position, l'octet suivant
     *  sinon. Remise à zéro à chaque ligne. */
    private int positionSansProgression = -1;

    /** Vrai quand le dernier candidat émis a touché une bordure —
     *  la comparaison d'identité suffit : emettre rend la MÊME
     *  instance pour un motif simple, une neuve pour une bordure
     *  (l'égalité de valeur sur l'état récursionne dans le graphe
     *  cyclique des règles — le débordement de pile l'a prouvé). */
    private boolean bordureTouchee;

    /** Les lignes dégradées par dépassement de budget, vu par ce lexeur. */
    private int incidents;

    /** Les octets de la ligne en cours — les matchers réutilisés s'y
     *  lient (une ligne de lexicographie, remis à vide à la suivante). */
    private byte @Nullable [] octetsLigne;

    /** Le cache d'échecs du chantier B (audit F8, modèle tm4e
     *  {@code JoniOnigRegExp.search}) : une expression qui a échoué
     *  depuis p a LU toute la fin de ligne sans rien trouver — la
     *  re-tentative depuis toute position SUPÉRIEURE rend le même
     *  échec sans relire (le lexeur retente la même règle au caractère
     *  suivant : le néant se sert). Remis à vide à chaque ligne (les
     *  positions sont relatives à la ligne). */
    private final IdentityHashMap<Regex, Integer> echecs =
            new IdentityHashMap<>();

    /** Les matchers joni de la ligne en cours, par expression : le
     *  Matcher se RÉUTILISE tant que les octets ne changent pas (une
     *  ligne) — l'allocation d'un Matcher par recherche était le
     *  premier coût du lexeur (chantier B : ~97 recherches par ligne
     *  python ordinaire). Remis à vide à chaque ligne. */
    private final IdentityHashMap<Regex, Matcher> matchers =
            new IdentityHashMap<>();

    GrammarLexer(Grammar grammaire) {
        this(grammaire, PROPORTIONNEL);
    }

    /**
     * Un lexeur à budget donné — le banc de conformité et les tests du
     * bornage s'en servent : le budget exact s'applique TEL QUEL (le
     * banc de bornage déborde à dessein sur des lignes courtes).
     *
     * @param grammaire    la grammaire du langage
     * @param budgetEtapes le nombre maximal d'essais d'expression par ligne
     */
    GrammarLexer(Grammar grammaire, int budgetEtapes) {
        this.grammaire = grammaire;
        this.budgetEtapes = budgetEtapes;
    }

    /** Les lignes dégradées par dépassement de budget depuis ce lexeur. */
    int incidents() {
        return incidents;
    }

    /** Les essais d'expression de la DERNIÈRE ligne lexée — le banc
     *  du cache de prochaine correspondance (chantier B) y lit le
     *  travail réel : une ligne ordinaire ne paie pas une recherche
     *  par règle par position quand les échecs se mémorisent. */
    long essaisDerniereLigne() {
        return etapes;
    }

    @Override
    public LexState initialState() {
        return GrammarState.EMPTY;
    }

    @Override
    public LexedLine lex(CharSequence line, LexState input) {
        GrammarState etat = (GrammarState) input;
        Utf8Line donnees = Utf8Line.preparer(line);
        // L'ancre de document (chantier C2) : la première ligne de la
        // vie du lexeur EST la première du document.
        premiereLigne = jamaisLexe;
        jamaisLexe = false;
        etapes = 0;
        deborde = false;
        positionSansProgression = -1;
        // Le cache vit UNE ligne : les positions mémorisées sont
        // relatives à la ligne, la suivante repart à vide.
        octetsLigne = donnees.octets();
        echecs.clear();
        matchers.clear();
        // Le budget de LA ligne : exact au banc, proportionnel au
        // régime livré — plancher pour les lignes ordinaires, plafond
        // contre les lignes monstres (jamais de gel, jamais de
        // truncature silencieuse d'une ligne dense ordinaire).
        budgetLigne = budgetEtapes != PROPORTIONNEL ? budgetEtapes
                : Math.min(PLAFOND_ETAPES,
                        Math.max(BUDGET_ETAPES_DEFAUT,
                                ESSAIS_PAR_CARACTERE * donnees.octets().length));
        List<Token> tokens = new ArrayList<>();
        int n = donnees.octets().length;
        int pos = 0;
        while (pos < n) {
            Candidat candidat = premier(donnees, pos, etat);
            if (candidat == null) {
                gap(donnees, tokens, pos, n, etat);
                return figer(tokens, etat);
            }
            gap(donnees, tokens, pos, candidat.start(), etat);
            bordureTouchee = false;
            etat = emettre(tokens, donnees, candidat, etat);
            pos = avancement(candidat, pos);
        }
        return figer(tokens, fermerEnFinDeLigne(donnees, etat));
    }

    /**
     * La position après l'émission du candidat : une correspondance
     * de largeur nulle À la position tient AU PREMIER tour sans
     * progression quand une bordure a changé l'état — le contexte
     * neuf relit le caractère sous la bordure, la sémantique
     * TextMate ; au SECOND tour au même endroit (paires
     * ouverture/fermeture toutes deux de largeur nulle), la garde
     * anti-boucle avance d'un octet, quoi qu'il arrive — le budget
     * d'essais borne le reste.
     */
    private int avancement(Candidat candidat, int pos) {
        int suivant = candidat.end();
        if (suivant != pos) {
            return suivant;
        }
        if (positionSansProgression == pos) {
            return pos + 1;
        }
        positionSansProgression = pos;
        return bordureTouchee ? pos : pos + 1;
    }

    /**
     * Les fermetures de largeur nulle À la fin de ligne (audit H8) :
     * « (?<=\}) » clos une déclaration APRÈS l'accolade que le corps
     * a consommée — le dernier tour de boucle ne les cherche jamais
     * (pos atteint n sans dernière recherche). Tant que le bloc
     * ouvert se ferme à n, il se dépile : l'état de sortie ne
     * traîne pas un bloc mort vers la ligne suivante.
     */
    private GrammarState fermerEnFinDeLigne(Utf8Line donnees, GrammarState etat) {
        while (etat.bloc() != null
                && chercher(donnees, donnees.octets().length, etat.bloc().end(),
                        Candidat.FIN, null, etat.bloc(), null) != null) {
            etat = etat.depiler();
        }
        return etat;
    }

    /**
     * Le candidat le plus à gauche à partir de la position : la
     * fermeture du bloc ouvert d'abord, les règles du contexte ensuite —
     * chaque liste référencée d'un contexte ne se considère qu'une fois.
     * Les injections (H-05) s'intercalent selon leur priorité : à gauche
     * ({@code L:}) avant la fermeture et le contexte, à droite
     * ({@code R:} et sans préfixe) après — l'hôte reste maître chez
     * lui. Rend null quand plus rien ne matche.
     */
    private @Nullable Candidat premier(Utf8Line donnees, int pos, GrammarState etat) {
        Set<List<Rule>> vus = Collections.newSetFromMap(new IdentityHashMap<>());
        Candidat meilleur = null;
        List<Injection> injections = grammaire.injections();
        if (injections.isEmpty()) {
            OpenBlock ouvert = etat.bloc();
            if (ouvert != null) {
                meilleur = chercher(donnees, pos, ouvert.end(), Candidat.FIN,
                        null, ouvert, null);
            }
            return parcourir(contexte(etat), donnees, pos, meilleur, vus);
        }
        List<String> pile = ScopeStacks.scopesOf(contextStack(etat));
        for (Injection injection : injections) {
            if (injection.prioriteGauche()
                    && injection.selector().score(pile) != null) {
                meilleur = parcourir(injection.regles(), donnees, pos,
                        meilleur, vus);
            }
        }
        OpenBlock ouvert = etat.bloc();
        if (ouvert != null) {
            meilleur = garder(meilleur, chercher(donnees, pos, ouvert.end(),
                    Candidat.FIN, null, ouvert, null));
        }
        meilleur = parcourir(contexte(etat), donnees, pos, meilleur, vus);
        for (Injection injection : injections) {
            if (!injection.prioriteGauche()
                    && injection.selector().score(pile) != null) {
                meilleur = parcourir(injection.regles(), donnees, pos,
                        meilleur, vus);
            }
        }
        return meilleur;
    }

    /** Les règles applicables : celles du bloc ouvert, ou celles de la grammaire. */
    private List<Rule> contexte(GrammarState etat) {
        OpenBlock ouvert = etat.bloc();
        return ouvert == null ? grammaire.patterns() : ouvert.regle().patterns();
    }

    /** Parcourt les règles — les références se suivent, chaque liste une fois. */
    private @Nullable Candidat parcourir(List<Rule> regles, Utf8Line donnees, int pos,
            @Nullable Candidat meilleur, Set<List<Rule>> vus) {
        for (Rule regle : regles) {
            if (regle instanceof ListRule liste) {
                if (vus.add(liste.regles())) {
                    meilleur = parcourir(liste.regles(), donnees, pos, meilleur, vus);
                }
            } else if (regle instanceof SimpleRule simple) {
                meilleur = garder(meilleur, chercher(donnees, pos, simple.pattern(),
                        Candidat.PATTERN, null, null, simple));
            } else if (regle instanceof BlockRule bloc) {
                meilleur = garder(meilleur, chercher(donnees, pos, bloc.start(),
                        Candidat.DEBUT, bloc, null, null));
            }
        }
        return meilleur;
    }

    /** Le matcher d'une expression pour LA ligne en cours : le premier
     *  appel le bâtit sur les octets de la ligne, les suivants le
     *  réemploient — le Matcher joni se réutilise tant que les octets
     *  sont les mêmes (une ligne de lexicographie). */
    private Matcher matcherDe(Regex pattern) {
        return matchers.computeIfAbsent(pattern, p -> p.matcher(octetsLigne));
    }

    /** Cherche la plus ancienne occurrence d'une expression à partir de
     *  la position — le cache d'échecs devant la recherche (chantier B,
     *  audit F8) : l'échec mémorisé d'une expression reste valable pour
     *  toute position supérieure (la recherche a LU toute la fin de
     *  ligne). L'ancre de document (chantier C2) : l'expression à
     *  {@code \A} se traduit en son double IMPOSSIBLE quand la ligne
     *  n'est pas la première du document. */
    private @Nullable Candidat chercher(Utf8Line donnees, int pos, Regex pattern, char type,
            @Nullable BlockRule bloc, @Nullable OpenBlock ferme, @Nullable SimpleRule simple) {
        if (etapes++ >= budgetLigne) {
            deborder(donnees);
            return null;
        }
        // La traduction d'ancre : le jeu ne vit que sur les grammaires
        // à \A (les autres ne paient qu'une lecture de carte vide).
        if (!premiereLigne) {
            Regex impossible = grammaire.ancres().get(pattern);
            if (impossible != null) {
                pattern = impossible;
            }
        }
        // Le cache d'échec : la recherche depuis echecPos a tout lu
        // jusqu'à la fin de ligne sans rien trouver — toute requête à
        // une position SUPÉRIEURE OU ÉGALE rend le même échec (un
        // sous-ensemble du vide reste vide), la re-tentative à p+1
        // (le cas du lexeur : la même règle au caractère suivant) ne
        // relit rien.
        Integer echec = echecs.get(pattern);
        if (echec != null && pos >= echec) {
            return null;
        }
        Matcher m = matcherDe(pattern);
        if (m.search(pos, donnees.octets().length, Option.NONE) < 0) {
            // L'échec se mémorise à la position de la recherche : toute
            // position inférieure relirait le même néant.
            echecs.put(pattern, pos);
            return null;
        }
        // La largeur nulle est LÉGITIME pour les bordures (audit
        // coloration H8) : fin en anticipation (?=…), ancre $ ou ^,
        // ouverture elle-même à largeur nulle — la rejeter laissait
        // le bloc ouvert POUR TOUJOURS, l'état contaminait la fin du
        // document. Elle ne l'est pas pour un motif simple : il ne
        // produirait rien (les moteurs de référence le sautent).
        if (m.getEnd() == m.getBegin() && type == Candidat.PATTERN) {
            return null;
        }
        return new Candidat(m.getBegin(), m.getEnd(), m, type,
                bloc, ferme, simple);
    }

    /** Le dépassement de budget se journalise une fois par ligne. */
    private void deborder(Utf8Line donnees) {
        if (!deborde) {
            deborde = true;
            incidents++;
            JOURNAL.log(Level.WARNING,
                    "budget de lexicographie dépassé sur une ligne de {0} caractères : "
                            + "la fin de ligne garde la portée de son contexte",
                    donnees.text().length());
        }
    }

    /** Le candidat strictement plus à gauche gagne ; à égalité, le premier trouvé. */
    private static @Nullable Candidat garder(@Nullable Candidat meilleur,
            @Nullable Candidat candidat) {
        if (candidat == null) {
            return meilleur;
        }
        if (meilleur == null || candidat.start() < meilleur.start()) {
            return candidat;
        }
        return meilleur;
    }

    /** Émet le jeton du candidat — motif, ouverture ou fermeture — et suit l'état. */
    private GrammarState emettre(List<Token> tokens, Utf8Line donnees, Candidat candidat,
            GrammarState etat) {
        if (candidat.type() == Candidat.FIN && candidat.ferme() != null) {
            emettreCaptures(tokens, donnees, candidat.matcher(),
                    candidat.ferme().regle().capturesFin(),
                    candidat.ferme().regle().portee(), etat);
            bordureTouchee = true;
            return etat.depiler();
        }
        if (candidat.type() == Candidat.DEBUT && candidat.bloc() != null) {
            BlockRule bloc = candidat.bloc();
            emettreCaptures(tokens, donnees, candidat.matcher(), bloc.capturesDebut(),
                    bloc.portee(), etat);
            OpenBlock ouvert = bloc.referencesFin()
                    ? OpenBlock.open(bloc, candidat.matcher(), donnees)
                    : OpenBlock.open(bloc);
            bordureTouchee = true;
            return etat.empiler(ouvert);
        }
        SimpleRule simple = candidat.simple();
        emettreCaptures(tokens, donnees, candidat.matcher(), simple == null ? null
                : simple.captures(), simple == null ? null : simple.portee(), etat);
        return etat;
    }

    /**
     * Émet un motif ou une bordure : sans capture, un jeton couvert par
     * la portée de base ; avec captures, chaque groupe capturé porte sa
     * portée et l'intervalle non couvert porte celle de la base — la
     * capture 0 couvre le tout. La base absente laisse la portée du
     * contexte. Chaque jeton porte sa PILE de portées internée (H-07)
     * — la couleur s'y résoudra par le thème, la sorte n'en reste que
     * la catégorie dérivée.
     */
    private void emettreCaptures(List<Token> tokens, Utf8Line donnees, Matcher m,
            @Nullable Map<Integer, Capture> captures, @Nullable String porteeBase,
            GrammarState etat) {
        String base = porteeBase != null ? porteeBase : contextePortee(etat);
        int contextStack = contextStack(etat);
        if (captures == null) {
            if (m.getEnd() > m.getBegin()) {
                tokens.add(token(contextStack, base, donnees, m.getBegin(),
                        m.getEnd()));
            }
            return;
        }
        Region groupes = m.getEagerRegion();
        int curseur = m.getBegin();
        for (Map.Entry<Integer, Capture> input : captures.entrySet()) {
            int number = input.getKey();
            if (number >= groupes.getNumRegs()) {
                continue;
            }
            int gd = groupes.getBeg(number);
            int gf = groupes.getEnd(number);
            if (gd < 0 || gf <= gd) {
                continue;
            }
            if (gd > curseur) {
                tokens.add(token(contextStack, base, donnees, curseur, gd));
            }
            Capture capture = input.getValue();
            tokens.add(token(contextStack,
                    capture.nom() != null ? capture.nom() : base,
                    donnees, gd, gf));
            curseur = gf;
        }
        if (m.getEnd() > curseur) {
            tokens.add(token(contextStack, base, donnees, curseur, m.getEnd()));
        }
    }

    /**
     * L'écart non couvert : les blancs seuls ne produisent rien, le
     * reste porte la pile de son contexte — le contenu du bloc le
     * plus profond qui porte une portée, la construction sinon, la
     * racine au plancher — rogné de ses blancs d'escorte.
     */
    private void gap(Utf8Line donnees, List<Token> tokens, int debutOctet, int finOctet,
            GrammarState etat) {
        CharSequence line = donnees.text();
        int start = donnees.versChar(debutOctet);
        int end = donnees.versChar(finOctet);
        int premier = premierNonBlanc(line, start, end);
        if (premier < 0) {
            return;
        }
        int dernier = dernierNonBlanc(line, start, end);
        int pile = contextStack(etat);
        tokens.add(new Token(ScopeStacks.kindOf(pile), premier, dernier + 1,
                pile));
    }

    /**
     * La pile de portées du contexte (H-07) : la racine de la
     * grammaire, puis chaque bloc ouvert de l'extérieur vers
     * l'intérieur — sa portée de contenu, sinon sa portée de
     * construction — internée une fois, partagée par tous les jetons
     * qui la portent.
     */
    private int contextStack(GrammarState etat) {
        List<String> portees = new ArrayList<>();
        for (GrammarState cadre = etat; cadre != null
                && !GrammarState.EMPTY.equals(cadre); cadre = cadre.parent()) {
            OpenBlock ouvert = cadre.bloc();
            if (ouvert == null) {
                break;
            }
            BlockRule regle = ouvert.regle();
            String portee = regle.contenu() != null
                    ? regle.contenu() : regle.portee();
            if (portee != null) {
                portees.add(portee);
            }
        }
        int pile = ScopeStacks.root(grammaire.porteeRacine());
        for (int i = portees.size() - 1; i >= 0; i--) {
            pile = ScopeStacks.push(pile, portees.get(i));
        }
        return pile;
    }

    /** La portée de la pile : contenu, construction, englobant, racine au plancher. */
    private String contextePortee(GrammarState etat) {
        for (GrammarState cadre = etat; cadre != null; cadre = cadre.parent()) {
            OpenBlock ouvert = cadre.bloc();
            if (ouvert == null) {
                break;
            }
            BlockRule regle = ouvert.regle();
            if (regle.contenu() != null) {
                return regle.contenu();
            }
            if (regle.portee() != null) {
                return regle.portee();
            }
        }
        return grammaire.porteeRacine();
    }

    private static LexedLine figer(List<Token> tokens, GrammarState etat) {
        return new LexedLine(tokens.toArray(new Token[0]), etat);
    }

    private static Token token(int contextStack, String portee, Utf8Line donnees,
            int debutOctet, int finOctet) {
        int pile = ScopeStacks.push(contextStack, portee);
        return new Token(ScopeStacks.kindOf(pile), donnees.versChar(debutOctet),
                donnees.versChar(finOctet), pile);
    }

    private static int premierNonBlanc(CharSequence line, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int dernierNonBlanc(CharSequence line, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return start;
    }

    /** Un candidat de lexicographie : bornes, correspondance et nature. */
    private record Candidat(int start, int end, Matcher matcher, char type,
            @Nullable BlockRule bloc, @Nullable OpenBlock ferme,
            @Nullable SimpleRule simple) {

        /** Un motif simple. */
        private static final char PATTERN = 'm';

        /** L'ouverture d'un bloc. */
        private static final char DEBUT = 'd';

        /** La fermeture du bloc ouvert. */
        private static final char FIN = 'f';
    }
}
