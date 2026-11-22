package jo.celeris.core.syntax.grammars;

import org.jspecify.annotations.Nullable;

/**
 * Une ligne transcodée en octets UTF-8 pour joni (ADR-014) : le moteur
 * d'expressions travaille sur des octets, le socle sur des unités de
 * code UTF-16 (ADR-003) — la ligne porte donc sa table de
 * correspondance. Les lignes ASCII (l'écrasante majorité du code) n'en
 * ont pas besoin : la position d'un octet EST celle du caractère,
 * aucune table n'est allouée. Les positions rendues par joni tombent
 * toujours sur des frontières de caractères : la table donne alors
 * l'indice du caractère qui commence à cette frontière.
 *
 * <p>La fin de ligne virtuelle (chantier C3, prompt du 2026-11-22) :
 * le buffer d'octets porte un {@code \n} APRÈS la ligne — vingt
 * grammaires livrées ont des motifs qui l'exigent (fermetures
 * {@code \n?}, guards shebang) ; la correspondance qui le touche se
 * ROGNE à la ligne au retour vers les unités de code (le modèle tm4e :
 * {@code lineText += '\n'} sans jamais dépasser lineLength).</p>
 */
final class Utf8Line {

    private final CharSequence text;
    private final byte[] octets;
    private final int @Nullable [] octetVersChar;

    private Utf8Line(CharSequence text, byte[] octets, int @Nullable [] octetVersChar) {
        this.text = text;
        this.octets = octets;
        this.octetVersChar = octetVersChar;
    }

    /** La ligne préparée : encode en UTF-8 et dresse la table si
     *  besoin — le buffer porte un {@code \n} de fin virtuel. */
    static Utf8Line preparer(CharSequence line) {
        int n = line.length();
        int longueurTexte = longueurUtf8(line);
        byte[] octets = new byte[longueurTexte + 1];
        int[] table = new int[octets.length + 1];
        boolean ascii = true;
        int curseur = 0;
        for (int i = 0; i < n; ) {
            int code = substitution(Character.codePointAt(line, i));
            int largeur = Character.charCount(code);
            for (int b = 0; b < largeur; b++) {
                table[curseur + b] = i;
            }
            curseur = ecrire(code, octets, curseur);
            if (code >= 0x80) {
                ascii = false;
            }
            i += largeur;
        }
        // La fin virtuelle : le \n se pose APRÈS la ligne, la table y
        // mène au BOUT du texte (une correspondance qui l'inclut se
        // rogne d'elle-même au retour vers les caractères).
        octets[curseur] = (byte) '\n';
        table[curseur] = n;
        table[curseur + 1] = n;
        return new Utf8Line(line, octets, ascii ? null : table);
    }

    /** Le texte de la ligne, en UTF-16. */
    CharSequence text() {
        return text;
    }

    /** Les octets UTF-8 de la ligne. */
    byte[] octets() {
        return octets;
    }

    /** La position UTF-16 du caractère qui commence à la frontière
     *  d'octet — la fin virtuelle ({@code \n}) mène au BOUT de la
     *  ligne : une correspondance qui l'inclut se rogne d'elle-même. */
    int versChar(int aByte) {
        int[] table = octetVersChar;
        int borne = table == null ? aByte : table[Math.min(aByte, table.length - 1)];
        return Math.min(borne, text.length());
    }

    /** Le nombre d'octets de l'encodage UTF-8 de la ligne. */
    private static int longueurUtf8(CharSequence line) {
        int total = 0;
        for (int i = 0; i < line.length(); ) {
            int code = substitution(Character.codePointAt(line, i));
            total += code < 0x80 ? 1 : code < 0x800 ? 2 : code < 0x10000 ? 3 : 4;
            i += Character.charCount(code);
        }
        return total;
    }

    /** Le substitut isolé devient le point d'interrogation, comme l'encodeur Java. */
    private static int substitution(int code) {
        return code >= Character.MIN_SURROGATE && code <= Character.MAX_SURROGATE
                ? '?' : code;
    }

    /**
     * Écrit un point de code en UTF-8 (les substituts isolés sont déjà
     * substitués par l'appelant).
     *
     * @return la position d'écriture suivante
     */
    private static int ecrire(int code, byte[] octets, int curseur) {
        if (code < 0x80) {
            octets[curseur++] = (byte) code;
        } else if (code < 0x800) {
            octets[curseur++] = (byte) (0xC0 | (code >> 6));
            octets[curseur++] = (byte) (0x80 | (code & 0x3F));
        } else if (code < 0x10000) {
            octets[curseur++] = (byte) (0xE0 | (code >> 12));
            octets[curseur++] = (byte) (0x80 | ((code >> 6) & 0x3F));
            octets[curseur++] = (byte) (0x80 | (code & 0x3F));
        } else {
            octets[curseur++] = (byte) (0xF0 | (code >> 18));
            octets[curseur++] = (byte) (0x80 | ((code >> 12) & 0x3F));
            octets[curseur++] = (byte) (0x80 | ((code >> 6) & 0x3F));
            octets[curseur++] = (byte) (0x80 | (code & 0x3F));
        }
        return curseur;
    }
}
