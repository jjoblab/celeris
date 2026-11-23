package jo.celeris.core.syntax.treesitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.EnginePathFactory;
import jo.celeris.core.text.TextSession;

/**
 * Le banc de la MÉCANIQUE de la voie (ADR-0024, généralisée
 * ADR-0033) : le module opt-in s'annonce par ServiceLoader, sa
 * fabrique rend null sans liaison chargée (JVM sans Android — le
 * repli des paquets sert) — et le pont, qui ne connaît que
 * l'interface, vit sa voie des grammaires sans jamais savoir que
 * la voie existe. La séparation se PROUVE. La fabrique
 * n'INSCRIT plus rien toute seule (ADR-0033) : l'enregistrement
 * des grammaires vit chez l'hôte, le registre reste vide —
 * l'inscription volontaire se banc en TreeSitterGrammarsTest.
 */
final class TreeSitterPathFactoryTest {

    /** Le module s'annonce : ServiceLoader trouve LA fabrique —
     *  l'inscription META-INF vit, la voie est embarquable. */
    @Test
    void leModuleSannonceParServiceLoader() {
        EnginePathFactory fabrique = ServiceLoader.load(
                EnginePathFactory.class,
                EnginePathFactory.class.getClassLoader())
                .findFirst().orElse(null);
        assertNotNull(fabrique,
                "la fabrique de la voie doit vivre sur le classpath du module");
        assertTrue(fabrique instanceof TreeSitterPathFactory,
                "la fabrique chargée est celle du module opt-in");
    }

    /** Sans bibliothèque partagée, la fabrique rend null — le pont
     *  branche la voie des grammaires, personne ne change. */
    @Test
    void sansBibliothequeLaFabriqueRendNull() {
        TreeSitterPathFactory fabrique = new TreeSitterPathFactory();
        TextSession session = TextSession.of("");
        assertNull(fabrique.create(null, session, null),
                "sans source de la voie, la fabrique ne fabrique rien");
    }

    /** La fabrique n'inscrit PLUS rien au registre de la plateforme
     *  (ADR-0033 : le choix vit chez l'hôte) — le registre reste
     *  vide après fabrication, même quand la bibliothèque vit. */
    @Test
    void laFabriqueNinscritPlusRienAuRegistre() {
        TreeSitterPathFactory fabrique = new TreeSitterPathFactory();
        TextSession session = TextSession.of("");
        fabrique.create(null, session, null);
        assertNull(jo.celeris.core.language.packs.TreeSitterLanguages
                        .syntaxOf("json"),
                "la fabrique ne registre plus rien — l'enregistrement vit chez l'hôte");
        assertNull(jo.celeris.core.language.packs.TreeSitterLanguages
                        .syntaxOf("python"),
                "la fabrique ne registre plus rien — l'enregistrement vit chez l'hôte");
    }
}
