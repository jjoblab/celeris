package jo.celeris.core.text;

/**
 * Projecteur de positions : état porteur d'offsets (diagnostics, plis,
 * composition IME, sélections) projeté à travers chaque édition appliquée
 * par la voie de transaction unique (ADR-003).
 *
 * <p>Le projecteur reçoit la {@link EditSpan plage appliquée} de CHAQUE
 * édition, y compris lors du rejeu de l'annulation et du refaire
 * (PIEGE-07) : un chemin de mutation direct désynchroniserait silencieusement
 * les offsets portés.</p>
 */
public interface PositionProjector {

    /**
     * Projette l'état porté à travers une édition appliquée.
     *
     * @param span la plage de l'édition qui vient d'être appliquée
     */
    void project(EditSpan span);
}
