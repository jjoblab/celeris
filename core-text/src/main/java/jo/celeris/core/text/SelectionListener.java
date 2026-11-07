package jo.celeris.core.text;

/**
 * Auditeur de sélection : notifié de chaque changement de la sélection
 * courante — pose directe, projection par édition, annulation et
 * refaire compris (la voie unique, PIEGE-07).
 *
 * <p>La barre de sélection des popups (M7.5, §7) s'y branche : elle
 * paraît, se repositionne et se ferme au fil des sélections, de toute
 * source — tactile, clavier matériel, IME, accessibilité. L'auditeur
 * n'est appelé que lorsque la sélection CHANGE vraiment : une pose
 * identique ne notifie pas.</p>
 */
@FunctionalInterface
public interface SelectionListener {

    /**
     * Appelé après un changement effectif de la sélection.
     *
     * @param selection la sélection courante, après le changement
     */
    void onSelectionChanged(MultiSelection selection);
}
