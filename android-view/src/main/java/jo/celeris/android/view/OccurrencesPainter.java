package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.Match;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.WordOccurrences;

/**
 * Couche de chrome des OCCURRENCES DU MOT AU CARET (C11, l'analyse
 * croisée) : les exemplaires entiers du mot que touche le caret
 * principal se voilent d'une teinte de sélection discrète — la
 * lecture d'un identifiant répété devient immédiate, le regard suit
 * le mot au lieu de chercher.
 *
 * <p>Le moteur est {@link WordOccurrences} (le portage du dépôt de
 * départ) : frontières d'identifiant, casse exacte, plafond de
 * collecte. La couche s'arme par {@code setOccurrencesVisibles} sur
 * la vue — la teinte vit dans le canal alpha (0x33, trois fois plus
 * discret que la sélection 0x99), se trace SOUS la sélection et le
 * texte, et se TAIT pendant une sélection étendue (le mot parlant
 * par sa sélection) comme sur les non-identifiants (ponctuation,
 * nombres : un « ; » n'allume pas le viewport).</p>
 */
public final class OccurrencesPainter extends PainterLayer {

    /**
     * L'alpha du voile d'occurrence — trois fois plus discret que la
     * sélection : le mot s'illumine sans crier, le texte reste roi.
     */
    static final int ALPHA = 0x33;

    /** Le voile est coupé pendant les sélections étendues. */
    private boolean visible;

    /**
     * Construit la couche d'occurrences, muette à la naissance
     * (l'hôte l'arme par la vue).
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public OccurrencesPainter(BrushFactory brushes) {
        super(brushes);
    }

    /** Arme ou coupe le voile des occurrences. */
    void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // le voile de la teinte de SÉLECTION (pas une encre neuve :
        // le mot répété parle le langage de la sélection, discrètement)
        return (frame.theme().selectionColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        if (!visible) {
            return;
        }
        MultiSelection selection = frame.selection();
        List<SelRange> plages = selection.ranges();
        // un curseur réduit SEUL : une sélection étendue parle déjà
        if (plages.size() != 1 || !plages.get(0).isEmpty()) {
            return;
        }
        int caret = plages.get(0).start();
        Document document = frame.document();
        if (caret > document.length()) {
            return;
        }
        Match mot = WordOccurrences.wordAt(document.text(), caret);
        if (mot == null || !WordOccurrences.isEligibleWord(
                document.text().substring(mot.start(), mot.end()))) {
            return;
        }
        List<Match> occurrences = WordOccurrences.findAll(
                document.text(), document.text().substring(
                        mot.start(), mot.end()));
        for (Match occurrence : occurrences) {
            peindrePlage(surface, frame, occurrence.start(), occurrence.end());
        }
    }

    /** Le voile d'une occurrence : la même géométrie bornée que la
     *  sélection (tranche de rangée, PIEGE-04 et B-05), sans
     *  déborder de la ligne. */
    private void peindrePlage(DrawSurface surface, FrameState frame,
            int debut, int fin) {
        Document document = frame.document();
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        int ligneDebut = document.lineForOffset(debut);
        int ligneFin = document.lineForOffset(fin > debut ? fin - 1 : fin);
        for (int ligne = ligneDebut; ligne <= ligneFin; ligne++) {
            int rangnee = projection.rowOfLine(ligne);
            if (rangnee < premiere || rangnee >= derniere) {
                continue;
            }
            int debutLigne = document.lineStart(ligne);
            int finLigne = document.lineEnd(ligne);
            int colonneDebut = Math.max(debut, debutLigne) - debutLigne;
            int colonneFin = Math.min(fin, finLigne) - debutLigne;
            int debutTranche = projection.spanStartAtRow(rangnee);
            colonneDebut = Math.max(colonneDebut, debutTranche);
            colonneFin = Math.min(colonneFin, projection.spanEndAtRow(rangnee));
            if (colonneFin <= colonneDebut) {
                continue;
            }
            float indent = projection.indentAtRow(rangnee) * metrics.charWidth();
            float gauche = frame.gutterWidth()
                    + indent + (colonneDebut - debutTranche) * metrics.charWidth()
                    - frame.scrollX();
            float haut = metrics.rowTop(rangnee) - frame.scrollY();
            float droite = frame.gutterWidth()
                    + indent + (colonneFin - debutTranche) * metrics.charWidth()
                    - frame.scrollX();
            float bas = haut + metrics.lineHeight();
            surface.drawRect(gauche, haut, droite, bas, brush());
        }
    }
}
