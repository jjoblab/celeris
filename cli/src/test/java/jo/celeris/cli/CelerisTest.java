package jo.celeris.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fumée de la cli : la liste des modules reflète la topologie de
 * l'ADR-002 (les six modules du cœur, aucun module Android) et le point
 * d'entrée annonce la version et les modules sur la sortie standard.
 */
public final class CelerisTest {

    /** Les six modules du cœur sont annoncés, dans l'ordre des dépendances. */
    @Test
    public void lesSixModulesDuCoeurSontAnnonces() {
        List<String> modules = Celeris.modules();
        assertEquals(6, modules.size());
        assertFalse(modules.contains("demo"));
    }

    /** Le point d'entrée écrit la version puis chaque module du cœur. */
    @Test
    public void lePointDEntreeAnnonceLaVersionEtLesModules() {
        PrintStream sortieOriginale = System.out;
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        System.setOut(new PrintStream(tampon, true, StandardCharsets.UTF_8));
        try {
            Celeris.main(new String[0]);
        } finally {
            System.setOut(sortieOriginale);
        }
        String sortie = tampon.toString(StandardCharsets.UTF_8);
        assertTrue(sortie.contains("celeris"), "la bannière doit citer celeris");
        assertTrue(sortie.contains("core-text"), "le premier module doit apparaître");
        assertTrue(sortie.contains("core-ai"), "le dernier module doit apparaître");
    }
}
