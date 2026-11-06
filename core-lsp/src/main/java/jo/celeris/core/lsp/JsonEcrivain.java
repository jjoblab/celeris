package jo.celeris.core.lsp;

/**
 * Le sérialiseur JSON : forme compacte — ni blanc ni saut de ligne —
 * les guillemets, revers et contrôles échappés, l'Unicode passé tel
 * quel (les trames sont encodées UTF-8 par le cadrage).
 */
public final class JsonEcrivain {

    private JsonEcrivain() {
    }

    /**
     * Écrit la valeur en forme compacte.
     *
     * @param valeur la valeur à écrire
     * @return le JSON compact
     */
    public static String ecrire(ValeurJson valeur) {
        StringBuilder texte = new StringBuilder();
        ecrireValeur(valeur, texte);
        return texte.toString();
    }

    private static void ecrireValeur(ValeurJson valeur, StringBuilder texte) {
        if (valeur instanceof JsonObjet objet) {
            ecrireObjet(objet, texte);
        } else if (valeur instanceof JsonTableau tableau) {
            ecrireTableau(tableau, texte);
        } else if (valeur instanceof JsonChaine chaine) {
            ecrireChaine(chaine.texte(), texte);
        } else if (valeur instanceof JsonNombre nombre) {
            ecrireNombre(nombre, texte);
        } else if (valeur instanceof JsonBooleen booleen) {
            texte.append(booleen.valeur());
        } else {
            texte.append("null");
        }
    }

    private static void ecrireObjet(JsonObjet objet, StringBuilder texte) {
        texte.append('{');
        boolean premier = true;
        for (String cle : objet.cles()) {
            if (!premier) {
                texte.append(',');
            }
            premier = false;
            ecrireChaine(cle, texte);
            texte.append(':');
            ecrireValeur(objet.membreOu(cle, JsonNul.nul()), texte);
        }
        texte.append('}');
    }

    private static void ecrireTableau(JsonTableau tableau, StringBuilder texte) {
        texte.append('[');
        for (int i = 0; i < tableau.taille(); i++) {
            if (i > 0) {
                texte.append(',');
            }
            ecrireValeur(tableau.element(i), texte);
        }
        texte.append(']');
    }

    private static void ecrireChaine(String texte, StringBuilder sortie) {
        sortie.append('"');
        for (int i = 0; i < texte.length(); i++) {
            char courrant = texte.charAt(i);
            switch (courrant) {
                case '"' -> sortie.append("\\\"");
                case '\\' -> sortie.append("\\\\");
                case '\b' -> sortie.append("\\b");
                case '\f' -> sortie.append("\\f");
                case '\n' -> sortie.append("\\n");
                case '\r' -> sortie.append("\\r");
                case '\t' -> sortie.append("\\t");
                default -> {
                    if (courrant < 0x20) {
                        sortie.append(String.format("\\u%04x", (int) courrant));
                    } else {
                        sortie.append(courrant);
                    }
                }
            }
        }
        sortie.append('"');
    }

    private static void ecrireNombre(JsonNombre nombre, StringBuilder texte) {
        if (nombre.estEntier()) {
            texte.append(nombre.entier());
        } else {
            texte.append(nombre.decimal());
        }
    }
}
