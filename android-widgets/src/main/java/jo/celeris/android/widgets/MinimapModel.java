package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * La minimap (chantier C9, la silhouette du dépôt de départ) : chaque
 * rangée de la carte porte la charge de son bouquet de lignes — la
 * longueur de la plus longue ligne plafonnée — ET les segments encrés
 * de sa ligne REPRÉSENTATIVE : le code, les chaînes, les commentaires,
 * les nombres et les mots-clés s'y découpent en plages de colonnes,
 * la silhouette se rend COLORÉE comme l'éditeur. Pur et testable sans
 * SDK : la vue la possède, le peintre la lit ; la densité est bornée
 * par construction — une rangée par cran, jamais plus.
 *
 * <p>Le bouquet d'une rangée couvre les lignes du document au prorata
 * : la rangée {@code r} d'une carte de {@code n} rangees couvre les
 * lignes de {@code r * lignes / n} à {@code (r + 1) * lignes / n - 1}
 * — chaque ligne compte une fois, la ligne orpheline d'une division
 * arrondie tombe dans la rangée qui la porte.</p>
 *
 * <p>La classification textuelle ignore la grammaire du langage : le
 * balayage d'une ligne découvre les chaînes à leurs guillemets, les
 * commentaires à leurs barres, les nombres à leurs chiffres, les
 * mots-clés à la table commune — la silhouette rend l'ESSENTIEL de
 * l'éditeur sans rien de son moteur, tout langage confondu.</p>
 */
public final class MinimapModel {

    /** Le plafond de charge d'une rangée — au-delà, le trait sature. */
    public static final int CHARGE_MAX = 120;

    /** L'encre du code nu — l'identifiant, l'opérateur, le blanc. */
    public static final int CODE_INK = 0;

    /** L'encre des chaînes littérales — guillemets compris. */
    public static final int STRING_INK = 1;

    /** L'encre des commentaires — barres, blocs et javadoc. */
    public static final int COMMENT_INK = 2;

    /** L'encre des nombres littéraux. */
    public static final int NUMBER_INK = 3;

    /** L'encre des mots-clés du langage — la table commune. */
    public static final int KEYWORD_INK = 4;

    /** Un segment d'une rangée : sa colonne de départ, ses colonnes, son encre. */
    public record MapSegment(int column, int colonnes, int encre) {
    }

    /** Une rangée de la carte : son cran, la charge de son bouquet, ses segments. */
    public record MapRow(int slot, int load, List<MapSegment> segments) {
    }

    private final List<MapRow> rows;

    private MinimapModel(List<MapRow> rows) {
        this.rows = List.copyOf(rows);
    }

    /**
     * Construit la silhouette d'un document.
     *
     * @param doc     le document porteur
     * @param height la hauteur de la carte en rangees
     * @return la carte, une rangée par cran
     */
    public static MinimapModel of(Document doc, int height) {
        int crans = Math.max(1, height);
        int lines = Math.max(1, doc.lineCount());
        int[] charges = new int[crans];
        int[] representants = new int[crans];
        for (int line = 0; line < doc.lineCount(); line++) {
            int cran = lineNotch(line, lines, crans);
            String text = doc.lineText(line);
            int charge = Math.min(CHARGE_MAX, text.stripTrailing().length());
            if (charge > charges[cran]) {
                charges[cran] = charge;
                representants[cran] = line;
            }
        }
        List<MapRow> carte = new ArrayList<>(crans);
        for (int cran = 0; cran < crans; cran++) {
            List<MapSegment> segments = charges[cran] == 0
                    ? List.of()
                    : segmentsDe(doc.lineText(representants[cran]));
            carte.add(new MapRow(cran, charges[cran], segments));
        }
        return new MinimapModel(carte);
    }

    /**
     * Le cran d'une ligne : sa rangée dans une carte de {@code crans}
     * rangees pour {@code lines} lignes — la dernière ligne tombe
     * toujours dans le dernier cran.
     *
     * @param line la ligne du document
     * @param lines le nombre de lignes du document
     * @param crans le nombre de rangees de la carte
     * @return le cran de la ligne, dans les bornes
     */
    public static int lineNotch(int line, int lines, int crans) {
        int cran = (int) ((long) line * crans / lines);
        return Math.max(0, Math.min(crans - 1, cran));
    }

    /** Les rangees de la carte, dans l'ordre des crans. */
    public List<MapRow> rows() {
        return rows;
    }

    /** Le nombre de rangees de la carte. */
    public int size() {
        return rows.size();
    }

    /**
     * La ligne visée par un cran — la premiere ligne de son bouquet :
     * le saut du toucher retombe sur une vraie ligne du document.
     *
     * @param cran  le cran visé
     * @param lines le nombre de lignes du document
     * @return la premiere ligne du bouquet du cran
     */
    public int notchLine(int cran, int lines) {
        int borne = Math.max(0, Math.min(rows.size() - 1, cran));
        return (int) ((long) borne * lines / Math.max(1, rows.size()));
    }

    // ── La classification textuelle — le balayage d'une ligne ─────

