package jo.celeris.android.view;

/**
 * Une touche du clavier matériel : le code de touche et ses
 * modificateurs — valeur immuable, comparable par valeur.
 *
 * <p>Les codes suivent la numérotation de la plateforme (DPAD, DEL,
 * lettres…) ; la classe n'en dépend pas : la vue traduit l'événement
 * plateforme en cette valeur pure.</p>
 *
 * @param keyCode le code de touche plateforme
 * @param shift   vrai si Maj est tenu
 * @param ctrl    vrai si Ctrl est tenu
 * @param alt     vrai si Alt est tenu
 */
public record KeyBinding(int keyCode, boolean shift, boolean ctrl, boolean alt) {

    /** Valide la touche. */
    public KeyBinding {
        if (keyCode <= 0) {
            throw new IllegalArgumentException("keyCode must be positive: " + keyCode);
        }
    }
}
