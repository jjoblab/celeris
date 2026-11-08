package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le comportement piloté par la configuration (M7.6) : UN SEUL test
 * paramétré sur TOUS les langages intégrés — commentaires, fermeture
 * automatique, indentation, motifs compilables. Ajouter un langage
 * ajoute une ligne de données dans son paquet, jamais une classe de
 * test ; le paramétrage suit l'index des paquets.
 */
final class PaquetsIntegresParametresTest {

    /** La fabrique de banc : la grammaire exacte n'importe pas ici. */
    private static final GrammarFactory FABRIQUE =
            (id, json) -> jo.celeris.core.syntax.JavaSyntax.INSTANCE;

    /** Les identifiants des paquets intégrés, par l'index du module. */
    static Stream<String> idsDesPaquets() throws IOException {
        return Packs.classpath("langages").packs().stream()
                .map(PackSource.PackFiles::name)
                .sorted();
    }

    private static LanguagePack paquet(String id) {
        try {
            PackLoader chargeur = PackLoader.with(FABRIQUE);
            for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
                LanguagePack pack = chargeur.load(fichiers);
                if (pack.manifest().id().equals(id)) {
                    return pack;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("paquet inconnu : " + id);
    }

    /** Les commentaires, la fermeture, l'indentation : une seule passe. */
    @ParameterizedTest(name = "configuration de {0}")
    @MethodSource("idsDesPaquets")
    void laConfigurationDuPaquetEstCoherente(String id) {
        LanguagePack pack = paquet(id);
        LanguageConfiguration configuration = pack.configuration();
        List<String> attendusAvecCommentaire = List.of("java", "kotlin", "kts",
                "groovy", "xml", "toml", "yaml", "properties", "markdown",
                "python", "shell", "sql", "c", "cpp", "proguard");
        if (attendusAvecCommentaire.contains(id)) {
            assertTrue(configuration.comments().commentable(),
                    id + " déclare ses commentaires");
        } else {
            assertFalse(configuration.comments().commentable(),
                    id + " n'a pas de commentaire");
        }
        assertFalse(configuration.effectiveAutoClosingPairs().isEmpty(),
                id + " ferme ses paires");
        assertTrue(configuration.indentSize() >= 1 && configuration.indentSize() <= 16,
                id + " indente d'une taille honnête");
        assertNotNull(configuration.brackets(), id + " a des crochets");
        seCompile(id, configuration.wordPattern());
        seCompile(id, configuration.indentRules().increase());
        seCompile(id, configuration.indentRules().decrease());
        seCompile(id, configuration.indentRules().indentNextLine());
        seCompile(id, configuration.indentRules().unindentedLine());
        seCompile(id, pack.descriptor().firstLine());
        seCompile(id, pack.descriptor().contentHint());
        for (String motif : pack.descriptor().filenamePatterns()) {
            assertFalse(motif.isEmpty(), id + " : motif de nom non vide");
        }
    }

    /** Une expression du paquet se compile — ou le paquet se refuse. */
    private static void seCompile(String id, @Nullable String expression) {
        if (expression == null) {
            return;
        }
        try {
            Pattern.compile(expression);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    id + " : motif invalide " + expression, e);
        }
    }
}
