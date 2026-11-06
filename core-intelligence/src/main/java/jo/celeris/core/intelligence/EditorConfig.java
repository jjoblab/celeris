package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

/**
 * Le respect de `.editorconfig` (B-26) : retrait, fins de ligne et
 * encodage pilotés par le fichier. Les sections se matchent par
 * extension — la plus spécifique gagne —, les propriétés hors section
 * (la racine) portent les défauts, les clés inconnues sont ignorées.
 */
public final class EditorConfig {

    /** Les règles résolues pour un fichier : style, fins de ligne, encodage. */
    public record Regles(IndentStyle indentStyle, String finDeLigne, String encodage) {

        /** Les règles canoniques : quatre espaces, rien de prescrit d'autre. */
        public static final Regles CANONIQUES = new Regles(IndentStyle.ESPACES_4, "", "");
    }

    /** Une section : son motif et ses propriétés. */
    private record Section(String motif, List<String[]> proprietes) {
    }

    private static final int LARGEUR_TAB_DEFAUT = 4;

    private final List<Section> sections = new ArrayList<>();

    private EditorConfig() {
    }

    /**
     * Analyse le contenu d'un fichier `.editorconfig`.
     *
     * @param contenu le texte du fichier, INI à sections
     * @return la configuration prête à résoudre des chemins
     */
    public static EditorConfig analyser(String contenu) {
        EditorConfig config = new EditorConfig();
        Section courante = new Section("", new ArrayList<>());
        config.sections.add(courante);
        for (String ligneBrute : contenu.split("\n", -1)) {
            String ligne = ligneBrute.strip();
            if (ligne.isEmpty() || ligne.startsWith("#") || ligne.startsWith(";")) {
                continue;
            }
            if (ligne.startsWith("[") && ligne.endsWith("]")) {
                courante = new Section(ligne.substring(1, ligne.length() - 1).strip(),
                        new ArrayList<>());
                config.sections.add(courante);
                continue;
            }
            int egale = ligne.indexOf('=');
            if (egale < 0) {
                continue;
            }
            courante.proprietes().add(new String[] {
                    ligne.substring(0, egale).strip(), ligne.substring(egale + 1).strip()});
        }
        return config;
    }

    /**
     * Les règles qui s'appliquent à un chemin : les sections dont le
     * motif répond à l'extension, la plus spécifique gagne, la racine
     * porte les défauts.
     *
     * @param chemin le chemin du fichier, nu ou complet
     * @return les règles résolues
     */
    public Regles pour(String chemin) {
        boolean tabs = false;
        int largeur = -1;
        String finDeLigne = "";
        String encodage = "";
        for (Section section : sections) {
            if (!motifRepond(section.motif(), chemin)) {
                continue;
            }
            for (String[] propriete : section.proprietes()) {
                switch (propriete[0]) {
                    case "indent_style" -> tabs = "tab".equals(propriete[1]);
                    case "indent_size" -> largeur = entierOu(propriete[1], largeur);
                    case "end_of_line" -> finDeLigne = propriete[1];
                    case "charset" -> encodage = propriete[1];
                    default -> {
                        // clé inconnue : ignorée, jamais de casse
                    }
                }
            }
        }
        if (largeur < 0) {
            largeur = LARGEUR_TAB_DEFAUT;
        }
        return new Regles(tabs ? IndentStyle.tabs(largeur) : IndentStyle.spaces(largeur),
                finDeLigne, encodage);
    }

    /**
     * Applique les fins de ligne d'un texte selon les règles : crlf
     * convertit en retours chariots, lf en sauts simples, sans règle le
     * texte reste tel quel.
     *
     * @param texte  le texte à convertir
     * @param regles les règles résolues
     * @return le texte converti
     */
    public static String appliquerFins(String texte, Regles regles) {
        return switch (regles.finDeLigne()) {
            case "crlf" -> texte.replace("\r\n", "\n").replace("\n", "\r\n");
            case "lf" -> texte.replace("\r\n", "\n");
            default -> texte;
        };
    }

    /** Vrai quand le motif répond au chemin : la racine répond à tout, sinon l'extension. */
    private static boolean motifRepond(String motif, String chemin) {
        if (motif.isEmpty() || "*".equals(motif)) {
            return true;
        }
        if (!motif.startsWith("*.")) {
            return chemin.equals(motif);
        }
        return chemin.endsWith(motif.substring(1));
    }

    private static int entierOu(String texte, int defaut) {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            return defaut;
        }
    }
}
