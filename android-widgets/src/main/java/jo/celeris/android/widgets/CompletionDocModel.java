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
    public record Section(String genre, String texte) {
    }

    private final String titre;
    private final List<Section> sections;
    private final String texteLibre;

    private CompletionDocModel(String titre, List<Section> sections, String texteLibre) {
        this.titre = titre;
        this.sections = List.copyOf(sections);
        this.texteLibre = texteLibre;
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
        String[] lignes = documentation.split("\n", -1);
        int i = 0;
        while (i < lignes.length) {
            String ligne = lignes[i];
            if (estOuvertureDeCloture(ligne)) {
                String genre = ligne.substring(3).trim();
                StringBuilder corps = new StringBuilder();
                i++;
                while (i < lignes.length && !estFermetureDeCloture(lignes[i])) {
                    if (corps.length() > 0) {
                        corps.append('\n');
                    }
                    corps.append(lignes[i]);
                    i++;
                }
                // la clôture fermante est consommée ; sans fermante, la fin du texte clot
                i++;
                sections.add(new Section(genre, corps.toString()));
            } else if (enteteMarkdown(ligne) != null) {
                // l'en-tête Markdown du serveur LSP : la section naît
                // sous son libellé, le corps vit jusqu'à l'en-tête suivant
                String genre = enteteMarkdown(ligne);
                StringBuilder corps = new StringBuilder();
                i++;
                while (i < lignes.length && !estOuvertureDeCloture(lignes[i])
                        && enteteMarkdown(lignes[i]) == null) {
                    if (corps.length() > 0) {
                        corps.append('\n');
                    }
                    corps.append(lignes[i]);
                    i++;
                }
                sections.add(new Section(genre, corps.toString()));
            } else {
                if (libre.length() > 0) {
                    libre.append('\n');
                }
                libre.append(ligne);
                i++;
            }
        }
        return new CompletionDocModel(titre, sections, libre.toString());
    }

    private static boolean estOuvertureDeCloture(String ligne) {
        return ligne.startsWith("```");
    }

    private static boolean estFermetureDeCloture(String ligne) {
        return ligne.trim().equals("```");
    }

    /**
     * Le genre d'un en-tête Markdown de section — la convention des
     * serveurs LSP (« **Parameters:** », « **Returns:** », « # Titre »)
     * — null pour toute autre ligne (le gras EN LIGNE reste au corps,
     * seul l'en-tête PORTÉ PAR LA LIGNE ENTIERÈ ouvre une section).
     */
    private static @Nullable String enteteMarkdown(String ligne) {
        String epuree = ligne.strip();
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
    public String texteLibre() {
        return texteLibre;
    }
}
