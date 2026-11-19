package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.view.InlayHintMark;
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
final class InlayHintsEngine {

    private InlayHintsEngine() {
    }

    /**
     * Les indices du document : les types des « var » littéraux en fin
     * de ligne utile, les noms de paramètres muets devant leurs
     * arguments littéraux — l'ordre suit le document.
     *
     * @param document le document ouvert
     * @return les indices par ligne, numérotée à partir de un
     */
    static List<InlayHintMark> inlayHints(Document document) {
        List<InlayHintMark> inlayHints = new ArrayList<>();
        Map<String, List<String>> methodes = methodesDuFichier(document);
        for (int line = 0; line < document.lineCount(); line++) {
            String text = document.lineText(line);
            String type = typeDuVar(text);
            if (type != null) {
                inlayHints.add(new InlayHintMark(line + 1,
                        text.stripTrailing().length(), type));
            }
            inlayHints.addAll(nomsParametres(methodes, line, text));
        }
        return List.copyOf(inlayHints);
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
        for (int line = 0; line < document.lineCount(); line++) {
            String body = lineAfterVisibility(document.lineText(line));
            int parenthese = body.indexOf('(');
            if (parenthese <= 0 || !body.contains(")")) {
                continue;
            }
            // le NOM vit en QUEUE de l'en-tête : « private void verser »
            // garde « verser » — le type de retour et la visibilité ne
            // sont pas le nom
            String nom = body.substring(0, parenthese).strip();
            int dernierPoint = nom.lastIndexOf('.');
            if (dernierPoint >= 0) {
                nom = nom.substring(dernierPoint + 1);
            }
            int derniereEspace = nom.lastIndexOf(' ');
            if (derniereEspace >= 0) {
                nom = nom.substring(derniereEspace + 1);
            }
            if (nom.isEmpty() || !TypesLitteraux.estIdentifiant(nom)) {
                continue;
            }
            List<String> parameters = nomsDesParametres(
                    body.substring(parenthese + 1,
                            body.indexOf(')')));
            if (!parameters.isEmpty()) {
                methodes.putIfAbsent(nom, parameters);
            }
        }
        return methodes;
    }

    /** La ligne privée de son en-tête de visibilité et de ses blancs
     *  de tête — la signature s'y lit d'un tenant. */
    private static String lineAfterVisibility(String ligneBrute) {
        String line = ligneBrute.strip();
        for (String motCle : new String[] {"public ", "private ",
                "protected ", "static ", "final ", "synchronized "}) {
            if (line.startsWith(motCle)) {
                return line.substring(motCle.length()).stripLeading();
            }
        }
        return line;
    }

    /** Les noms des paramètres d'une signature — « int solde, boolean
     *  vif » porte [solde, vif] ; les vides se sautent. */
    private static List<String> nomsDesParametres(String entreParentheses) {
        List<String> noms = new ArrayList<>();
        for (String parametre : entreParentheses.split(",")) {
            String[] parties = parametre.strip().split("\\s+");
            if (parties.length == 2 && TypesLitteraux.estIdentifiant(parties[1])) {
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
    private static List<InlayHintMark> nomsParametres(
            Map<String, List<String>> methodes, int line, String texteBrut) {
        List<InlayHintMark> inlayHints = new ArrayList<>();
        String text = texteBrut.strip();
        // le décalage du contenu dépouillé — l'indice vit DANS la ligne,
        // l'indentation compte (la colonne de l'argument, pas de la marge)
        int indentation = texteBrut.indexOf(text);
        int parenthese = text.indexOf('(');
        while (parenthese > 0) {
            int fermeture = fermetureDe(text, parenthese);
            if (fermeture < 0) {
                break;
            }
            String nom = nomDAppel(text, parenthese);
            List<String> parameters = methodes.get(nom);
            if (parameters != null) {
                int rang = 0;
                int cursor = parenthese + 1;
                while (rang < parameters.size()) {
                    int end = finArgument(text, cursor, fermeture);
                    if (end < 0) {
                        break;
                    }
                    String argument = text.substring(cursor, end).strip();
                    if (argument.isEmpty()) {
                        break;
                    }
                    // le nom ne se propose que si l'argument ne le
                    // porte pas déjà (nom: valeur) et se tranche
                    // littéral — l'IDE de référence n'insiste pas sur
                    // les expressions
                    if (!argument.contains(":")
                            && TypesLitteraux.duLitteral(argument) != null) {
                        inlayHints.add(new InlayHintMark(line + 1,
                                indentation + cursor,
                                parameters.get(rang) + ":"));
                    }
                    if (end >= fermeture) {
                        break;
                    }
                    cursor = end + 1;
                    rang++;
                }
            }
            parenthese = text.indexOf('(', fermeture);
        }
        return inlayHints;
    }

    /** Le nom de la méthode qui précède la parenthèse ouvrante — les
     *  qualifiés gardent leur queue, les appels d'expression ne se
     *  devinent pas. */
    private static String nomDAppel(String line, int parenthese) {
        int end = parenthese;
        int start = end;
        while (start > 0 && TypesLitteraux.estIdentifiant(line.charAt(start - 1))) {
            start--;
        }
        return start == end ? "" : line.substring(start, end);
    }

    /** La parenthèse fermante qui répond à l'ouvrante — les appels
     *  imbriqués comptent leurs niveaux ; —1 si la ligne ne ferme pas. */
    private static int fermetureDe(String line, int ouvrante) {
        int profondeur = 0;
        for (int i = ouvrante; i < line.length(); i++) {
            if (line.charAt(i) == '(') {
                profondeur++;
            } else if (line.charAt(i) == ')' && --profondeur == 0) {
                return i;
            }
        }
        return -1;
    }

    /** La fin de l'argument qui commence au curseur — la virgule au
     *  niveau zéro ou la fermeture ; —1 si la ligne ne suffit pas. */
    private static int finArgument(String line, int cursor, int fermeture) {
        int profondeur = 0;
        for (int i = cursor; i < line.length(); i++) {
            char c = line.charAt(i);
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
        String line = ligneBrute.strip();
        if (line.length() < 5 || !line.startsWith("var")
                || !Character.isWhitespace(line.charAt(3))) {
            return null;
        }
        String body = line.substring(4).stripLeading();
        int egale = body.indexOf('=');
        if (egale <= 0) {
            return null;
        }
        String nom = body.substring(0, egale).stripTrailing();
        if (!TypesLitteraux.estIdentifiant(nom)) {
            return null;
        }
        String value = body.substring(egale + 1).strip();
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).stripTrailing();
        }
        if (value.isEmpty()) {
            return null;
        }
        return TypesLitteraux.duLitteral(value);
    }

}
