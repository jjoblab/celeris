package jo.celeris.android.view.internal.model;

import jo.celeris.core.text.Document;

/**
 * L'appariement des accolades et parenthèses (D-23, la reprise du
 * propriétaire) : la paire qui encadre le curseur se cherche par un
 * balayage de profondeur naïf, BORNÉ — une parenthèse non appariée
 * dans un très grand fichier ne coûte jamais O(N) par frappe.
 *
 * <p>La sonde presse d'abord la borne AVANT le curseur (le curseur
 * juste après une fermante surligne son ouvrante), puis la borne SUR
 * le curseur (le curseur posé sur une ouvrante surligne sa fermante).
 * Le balayage ignore chaînes et commentaires — suffisant pour un
 * surlignage, la voie de structure reste à l'arbre.</p>
 *
 * <p>Pur et sans SDK : la couche de rendu le consulte à chaque image,
 * les bancs le prouvent sans appareil. Le document porte l'accès
 * direct aux caractères ({@code charAt} en O(1) amorti).</p>
 */
public final class BracePair {

    /**
     * Le plafond du balayage, en caractères : au-delà, l'appariement
     * renonce — une parenthèse orpheline au milieu d'un très grand
     * fichier ne ralentit jamais l'image.
     */
    public static final int CEILING = 50_000;

    private BracePair() {
        // Utilitaire statique — pas d'instance.
    }

    /**
     * La paire qui encadre le curseur, écrite dans le tableau donné :
     * la borne ouvrante puis la fermante.
     *
     * @param text  le document porteur
     * @param caret  la position du curseur, en offset
     * @param pair  le réceptacle des deux bornes, longueur ≥ 2 —
     *               réutilisé d'une image à l'autre (PIEGE-15)
     * @return vrai quand la paire s'est trouvée, faux sinon (le
     *         réceptacle reste alors muet)
     */
    public static boolean appariee(Document text, int caret, int[] pair) {
        for (int sonde = caret - 1; sonde <= caret; sonde++) {
            if (sonde < 0 || sonde >= text.length()) {
                continue;
            }
            char caractere = text.charAt(sonde);
            char fermante = fermanteDe(caractere);
            if (fermante != 0) {
                int profondeur = 0;
                int limite = Math.min(text.length(), sonde + CEILING);
                for (int i = sonde; i < limite; i++) {
                    char c = text.charAt(i);
                    if (c == caractere) {
                        profondeur++;
                    } else if (c == fermante) {
                        profondeur--;
                        if (profondeur == 0) {
                            pair[0] = sonde;
                            pair[1] = i;
                            return true;
                        }
                    }
                }
            } else {
                char ouvrante = ouvranteDe(caractere);
                if (ouvrante == 0) {
                    continue;
                }
                int profondeur = 0;
                int limite = Math.max(0, sonde - CEILING);
                for (int i = sonde; i >= limite; i--) {
                    char c = text.charAt(i);
                    if (c == caractere) {
                        profondeur++;
                    } else if (c == ouvrante) {
                        profondeur--;
                        if (profondeur == 0) {
                            pair[0] = i;
                            pair[1] = sonde;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** La fermante d'une ouvrante, 0 si le caractère n'en est pas une. */
    private static char fermanteDe(char c) {
        switch (c) {
            case '(': return ')';
            case '[': return ']';
            case '{': return '}';
            default: return 0;
        }
    }

    /** L'ouvrante d'une fermante, 0 si le caractère n'en est pas une. */
    private static char ouvranteDe(char c) {
        switch (c) {
            case ')': return '(';
            case ']': return '[';
            case '}': return '{';
            default: return 0;
        }
    }
}
