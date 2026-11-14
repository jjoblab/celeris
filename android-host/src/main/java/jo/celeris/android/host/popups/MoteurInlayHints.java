package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.view.IndiceInlay;
import jo.celeris.core.text.Document;

/**
 * Le moteur des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14 : « aussi la fonctionnalité de inlay hint », complété
 * le même jour : « assure-toi qu'il soit complet comme Android
 * Studio ») : le TYPE d'un « var » à main-d'œuvre LITTÉRALE se lit en
 * fin de ligne utile, et le NOM d'un paramètre muet se lit devant son
 * argument aux appels des méthodes du fichier — la convention de
 * l'IDE de référence, qui trace les deux familles.
 *
 * <p>Pur, sans appareil : la syntaxe seule décide, JAMAIS le moteur ne
 * devine — un « var » à main-d'œuvre inconnue (identifiant, appel,
 * {@code null}) ne porte AUCUN indice, un argument littéral dont le
 * nom ne se résout pas n'en porte pas non plus. Les littéraux seuls y
 * tranchent : entier, décimal, suffixes L/f/d, booléens, chaînes,
 * caractères, tableaux. Les méthodes du fichier se collectent d'une
 * passe (le plan léger des signatures), leurs paramètres servent les
 * appels littéraux — le nom du paramètre se lit AVANT son argument,
 * aux parenthèses de l'appel. Le jour où le serveur sert
 * {@code inlayHint}, l'hôte y branche sa réponse — le rendu de la vue
 * reste le même.</p>
 */
final class MoteurInlayHints {

    private MoteurInlayHints() {
    }

    /**
     * Les indices du document : les types des « var » littéraux en fin
     * de ligne utile, les noms de paramètres muets devant leurs
     * arguments littéraux — l'ordre suit le document.
     *
     * @param document le document ouvert
     * @return les indices par ligne, numérotée à partir de un
     */
    static List<IndiceInlay> indices(Document document) {
        List<IndiceInlay> indices = new ArrayList<>();
        Map<String, List<String>> methodes = methodesDuFichier(document);
        for (int ligne = 0; ligne < document.lineCount(); ligne++) {
            String texte = document.lineText(ligne);
            String type = typeDuVar(texte);
            if (type != null) {
                indices.add(new IndiceInlay(ligne + 1,
                        texte.stripTrailing().length(), type));
            }
            indices.addAll(nomsParametres(methodes, ligne, texte));
        }
        return List.copyOf(indices);
    }

    /**
     * Le plan léger des méthodes du fichier : le nom de chaque méthode
     * y porte SES paramètres (les noms, dans l'ordre) — les appels
     * littéraux s'y résolvent. Pur et tolérant : la ligne de
     * signature qui ne se parse pas se saute, jamais le moteur ne
     * devine.
     */
    private static Map<String, List<String>> methodesDuFichier(
            Document document) {
        Map<String, List<String>> methodes = new HashMap<>();
        for (int ligne = 0; ligne < document.lineCount(); ligne++) {
            String corps = ligneApresVisibilite(document.lineText(ligne));
            int parenthese = corps.indexOf('(');
            if (parenthese <= 0 || !corps.contains(")")) {
                continue;
            }
            String nom = corps.substring(0, parenthese).strip();
            int dernierPoint = nom.lastIndexOf('.');
            if (dernierPoint >= 0) {
                nom = nom.substring(dernierPoint + 1);
            }
            if (nom.isEmpty() || !estIdentifiant(nom)) {
                continue;
            }
            List<String> parametres = nomsDesParametres(
                    corps.substring(parenthese + 1,
                            corps.indexOf(')')));
            if (!parametres.isEmpty()) {
                methodes.putIfAbsent(nom, parametres);
            }
        }
        return methodes;
    }

    /** La ligne privée de son en-tête de visibilité et de ses blancs
     *  de tête — la signature s'y lit d'un tenant. */
    private static String ligneApresVisibilite(String ligneBrute) {
        String ligne = ligneBrute.strip();
        for (String motCle : new String[] {"public ", "private ",
                "protected ", "static ", "final ", "synchronized "}) {
            if (ligne.startsWith(motCle)) {
                return ligne.substring(motCle.length()).stripLeading();
            }
        }
        return ligne;
    }

    /** Les noms des paramètres d'une signature — « int solde, boolean
     *  vif » porte [solde, vif] ; les vides se sautent. */
    private static List<String> nomsDesParametres(String entreParentheses) {
        List<String> noms = new ArrayList<>();
        for (String parametre : entreParentheses.split(",")) {
            String[] parties = parametre.strip().split("\\s+");
            if (parties.length == 2 && estIdentifiant(parties[1])) {
                noms.add(parties[1]);
            } else {
                // un paramètre qui ne se parse pas rompt l'ordre —
                // jamais de nom décalé
                return List.of();
            }
        }
        return noms;
    }

