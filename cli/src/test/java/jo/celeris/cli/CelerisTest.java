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
 * l'ADR-002 (les sept modules du cœur, aucun module Android) et le point
 * d'entrée annonce la version et les modules sur la sortie standard.
 */
public final class CelerisTest {

    /** Les sept modules du cœur sont annoncés, dans l'ordre des dépendances. */
    @Test
    public void lesSeptModulesDuCoeurSontAnnonces() {
        List<String> modules = Celeris.modules();
        assertEquals(7, modules.size());
        assertFalse(modules.contains("demo"));
        assertTrue(modules.contains("core-syntax-grammars"),
                "la voie de grammaires fait partie du cœur");
    }

    /** Le point d'entrée écrit la version puis chaque module du cœur. */
    @Test
    public void lePointDEntreeAnnonceLaVersionEtLesModules() {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        int code = Celeris.executer(new String[0],
                new PrintStream(tampon, true, StandardCharsets.UTF_8));
        assertEquals(0, code, "sans argument : succès");
        String sortie = tampon.toString(StandardCharsets.UTF_8);
        assertTrue(sortie.contains("celeris"), "la bannière doit citer celeris");
        assertTrue(sortie.contains("core-text"), "le premier module doit apparaître");
        assertTrue(sortie.contains("core-ai"), "le dernier module doit apparaître");
    }

    /**
     * La bannière épingle la version de la sortie courante — le cliquet
     * du chantier C3 : la constante datait de l'avant-publication.
     */
    @Test
    public void laBanniereAnnonceLaVersionDeLaSortie() {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        Celeris.executer(new String[0],
                new PrintStream(tampon, true, StandardCharsets.UTF_8));
        assertTrue(tampon.toString(StandardCharsets.UTF_8).startsWith("celeris 1.0.0"),
                "la bannière doit annoncer la version du dépôt");
    }
}
