package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.language.LanguagePack;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.language.PackLoader;
import jo.celeris.core.language.PackSource;
import jo.celeris.core.language.Packs;
import jo.celeris.core.syntax.LanguageSyntax;

import org.junit.jupiter.api.Test;

/**
 * L'intégration réelle des paquets intégrés par la fabrique de
 * grammaires (M7.6, chantier C5) : les vingt paquets du module du
 * bas se chargent par la couture {@link PackGrammars} — les quatorze
 * grammaires compilent (la vraie voie, pas la fabrique de banc), et
 * le registre assemblé sert les sources syntaxiques attendues.
 */
final class PackGrammarsTest {

    /** Les vingt paquets intégrés, chargés par la vraie fabrique. */
    private static List<LanguagePack> paquetsCharges() throws IOException {
        PackLoader chargeur = PackLoader.with(PackGrammars.INSTANCE);
        List<LanguagePack> charges = new ArrayList<>();
        for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
            charges.add(chargeur.load(fichiers));
        }
        return charges;
    }

    @Test
    void lesVingtPaquetsSeChargentParLaVraieFabrique() throws IOException {
        List<LanguagePack> charges = paquetsCharges();
        assertEquals(20, charges.size());
        for (LanguagePack pack : charges) {
            assertEquals(1, pack.manifest().schemaVersion(),
                    pack.manifest().id() + " au schéma courant");
        }
    }

    @Test
    void lesQuatorzeGrammairesCompilentEnSourcesSyntaxiques() throws IOException {
        List<LanguagePack> charges = paquetsCharges();
        List<String> aGrammaire = List.of("toml", "yaml", "properties", "markdown",
                "python", "shell", "sql", "c", "cpp", "proguard", "html",
                "css", "javascript", "typescript");
        for (LanguagePack pack : charges) {
            if (aGrammaire.contains(pack.manifest().id())) {
                LanguageSyntax source = pack.grammar();
                assertNotNull(source, pack.manifest().id() + " compile");
                assertEquals(pack.manifest().id(), source.id());
                assertTrue(source.lexer() instanceof GrammaireLexeur,
                        pack.manifest().id() + " sert la voie de grammaires");
            }
        }
    }

    @Test
    void leRegistreAssembleLesVingtLangages() {
        LanguageRegistry registre = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks().build();
        assertEquals(20, registre.supports().size());
        LanguageSupport toml = registre.byId("toml");
        assertNotNull(toml, "toml est là");
        LanguageSyntax source = toml.syntax();
        assertNotNull(source, "toml a sa source");
        assertSame(GrammaireSyntax.class, source.getClass());
        assertEquals("toml", source.id());
    }
}
