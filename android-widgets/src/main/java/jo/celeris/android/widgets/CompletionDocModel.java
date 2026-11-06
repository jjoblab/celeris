package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

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
 */
public final class CompletionDocModel {

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
     * @param documentation la documentation Markdown, clôtures comprises
     * @return le modèle — sans clôture, tout le texte est libre
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
