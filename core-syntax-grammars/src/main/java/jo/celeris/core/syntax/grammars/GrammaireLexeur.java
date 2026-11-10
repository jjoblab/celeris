package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.ScopeStacks;
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
 * la portée racine sert de plancher. Le budget par ligne borne le
 * travail (ADR-014) : au dépassement, la fin de ligne garde la portée
 * de son contexte, l'incident se journalise — jamais un gel de
 * l'interface.
 */
final class GrammaireLexeur implements LanguageLexer {

    // L'API de journalisation du JDK, pas celle de System.Logger :
    // la seconde manque sur appareil avant le niveau 33 (R8 l'a
    // révélé à la réduction, M7.6 étape 7) — la première vit sur le
    // bureau comme sur Android depuis toujours.
    private static final Logger JOURNAL =
            Logger.getLogger(GrammaireLexeur.class.getName());

    /** Le budget par défaut : essais d'expression par ligne. */
    private static final int BUDGET_ETAPES_DEFAUT = 20_000;

    private final Grammaire grammaire;
    private final int budgetEtapes;

    /** Les essais de la ligne en cours — le budget est par ligne. */
    private long etapes;

    /** Vrai dès que la ligne en cours a dépassé son budget. */
    private boolean deborde;

    /** Les lignes dégradées par dépassement de budget, vu par ce lexeur. */
    private int incidents;

    GrammaireLexeur(Grammaire grammaire) {
        this(grammaire, BUDGET_ETAPES_DEFAUT);
    }

    /**
     * Un lexeur à budget donné — le banc de conformité et les tests du
     * bornage s'en servent.
     *
     * @param grammaire    la grammaire du langage
     * @param budgetEtapes le nombre maximal d'essais d'expression par ligne
     */
    GrammaireLexeur(Grammaire grammaire, int budgetEtapes) {
        this.grammaire = grammaire;
        this.budgetEtapes = budgetEtapes;
    }

    /** Les lignes dégradées par dépassement de budget depuis ce lexeur. */
    int incidents() {
        return incidents;
    }

    @Override
    public LexState initialState() {
        return EtatGrammaire.VIDE;
    }

    @Override
    public LexedLine lex(CharSequence ligne, LexState entree) {
        EtatGrammaire etat = (EtatGrammaire) entree;
        Utf8Line donnees = Utf8Line.preparer(ligne);
        etapes = 0;
        deborde = false;
        List<Token> jetons = new ArrayList<>();
        int n = donnees.octets().length;
        int pos = 0;
        while (pos < n) {
            Candidat candidat = premier(donnees, pos, etat);
            if (candidat == null) {
                ecart(donnees, jetons, pos, n, etat);
                return figer(jetons, etat);
            }
            ecart(donnees, jetons, pos, candidat.debut(), etat);
            etat = emettre(jetons, donnees, candidat, etat);
            pos = candidat.fin();
        }
        return figer(jetons, etat);
    }

    /**
     * Le candidat le plus à gauche à partir de la position : la
     * fermeture du bloc ouvert d'abord, les règles du contexte ensuite —
     * chaque liste référencée d'un contexte ne se considère qu'une fois.
     * Rend null quand plus rien ne matche.
     */
    private @Nullable Candidat premier(Utf8Line donnees, int pos, EtatGrammaire etat) {
        Set<List<Regle>> vus = Collections.newSetFromMap(new IdentityHashMap<>());
        Candidat meilleur = null;
        BlocOuvert ouvert = etat.bloc();
        if (ouvert != null) {
            meilleur = chercher(donnees, pos, ouvert.fin(), Candidat.FIN,
                    null, ouvert, null);
        }
        return parcourir(contexte(etat), donnees, pos, meilleur, vus);
    }

    /** Les règles applicables : celles du bloc ouvert, ou celles de la grammaire. */
    private List<Regle> contexte(EtatGrammaire etat) {
        BlocOuvert ouvert = etat.bloc();
        return ouvert == null ? grammaire.motifs() : ouvert.regle().motifs();
    }

