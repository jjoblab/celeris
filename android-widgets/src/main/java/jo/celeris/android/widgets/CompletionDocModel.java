package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Le modèle de documentation de complétion (B-09) : la documentation
 * structurée du serveur — des blocs typés balisés par des clôtures
 * Markdown — devient des sections distinctes, le texte hors clôtures
 * vivant à part. Pur et testable sans SDK : la popup le lit, le
 * peintre le dessine.
 *
 * <p>PIEGE-16 (hérité du dépôt de départ) : le panneau fusionnait
 * titre et élément en ignorant la signature (clôture typée) du
 * serveur — les blocs typés restent ici distincts, le titre jamais
 * fondu dans le corps.</p>
 *
 * <p>La convention des serveurs LSP (l'audit de conformité Android
 * Studio du 2026-11-14) : le markdown HOVER sert ses sections par
 * en-têtes — « **Parameters:** », « **Returns:** », « **Throws:** »
 * ou « # Titre » — leurs listes à tirets vivant sous l'en-tête. Le
 * modèle les lit comme les clôtures typées : une section par
 * en-tête, le corps jusqu'à l'en-tête suivant — la doc rapide
 * d'Android Studio y affiche ses sections « Parameters », « Returns »,
 * « Throws » de la même façon.</p>
 */
public final class CompletionDocModel {

    /** L'en-tête gras porté par la ligne entière — « **Parameters:** ». */
    private static final Pattern GRAS_ENTIER = Pattern.compile("\\*\\*[^*]+\\*\\*:?");

    /** Le titre Markdown — « # Titre », jusqu'à six niveaux. */
    private static final Pattern TITRE_MARKDOWN = Pattern.compile("#{1,6}\\s+(.+)");

    /** Une section typée : le genre de la clôture et son texte, sans les marqueurs. */
    public record Section(String kind, String text) {
    }

    private final String titre;
    private final List<Section> sections;
    private final String freeText;

    private CompletionDocModel(String titre, List<Section> sections, String freeText) {
        this.titre = titre;
        this.sections = List.copyOf(sections);
        this.freeText = freeText;
    }

    /**
     * Construit le modèle d'une documentation serveur.
     *
     * @param titre          le libellé de l'élément — distinct, jamais fondu
     * @param documentation la documentation Markdown, clôtures et
     *                       en-têtes compris
     * @return le modèle — sans clôture ni marqueur d'en-tête, les
     *         listes restent au corps de leur section
     */
    public static CompletionDocModel of(String titre, String documentation) {
        List<Section> sections = new ArrayList<>();
        StringBuilder libre = new StringBuilder();
        String[] lines = documentation.split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (estOuvertureDeCloture(line)) {
                String kind = line.substring(3).trim();
                StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.length && !estFermetureDeCloture(lines[i])) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }
                    body.append(lines[i]);
                    i++;
                }
                // la clôture fermante est consommée ; sans fermante, la fin du texte clot
                i++;
                sections.add(new Section(kind, body.toString()));
            } else if (enteteMarkdown(line) != null) {
                // l'en-tête Markdown du serveur LSP : la section naît
                // sous son libellé, le corps vit jusqu'à l'en-tête suivant
                String kind = enteteMarkdown(line);
                StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.length && !estOuvertureDeCloture(lines[i])
                        && enteteMarkdown(lines[i]) == null) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }
                    body.append(lines[i]);
                    i++;
                }
                sections.add(new Section(kind, body.toString()));
            } else {
                if (libre.length() > 0) {
                    libre.append('\n');
                }
                libre.append(line);
                i++;
            }
        }
        return new CompletionDocModel(titre, sections, libre.toString());
    }

    private static boolean estOuvertureDeCloture(String line) {
        return line.startsWith("```");
    }

    private static boolean estFermetureDeCloture(String line) {
        return line.trim().equals("```");
    }

    /**
     * Le genre d'un en-tête Markdown de section — la convention des
     * serveurs LSP (« **Parameters:** », « **Returns:** », « # Titre »)
     * — null pour toute autre ligne (le gras EN LIGNE reste au corps,
     * seul l'en-tête PORTÉ PAR LA LIGNE ENTIERÈ ouvre une section).
     */
    private static @Nullable String enteteMarkdown(String line) {
        String epuree = line.strip();
        // l'en-tête gras occupe la ligne ENTIÈRE — le gras en ligne
        // (une phrase qui porte un mot en gras) reste au corps
        if (GRAS_ENTIER.matcher(epuree).matches()) {
            return epuree.substring(2, epuree.length() - 2);
        }
        Matcher titre = TITRE_MARKDOWN.matcher(epuree);
        if (titre.matches()) {
            return titre.group(1).strip();
        }
        return null;
    }

    /** Le titre — le libellé de l'élément, jamais mêlé aux sections. */
    public String titre() {
        return titre;
    }

    /** Les sections typées, dans l'ordre des clôtures du serveur. */
    public List<Section> sections() {
        return sections;
    }

    /** Le texte hors clôtures — épuré, jamais mêlé aux sections. */
    public String freeText() {
        return freeText;
    }
}
