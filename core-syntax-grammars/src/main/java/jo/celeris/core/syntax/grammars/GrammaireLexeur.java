package jo.celeris.core.syntax.grammars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;

import org.jspecify.annotations.Nullable;

/**
 * Lexeur d'une grammaire (ADR-004) : lexicographie d'UNE ligne à la
 * fois, pilotée par la pile des blocs ouverts transportée d'une ligne à
 * la suivante (PIEGE-21). À chaque position, le candidat le plus à
 * gauche gagne — les ex æquo se départagent par l'ordre des règles, la
 * fermeture du bloc ouvert prime — les motifs ancrés {@code ^} ne
 * matched qu'en tête de ligne. Les écarts non couverts portent la
 * portée de contenu du bloc ouvert, identificateur hors de tout bloc ;
 * les blancs seuls ne produisent jamais de jeton.
 */
final class GrammaireLexeur implements LanguageLexer {

    private final Grammaire grammaire;

    GrammaireLexeur(Grammaire grammaire) {
        this.grammaire = grammaire;
    }

    @Override
    public LexState initialState() {
        return EtatGrammaire.VIDE;
    }

    @Override
    public LexedLine lex(CharSequence ligne, LexState entree) {
        EtatGrammaire etat = (EtatGrammaire) entree;
        List<Token> jetons = new ArrayList<>();
        int n = ligne.length();
        int pos = 0;
        while (pos < n) {
            Candidat candidat = premier(ligne, pos, etat);
            if (candidat == null) {
                ecart(ligne, jetons, pos, n, etat);
                return new LexedLine(jetons.toArray(new Token[0]), etat);
            }
            ecart(ligne, jetons, pos, candidat.debut(), etat);
            etat = emettre(jetons, candidat, etat);
            pos = candidat.fin();
        }
        return new LexedLine(jetons.toArray(new Token[0]), etat);
    }

    /**
     * Le candidat le plus à gauche à partir de la position : la
     * fermeture du bloc ouvert d'abord, les règles du contexte ensuite —
     * chaque liste référencée d'un contexte ne se considère qu'une fois.
     * Rend null quand plus rien ne matched.
     */
    private @Nullable Candidat premier(CharSequence ligne, int pos, EtatGrammaire etat) {
        Set<List<Regle>> vus = Collections.newSetFromMap(new IdentityHashMap<>());
        Candidat meilleur = null;
        RegleBloc bloc = etat.bloc();
        if (bloc != null) {
            meilleur = chercher(ligne, pos, bloc.fin(), Candidat.FIN, bloc, null);
        }
        return parcourir(contexte(etat), ligne, pos, meilleur, vus);
    }

    /** Les règles applicables : celles du bloc ouvert, ou celles de la grammaire. */
    private List<Regle> contexte(EtatGrammaire etat) {
        RegleBloc bloc = etat.bloc();
        return bloc == null ? grammaire.motifs() : bloc.motifs();
    }

    /** Parcourt les règles — les références se suivent, chaque liste une fois. */
    private @Nullable Candidat parcourir(List<Regle> regles, CharSequence ligne, int pos,
            @Nullable Candidat meilleur, Set<List<Regle>> vus) {
        for (Regle regle : regles) {
            if (regle instanceof RegleListe liste) {
                if (vus.add(liste.regles())) {
                    meilleur = parcourir(liste.regles(), ligne, pos, meilleur, vus);
                }
            } else if (regle instanceof RegleSimple simple) {
                meilleur = garder(meilleur, chercher(ligne, pos, simple.motif(),
                        Candidat.MOTIF, null, simple));
            } else if (regle instanceof RegleBloc bloc) {
                meilleur = garder(meilleur, chercher(ligne, pos, bloc.debut(),
                        Candidat.DEBUT, bloc, null));
            }
        }
        return meilleur;
    }

    /** Cherche la plus ancienne occurrence d'une expression à partir de la position. */
    private @Nullable Candidat chercher(CharSequence ligne, int pos, Pattern motif, char type,
            @Nullable RegleBloc bloc, @Nullable RegleSimple simple) {
        Matcher m = motif.matcher(ligne);
        if (!m.find(pos) || m.end() == m.start()) {
            return null;
        }
        return new Candidat(m.start(), m.end(), m, type, bloc, simple);
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
    private EtatGrammaire emettre(List<Token> jetons, Candidat candidat,
            EtatGrammaire etat) {
        if (candidat.type() == Candidat.DEBUT && candidat.bloc() != null) {
            jetons.add(new Token(Portees.de(candidat.bloc().porteeDebut()),
                    candidat.debut(), candidat.fin()));
            return etat.empiler(candidat.bloc());
        }
        if (candidat.type() == Candidat.FIN && candidat.bloc() != null) {
            jetons.add(new Token(Portees.de(candidat.bloc().porteeFin()),
                    candidat.debut(), candidat.fin()));
            return etat.depiler();
        }
        emettreMotif(jetons, candidat);
        return etat;
    }

    /**
     * Émet un motif simple : sans capture, un jeton couvert par la
     * portée de la règle ; avec captures, chaque groupe capturé porte
     * sa portée et l'intervalle non couvert porte celle de la règle.
     */
    private void emettreMotif(List<Token> jetons, Candidat candidat) {
        RegleSimple simple = candidat.simple();
        String portee = simple == null ? "meta.rule" : simple.portee();
        Map<Integer, String> captures = simple == null ? null : simple.captures();
        if (captures == null) {
            jetons.add(new Token(Portees.de(portee), candidat.debut(), candidat.fin()));
            return;
        }
        int curseur = candidat.debut();
        for (Map.Entry<Integer, String> capture : captures.entrySet()) {
            int gd = candidat.matcher().start(capture.getKey());
            int gf = candidat.matcher().end(capture.getKey());
            if (gd < 0 || gf <= gd) {
                continue;
            }
            if (gd > curseur) {
                jetons.add(new Token(Portees.de(portee), curseur, gd));
            }
            jetons.add(new Token(Portees.de(capture.getValue()), gd, gf));
            curseur = gf;
        }
        if (candidat.fin() > curseur) {
            jetons.add(new Token(Portees.de(portee), curseur, candidat.fin()));
        }
    }

    /**
     * L'écart non couvert : les blancs seuls ne produisent rien, le
     * reste porte la portée de contenu du bloc ouvert — identificateur
     * hors de tout bloc — rogné de ses blancs d'escorte.
     */
    private void ecart(CharSequence ligne, List<Token> jetons, int debut, int fin,
            EtatGrammaire etat) {
        int premier = premierNonBlanc(ligne, debut, fin);
        if (premier < 0) {
            return;
        }
        int dernier = dernierNonBlanc(ligne, debut, fin);
        jetons.add(new Token(Portees.de(contextePortee(etat)), premier, dernier + 1));
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

    /** La portée de contenu du bloc ouvert, meta.plain hors de tout bloc. */
    private static String contextePortee(EtatGrammaire etat) {
        RegleBloc bloc = etat.bloc();
        return bloc == null || bloc.porteeContenu() == null
                ? "meta.plain" : bloc.porteeContenu();
    }

    /** Un candidat de lexicographie : bornes, correspondance et nature. */
    private record Candidat(int debut, int fin, Matcher matcher, char type,
            @Nullable RegleBloc bloc, @Nullable RegleSimple simple) {

        /** Un motif simple. */
        private static final char MOTIF = 'm';

        /** L'ouverture d'un bloc. */
        private static final char DEBUT = 'd';

        /** La fermeture du bloc ouvert. */
        private static final char FIN = 'f';
    }
}
