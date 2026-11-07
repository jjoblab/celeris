package jo.celeris.android.view;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Table de raccourcis du clavier matériel (D-17) : chaque action a sa
 * touche, le remappage produit une nouvelle table (jamais de mutation),
 * une touche déjà prise par une autre action est refusée en la nommant
 * — le conflit se détecte, il ne se devine pas.
 *
 * <p>Le codage en dictionnaire {@code action → touche compacte} fait le
 * va-et-vient : l'hôte persiste la table hors de l'objet.</p>
 */
public final class KeyBindings {

    private static final int MASQUE_SHIFT = 1;
    private static final int MASQUE_CTRL = 2;
    private static final int MASQUE_ALT = 4;

    private final Map<KeyBinding, EditorAction> parTouche;
    private final Map<EditorAction, KeyBinding> parAction;

    private KeyBindings(Map<KeyBinding, EditorAction> parTouche) {
        this.parTouche = new HashMap<>(parTouche);
        this.parAction = new EnumMap<>(EditorAction.class);
        for (Map.Entry<KeyBinding, EditorAction> entree : parTouche.entrySet()) {
            parAction.put(entree.getValue(), entree.getKey());
        }
    }

    /**
     * La table par défaut : chaque action liée une fois, aucun conflit.
     *
     * <p>Codes plateforme@n@: DPAD 19–22, PAGE_UP 92, PAGE_DOWN 93, DEL
     * 67, FORWARD_DEL 112, MOVE_HOME 122, MOVE_END 123, A 29, Y 52,
     * Z 54. Alt+DPAD déplace par mots, Ctrl+MOVE_HOME/END saute aux
     * bornes du document, Maj étend la sélection.</p>
     *
     * @return la table par défaut
     */
    public static KeyBindings defaults() {
        Map<KeyBinding, EditorAction> table = new HashMap<>();
        lier(table, EditorAction.MOVE_LEFT, new KeyBinding(21, false, false, false));
        lier(table, EditorAction.MOVE_RIGHT, new KeyBinding(22, false, false, false));
        lier(table, EditorAction.MOVE_UP, new KeyBinding(19, false, false, false));
        lier(table, EditorAction.MOVE_DOWN, new KeyBinding(20, false, false, false));
        lier(table, EditorAction.MOVE_WORD_LEFT, new KeyBinding(21, false, false, true));
        lier(table, EditorAction.MOVE_WORD_RIGHT, new KeyBinding(22, false, false, true));
        lier(table, EditorAction.LINE_START, new KeyBinding(122, false, false, false));
        lier(table, EditorAction.LINE_END, new KeyBinding(123, false, false, false));
        lier(table, EditorAction.PAGE_UP, new KeyBinding(92, false, false, false));
        lier(table, EditorAction.PAGE_DOWN, new KeyBinding(93, false, false, false));
        lier(table, EditorAction.DOC_START, new KeyBinding(122, false, true, false));
        lier(table, EditorAction.DOC_END, new KeyBinding(123, false, true, false));
        lier(table, EditorAction.SELECT_LEFT, new KeyBinding(21, true, false, false));
        lier(table, EditorAction.SELECT_RIGHT, new KeyBinding(22, true, false, false));
        lier(table, EditorAction.SELECT_UP, new KeyBinding(19, true, false, false));
        lier(table, EditorAction.SELECT_DOWN, new KeyBinding(20, true, false, false));
        lier(table, EditorAction.SELECT_WORD_LEFT, new KeyBinding(21, true, false, true));
        lier(table, EditorAction.SELECT_WORD_RIGHT, new KeyBinding(22, true, false, true));
        lier(table, EditorAction.SELECT_LINE_START, new KeyBinding(122, true, false, false));
        lier(table, EditorAction.SELECT_LINE_END, new KeyBinding(123, true, false, false));
        lier(table, EditorAction.SELECT_ALL, new KeyBinding(29, false, true, false));
        lier(table, EditorAction.DELETE_BACKWARD, new KeyBinding(67, false, false, false));
        lier(table, EditorAction.DELETE_FORWARD, new KeyBinding(112, false, false, false));
        lier(table, EditorAction.UNDO, new KeyBinding(54, false, true, false));
        lier(table, EditorAction.REDO, new KeyBinding(52, false, true, false));
        lier(table, EditorAction.ACCEPT_FANTOME,
                new KeyBinding(61, false, false, false));
        return new KeyBindings(table);
    }

    /**
     * L'action servie par la touche donnée.
     *
     * @param touche la touche pressée
     * @return l'action liée, null si la touche est libre
     */
    public @Nullable EditorAction actionOf(KeyBinding touche) {
        return parTouche.get(touche);
    }

    /**
     * La touche qui sert l'action donnée.
     *
     * @param action l'action cherchée
     * @return la touche liée, vide si l'action est libre
     */
    public java.util.Optional<KeyBinding> bindingOf(EditorAction action) {
        return java.util.Optional.ofNullable(parAction.get(action));
    }

    /**
     * Remappe l'action sur la touche donnée : une nouvelle table, sans
     * mutation de l'originale@n@; l'ancienne touche de l'action se
     * libère.
     *
     * @param action l'action à relier
     * @param touche la nouvelle touche
     * @return la table remappée
     * @throws IllegalStateException si une AUTRE action tient déjà la
     *                               touche — le conflit se détecte en la
     *                               nommant
     */
    public KeyBindings rebound(EditorAction action, KeyBinding touche) {
        EditorAction tenanciere = parTouche.get(touche);
        if (tenanciere != null && tenanciere != action) {
            throw new IllegalStateException("key " + touche.keyCode()
                    + " already bound to " + tenanciere);
        }
        Map<KeyBinding, EditorAction> table = new HashMap<>(parTouche);
        KeyBinding ancienne = parAction.get(action);
        if (ancienne != null) {
            table.remove(ancienne);
        }
        table.put(touche, action);
        return new KeyBindings(table);
    }

    /**
     * Le codage persistable@n@: nom d'action → touche compactée (code
     * et modificateurs).
     *
     * @return le dictionnaire de la table
     */
    public Map<String, Integer> encode() {
        Map<String, Integer> codee = new HashMap<>();
        for (Map.Entry<EditorAction, KeyBinding> entree : parAction.entrySet()) {
            KeyBinding touche = entree.getValue();
            int compact = touche.keyCode() * 8
                    + (touche.shift() ? MASQUE_SHIFT : 0)
                    + (touche.ctrl() ? MASQUE_CTRL : 0)
                    + (touche.alt() ? MASQUE_ALT : 0);
            codee.put(entree.getKey().name(), compact);
        }
        return codee;
    }

    /**
     * Relit une table codée par {@link #encode()}.
     *
     * @param codee le dictionnaire nom d'action → touche compactée
     * @return la table relue
     * @throws IllegalArgumentException si un nom d'action est inconnu
     */
    public static KeyBindings decode(Map<String, Integer> codee) {
        Map<KeyBinding, EditorAction> table = new HashMap<>();
        for (Map.Entry<String, Integer> entree : codee.entrySet()) {
            EditorAction action;
            try {
                action = EditorAction.valueOf(entree.getKey());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown action: "
                        + entree.getKey(), e);
            }
            int compact = entree.getValue();
            table.put(new KeyBinding(compact / 8, (compact & MASQUE_SHIFT) != 0,
                    (compact & MASQUE_CTRL) != 0, (compact & MASQUE_ALT) != 0), action);
        }
        return new KeyBindings(table);
    }

    private static void lier(Map<KeyBinding, EditorAction> table, EditorAction action,
            KeyBinding touche) {
        table.put(touche, action);
    }
}
