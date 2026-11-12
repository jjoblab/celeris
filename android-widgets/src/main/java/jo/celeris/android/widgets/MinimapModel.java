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
    public static final int ENCRE_CODE = 0;

    /** L'encre des chaînes littérales — guillemets compris. */
    public static final int ENCRE_CHAINE = 1;

    /** L'encre des commentaires — barres, blocs et javadoc. */
    public static final int ENCRE_COMMENTAIRE = 2;

    /** L'encre des nombres littéraux. */
    public static final int ENCRE_NOMBRE = 3;

    /** L'encre des mots-clés du langage — la table commune. */
    public static final int ENCRE_MOTCLE = 4;

    /** Un segment d'une rangée : sa colonne de départ, ses colonnes, son encre. */
    public record MapSegment(int colonne, int colonnes, int encre) {
    }

    /** Une rangée de la carte : son cran, la charge de son bouquet, ses segments. */
    public record MapRow(int slot, int load, List<MapSegment> segments) {
    }

    private final List<MapRow> rangees;

    private MinimapModel(List<MapRow> rangees) {
        this.rangees = List.copyOf(rangees);
    }

    /**
     * Construit la silhouette d'un document.
     *
     * @param doc     le document porteur
     * @param hauteur la hauteur de la carte en rangees
     * @return la carte, une rangée par cran
     */
    public static MinimapModel of(Document doc, int hauteur) {
        int crans = Math.max(1, hauteur);
        int lignes = Math.max(1, doc.lineCount());
        int[] charges = new int[crans];
        int[] representants = new int[crans];
        for (int ligne = 0; ligne < doc.lineCount(); ligne++) {
            int cran = cranDeLigne(ligne, lignes, crans);
            String texte = doc.lineText(ligne);
            int charge = Math.min(CHARGE_MAX, texte.stripTrailing().length());
            if (charge > charges[cran]) {
                charges[cran] = charge;
                representants[cran] = ligne;
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
     * rangees pour {@code lignes} lignes — la dernière ligne tombe
     * toujours dans le dernier cran.
     *
     * @param ligne la ligne du document
     * @param lignes le nombre de lignes du document
     * @param crans le nombre de rangees de la carte
     * @return le cran de la ligne, dans les bornes
     */
    public static int cranDeLigne(int ligne, int lignes, int crans) {
        int cran = (int) ((long) ligne * crans / lignes);
        return Math.max(0, Math.min(crans - 1, cran));
    }

    /** Les rangees de la carte, dans l'ordre des crans. */
    public List<MapRow> rangees() {
        return rangees;
    }

    /** Le nombre de rangees de la carte. */
    public int taille() {
        return rangees.size();
    }

    /**
     * La ligne visée par un cran — la premiere ligne de son bouquet :
     * le saut du toucher retombe sur une vraie ligne du document.
     *
     * @param cran  le cran visé
     * @param lignes le nombre de lignes du document
     * @return la premiere ligne du bouquet du cran
     */
    public int ligneDeCran(int cran, int lignes) {
        int borne = Math.max(0, Math.min(rangees.size() - 1, cran));
        return (int) ((long) borne * lignes / Math.max(1, rangees.size()));
    }

    // ── La classification textuelle — le balayage d'une ligne ─────

    /**
     * Les segments encrés d'une ligne : le code, les chaînes, les
     * commentaires, les nombres et les mots-clés s'y découpent en
     * plages de colonnes — la colonne de départ et la longueur de
     * chaque plage, jamais son texte.
     */
    static List<MapSegment> segmentsDe(String ligne) {
        List<MapSegment> segments = new ArrayList<>(commentaireEntier(ligne));
        int n = ligne.length();
        // la ligne entière en commentaire ne se balaye pas : toute la
        // ligne porte déjà son encre
        if (!segments.isEmpty()) {
            return segments;
        }
        int i = 0;
        while (i < n) {
            char c = ligne.charAt(i);
            if (c == '/' && i + 1 < n && ligne.charAt(i + 1) == '/') {
                // le commentaire de fin de ligne : du « // » à la fin
                segments.add(souder(segments, i, finUtile(ligne, n),
                        ENCRE_COMMENTAIRE));
                return segments;
            }
            if (c == '"' || c == '\'') {
                int fin = finDeLitteral(ligne, i, c);
                segments.add(souder(segments, i, Math.min(fin + 1, n),
                        ENCRE_CHAINE));
                i = fin + 1;
                continue;
            }
            if (Character.isDigit(c) && (i == 0
                    || !Character.isJavaIdentifierPart(ligne.charAt(i - 1)))) {
                int fin = finDeNombre(ligne, i);
                segments.add(souder(segments, i, fin, ENCRE_NOMBRE));
                i = fin;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int fin = finDeMot(ligne, i, n);
                String mot = ligne.substring(i, fin);
                segments.add(souder(segments, i, fin,
                        MOTS_CLES.contains(mot) ? ENCRE_MOTCLE : ENCRE_CODE));
                i = fin;
                continue;
            }
            // tout le reste — signes, blancs, opérateurs — code nu :
            // les plages voisines de même encre se soudent
            segments.add(souder(segments, i, i + 1, ENCRE_CODE));
            i++;
        }
        return compacter(segments);
    }

    /** La ligne ENTIÈRE en commentaire : le segment unique qui la
     *  porte — « // », « /* », « * » en tête dépouillée ; liste vide
     *  quand la ligne n'est pas un commentaire entier. */
    private static List<MapSegment> commentaireEntier(String ligne) {
        String depouillee = ligne.stripLeading();
        if ((depouillee.startsWith("//") || depouillee.startsWith("/*")
                || depouillee.startsWith("*")) && !depouillee.isEmpty()) {
            return List.of(new MapSegment(0,
                    Math.min(CHARGE_MAX, ligne.stripTrailing().length()),
                    ENCRE_COMMENTAIRE));
        }
        return List.of();
    }

    /** La fin d'un littéral numérique : chiffres, points, exposants
     *  et affixes — la lettre d'un identifiant ne s'y rallonge pas. */
    private static int finDeNombre(String ligne, int debut) {
        int n = ligne.length();
        int fin = debut + 1;
        while (fin < n && "0123456789._eEfdLxXaAbBcCdDfF".indexOf(
                ligne.charAt(fin)) >= 0
                && (Character.isLetterOrDigit(ligne.charAt(fin))
                || ligne.charAt(fin) == '.')) {
            fin++;
        }
        return fin;
    }

    /** La fin du mot qui commence à la borne — identifiant ou mot-clé. */
    private static int finDeMot(String ligne, int debut, int finMax) {
        int fin = debut;
        while (fin < finMax && Character.isJavaIdentifierPart(
                ligne.charAt(fin))) {
            fin++;
        }
        return fin;
    }

    /** La fin utile d'une ligne — la queue de blancs ne se rend pas. */
    private static int finUtile(String ligne, int n) {
        int fin = n;
        while (fin > 0 && Character.isWhitespace(ligne.charAt(fin - 1))) {
            fin--;
        }
        return fin;
    }

    /** La fin d'un littéral : le guillemet fermant, échappés exceptés. */
    private static int finDeLitteral(String ligne, int debut, char quote) {
        for (int i = debut + 1; i < ligne.length(); i++) {
            if (ligne.charAt(i) == '\\') {
                i++;
                continue;
            }
            if (ligne.charAt(i) == quote) {
                return i;
            }
        }
        // non fermé : la chaîne vit jusqu'à la fin de la ligne
        return ligne.length() - 1;
    }

    /** Soude la plage à la queue — les plages voisines de même encre ne se doublent pas. */
    private static MapSegment souder(List<MapSegment> segments,
            int colonne, int fin, int encre) {
        return new MapSegment(colonne, Math.max(0, fin - colonne), encre);
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
                        && dernier.colonne() + dernier.colonnes()
                        == segment.colonne()) {
                    compacts.set(compacts.size() - 1,
                            new MapSegment(dernier.colonne(),
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
    private static final java.util.Set<String> MOTS_CLES = java.util.Set.of(
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
