package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.view.IndiceInlay;
import jo.celeris.core.text.Document;

/**
 * Le moteur des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14 : le propriétaire — « aussi la fonctionnalité de inlay
 * hint ») : le TYPE d'un « var » à main-d'œuvre LITTÉRALE se lit en
 * fin de ligne utile — la convention du simulateur de référence
 * (« var resultat = solde; » porte « : int », jamais dans le flux).
 *
 * <p>Pur, sans appareil : la syntaxe seule décide, JAMAIS le moteur ne
 * devine — un « var » à main-d'œuvre inconnue (identifiant, appel,
 * {@code null}) ne porte AUCUN indice (l'IDE de référence ne devine
 * pas non plus sans sémantique). Les littéraux seuls y tranchent :
 * entier, décimal, suffixes L/f/d, booléens, chaînes, caractères. Le
 * jour où le serveur sert {@code inlayHint}, l'hôte y branche sa
 * réponse — le rendu de la vue reste le même.</p>
 */
final class MoteurInlayHints {

    private MoteurInlayHints() {
    }

    /**
     * Les indices du document : un par « var » littéral — le rendu se
     * lit en fin de ligne utile, à l'encre muette italique.
     *
     * @param document le document ouvert
     * @return les indices par ligne, numérotée à partir de un
     */
    static List<IndiceInlay> indices(Document document) {
        List<IndiceInlay> indices = new ArrayList<>();
        for (int ligne = 0; ligne < document.lineCount(); ligne++) {
            String type = typeDuVar(document.lineText(ligne));
            if (type != null) {
                indices.add(new IndiceInlay(ligne + 1, type));
            }
        }
        return List.copyOf(indices);
    }

    /**
     * Le rendu du type d'une ligne de « var » — null si la ligne n'en
     * déclare pas un ou si la main-d'œuvre ne se tranchera pas.
     */
    private static String typeDuVar(String ligneBrute) {
        String ligne = ligneBrute.strip();
        if (ligne.length() < 5 || !ligne.startsWith("var")
                || !Character.isWhitespace(ligne.charAt(3))) {
            return null;
        }
        String corps = ligne.substring(4).stripLeading();
        int egale = corps.indexOf('=');
        if (egale <= 0) {
            return null;
        }
        String nom = corps.substring(0, egale).stripTrailing();
        if (!estIdentifiant(nom)) {
            return null;
        }
        String valeur = corps.substring(egale + 1).strip();
        if (valeur.endsWith(";")) {
            valeur = valeur.substring(0, valeur.length() - 1).stripTrailing();
        }
        if (valeur.isEmpty()) {
            return null;
        }
        return typeDuLitteral(valeur);
    }

    /** Vrai si le texte est un identifiant Java bien formé. */
    private static boolean estIdentifiant(String texte) {
        if (texte.isEmpty()
                || !Character.isJavaIdentifierStart(texte.charAt(0))) {
            return false;
        }
        for (int i = 1; i < texte.length(); i++) {
            if (!Character.isJavaIdentifierPart(texte.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Le type du LITTÉRAL — null quand il ne se tranchera pas. */
    private static String typeDuLitteral(String valeur) {
        if (valeur.equals("true") || valeur.equals("false")) {
            return ": boolean";
        }
        if (valeur.equals("null")) {
            // l'IDE de référence ne devine pas le néant
            return null;
        }
        if (estChaine(valeur)) {
            return ": String";
        }
        if (estCaractere(valeur)) {
            return ": char";
        }
        return typeNumerique(valeur);
    }

    /** Vrai si la valeur est une chaîne entre guillemets. */
    private static boolean estChaine(String valeur) {
        return valeur.length() >= 2 && valeur.startsWith("\"")
                && valeur.endsWith("\"");
    }

    /** Vrai si la valeur est un caractère entre quotes. */
    private static boolean estCaractere(String valeur) {
        return valeur.length() >= 3 && valeur.startsWith("'")
                && valeur.endsWith("'");
    }

    /**
     * Le type du littéral NUMÉRIQUE — int, long, float, double selon
     * la forme et le suffixe ; null quand la valeur n'est pas littérale
     * (identifiant, appel, hexadécimal — jamais deviné).
     */
    private static String typeNumerique(String valeur) {
        char dernier = valeur.charAt(valeur.length() - 1);
        String sansSuffix = valeur;
        if ("fFdDlL".indexOf(dernier) >= 0) {
            sansSuffix = valeur.substring(0, valeur.length() - 1);
        }
        boolean entier = sansSuffix.matches("\\d+");
        boolean decimal = sansSuffix.matches("\\d*\\.\\d+");
        if (!entier && !decimal) {
            // identifiant, appel, expression — jamais deviné
            return null;
        }
        if (decimal) {
            return (dernier == 'f' || dernier == 'F') ? ": float"
                    : ": double";
        }
        if (dernier == 'f' || dernier == 'F') {
            return ": float";
        }
        if (dernier == 'd' || dernier == 'D') {
            return ": double";
        }
        if (dernier == 'l' || dernier == 'L') {
            return ": long";
        }
        return ": int";
    }
}
