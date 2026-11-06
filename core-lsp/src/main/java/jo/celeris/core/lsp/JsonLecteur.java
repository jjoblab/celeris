package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L'analyseur JSON (RFC 8259) : strict — le moindre reste après la
 * valeur, la moindre structure ouverte est refusé. Les clés en doublon
 * voient la dernière écriture l'emporter, comme le veut l'usage du
 * protocole. Les structures vivent ici, les chaînes et les nombres
 * chez leurs lecteurs dédiés.
 */
public final class JsonLecteur {

    private JsonLecteur() {
    }

    /**
     * Lit la première valeur du texte — et exige que rien d'autre ne
     * suive que des blancs.
     *
     * @param texte le texte à analyser
     * @return la valeur lue
     * @throws IllegalArgumentException si le texte n'est pas un JSON valide
     */
    public static ValeurJson lire(String texte) {
        CurseurSource curseur = new CurseurSource(texte);
        curseur.passerBlancs();
        if (curseur.aFini()) {
            throw new IllegalArgumentException("empty JSON input");
        }
        ValeurJson valeur = lireValeur(curseur);
        curseur.passerBlancs();
        if (!curseur.aFini()) {
            throw new IllegalArgumentException("trailing content at " + curseur.position);
        }
        return valeur;
    }

    private static ValeurJson lireValeur(CurseurSource curseur) {
        char premier = curseur.courrant();
        return switch (premier) {
            case '{' -> lireObjet(curseur);
            case '[' -> lireTableau(curseur);
            case '"' -> LecteurChaine.lire(curseur);
            case 't' -> lireMot(curseur, "true", JsonBooleen.VRAI);
            case 'f' -> lireMot(curseur, "false", JsonBooleen.FAUX);
            case 'n' -> lireMot(curseur, "null", JsonNul.nul());
            default -> LecteurNombre.lire(curseur);
        };
    }

    private static ValeurJson lireMot(CurseurSource curseur, String mot, ValeurJson valeur) {
        if (!curseur.commencePar(mot)) {
            throw new IllegalArgumentException("invalid literal at " + curseur.position);
        }
        curseur.position += mot.length();
        return valeur;
    }

    private static ValeurJson lireObjet(CurseurSource curseur) {
        curseur.position++;
        Map<String, ValeurJson> membres = new LinkedHashMap<>();
        curseur.passerBlancs();
        if (!curseur.aFini() && curseur.courrant() == '}') {
            curseur.position++;
            return enObjet(membres);
        }
        while (true) {
            curseur.passerBlancs();
            curseur.exiger('"', "object key");
            String cle = LecteurChaine.lireTexte(curseur);
            curseur.passerBlancs();
            curseur.exiger(':', "colon");
            curseur.passerBlancs();
            membres.put(cle, lireValeur(curseur));
            if (!avalerSeparateur(curseur, '}')) {
                return enObjet(membres);
            }
        }
    }

    private static ValeurJson lireTableau(CurseurSource curseur) {
        curseur.position++;
        List<ValeurJson> elements = new ArrayList<>();
        curseur.passerBlancs();
        if (!curseur.aFini() && curseur.courrant() == ']') {
            curseur.position++;
            return enTableau(elements);
        }
        while (true) {
            curseur.passerBlancs();
            elements.add(lireValeur(curseur));
            if (!avalerSeparateur(curseur, ']')) {
                return enTableau(elements);
            }
        }
    }

    /**
     * Avale le séparateur après un membre : la fermeture termine la
     * structure (rend faux), la virgule continue (rend vrai), tout
     * autre caractère est refusé.
     */
    private static boolean avalerSeparateur(CurseurSource curseur, char fermeture) {
        curseur.passerBlancs();
        if (curseur.aFini()) {
            throw new IllegalArgumentException("unterminated structure");
        }
        char apres = curseur.courrant();
        curseur.position++;
        if (apres == fermeture) {
            return false;
        }
        if (apres == ',') {
            return true;
        }
        throw new IllegalArgumentException(
                "expected , or " + fermeture + " at " + (curseur.position - 1));
    }

    private static JsonObjet enObjet(Map<String, ValeurJson> membres) {
        JsonObjet objet = JsonObjet.vide();
        for (Map.Entry<String, ValeurJson> entree : membres.entrySet()) {
            objet = objet.ajouter(entree.getKey(), entree.getValue());
        }
        return objet;
    }

    private static JsonTableau enTableau(List<ValeurJson> elements) {
        JsonTableau tableau = JsonTableau.vide();
        for (ValeurJson element : elements) {
            tableau = tableau.ajouter(element);
        }
        return tableau;
    }
}
