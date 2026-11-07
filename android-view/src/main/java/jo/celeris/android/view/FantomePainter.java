package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.Document;

/**
 * La couche du texte fantôme (F-01, ADR-012) : la suggestion se
 * dessine éphémère au chemin des indices en ligne — une cellule d'encre
 * pleine par caractère, sur la même ligne de base que le code
 * (PIEGE-11), à l'encre fanée.
 *
 * <p>La première ligne se pose à l'ancre (la position du curseur au
 * départ du vol), les suivantes au bord gauche du texte, une rangée
 * chacune — par-dessus le texte en place, l'encre fanée les distingue.
 * Le découpage en lignes se prend une fois pour toutes au dépôt de la
 * suggestion : aucune allocation par image (PIEGE-15). La police de
 * l'image se repose par la couche avant tout tracé (PIEGE-22) — la
 * règle qui mesure est la police qui trace.</p>
 */
final class FantomePainter extends PainterLayer {

    /** L'opacité de l'encre fanée : soixante pour cent de l'encre de base. */
    private static final int OPACITE = 0x99;

    private @Nullable SuggestionFantome suggestion;
    private String[] lignes = new String[0];

    /**
     * Construit la couche du fantôme.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    FantomePainter(BrushFactory brushes) {
        super(brushes);
    }

    /**
     * Dépose (ou retire) la suggestion visible — le découpage en
     * lignes s'y prend, jamais à l'image.
     *
     * @param nouvelle la suggestion à dessiner, ou {@code null} pour
     *                 fermer le fantôme
     */
    void setSuggestion(@Nullable SuggestionFantome nouvelle) {
        suggestion = nouvelle;
        lignes = nouvelle == null ? new String[0] : couperEnLignes(nouvelle.texte());
    }

    /** La suggestion visible, ou {@code null}. */
    @Nullable
    SuggestionFantome suggestion() {
        return suggestion;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return voiler(frame.theme().baseTextColor());
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        SuggestionFantome courante = suggestion;
        if (courante == null) {
            return;
        }
        Document document = frame.document();
        if (courante.offset() > document.length()) {
            return;
        }
        VisualProjection projection = frame.projection();
        EditorMetrics metrics = frame.metrics();
        int rangee = CaretGeometry.rowOfOffset(document, projection,
                courante.offset());
        float x = CaretGeometry.xOfOffset(document, projection, metrics,
                frame.gutterWidth(), frame.scrollX(), courante.offset());
        float baseY = metrics.rowBaseline(rangee) - frame.scrollY();
        for (int i = 0; i < lignes.length; i++) {
            if (i > 0) {
                x = frame.gutterWidth() - frame.scrollX();
            }
            if (!lignes[i].isEmpty()) {
                surface.drawText(lignes[i], x,
                        baseY + i * metrics.lineHeight(), brush());
            }
        }
    }

    /**
     * Le point touche-t-il la première ligne du fantôme ? La cellule
     * d'encre de la première ligne, bord à bord — la cible du geste
     * d'acceptation.
     *
     * @param frame l'état d'image courant
     * @param x     l'abscisse du point, en pixels
     * @param y     l'ordonnée du point, en pixels
     * @return vrai si le geste vise le fantôme
     */
    boolean premiereLigneTouchee(FrameState frame, float x, float y) {
        SuggestionFantome courante = suggestion;
        if (courante == null || lignes.length == 0 || lignes[0].isEmpty()) {
            return false;
        }
        Document document = frame.document();
        if (courante.offset() > document.length()) {
            return false;
        }
        EditorMetrics metrics = frame.metrics();
        int rangee = CaretGeometry.rowOfOffset(document, frame.projection(),
                courante.offset());
        float gauche = CaretGeometry.xOfOffset(document, frame.projection(),
                metrics, frame.gutterWidth(), frame.scrollX(),
                courante.offset());
        float haut = metrics.rowTop(rangee) - frame.scrollY();
        return x >= gauche && x <= gauche + lignes[0].length() * metrics.charWidth()
                && y >= haut && y <= haut + metrics.lineHeight();
    }

    /**
     * L'encre fanée : l'encre de base du thème, voilée d'opacité — le
     * fantôme se lit sans se confondre avec le code.
     */
    private static int voiler(int encre) {
        return (encre & 0x00FFFFFF) | (OPACITE << 24);
    }

    /**
     * Coupe le texte en lignes : le texte vide ne rend aucune ligne, le
     * saut final n'en ouvre pas une vide.
     */
    private static String[] couperEnLignes(String texte) {
        int nombre = 1;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                nombre++;
            }
        }
        String[] coupees = new String[nombre];
        int index = 0;
        int debut = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                coupees[index++] = texte.substring(debut, i);
                debut = i + 1;
            }
        }
        coupees[index] = texte.substring(debut);
        return coupees;
    }
}
