package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests de cohérence des mises en page : chaque vue d'un layout doit
 * porter ses dimensions ({@code layout_width}, {@code layout_height}).
 * Une vue sans dimensions ne fait échouer ni la compilation ni
 * l'assemblage — seule l'inflation à l'exécution meurt, et
 * l'application s'ouvre en arrêt forcé. Ce test dit le silence du
 * build avant l'appareil.
 */
final class LayoutsCoherentsTest {

    /** L'espace de noms Android des attributs de mise en page. */
    private static final String ESPACE_ANDROID =
            "http://schemas.android.com/apk/res/android";

    /** Les balises sans dimensions propres : elles héritent ou
     *  remplacent. */
    private static final Set<String> SANS_DIMENSIONS =
            Set.of("include", "requestFocus", "merge", "fragment");

    @Test
    void chaqueVuePorteSesDimensions() throws Exception {
        List<Path> layouts;
        try (var chemins = Files.list(cheminLayouts())) {
            layouts = chemins.filter(chemin ->
                    chemin.toString().endsWith(".xml")).sorted().toList();
        }
        assertTrue(layouts.size() >= 5,
                "les mises en page de la démonstration doivent être trouvées");
        for (Path layout : layouts) {
            for (Element vue : vuesDe(layout)) {
                String contexte = nomDe(layout) + " : <" + vue.getTagName() + ">";
                assertFalse(
                        vue.getAttributeNS(ESPACE_ANDROID, "layout_width").isBlank(),
                        contexte + " sans layout_width — l'inflation s'arrête");
                assertFalse(
                        vue.getAttributeNS(ESPACE_ANDROID, "layout_height").isBlank(),
                        contexte + " sans layout_height — l'inflation s'arrête");
            }
        }
    }

    @Test
    void lesLayoutsSontBienFormes() throws Exception {
        List<Path> layouts;
        try (var chemins = Files.list(cheminLayouts())) {
            layouts = chemins.filter(chemin ->
                    chemin.toString().endsWith(".xml")).sorted().toList();
        }
        for (Path layout : layouts) {
            assertFalse(vuesDe(layout).isEmpty(),
                    nomDe(layout) + " : aucune vue lue — XML mal formé ?");
        }
    }

    private static Path cheminLayouts() {
        return Path.of("src", "main", "res", "layout");
    }

    private static String nomDe(Path layout) {
        return layout.getFileName().toString();
    }

    private static List<Element> vuesDe(Path layout) throws Exception {
        DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        fabrique.setNamespaceAware(true);
        NodeList tout = fabrique.newDocumentBuilder()
                .parse(layout.toFile()).getElementsByTagName("*");
        List<Element> vues = new ArrayList<>();
        for (int i = 0; i < tout.getLength(); i++) {
            Element vue = (Element) tout.item(i);
            if (!SANS_DIMENSIONS.contains(vue.getTagName())) {
                vues.add(vue);
            }
        }
        return vues;
    }
}
