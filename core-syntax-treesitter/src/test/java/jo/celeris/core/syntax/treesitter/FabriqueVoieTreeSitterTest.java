package jo.celeris.core.syntax.treesitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.FabriqueMoteurVoie;
import jo.celeris.core.text.TextSession;

/**
 * Le banc de la MÉCANIQUE de la voie (ADR-0024) : le module opt-in
 * s'annonce par ServiceLoader, sa fabrique rend null sans
 * bibliothèque partagée (le repli des paquets sert) — et le pont,
 * qui ne connaît que l'interface, vit sa voie des grammaires sans
 * jamais savoir que la voie existe. La séparation se PROUVE.
 */
final class FabriqueVoieTreeSitterTest {

    /** Le module s'annonce : ServiceLoader trouve LA fabrique —
     *  l'inscription META-INF vit, la voie est embarquable. */
    @Test
    void leModuleSannonceParServiceLoader() {
        FabriqueMoteurVoie fabrique = ServiceLoader.load(
                FabriqueMoteurVoie.class,
                FabriqueMoteurVoie.class.getClassLoader())
                .findFirst().orElse(null);
        assertNotNull(fabrique,
                "la fabrique de la voie doit vivre sur le classpath du module");
        assertTrue(fabrique instanceof FabriqueVoieTreeSitter,
                "la fabrique chargée est celle du module opt-in");
    }

    /** Sans bibliothèque partagée, la fabrique rend null — le pont
     *  branche la voie des grammaires, personne ne change. */
    @Test
    void sansBibliothequeLaFabriqueRendNull() {
        FabriqueVoieTreeSitter fabrique = new FabriqueVoieTreeSitter();
        TextSession session = TextSession.of("");
        assertNull(fabrique.creer(null, session, null),
                "sans source de la voie, la fabrique ne fabrique rien");
    }

    /** L'inscription au registre de la plateforme ne casse rien sans
     *  bibliothèque : rien ne s'inscrit, le registre reste vide. */
    @Test
    void lInscriptionSansBibliothequeResteVide() {
        FabriqueVoieTreeSitter fabrique = new FabriqueVoieTreeSitter();
        TextSession session = TextSession.of("");
        fabrique.creer(null, session, null);   // déclenche l'inscription
        assertNull(jo.celeris.core.language.packs.TreeSitterLanguages
                        .syntaxOf("json"),
                "sans bibliothèque, le registre reste vide — le repli sert");
    }
}
