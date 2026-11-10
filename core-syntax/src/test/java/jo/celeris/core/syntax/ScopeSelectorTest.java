package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les tests du sélecteur de portées (M7.6, H-07) : le format TextMate
 * — virgules, descendance, exclusion, préfixe hiérarchique — et le
 * score qui départage les règles : la profondeur d'abord, le nombre
 * d'éléments couplés ensuite.
 */
final class ScopeSelectorTest {

    private static final List<String> PILE = List.of(
            "source.python", "meta.function.python",
            "entity.name.function.python");

    @Test
    void lElementMatcheParPrefixeHierarchique() {
        assertNotNull(ScopeSelector.parse("entity.name").score(PILE));
        assertNotNull(ScopeSelector.parse("entity.name.function").score(PILE));
        assertNull(ScopeSelector.parse("entity.namef").score(PILE),
                "le préfixe s'arrête au point, jamais dans le mot");
        assertNull(ScopeSelector.parse("entity.name.tag").score(PILE));
    }

    @Test
    void laDescendanceExigeLOrdrePasLaProximite() {
        assertNotNull(ScopeSelector.parse("meta.function entity.name")
                .score(PILE), "l'élément fils matche plus profond que le père");
        assertNotNull(ScopeSelector.parse("source.python entity.name.function")
                .score(PILE), "la descendance saute les étages intermédiaires");
        assertNull(ScopeSelector.parse("entity.name meta.function").score(PILE),
                "l'ordre descendant se refuse à l'inverse");
    }

    @Test
    void lexclusionEcarteLaPileQuiLaPorte() {
        assertNotNull(ScopeSelector.parse("entity.name.function").score(PILE));
        assertNull(ScopeSelector.parse("entity.name.function - meta.function")
                .score(PILE), "la pile porte l'exclusion : pas de match");
        assertNotNull(ScopeSelector.parse("entity.name.function - meta.tag")
                .score(PILE), "l'exclusion absente laisse passer");
    }

    @Test
    void lesVirgulesSeparentLesSelecteursComplexes() {
        assertNotNull(ScopeSelector.parse("keyword, entity.name.function")
                .score(PILE));
        assertNotNull(ScopeSelector.parse("string.quoted, comment")
                .score(List.of("source.demo", "string.quoted.double")));
    }

    @Test
    void leScoreCroitAvecLaProfondeurEtLesElements() {
        Integer profondSeul =
                ScopeSelector.parse("entity.name.function").score(PILE);
        Integer profondDouble =
                ScopeSelector.parse("meta.function entity.name.function")
                        .score(PILE);
        Integer haut =
                ScopeSelector.parse("source.python").score(PILE);
        assertNotNull(profondSeul);
        assertNotNull(profondDouble);
        assertNotNull(haut);
        assertTrue(profondDouble > profondSeul,
                "deux éléments couplés comptent plus qu'un seul");
        assertTrue(profondSeul > haut,
                "la profondeur l'emporte sur la déclaration");
    }

    @Test
    void leSelecteurMalFormeSeRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> ScopeSelector.parse(""));
        assertThrows(IllegalArgumentException.class,
                () -> ScopeSelector.parse("a, , b"));
        assertThrows(IllegalArgumentException.class,
                () -> ScopeSelector.parse("- seul"));
    }

    @Test
    void leTexteDuSelecteurSurvitAuVoyage() {
        assertEquals("meta.function entity.name.function - meta.tag",
                ScopeSelector.parse(
                        "meta.function entity.name.function - meta.tag")
                        .toString());
        assertEquals("keyword, string",
                ScopeSelector.parse("keyword, string").toString());
    }
}
