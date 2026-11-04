package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests de la table de raccourcis du clavier matériel (D-17) : table
 * par défaut complète, remappage persistant, conflits détectés.
 *
 * <p>Les codes de touche suivent la numérotation de la plateforme
 * (DPAD, MOVE_HOME, DEL…) ; la classe n'en dépend pas — la vue traduit
 * l'événement platforme en {@link KeyBinding} pur.</p>
 */
class KeyBindingsTest {

    /** D-17 — la table par défaut est complète : chaque action a sa touche. */
    @Test
    void keyboard_defaultTableBindsEveryAction() {
        KeyBindings table = KeyBindings.defaults();
        for (EditorAction action : EditorAction.values()) {
            assertTrue(table.bindingOf(action).isPresent(),
                    "action sans touche : " + action);
            assertEquals(action, table.actionOf(table.bindingOf(action).orElseThrow()),
                    "aller-retour touche → action → touche pour " + action);
        }
        // Quelques touches de référence : DPAD gauche sans modificateur
        // déplace, DEL supprime en arrière, FORWARD_DEL en avant.
        assertEquals(EditorAction.MOVE_LEFT,
                table.actionOf(new KeyBinding(21, false, false, false)));
        assertEquals(EditorAction.DELETE_BACKWARD,
                table.actionOf(new KeyBinding(67, false, false, false)));
        assertEquals(EditorAction.DELETE_FORWARD,
                table.actionOf(new KeyBinding(112, false, false, false)));
        assertEquals(EditorAction.SELECT_ALL,
                table.actionOf(new KeyBinding(29, false, true, false)));
        // Une touche non liée ne fait rien.
        assertNull(table.actionOf(new KeyBinding(9999, false, false, false)));
    }

    /** D-17 — le remappage tient : la nouvelle touche sert l'action, l'ancienne la libère. */
    @Test
    void keyboard_reboundKeyServesTheAction() {
        KeyBindings table = KeyBindings.defaults();
        KeyBindings remappee = table.rebound(EditorAction.MOVE_LEFT,
                new KeyBinding(96, false, false, false));

        assertEquals(EditorAction.MOVE_LEFT,
                remappee.actionOf(new KeyBinding(96, false, false, false)));
        assertNull(remappee.actionOf(new KeyBinding(21, false, false, false)),
                "l'ancienne touche ne sert plus l'action");
        // La table d'origine est intacte (remappage sans mutation).
        assertEquals(EditorAction.MOVE_LEFT,
                table.actionOf(new KeyBinding(21, false, false, false)));
    }

    /** D-17 — les conflits sont détectés : une touche déjà prise est refusée. */
    @Test
    void keyboard_conflictingBindingIsRefused() {
        KeyBindings table = KeyBindings.defaults();
        // 67 sert DELETE_BACKWARD : la lier à MOVE_LEFT est un conflit.
        IllegalStateException conflit = assertThrows(IllegalStateException.class,
                () -> table.rebound(EditorAction.MOVE_LEFT,
                        new KeyBinding(67, false, false, false)));
        assertTrue(conflit.getMessage().contains("DELETE_BACKWARD"),
                "le message nomme l'action en conflit : " + conflit.getMessage());
    }

    /** D-17 — le codage fait le va-et-vient : la table persiste hors de l'objet. */
    @Test
    void keyboard_encodingRoundTrips() {
        KeyBindings remappee = KeyBindings.defaults().rebound(EditorAction.UNDO,
                new KeyBinding(200, false, false, false));
        Map<String, Integer> codee = remappee.encode();

        KeyBindings relue = KeyBindings.decode(codee);

        for (EditorAction action : EditorAction.values()) {
            assertEquals(remappee.bindingOf(action), relue.bindingOf(action),
                    "la table relue sert les mêmes touches pour " + action);
        }
        assertEquals(EditorAction.UNDO,
                relue.actionOf(new KeyBinding(200, false, false, false)));
        // Un nom d'action inconnu est refusé net.
        assertThrows(IllegalArgumentException.class,
                () -> KeyBindings.decode(Map.of("NIMPORTE_QUOI", 21)));
    }
}