    /**
     * Les segments encrés d'une ligne : le code, les chaînes, les
     * commentaires, les nombres et les mots-clés s'y découpent en
     * plages de colonnes — la colonne de départ et la longueur de
     * chaque plage, jamais son texte.
     */
    static List<MapSegment> segmentsDe(String line) {
        List<MapSegment> segments = new ArrayList<>(commentaireEntier(line));
        int n = line.length();
        // la ligne entière en commentaire ne se balaye pas : toute la
        // ligne porte déjà son encre
        if (!segments.isEmpty()) {
            return segments;
        }
        int i = 0;
        while (i < n) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                // le commentaire de fin de ligne : du « // » à la fin
                segments.add(souder(segments, i, finUtile(line, n),
                        COMMENT_INK));
                return segments;
            }
            if (c == '"' || c == '\'') {
                int end = finDeLitteral(line, i, c);
                segments.add(souder(segments, i, Math.min(end + 1, n),
                        STRING_INK));
                i = end + 1;
                continue;
            }
            if (Character.isDigit(c) && (i == 0
                    || !Character.isJavaIdentifierPart(line.charAt(i - 1)))) {
                int end = finDeNombre(line, i);
                segments.add(souder(segments, i, end, NUMBER_INK));
                i = end;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int end = finDeMot(line, i, n);
                String mot = line.substring(i, end);
                segments.add(souder(segments, i, end,
                        KEYWORDS.contains(mot) ? KEYWORD_INK : CODE_INK));
                i = end;
                continue;
            }
            // tout le reste — signes, blancs, opérateurs — code nu :
            // les plages voisines de même encre se soudent
            segments.add(souder(segments, i, i + 1, CODE_INK));
            i++;
        }
        return compacter(segments);
    }

    /** La ligne ENTIÈRE en commentaire : le segment unique qui la
     *  porte — « // », « /* », « * » en tête dépouillée ; liste vide
     *  quand la ligne n'est pas un commentaire entier. */
    private static List<MapSegment> commentaireEntier(String line) {
        String depouillee = line.stripLeading();
        if ((depouillee.startsWith("//") || depouillee.startsWith("/*")
                || depouillee.startsWith("*")) && !depouillee.isEmpty()) {
            return List.of(new MapSegment(0,
                    Math.min(CHARGE_MAX, line.stripTrailing().length()),
                    COMMENT_INK));
        }
        return List.of();
    }

    /** La fin d'un littéral numérique : chiffres, points, exposants
     *  et affixes — la lettre d'un identifiant ne s'y rallonge pas. */
    private static int finDeNombre(String line, int start) {
        int n = line.length();
        int end = start + 1;
        while (end < n && "0123456789._eEfdLxXaAbBcCdDfF".indexOf(
                line.charAt(end)) >= 0
                && (Character.isLetterOrDigit(line.charAt(end))
                || line.charAt(end) == '.')) {
            end++;
        }
        return end;
    }

    /** La fin du mot qui commence à la borne — identifiant ou mot-clé. */
    private static int finDeMot(String line, int start, int finMax) {
        int end = start;
        while (end < finMax && Character.isJavaIdentifierPart(
                line.charAt(end))) {
            end++;
        }
        return end;
    }

    /** La fin utile d'une ligne — la queue de blancs ne se rend pas. */
    private static int finUtile(String line, int n) {
        int end = n;
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    /** La fin d'un littéral : le guillemet fermant, échappés exceptés. */
    private static int finDeLitteral(String line, int start, char quote) {
        for (int i = start + 1; i < line.length(); i++) {
            if (line.charAt(i) == '\\') {
                i++;
                continue;
            }
            if (line.charAt(i) == quote) {
                return i;
            }
        }
        // non fermé : la chaîne vit jusqu'à la fin de la ligne
        return line.length() - 1;
    }

    /** Soude la plage à la queue — les plages voisines de même encre ne se doublent pas. */
    private static MapSegment souder(List<MapSegment> segments,
            int column, int end, int encre) {
        return new MapSegment(column, Math.max(0, end - column), encre);
    }

    /** Compacte les segments : les voisins de même encre se soudent en une plage. */
    private static List<MapSegment> compacter(List<MapSegment> bruts) {
        List<MapSegment> compacts = new ArrayList<>(bruts.size());
        for (MapSegment segment : bruts) {
            if (segment.colonnes() <= 0) {
                continue;
            }
            if (!compacts.isEmpty()) {
                MapSegment dernier = compacts.get(compacts.size() - 1);
                if (dernier.encre() == segment.encre()
                        && dernier.column() + dernier.colonnes()
                        == segment.column()) {
                    compacts.set(compacts.size() - 1,
                            new MapSegment(dernier.column(),
                                    dernier.colonnes() + segment.colonnes(),
                                    dernier.encre()));
                    continue;
                }
            }
            compacts.add(segment);
        }
        return compacts;
    }

    /** La table commune des mots-clés — java au cœur, la longue traîne s'y rallonge. */
    private static final java.util.Set<String> KEYWORDS = java.util.Set.of(
            "class", "interface", "enum", "record", "public", "private",
            "protected", "static", "final", "void", "int", "long", "short",
            "byte", "boolean", "char", "float", "double", "if", "else",
            "for", "while", "do", "switch", "case", "default", "try",
            "catch", "finally", "throw", "throws", "return", "new", "this",
            "super", "extends", "implements", "import", "package", "var",
            "null", "true", "false", "break", "continue", "abstract",
            "synchronized", "assert", "yield", "transient", "volatile",
            "native", "instanceof", "fn", "fun", "let", "def", "end");
}
