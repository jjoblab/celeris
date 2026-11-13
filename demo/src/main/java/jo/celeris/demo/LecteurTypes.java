package jo.celeris.demo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Le lecteur de types du terrain de démonstration (catégorie A de
 * l'inventaire — le fil rouge du guide du développeur, la seconde
 * surface réveillée du signal du propriétaire) : le type d'un symbole
 * se lit à sa DÉCLARATION dans le document — « Type symbole », le mot
 * qui précède le nom, hors mots de conduite. Pur et testé sans
 * appareil : ce qui ne se lit pas rend null, jamais un type deviné —
 * la carte d'info de type ne se pose pas sans type (aucune fausse
 * oracle).
 */
final class LecteurTypes {

    /** Les mots de conduite — jamais pris pour un type : « return
     *  total » ne déclare rien. */
    private static final List<String> MOTS_DE_CONDUITE = List.of(
            "assert", "break", "case", "catch", "continue", "do",
            "else", "finally", "for", "fun", "if", "instanceof",
            "new", "return", "super", "switch", "throw", "try",
            "val", "var", "while", "yield");

    /** Le constructeur du lecteur — purement statique, il reste muet. */
    private LecteurTypes() {
    }

    /**
     * Le type du symbole à l'offset : la DÉCLARATION se lit dans le
     * document — « Type symbole », le mot qui précède le nom à la
     * tête d'une instruction, hors mots de conduite — la première
     * déclaration gagne, où qu'elle vive. Ce qui ne se lit pas rend
     * null, jamais un type deviné.
     *
     * @param document le document ouvert
     * @param offset   la position visitée
     * @return le type déclaré, null si le document ne déclare pas
     */
    static @Nullable String typeDe(Document document, int offset) {
        String symbole = PopupCoach.symboleA(document, offset);
        if (symbole == null) {
            return null;
        }
        Pattern forme = Pattern.compile(
                "(?<![\\w$.])(?:(?:final|public|private|protected|static)\\s+)*"
                        + "([\\w$.]+(?:<[^=;()]*>)?(?:\\[\\])*)\\s+"
                        + Pattern.quote(symbole) + "(?![\\w$])");
        for (int ligne = 0; ligne < document.lineCount(); ligne++) {
            Matcher corps = forme.matcher(document.lineText(ligne));
            while (corps.find()) {
                String candidat = corps.group(1);
                if (!MOTS_DE_CONDUITE.contains(candidat)) {
                    return candidat;
                }
            }
        }
        return null;
    }
}
