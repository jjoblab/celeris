package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

/**
 * Les règles de conservation vivent DANS le jar publié (chantier B2
 * de la revue du 2026-11-17) : R8 lit META-INF/proguard/ des jars de
 * dépendances — l'hôte qui active la réduction n'a plus à copier le
 * fichier à la main (le point 11 de la revue). Le banc lit le JAR
 * compilé du module (le même que la publication emballe) et prouve :
 * l'entrée existe, son contenu est l'octet près celui de la source
 * unique à la racine du module (l'exemplenstration, hors périmètre,
 * l'y lit encore), et la règle garde bien le fil réflexif de l'ADR-015.
 */
final class ReglesR8EmbarqueesTest {

    /** Le jar compilé du module — build/libs/lsp-protocole-*.jar. */
    private static Path jarCompile() throws IOException {
        try (Stream<Path> fichiers = Files.list(
                Paths.get("build", "libs"))) {
            List<Path> jars = fichiers
                    .filter(f -> f.getFileName().toString()
                            .startsWith("lsp-protocole-")
                            && f.getFileName().toString().endsWith(".jar")
                            && !f.getFileName().toString()
                                    .contains("-sources")
                            && !f.getFileName().toString()
                                    .contains("-javadoc"))
                    .toList();
            assertEquals(1, jars.size(),
                    "un seul jar compilé attendu dans build/libs");
            return jars.get(0);
        }
    }

    /** L'entrée META-INF/proguard/celeris-lsp.pro vit dans le jar,
     *  identique à la source à l'octet près. */
    @Test
    void leJarPubliePorteLesRegles() throws IOException {
        Path jar = jarCompile();
        try (ZipFile archive = new ZipFile(jar.toFile())) {
            ZipEntry entree = archive.getEntry(
                    "META-INF/proguard/celeris-lsp.pro");
            assertNotNull(entree,
                    "le jar porte META-INF/proguard/celeris-lsp.pro");
            String embarquee = lire(archive.getInputStream(entree));
            String source = Files.readString(
                    Paths.get("regles-conservation.pro"),
                    StandardCharsets.UTF_8);
            assertEquals(source, embarquee,
                    "la règle embarquée est la source à l'octet près");
        }
    }

    /** La règle garde le fil réflexif de la frontière (ADR-015) : le
     *  rouage JSON, le cadre jsonrpc et les attributs de signature —
     *  le contenu se vérifie, pas seulement la présence. */
    @Test
    void lesReglesGardentLeFilReflexif() throws IOException {
        Path jar = jarCompile();
        try (ZipFile archive = new ZipFile(jar.toFile())) {
            ZipEntry entree = archive.getEntry(
                    "META-INF/proguard/celeris-lsp.pro");
            assertNotNull(entree, "l'entrée existe (re-vérifiée ici)");
            String regles = lire(archive.getInputStream(entree));
            assertTrue(regles.contains("-keep class org.eclipse.lsp4j.**"),
                    "la bibliothèque de la frontière se garde");
            assertTrue(regles.contains(
                    "-keep class com.google.gson.**"),
                    "le rouage JSON se garde");
            assertTrue(regles.contains(
                    "-keepattributes Signature,InnerClasses"),
                    "les attributs réflexifs se gardent");
        }
    }

    private static String lire(InputStream flot) throws IOException {
        return new String(flot.readAllBytes(), StandardCharsets.UTF_8);
    }
}
