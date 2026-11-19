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
 * ligne borne le travail (ADR-014) : au dépassement, la fin de ligne
 * garde la portée de son contexte, l'incident se journalise — jamais un
 * gel de l'interface.
 */
final class GrammarLexer implements LanguageLexer {

    // L'API de journalisation du JDK, pas celle de System.Logger :
    // la seconde manque sur appareil avant le niveau 33 (R8 l'a
    // révélé à la réduction, M7.6 étape 7) — la première vit sur le
    // bureau comme sur Android depuis toujours.
    private static final Logger JOURNAL =
            Logger.getLogger(GrammarLexer.class.getName());

    /** Le budget par défaut : essais d'expression par ligne. */
    private static final int BUDGET_ETAPES_DEFAUT = 20_000;

    private final Grammar grammaire;
    private final int budgetEtapes;

    /** Les essais de la ligne en cours — le budget est par ligne. */
    private long etapes;

    /** Vrai dès que la ligne en cours a dépassé son budget. */
    private boolean deborde;

    /** Les lignes dégradées par dépassement de budget, vu par ce lexeur. */
    private int incidents;

    GrammarLexer(Grammar grammaire) {
        this(grammaire, BUDGET_ETAPES_DEFAUT);
    }

    /**
     * Un lexeur à budget donné — le banc de conformité et les tests du
     * bornage s'en servent.
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

    @Override
    public LexState initialState() {
        return GrammarState.EMPTY;
    }

    @Override
    public LexedLine lex(CharSequence line, LexState input) {
        GrammarState etat = (GrammarState) input;
        Utf8Line donnees = Utf8Line.preparer(line);
        etapes = 0;
        deborde = false;
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
            etat = emettre(tokens, donnees, candidat, etat);
            pos = candidat.end();
        }
        return figer(tokens, etat);
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

    /** Cherche la plus ancienne occurrence d'une expression à partir de la position. */
    private @Nullable Candidat chercher(Utf8Line donnees, int pos, Regex pattern, char type,
            @Nullable BlockRule bloc, @Nullable OpenBlock ferme, @Nullable SimpleRule simple) {
        if (etapes++ >= budgetEtapes) {
            deborder(donnees);
            return null;
        }
        Matcher m = pattern.matcher(donnees.octets());
        if (m.search(pos, donnees.octets().length, Option.NONE) < 0
                || m.getEnd() == m.getBegin()) {
            return null;
        }
        return new Candidat(m.getBegin(), m.getEnd(), m, type, bloc, ferme, simple);
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
            return etat.depiler();
        }
        if (candidat.type() == Candidat.DEBUT && candidat.bloc() != null) {
            BlockRule bloc = candidat.bloc();
            emettreCaptures(tokens, donnees, candidat.matcher(), bloc.capturesDebut(),
                    bloc.portee(), etat);
            OpenBlock ouvert = bloc.referencesFin()
                    ? OpenBlock.open(bloc, candidat.matcher(), donnees)
                    : OpenBlock.open(bloc);
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
            tokens.add(token(contextStack, base, donnees, m.getBegin(),
                    m.getEnd()));
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
