package jo.celeris.core.text;

import org.jspecify.annotations.Nullable;

/**
 * Instantané immuable d'un document : une rope, un index de lignes, un
 * numéro de révision.
 *
 * <p>Chaque état du document est un instantané complet, lisible sans verrou
 * depuis tout fil (ADR-003) ; l'éditeur d'un instantané produit un nouvel
 * instantané, l'original n'est jamais muté. Le numéro de révision s'incrémente
 * à chaque édition effective — les consommateurs distants (pont LSP, agents)
 * datent leurs résultats par instantané et jettent les résultats périmés
 * (ADR-007).</p>
 *
 * <p>Index de lignes (portage du dépôt de départ, registre de portage) :
 * tableau parallèle des débuts de ligne — l'entrée 0 vaut 0, chaque entrée
 * suivante est l'offset qui suit un saut de ligne — donnant la recherche de
 * ligne en O(log n). Le remplacement splice l'index incrémentalement :
 * préfixe inchangé réutilisé, sauts de ligne du remplacement scannés,
 * suffixe décalé du delta d'édition. La {@link Transaction} étant la seule
 * voie de mutation et s'appliquant par {@link #replace}, tout chemin —
 * édition, annulation, refaire — maintient l'index (PIEGE-07).</p>
 *
 * <p>Sûreté de threads : les champs ci-dessous ne sont écrits que par le
 * constructeur et sont finaux — la publication d'un instantané vers un autre
 * fil est garantie (sémantique des champs finaux, JMM §17.5) même si la
 * référence est relayée par un chemin non synchronisé. Le mémo du texte
 * complet est une référence volatile : course bénigne, tous les écrivains
 * recalculent la même chaîne, la lecture volatile garantit qu'un lecteur ne
 * voit jamais un mémo à moitié publié.</p>
 */
public final class Document {

    /** Seuil « gros document » en caractères (portage du dépôt de départ). */
    private static final int LIMITE_GROS_CARACTERES = 2_500_000;

    /** Seuil « gros document » en lignes (portage du dépôt de départ). */
    private static final int LIMITE_GROS_LIGNES = 50_000;

    private final Rope rope;
    private final int[] lineStarts;
    private final int revision;

    /** Mémo du texte complet — chaîne unique, recalculée identique. */
    private @Nullable volatile String cachedText;

    private Document(Rope rope, int[] lineStarts, int revision) {
        this.rope = rope;
        this.lineStarts = lineStarts;
        this.revision = revision;
    }

    /**
     * Crée un instantané du texte donné.
     *
     * @param text le contenu initial, jamais nul (contract {@code @NullMarked})
     */
    public static Document of(String text) {
        Rope rope = Rope.fromString(text);
        return new Document(rope, computeLineStarts(text), 0);
    }

    /** Nombre total de caractères (unités de code UTF-16). */
    public int length() {
        return rope.length();
    }

    /**
     * Contenu complet du document, mémoïsé : la référence est stable sur un
     * même instantané — les lecteurs répétés partagent la même chaîne au
     * lieu de la recalculer.
     *
     * @return le texte complet
     */
    public String text() {
        String memo = cachedText;
        if (memo == null) {
            memo = rope.toString();
            cachedText = memo;
        }
        return memo;
    }

    /** Numéro de révision : zéro à la création, incrémenté par édition. */
    public int revision() {
        return revision;
    }

    /**
     * Extrait la plage {@code [start, end)} sans matérialiser le document
     * entier (descente d'arbre + tranche seule).
     *
     * @param start offset de début (inclusif)
     * @param end   offset de fin (exclusif)
     * @return le texte de la plage
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    public String subText(int start, int end) {
        if (start < 0 || end > rope.length() || start > end) {
            throw new IndexOutOfBoundsException(
                "start=" + start + ", end=" + end + ", length=" + rope.length());
        }
        if (start == end) {
            return "";
        }
        return rope.sub(start, end).toString();
    }

    /**
     * Remplace la plage {@code [start, end)} par l'insertion donnée et
     * retourne le nouvel instantané (révision incrémentée, index de lignes
     * splicé). Une édition sans effet retourne ce même instantané, révision
     * inchangée.
     *
     * @param start     offset de début (inclusif)
     * @param end       offset de fin (exclusif)
     * @param insertion le texte de remplacement
     * @return le nouvel instantané
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    public Document replace(int start, int end, String insertion) {
        if (start < 0 || end > rope.length() || start > end) {
            throw new IndexOutOfBoundsException(
                "start=" + start + ", end=" + end + ", length=" + rope.length());
        }
        if (start == end && insertion.isEmpty()) {
            return this;
        }
        Rope nouvelle = rope.replace(start, end, insertion);
        return new Document(nouvelle, spliceLineStarts(start, end, insertion), revision + 1);
    }

    /** Caractère à l'offset donné (descente d'arbre, O(log n)). */
    public char charAt(int offset) {
        return rope.charAt(offset);
    }

