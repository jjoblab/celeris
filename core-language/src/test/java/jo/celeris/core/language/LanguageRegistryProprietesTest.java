package jo.celeris.core.language;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Les propriétés du registre (M7.6) : la construction refuse toujours
 * deux langages qui revendiquent le même nom exact, et tout nom exact
 * enregistré se détecte toujours — sur des descripteurs arbitraires,
 * pas seulement ceux du banc.
 */
final class LanguageRegistryProprietesTest {

    @Provide
    Arbitrary<String> nomsFichiers() {
        return Arbitraries.of("Dockerfile", "Makefile", "CMakeLists.txt",
                "notes", "gradle.properties", "README");
    }

    @Provide
    Arbitrary<List<String>> listesDeNoms() {
        return Arbitraries.of("Dockerfile", "Makefile", "CMakeLists.txt",
                "notes", "gradle.properties", "README").list();
    }

    private static LanguageSupport supportDunNom(String nom) {
        LanguageDescriptor descriptor = new LanguageDescriptor(
                nom.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#-]", "-"),
                nom, List.of(), List.of(), List.of(nom), List.of(), null,
                List.of(), null, null, "X", 0xFF000000);
        return LanguageSupport.builder(descriptor).build();
    }

    @Property
    void deuxRevendicationsDuMemeNomExactSontToujoursRefusees(
            @ForAll("nomsFichiers") String nom,
            @ForAll("nomsFichiers") String autreNom) {
        LanguageSupport premier = supportDunNom(nom);
        LanguageSupport second = supportDunNom(autreNom);
        boolean memeNom = nom.toLowerCase(Locale.ROOT)
                    .equals(autreNom.toLowerCase(Locale.ROOT));
        boolean memeId = premier.id().equals(second.id());
        if (memeNom || memeId) {
            assertThrows(IllegalArgumentException.class, () ->
                    LanguageRegistry.builder().support(premier).support(second).build());
        } else {
            LanguageRegistry registre = LanguageRegistry.builder()
                    .support(premier).support(second).build();
            assertEquals(premier, registre.byFilename(nom));
            assertEquals(second, registre.byFilename(autreNom));
        }
    }

    @Property
    void toutNomExactEnregistreSeDetecteToujours(
            @ForAll("listesDeNoms") List<String> noms) {
        Set<String> uniques = new LinkedHashSet<>();
        for (String nom : noms) {
            uniques.add(nom.toLowerCase(Locale.ROOT));
        }
        LanguageRegistry.Builder batisseur = LanguageRegistry.builder();
        for (String nom : uniques) {
            batisseur.support(supportDunNom(nom));
        }
        LanguageRegistry registre = batisseur.build();
        for (String nom : uniques) {
            LanguageSupport support = registre.byFilename(nom);
            assertNotNull(support, nom + " devrait se détecter");
            assertTrue(support.descriptor().filenames().contains(nom));
        }
        assertNull(registre.byFilename("inconnu.sans-espoir"));
    }
}
