package jo.celeris.core.language;

import java.util.Map;
import java.util.Set;

import jo.celeris.core.syntax.clike.GroovySyntax;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.json.JsonSyntax;
import jo.celeris.core.syntax.clike.KotlinSyntax;
import jo.celeris.core.syntax.clike.KtsSyntax;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.xml.XmlSyntax;







import org.jspecify.annotations.Nullable;

/**
 * L'enregistrement des analyseurs écrits à la main (M7.6) : les six
 * langages à source syntaxique dédiée (Java, Kotlin, KTS, Groovy, XML,
 * JSON) déclarent ici leur analyseur — l'équivalent Java d'un paquet.
 * C'est du code d'enregistrement : le contrôle d'architecture
 * (check-language-registration) y autorise les littéraux
 * d'identifiant, comme dans les paquets, et nulle part ailleurs.
 */
public final class WrittenLanguages {

    private static final Map<String, LanguageSyntax> SYNTAXES = Map.of(
            "java", JavaSyntax.INSTANCE,
            "kotlin", KotlinSyntax.INSTANCE,
            "kts", KtsSyntax.INSTANCE,
            "groovy", GroovySyntax.INSTANCE,
            "xml", XmlSyntax.INSTANCE,
            "json", JsonSyntax.INSTANCE);

    private WrittenLanguages() {
    }

    /**
     * La source syntaxique écrite d'un langage.
     *
     * @param id l'identifiant du langage
     * @return son analyseur, ou null si le langage n'en a pas
     */
    public static @Nullable LanguageSyntax syntaxOf(String id) {
        return SYNTAXES.get(id);
    }

    /** Les identifiants des langages à analyseur écrit. */
    public static Set<String> ids() {
        return SYNTAXES.keySet();
    }
}