    // ── Requêtes de ligne ─────────────────────────────────────────

    /** Nombre total de lignes : au moins une, même sur document vide. */
    public int lineCount() {
        return lineStarts.length;
    }

    /**
     * Numéro de la ligne contenant l'offset donné — recherche dichotomique
     * sur les débuts de ligne. L'offset du saut de ligne appartient à la
     * ligne qu'il termine ; les bornes sont clampées : négatif vers la
     * première ligne, au-delà de la longueur vers la dernière.
     *
     * @param offset offset de caractère quelconque
     * @return le numéro de ligne, dans {@code [0, lineCount() - 1]}
     */
    public int lineForOffset(int offset) {
        if (offset < 0) {
            return 0;
        }
        if (offset >= rope.length()) {
            return lineStarts.length - 1;
        }
        int bas = 0;
        int haut = lineStarts.length - 1;
        while (bas < haut) {
            int milieu = (bas + haut + 1) >>> 1;
            if (lineStarts[milieu] <= offset) {
                bas = milieu;
            } else {
                haut = milieu - 1;
            }
        }
        return bas;
    }

    /**
     * Offset de début de la ligne donnée (borné : négatif vers la première
     * ligne, au-delà vers la dernière).
     *
     * @param line numéro de ligne quelconque
     * @return l'offset du premier caractère de la ligne
     */
    public int lineStart(int line) {
        return lineStarts[clampLine(line)];
    }

    /**
     * Offset de fin de contenu de la ligne donnée (borné) : l'offset du
     * caractère de saut de ligne, ou la longueur du document pour la
     * dernière ligne.
     *
     * @param line numéro de ligne quelconque
     * @return la fin exclusive du contenu de la ligne
     */
    public int lineEnd(int line) {
        int borne = clampLine(line);
        if (borne + 1 < lineStarts.length) {
            return lineStarts[borne + 1] - 1;
        }
        return rope.length();
    }

    /**
     * Contenu texte de la ligne donnée, sans le saut de ligne final —
     * extraction par tranche, sans matérialiser le document entier.
     *
     * @param line numéro de ligne quelconque
     * @return le contenu de la ligne, vide pour une ligne vide
     */
    public String lineText(int line) {
        int debut = lineStart(line);
        int fin = lineEnd(line);
        if (debut >= fin) {
            return "";
        }
        return subText(debut, fin);
    }

    /**
     * Vrai si ce document dépasse les seuils « gros document » du dépôt de
     * départ : 2,5 M de caractères ou 50 k lignes.
     *
     * @return vrai au-delà des seuils
     */
    public boolean isLarge() {
        return rope.length() > LIMITE_GROS_CARACTERES || lineStarts.length > LIMITE_GROS_LIGNES;
    }

    // ── Maintien de l'index ───────────────────────────────────────

    /**
     * Calcule les débuts de ligne du texte donné : l'entrée 0 vaut 0, chaque
     * entrée suivante est l'offset qui suit un saut de ligne.
     */
    private static int[] computeLineStarts(String text) {
        if (text.isEmpty()) {
            return new int[] {0};
        }
        int[] temporaires = new int[text.length() + 1];
        int nombre = 1;
        temporaires[0] = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                temporaires[nombre++] = i + 1;
            }
        }
        int[] result = new int[nombre];
        System.arraycopy(temporaires, 0, result, 0, nombre);
        return result;
    }

    /**
     * Splice incrémental de l'index pour le remplacement de
     * {@code [start, end)} par l'insertion : préfixe jusqu'à la première
     * ligne touchée réutilisé tel quel, débuts créés par les sauts de ligne
     * de l'insertion, suffixe au-delà de la dernière ligne touchée décalé
     * du delta d'édition.
     */
    private int[] spliceLineStarts(int start, int end, String insertion) {
        int delta = insertion.length() - (end - start);
        int premiereLigne = lineForOffset(start);
        int derniereLigne = (end > start) ? lineForOffset(end) : premiereLigne;

        int sauts = 0;
        for (int i = 0; i < insertion.length(); i++) {
            if (insertion.charAt(i) == '\n') {
                sauts++;
            }
        }

        int lignesDeQueue = lineStarts.length - 1 - derniereLigne;
        int[] nouvelles = new int[premiereLigne + 1 + sauts + lignesDeQueue];
        System.arraycopy(lineStarts, 0, nouvelles, 0, premiereLigne + 1);

        int curseur = premiereLigne + 1;
        for (int i = 0; i < insertion.length(); i++) {
            if (insertion.charAt(i) == '\n') {
                nouvelles[curseur++] = start + i + 1;
            }
        }
        for (int ligne = derniereLigne + 1; ligne < lineStarts.length; ligne++) {
            nouvelles[curseur++] = lineStarts[ligne] + delta;
        }
        return nouvelles;
    }

    private int clampLine(int line) {
        return Math.max(0, Math.min(line, lineStarts.length - 1));
    }
}
