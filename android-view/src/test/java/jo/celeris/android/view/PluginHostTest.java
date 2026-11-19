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
        PluginHost host = new PluginHost();
        List<String> peints = new ArrayList<>();
        host.enregistrer(image -> peints.add("premier:" + image));
        host.enregistrer(image -> {
            throw new IllegalStateException("pan");
        });
        host.enregistrer(image -> peints.add("troisieme:" + image));
        boolean toutes = host.executer("image-1");
        assertFalse(toutes, "la défaillance se sait");
        assertEquals(List.of("premier:image-1", "troisieme:image-1"), peints);
        assertEquals(1, host.journal().size());
        assertTrue(host.journal().get(0).contains("pan"));
    }

    /** L'image suivante est intacte : l'erreur ne se propage pas. */
    @Test
    void imageSuivanteIntacte() {
        PluginHost host = new PluginHost();
        List<String> peints = new ArrayList<>();
        host.enregistrer(image -> {
            throw new RuntimeException("toujours pan");
        });
        host.enregistrer(image -> peints.add(image));
        host.executer("image-1");
        host.executer("image-2");
        assertEquals(List.of("image-1", "image-2"), peints);
        assertEquals(2, host.journal().size());
    }

    /** Un hôte sans défaillance rend vrai et ne journalise rien. */
    @Test
    void sansDefaillanceRendVrai() {
        PluginHost host = new PluginHost();
        host.enregistrer(image -> {
        });
        assertTrue(host.executer("image"));
        assertTrue(host.journal().isEmpty());
    }

    /** Le journal est borné : les lignes anciennes tombent. */
    @Test
    void journalBorne() {
        PluginHost host = new PluginHost(3);
        host.enregistrer(image -> {
            throw new RuntimeException("pan");
        });
        for (int i = 0; i < 5; i++) {
            host.executer("image-" + i);
        }
        assertEquals(3, host.journal().size());
    }

    /** D13 (l'analyse croisée) : une extension qui s'enregistre
     *  PENDANT l'exécution ne casse plus l'itération — la liste
     *  d'écouteurs nue jetait ConcurrentModificationException ; la
     *  copie du tour la rend sûre (l'inscription prendra effet au
     *  tour suivant). */
    @Test
    void inscriptionPendantExecutionNeCassePasLeTour() {
        PluginHost host = new PluginHost();
        List<String> peints = new ArrayList<>();
        host.enregistrer(image -> peints.add("premier:" + image));
        // la 2e extension s'enregistre PENDANT son propre tour : la
        // liste change sous les pieds de l'itérateur
        host.enregistrer(image -> {
            peints.add("deuxieme:" + image);
            host.enregistrer(autre -> peints.add("tardif:" + autre));
        });
        boolean toutes = host.executer("image-1");
        assertTrue(toutes, "aucune défaillance");
        assertEquals(List.of("premier:image-1", "deuxieme:image-1"), peints,
                "le tour ne casse pas, la tardive attend le suivant");
        // et l'image suivante sert la tardive
        host.executer("image-2");
        assertTrue(peints.contains("tardif:image-2"));
    }
}
