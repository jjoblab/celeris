package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jo.celeris.core.syntax.LanguageSyntax;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Les champs du paquet (M7.6) : les noms de VS Code se lisent — alias,
 * types MIME, motifs de mot, plis (offSide et marqueurs), règles
 * d'indentation, contextes d'exclusion, extension au point de tête —
 * et les valeurs fausses se refusent avec le champ en cause. Un seul
 * banc passe sur tous les champs : le format se prouve une fois, pas
 * une fois par langage.
 */
final class PackChampsTest {

    /** La fabrique de banc — la grammaire n'entre pas en jeu ici. */
    private static final GrammarFactory FABRIQUE =
            (id, json) -> jo.celeris.core.syntax.JavaSyntax.INSTANCE;

    @TempDir
    Path bac;

    private LanguagePack paquetAvec(String languageJson) throws IOException {
        Path dossier = bac.resolve("champs");
        Files.createDirectories(dossier);
        Files.writeString(dossier.resolve("pack.json"), """
                {"id": "champs", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("language.json"), languageJson,
                StandardCharsets.UTF_8);
        return PackLoader.with(FABRIQUE).load(
                Packs.folder(bac).packs().get(0));
    }

    @Test
    void lesChampsDeVsCodeSeLisent() throws IOException {
        LanguagePack pack = paquetAvec("""
                {"id": "champs", "name": "Champs", "aliases": ["Ch", "CH"],
                 "extensions": [".ch", "champs"],
                 "mimetypes": ["text/x-champs"],
                 "wordPattern": "[A-Za-z][A-Za-z0-9_]*",
                 "comments": {"lineComment": "# ",
                              "blockComment": ["/*", "*/"]},
                 "brackets": [["(", ")"]],
                 "autoClosingPairs": [{"open": "(", "close": ")",
                                        "notIn": ["string", "comment"]}],
                 "surroundingPairs": [["(", ")"]],
                 "folding": {"offSide": true,
                             "markers": {"start": "^\\\\s*#region",
                                          "end": "^\\\\s*#endregion"}},
                 "indentationRules": {"increaseIndentPattern": "\\\\{$",
                                       "decreaseIndentPattern": "^\\\\s*\\\\}"},
                 "indentSize": 2, "tabIndent": true,
                 "syntax": {"preferred": "grammar"}}
                """);
        LanguageDescriptor descripteur = pack.descriptor();
        assertEquals(List.of("Ch", "CH"), descripteur.aliases());
        assertEquals(List.of("ch", "champs"), descripteur.extensions(),
                "le point de tête de VS Code se retire");
        assertEquals(List.of("text/x-champs"), descripteur.mimeTypes());
        assertEquals("[A-Za-z][A-Za-z0-9_]*", pack.configuration().wordPattern());
        LanguageConfiguration configuration = pack.configuration();
        assertEquals("# ", configuration.comments().lineComment());
        assertEquals("/*", configuration.comments().blockStart());
        assertTrue(configuration.folding().offSide());
        assertEquals("^\\s*#region", configuration.folding().beginMarker());
        assertEquals("^\\s*#endregion", configuration.folding().endMarker());
        assertEquals("\\{$", configuration.indentRules().increase());
        assertEquals("^\\s*\\}", configuration.indentRules().decrease());
        assertEquals(2, configuration.indentSize());
        assertTrue(configuration.tabIndent());
        assertEquals(1, configuration.brackets().size());
        assertEquals(1, configuration.autoClosingPairs().size());
        assertTrue(configuration.autoClosingPairs().get(0).notIn()
                .containsAll(List.of("string", "comment")));
    }

    @Test
    void leBadgeParDefautSeDeriveDeLidentifiant() throws IOException {
        LanguagePack pack = paquetAvec("""
                {"id": "champs", "name": "Champs", "extensions": [".ch"],
                 "syntax": {"preferred": "grammar"}}
                """);
        assertEquals("CH", pack.descriptor().badge());
        assertEquals(0xFF9AA0A6, pack.descriptor().badgeColor());
    }

    @Test
    void unBlocDeCommentaireMalformeSeRefuse() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> paquetAvec("""
                {"id": "champs", "name": "Champs", "extensions": [".ch"],
                 "comments": {"blockComment": ["/*"]},
                 "syntax": {"preferred": "grammar"}}
                """));
    }

    @Test
    void unePaireMalFormeeSeRefuse() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> paquetAvec("""
                {"id": "champs", "name": "Champs", "extensions": [".ch"],
                 "brackets": [["("]],
                 "syntax": {"preferred": "grammar"}}
                """));
    }

    @Test
    void uneTailleIndentationDebordeeSeRefuse() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> paquetAvec("""
                {"id": "champs", "name": "Champs", "extensions": [".ch"],
                 "indentSize": 32,
                 "syntax": {"preferred": "grammar"}}
                """));
    }

    @Test
    void uneExtensionVideSeRefuse() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> paquetAvec("""
                {"id": "champs", "name": "Champs", "extensions": ["."],
                 "syntax": {"preferred": "grammar"}}
                """));
    }

    /** Les invariants des petits enregistrements du modèle. */
    @Test
    void lesModelesRefusentLeursVides() {
        assertThrows(IllegalArgumentException.class,
                () -> new PackManifest("", "1", 1, "X", "banc"));
        assertThrows(IllegalArgumentException.class,
                () -> new PackManifest("id", "", 1, "X", "banc"));
        assertThrows(IllegalArgumentException.class,
                () -> new PackManifest("id", "1", 1, "", "banc"));
        assertThrows(IllegalArgumentException.class,
                () -> new PackManifest("id", "1", 1, "X", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new PackSnippet("nom", "pre", List.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new PackSnippet("", "pre", List.of("x"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageInjection("", "java"));
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageInjection("text.x", ""));
    }

    /** Le descripteur : identifiants valides, familles sans boucle. */
    @Test
    void leDescripteurValideSesChamps() {
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageDescriptor("Java", "Java", List.of(),
                        List.of(), List.of(), List.of(), null, List.of(),
                        null, null, "Jv", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageDescriptor("java", "", List.of(),
                        List.of(), List.of(), List.of(), null, List.of(),
                        null, null, "Jv", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageDescriptor("java", "Java", List.of(),
                        List.of(), List.of(), List.of(), null, List.of(),
                        "java", null, "Jv", 0),
                "un langage n'est pas son propre parent");
        LanguageDescriptor simple = LanguageDescriptor.of("go", "Go",
                List.of("go"));
        assertEquals("go", simple.id());
        assertEquals(List.of("go"), simple.extensions());
        assertEquals("GO", simple.badge());
        assertTrue(simple.filenames().isEmpty());
        assertTrue(simple.filenamePatterns().isEmpty());
    }

    /** Les préférences syntaxiques sans source se refusent avec précision. */
    @Test
    void lesPreferencesSyntaxiquesSeRefusentSansSource(@TempDir Path bac)
            throws IOException {
        Path dossier = bac.resolve("sans-source");
        Files.createDirectories(dossier);
        Files.writeString(dossier.resolve("pack.json"), """
                {"id": "sans-source", "version": "1.0.0", "schemaVersion": 1,
                 "license": "X", "provenance": "banc"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("language.json"), """
                {"id": "sans-source", "name": "SansSource",
                 "extensions": [".ss"],
                 "syntax": {"preferred": "handwritten"}}
                """, StandardCharsets.UTF_8);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> LanguagePlatform.with(FABRIQUE)
                        .addPackSource(Packs.folder(bac)));
        assertTrue(String.valueOf(echec.getMessage()).contains("handwritten"),
                "la préférence en cause se nomme");
    }

