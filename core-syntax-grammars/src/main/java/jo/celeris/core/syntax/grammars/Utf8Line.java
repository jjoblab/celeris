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
 */
final class Utf8Line {

    private final CharSequence texte;
    private final byte[] octets;
    private final int @Nullable [] octetVersChar;

    private Utf8Line(CharSequence texte, byte[] octets, int @Nullable [] octetVersChar) {
        this.texte = texte;
        this.octets = octets;
        this.octetVersChar = octetVersChar;
    }

    /** La ligne préparée : encode en UTF-8 et dresse la table si besoin. */
    static Utf8Line preparer(CharSequence ligne) {
        int n = ligne.length();
        byte[] octets = new byte[longueurUtf8(ligne)];
        int[] table = new int[octets.length + 1];
        boolean ascii = true;
        int curseur = 0;
        for (int i = 0; i < n; ) {
            int code = substitution(Character.codePointAt(ligne, i));
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
        table[octets.length] = n;
        return new Utf8Line(ligne, octets, ascii ? null : table);
    }

    /** Le texte de la ligne, en UTF-16. */
    CharSequence texte() {
        return texte;
    }

    /** Les octets UTF-8 de la ligne. */
    byte[] octets() {
        return octets;
    }

    /** La position UTF-16 du caractère qui commence à la frontière d'octet. */
    int versChar(int octet) {
        int[] table = octetVersChar;
        return table == null ? octet : table[octet];
    }

    /** Le nombre d'octets de l'encodage UTF-8 de la ligne. */
    private static int longueurUtf8(CharSequence ligne) {
        int total = 0;
        for (int i = 0; i < ligne.length(); ) {
            int code = substitution(Character.codePointAt(ligne, i));
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
