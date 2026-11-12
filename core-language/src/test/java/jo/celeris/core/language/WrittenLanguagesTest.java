package jo.celeris.core.language;

import jo.celeris.core.syntax.clike.GroovySyntax;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.clike.KotlinSyntax;
import jo.celeris.core.syntax.clike.KtsSyntax;
import jo.celeris.core.syntax.json.JsonSyntax;
import jo.celeris.core.syntax.xml.XmlSyntax;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;






import jo.celeris.core.syntax.LanguageSyntax;


import org.junit.jupiter.api.Test;

/**
 * Les tests de l'enregistrement des analyseurs écrits (M7.6) : les six
 * langages à source dédiée résolvent leur analyseur, et rien d'autre
 * n'en résout — l'enregistrement est exhaustif et fermé.
 */
final class WrittenLanguagesTest {

    @Test
    void lesSixLangagesEcritsResolutionnentLeurAnalyseur() {
        assertSame(JavaSyntax.INSTANCE, WrittenLanguages.syntaxOf("java"));
        assertSame(KotlinSyntax.INSTANCE, WrittenLanguages.syntaxOf("kotlin"));
        assertSame(KtsSyntax.INSTANCE, WrittenLanguages.syntaxOf("kts"));
        assertSame(GroovySyntax.INSTANCE, WrittenLanguages.syntaxOf("groovy"));
        assertSame(XmlSyntax.INSTANCE, WrittenLanguages.syntaxOf("xml"));
        assertSame(JsonSyntax.INSTANCE, WrittenLanguages.syntaxOf("json"));
    }

    @Test
    void lAnalyseurPorteLidentifiantDeSonLangage() {
        for (String id : WrittenLanguages.ids()) {
            LanguageSyntax analyseur = WrittenLanguages.syntaxOf(id);
            assertNotNull(analyseur, id);
            assertEquals(id, analyseur.id());
        }
    }

    @Test
    void sixIdentifiantsNiPlusNiMoins() {
        assertEquals(Set.of("java", "kotlin", "kts", "groovy", "xml", "json"),
                WrittenLanguages.ids());
    }

    @Test
    void unLangageSansAnalyseurEcritRendNull() {
        assertNull(WrittenLanguages.syntaxOf("python"));
        assertNull(WrittenLanguages.syntaxOf(""));
    }
}