    /** Parcourt les règles — les références se suivent, chaque liste une fois. */
    private @Nullable Candidat parcourir(List<Regle> regles, Utf8Line donnees, int pos,
            @Nullable Candidat meilleur, Set<List<Regle>> vus) {
        for (Regle regle : regles) {
            if (regle instanceof RegleListe liste) {
                if (vus.add(liste.regles())) {
                    meilleur = parcourir(liste.regles(), donnees, pos, meilleur, vus);
                }
            } else if (regle instanceof RegleSimple simple) {
                meilleur = garder(meilleur, chercher(donnees, pos, simple.motif(),
                        Candidat.MOTIF, null, null, simple));
            } else if (regle instanceof RegleBloc bloc) {
                meilleur = garder(meilleur, chercher(donnees, pos, bloc.debut(),
                        Candidat.DEBUT, bloc, null, null));
            }
        }
        return meilleur;
    }

    /** Cherche la plus ancienne occurrence d'une expression à partir de la position. */
    private @Nullable Candidat chercher(Utf8Line donnees, int pos, Regex motif, char type,
            @Nullable RegleBloc bloc, @Nullable BlocOuvert ferme, @Nullable RegleSimple simple) {
        if (etapes++ >= budgetEtapes) {
            deborder(donnees);
            return null;
        }
        Matcher m = motif.matcher(donnees.octets());
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
                    donnees.texte().length());
        }
    }

    /** Le candidat strictement plus à gauche gagne ; à égalité, le premier trouvé. */
    private static @Nullable Candidat garder(@Nullable Candidat meilleur,
            @Nullable Candidat candidat) {
        if (candidat == null) {
            return meilleur;
        }
        if (meilleur == null || candidat.debut() < meilleur.debut()) {
            return candidat;
        }
        return meilleur;
    }

    /** Émet le jeton du candidat — motif, ouverture ou fermeture — et suit l'état. */
    private EtatGrammaire emettre(List<Token> jetons, Utf8Line donnees, Candidat candidat,
            EtatGrammaire etat) {
        if (candidat.type() == Candidat.FIN && candidat.ferme() != null) {
            emettreCaptures(jetons, donnees, candidat.matcher(),
                    candidat.ferme().regle().capturesFin(),
                    candidat.ferme().regle().portee(), etat);
            return etat.depiler();
        }
        if (candidat.type() == Candidat.DEBUT && candidat.bloc() != null) {
            RegleBloc bloc = candidat.bloc();
            emettreCaptures(jetons, donnees, candidat.matcher(), bloc.capturesDebut(),
                    bloc.portee(), etat);
            BlocOuvert ouvert = bloc.referencesFin()
                    ? BlocOuvert.ouvrir(bloc, candidat.matcher(), donnees)
                    : BlocOuvert.ouvrir(bloc);
            return etat.empiler(ouvert);
        }
        RegleSimple simple = candidat.simple();
        emettreCaptures(jetons, donnees, candidat.matcher(), simple == null ? null
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
    private void emettreCaptures(List<Token> jetons, Utf8Line donnees, Matcher m,
            @Nullable Map<Integer, Capture> captures, @Nullable String porteeBase,
            EtatGrammaire etat) {
        String base = porteeBase != null ? porteeBase : contextePortee(etat);
        int pileContexte = pileContexte(etat);
        if (captures == null) {
            jetons.add(jeton(pileContexte, base, donnees, m.getBegin(),
                    m.getEnd()));
            return;
        }
        Region groupes = m.getEagerRegion();
        int curseur = m.getBegin();
        for (Map.Entry<Integer, Capture> entree : captures.entrySet()) {
            int numero = entree.getKey();
            if (numero >= groupes.getNumRegs()) {
                continue;
            }
            int gd = groupes.getBeg(numero);
            int gf = groupes.getEnd(numero);
            if (gd < 0 || gf <= gd) {
                continue;
            }
            if (gd > curseur) {
                jetons.add(jeton(pileContexte, base, donnees, curseur, gd));
            }
            Capture capture = entree.getValue();
            jetons.add(jeton(pileContexte,
                    capture.nom() != null ? capture.nom() : base,
                    donnees, gd, gf));
            curseur = gf;
        }
        if (m.getEnd() > curseur) {
            jetons.add(jeton(pileContexte, base, donnees, curseur, m.getEnd()));
        }
    }

    /**
     * L'écart non couvert : les blancs seuls ne produisent rien, le
     * reste porte la pile de son contexte — le contenu du bloc le
     * plus profond qui porte une portée, la construction sinon, la
     * racine au plancher — rogné de ses blancs d'escorte.
     */
    private void ecart(Utf8Line donnees, List<Token> jetons, int debutOctet, int finOctet,
            EtatGrammaire etat) {
        CharSequence ligne = donnees.texte();
        int debut = donnees.versChar(debutOctet);
        int fin = donnees.versChar(finOctet);
        int premier = premierNonBlanc(ligne, debut, fin);
        if (premier < 0) {
            return;
        }
        int dernier = dernierNonBlanc(ligne, debut, fin);
        int pile = pileContexte(etat);
        jetons.add(new Token(ScopeStacks.kindOf(pile), premier, dernier + 1,
                pile));
    }

    /**
     * La pile de portées du contexte (H-07) : la racine de la
     * grammaire, puis chaque bloc ouvert de l'extérieur vers
     * l'intérieur — sa portée de contenu, sinon sa portée de
     * construction — internée une fois, partagée par tous les jetons
     * qui la portent.
     */
    private int pileContexte(EtatGrammaire etat) {
        List<String> portees = new ArrayList<>();
        for (EtatGrammaire cadre = etat; cadre != null
                && !EtatGrammaire.VIDE.equals(cadre); cadre = cadre.parent()) {
            BlocOuvert ouvert = cadre.bloc();
            if (ouvert == null) {
                break;
            }
            RegleBloc regle = ouvert.regle();
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
    private String contextePortee(EtatGrammaire etat) {
        for (EtatGrammaire cadre = etat; cadre != null; cadre = cadre.parent()) {
            BlocOuvert ouvert = cadre.bloc();
            if (ouvert == null) {
                break;
            }
            RegleBloc regle = ouvert.regle();
            if (regle.contenu() != null) {
                return regle.contenu();
            }
            if (regle.portee() != null) {
                return regle.portee();
            }
        }
        return grammaire.porteeRacine();
    }

    private static LexedLine figer(List<Token> jetons, EtatGrammaire etat) {
        return new LexedLine(jetons.toArray(new Token[0]), etat);
    }

    private static Token jeton(int pileContexte, String portee, Utf8Line donnees,
            int debutOctet, int finOctet) {
        int pile = ScopeStacks.push(pileContexte, portee);
        return new Token(ScopeStacks.kindOf(pile), donnees.versChar(debutOctet),
                donnees.versChar(finOctet), pile);
    }

    private static int premierNonBlanc(CharSequence ligne, int debut, int fin) {
        for (int i = debut; i < fin; i++) {
            if (!Character.isWhitespace(ligne.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int dernierNonBlanc(CharSequence ligne, int debut, int fin) {
        for (int i = fin - 1; i >= debut; i--) {
            if (!Character.isWhitespace(ligne.charAt(i))) {
                return i;
            }
        }
        return debut;
    }

    /** Un candidat de lexicographie : bornes, correspondance et nature. */
    private record Candidat(int debut, int fin, Matcher matcher, char type,
            @Nullable RegleBloc bloc, @Nullable BlocOuvert ferme,
            @Nullable RegleSimple simple) {

        /** Un motif simple. */
        private static final char MOTIF = 'm';

        /** L'ouverture d'un bloc. */
        private static final char DEBUT = 'd';

        /** La fermeture du bloc ouvert. */
        private static final char FIN = 'f';
    }
}