    /**
     * Les noms de paramètres muets d'une ligne d'appel : la méthode
     * connue du fichier y porte SES paramètres, chaque argument
     * LITTÉRAL gagne le nom de son paramètre — devant l'argument, à
     * l'IDE de référence. Les arguments nommés d'avance et les
     * expressions ne portent rien.
     */
    private static List<IndiceInlay> nomsParametres(
            Map<String, List<String>> methodes, int ligne, String texteBrut) {
        List<IndiceInlay> indices = new ArrayList<>();
        String texte = texteBrut.strip();
        // le décalage du contenu dépouillé — l'indice vit DANS la ligne,
        // l'indentation compte (la colonne de l'argument, pas de la marge)
        int indentation = texteBrut.indexOf(texte);
        int parenthese = texte.indexOf('(');
        while (parenthese > 0) {
            int fermeture = fermetureDe(texte, parenthese);
            if (fermeture < 0) {
                break;
            }
            String nom = nomDAppel(texte, parenthese);
            List<String> parametres = methodes.get(nom);
            if (parametres != null) {
                int rang = 0;
                int curseur = parenthese + 1;
                while (rang < parametres.size()) {
                    int fin = finArgument(texte, curseur, fermeture);
                    if (fin < 0) {
                        break;
                    }
                    String argument = texte.substring(curseur, fin).strip();
                    if (argument.isEmpty()) {
                        break;
                    }
                    // le nom ne se propose que si l'argument ne le
                    // porte pas déjà (nom: valeur) et se tranche
                    // littéral — l'IDE de référence n'insiste pas sur
                    // les expressions
                    if (!argument.contains(":")
                            && typeDuLitteral(argument) != null) {
                        indices.add(new IndiceInlay(ligne + 1,
                                indentation + curseur,
                                parametres.get(rang) + ":"));
                    }
                    if (fin >= fermeture) {
                        break;
                    }
                    curseur = fin + 1;
                    rang++;
                }
            }
            parenthese = texte.indexOf('(', fermeture);
        }
        return indices;
    }

    /** Le nom de la méthode qui précède la parenthèse ouvrante — les
     *  qualifiés gardent leur queue, les appels d'expression ne se
     *  devinent pas. */
    private static String nomDAppel(String ligne, int parenthese) {
        int fin = parenthese;
        int debut = fin;
        while (debut > 0 && estIdentifiant(ligne.charAt(debut - 1))) {
            debut--;
        }
        return debut == fin ? "" : ligne.substring(debut, fin);
    }

    /** La parenthèse fermante qui répond à l'ouvrante — les appels
     *  imbriqués comptent leurs niveaux ; —1 si la ligne ne ferme pas. */
    private static int fermetureDe(String ligne, int ouvrante) {
        int profondeur = 0;
        for (int i = ouvrante; i < ligne.length(); i++) {
            if (ligne.charAt(i) == '(') {
                profondeur++;
            } else if (ligne.charAt(i) == ')' && --profondeur == 0) {
                return i;
            }
        }
        return -1;
    }

    /** La fin de l'argument qui commence au curseur — la virgule au
     *  niveau zéro ou la fermeture ; —1 si la ligne ne suffit pas. */
    private static int finArgument(String ligne, int curseur, int fermeture) {
        int profondeur = 0;
        for (int i = curseur; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (c == '(') {
                profondeur++;
            } else if (c == ')') {
                if (profondeur == 0) {
                    return Math.min(i, fermeture);
                }
                profondeur--;
            } else if (c == ',' && profondeur == 0) {
                return i;
            }
        }
        return -1;
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

    /** Vrai si le caractère peut vivre dans un identifiant. */
    private static boolean estIdentifiant(char c) {
        return c == '_' || c == '$' || Character.isLetterOrDigit(c);
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
        if (estTableau(valeur)) {
            return typeTableau(valeur);
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

    /** Vrai si la valeur est un initialiseur de tableau. */
    private static boolean estTableau(String valeur) {
        return valeur.startsWith("{") && valeur.endsWith("}");
    }

    /** Le type d'un initialiseur de tableau : le type de son PREMIER
     *  élément porté en tableau — « {1, 2} » lit « : int[] », vide ou
     *  hétérogène ne se devine pas. */
    private static String typeTableau(String valeur) {
        String corps = valeur.substring(1, valeur.length() - 1).strip();
        if (corps.isEmpty()) {
            return null;
        }
        String premier = corps.split(",")[0].strip();
        String type = typeDuLitteral(premier);
        return type == null ? null : type + "[]";
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
