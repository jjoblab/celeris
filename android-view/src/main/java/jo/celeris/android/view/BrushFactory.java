package jo.celeris.android.view;

/**
 * Fabrique de pinceaux : chaque couche y prend le sien à sa construction
 * (PIEGE-01 — la couche possède son pinceau).
 */
public interface BrushFactory {

    /**
     * Crée un pinceau neuf pour la couche appelante.
     *
     * @return le pinceau possédé par la couche
     */
    TextBrush newBrush();
}