    /** Une préférence inconnue se refuse — jamais de déviation silencieuse. */
    @Test
    void unePreferenceInconnueSeRefuse(@TempDir Path bac) throws IOException {
        Path dossier = bac.resolve("etranger");
        Files.createDirectories(dossier);
        Files.writeString(dossier.resolve("pack.json"), """
                {"id": "etranger", "version": "1.0.0", "schemaVersion": 1,
                 "license": "X", "provenance": "banc"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("language.json"), """
                {"id": "etranger", "name": "Étranger", "extensions": [".et"],
                 "syntax": {"preferred": "sorcier"}}
                """, StandardCharsets.UTF_8);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> LanguagePlatform.with(FABRIQUE)
                        .addPackSource(Packs.folder(bac)));
        assertTrue(String.valueOf(echec.getMessage()).contains("sorcier"));
    }

    /** Le repli déclaré rattrape une préférence sans source. */
    @Test
    void leRepliRattrapeLaPreference(@TempDir Path bac) throws IOException {
        Path dossier = bac.resolve("avec-repli");
        Files.createDirectories(dossier);
        Files.writeString(dossier.resolve("pack.json"), """
                {"id": "avec-repli", "version": "1.0.0", "schemaVersion": 1,
                 "license": "X", "provenance": "banc"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("language.json"), """
                {"id": "avec-repli", "name": "AvecRepli", "extensions": [".ar"],
                 "syntax": {"preferred": "handwritten", "fallback": "grammar"}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dossier.resolve("grammar.json"), """
                {"name": "AvecRepli", "scopeName": "source.avec-repli",
                 "patterns": []}
                """, StandardCharsets.UTF_8);
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .addPackSource(Packs.folder(bac))
                .build();
        LanguageSupport support = registre.byId("avec-repli");
        LanguageSyntax source = support == null ? null : support.syntax();
        assertNotNull(source, "le repli grammar sert (fabrique de banc : Java)");
    }

}
