package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les plugins isolés (G-04, M5) : l'erreur d'un plugin ne tue jamais
 * l'éditeur — l'extension qui lève est journalisée, les suivantes
 * courent, l'image suivante est intacte. Le journal est une liste de
 * lignes, l'hôte ne connaît ni Android ni le peintre.
 */
class PluginHostTest {

    /** L'extension qui lève est journalisée, les suivantes courent. */
    @Test
    void extensionQuiLeveEstJournalisee() {
        PluginHost hote = new PluginHost();
        List<String> peints = new ArrayList<>();
        hote.enregistrer(image -> peints.add("premier:" + image));
        hote.enregistrer(image -> {
            throw new IllegalStateException("pan");
        });
        hote.enregistrer(image -> peints.add("troisieme:" + image));
        boolean toutes = hote.executer("image-1");
        assertFalse(toutes, "la défaillance se sait");
        assertEquals(List.of("premier:image-1", "troisieme:image-1"), peints);
        assertEquals(1, hote.journal().size());
        assertTrue(hote.journal().get(0).contains("pan"));
    }

    /** L'image suivante est intacte : l'erreur ne se propage pas. */
    @Test
    void imageSuivanteIntacte() {
        PluginHost hote = new PluginHost();
        List<String> peints = new ArrayList<>();
        hote.enregistrer(image -> {
            throw new RuntimeException("toujours pan");
        });
        hote.enregistrer(image -> peints.add(image));
        hote.executer("image-1");
        hote.executer("image-2");
        assertEquals(List.of("image-1", "image-2"), peints);
        assertEquals(2, hote.journal().size());
    }

    /** Un hôte sans défaillance rend vrai et ne journalise rien. */
    @Test
    void sansDefaillanceRendVrai() {
        PluginHost hote = new PluginHost();
        hote.enregistrer(image -> {
        });
        assertTrue(hote.executer("image"));
        assertTrue(hote.journal().isEmpty());
    }

    /** Le journal est borné : les lignes anciennes tombent. */
    @Test
    void journalBorne() {
        PluginHost hote = new PluginHost(3);
        hote.enregistrer(image -> {
            throw new RuntimeException("pan");
        });
        for (int i = 0; i < 5; i++) {
            hote.executer("image-" + i);
        }
        assertEquals(3, hote.journal().size());
    }
}
