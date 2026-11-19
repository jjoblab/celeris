package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Les caractères invisibles (M5, A-19) : la matérialisation des blancs
 * du rendu — l'espace finale toujours (elle trahit les oublis), la
 * tabulation toujours (elle décale la colonne), l'espace fine —
 * insécable ou étroite — toujours (elle se cache dans le texte), l'
 * espace interne en mode « tout », la fin de ligne sur demande. Pur et
 * testable sans SDK : le peintre dessine les marques.
 */
public final class InvisibleChars {

    /** La sorte de blanc matérialisé. */
    public enum Kind {

        /** Espace ordinaire (finale, ou interne en mode « tout »). */
        ESPACE,

        /** Espace fine : insécable, étroite, insécable étroite. */
        ESPACE_FINE,

        /** Tabulation. */
        TABULATION,

        /** Fin de ligne. */
        FIN_DE_LIGNE
    }

    /** Une marque : la colonne du blanc et sa sorte. */
    public record Mark(int column, Kind sorte) {
    }

    private InvisibleChars() {
    }

    /**
     * Les blancs à matérialiser sur une ligne.
     *
     * @param line          le texte de la ligne, sans le saut final
     * @param espacesInternes vrai pour matérialiser aussi les espaces internes
     * @param finsDeLigne     vrai pour matérialiser la fin de ligne
     * @return les marques, dans l'ordre des colonnes
     */
    public static List<Mark> marks(String line, boolean espacesInternes,
            boolean finsDeLigne) {
        int finBlancs = line.length();
        while (finBlancs > 0 && estBlancLarge(line.charAt(finBlancs - 1))) {
            finBlancs--;
        }
        List<Mark> marks = new ArrayList<>(line.length() / 4 + 1);
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                marks.add(new Mark(i, Kind.TABULATION));
            } else if (estFine(c)) {
                marks.add(new Mark(i, Kind.ESPACE_FINE));
            } else if (c == ' ') {
                boolean finale = i >= finBlancs;
                if (finale || espacesInternes) {
                    marks.add(new Mark(i, Kind.ESPACE));
                }
            }
        }
        if (finsDeLigne) {
            marks.add(new Mark(line.length(), Kind.FIN_DE_LIGNE));
        }
        return marks;
    }

    private static boolean estBlancLarge(char c) {
        return c == ' ' || c == '\t';
    }

    private static boolean estFine(char c) {
        return c == '\u00a0' || c == '\u202f' || c == '\u2009';
    }
}
