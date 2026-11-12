package jo.celeris.android.view;

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
final class PaireAccolades {

    /**
     * Le plafond du balayage, en caractères : au-delà, l'appariement
     * renonce — une parenthèse orpheline au milieu d'un très grand
     * fichier ne ralentit jamais l'image.
     */
    static final int PLAFOND = 50_000;

    private PaireAccolades() {
        // Utilitaire statique — pas d'instance.
    }

    /**
     * La paire qui encadre le curseur, écrite dans le tableau donné :
     * la borne ouvrante puis la fermante.
     *
     * @param texte  le document porteur
     * @param caret  la position du curseur, en offset
     * @param paire  le réceptacle des deux bornes, longueur ≥ 2 —
     *               réutilisé d'une image à l'autre (PIEGE-15)
     * @return vrai quand la paire s'est trouvée, faux sinon (le
     *         réceptacle reste alors muet)
     */
    static boolean appariee(Document texte, int caret, int[] paire) {
        for (int sonde = caret - 1; sonde <= caret; sonde++) {
            if (sonde < 0 || sonde >= texte.length()) {
                continue;
            }
            char caractere = texte.charAt(sonde);
            char fermante = fermanteDe(caractere);
            if (fermante != 0) {
                int profondeur = 0;
                int limite = Math.min(texte.length(), sonde + PLAFOND);
                for (int i = sonde; i < limite; i++) {
                    char c = texte.charAt(i);
                    if (c == caractere) {
                        profondeur++;
                    } else if (c == fermante) {
                        profondeur--;
                        if (profondeur == 0) {
                            paire[0] = sonde;
                            paire[1] = i;
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
                int limite = Math.max(0, sonde - PLAFOND);
                for (int i = sonde; i >= limite; i--) {
                    char c = texte.charAt(i);
                    if (c == caractere) {
                        profondeur++;
                    } else if (c == ouvrante) {
                        profondeur--;
                        if (profondeur == 0) {
                            paire[0] = i;
                            paire[1] = sonde;
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
