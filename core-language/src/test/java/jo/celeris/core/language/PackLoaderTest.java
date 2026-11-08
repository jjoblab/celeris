package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Les tests du chargeur de paquets (M7.6) : un paquet valide se charge
 * entier (manifeste, descripteur, configuration, grammaire compilée,
 * extraits), le cache par empreinte ne recompile rien, et chaque paquet
 * invalide se refusé avec le message précis de sa faute — schéma
 * inconnu, identifiants divergents, grammaire rejetée, fabrique
 * manquante, tailles et profondeur hors bornes, zip qui remonte
 * (zip-slip), lien symbolique. Un paquet est de la donnée non fiable :
 * rien ne se charge à moitié.
 */
final class PackLoaderTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE = (id, json) ->
            jo.celeris.core.syntax.JavaSyntax.INSTANCE;

    @TempDir
    Path bac;

    private Path paquetValide() throws IOException {
        Path dossier = bac.resolve("essai");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json", """
                {"id": "essai", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc de test"}
                """);
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "badge": "Es", "badgeColor": "#123456",
                 "syntax": {"preferred": "grammar"},
                 "comments": {"lineComment": "# "},
                 "brackets": [["(", ")"]],
                 "autoClosingPairs": [{"open": "(", "close": ")"}]}
                """);
        ecrire(dossier, "grammar.json", "{\"name\": \"Essai\", \"scopeName\": \"source.essai\", \"patterns\": []}");
        return dossier;
    }

    private static void ecrire(Path dossier, String fichier, String contenu)
            throws IOException {
        Files.writeString(dossier.resolve(fichier), contenu, StandardCharsets.UTF_8);
    }

    @Test
    void unPaquetValideSeChargeEntier() throws IOException {
        paquetValide();
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        LanguagePack pack = chargeur.load(
                Packs.folder(bac).packs().get(0));
        assertEquals("essai", pack.manifest().id());
        assertEquals("Essai", pack.descriptor().displayName());
        assertEquals(List.of("essai"), pack.descriptor().extensions());
        assertEquals("# ", pack.configuration().comments().lineComment());
        assertNotNull(pack.grammar(), "la grammaire se compile");
        assertEquals(64, pack.fingerprint().length());
        assertTrue(pack.snippets().isEmpty());
    }

    @Test
    void leCacheParEmpreinteRendLaMemeInstance() throws IOException {
        paquetValide();
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        LanguagePack premier = chargeur.load(Packs.folder(bac).packs().get(0));
        LanguagePack second = chargeur.load(Packs.folder(bac).packs().get(0));
        assertSame(premier, second);
        assertSame(premier, chargeur.cached(premier.fingerprint()));
    }

    @Test
    void uneVersionDeSchemaInconnueSeRefuse() throws IOException {
        Path dossier = paquetValide();
        ecrire(dossier, "pack.json", """
                {"id": "essai", "version": "1.0.0", "schemaVersion": 99,
                 "license": "Apache-2.0", "provenance": "banc"}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("schemaVersion"));
    }

    @Test
    void unPackJsonSansLanguageJsonNeSeChargePas() throws IOException {
        Path dossier = bac.resolve("muette");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json", """
                {"id": "muette", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc"}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        assertThrows(IOException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
    }

    @Test
    void lesIdentifiantsDivergentsSeRefusent() throws IOException {
        Path dossier = paquetValide();
        ecrire(dossier, "language.json", """
                {"id": "autre", "name": "Autre", "extensions": [".x"],
                 "syntax": {"preferred": "grammar"}}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("differs"));
    }

    @Test
    void uneGrammaireRejeteeRefuseLePaquetAvecLeMessage() throws IOException {
        paquetValide();
        GrammarFactory refuseuse = (id, json) -> {
            throw new IllegalArgumentException("unknown include #absent");
        };
        PackLoader chargeur = PackLoader.with(refuseuse);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("grammar.json rejected"));
        assertTrue(String.valueOf(echec.getMessage()).contains("unknown include"));
    }

    @Test
    void uneGrammaireSansFabriqueSeRefuse() throws IOException {
        paquetValide();
        PackLoader chargeur = PackLoader.create();
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("grammar factory"));
    }

    @Test
    void unFichierHorsTailleSeRefuse() throws IOException {
        Path dossier = bac.resolve("lourd");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json",
                "{\"id\": \"lourd\", \"version\": \"1\", \"schemaVersion\": 1,"
                        + " \"license\": \"X\", \"provenance\": \"banc\"}");
        Files.writeString(dossier.resolve("language.json"),
                "x".repeat(PackLoader.MAX_FILE_BYTES + 1), StandardCharsets.UTF_8);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("exceeds"));
    }

    @Test
    void uneImbricationTropProfondeSeRefuse() throws IOException {
        Path dossier = bac.resolve("profond");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json",
                "{\"id\": \"profond\", \"version\": \"1\", \"schemaVersion\": 1,"
                        + " \"license\": \"X\", \"provenance\": \"banc\"}");
        StringBuilder json = new StringBuilder(
                "{\"id\": \"profond\", \"name\": \"Profond\", \"extensions\": [\".p\"],"
                        + " \"syntax\": {\"preferred\": \"grammar\"}, \"x\": ");
        for (int i = 0; i < JsonChamps.MAX_DEPTH + 1; i++) {
            json.append('[');
        }
        for (int i = 0; i < JsonChamps.MAX_DEPTH + 1; i++) {
            json.append(']');
        }
        json.append('}');
        ecrire(dossier, "language.json", json.toString());
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("nests deeper"));
    }

    @Test
    void uneArchiveQuiRemonteSeRefuse() throws IOException {
        Path zip = bac.resolve("piege.zip");
        try (ZipOutputStream sortie = new ZipOutputStream(Files.newOutputStream(zip))) {
            sortie.putNextEntry(new ZipEntry("../ evasion.json"));
            sortie.write("1".getBytes(StandardCharsets.UTF_8));
        }
        PackSource source = Packs.archive(zip);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                source::packs);
        assertTrue(String.valueOf(echec.getMessage()).contains("escapes"));
    }

    @Test
    void uneArchiveSaineSeCharge() throws IOException {
        Path zip = bac.resolve("essai.zip");
        try (ZipOutputStream sortie = new ZipOutputStream(Files.newOutputStream(zip))) {
            sortie.putNextEntry(new ZipEntry("pack.json"));
            sortie.write("""
                    {"id": "essai", "version": "1.0.0", "schemaVersion": 1,
                     "license": "Apache-2.0", "provenance": "banc"}
                    """.getBytes(StandardCharsets.UTF_8));
            sortie.putNextEntry(new ZipEntry("language.json"));
            sortie.write("""
                    {"id": "essai", "name": "Essai", "extensions": [".essai"],
                     "syntax": {"preferred": "grammar"}}
                    """.getBytes(StandardCharsets.UTF_8));
        }
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        LanguagePack pack = chargeur.load(Packs.archive(zip).packs().get(0));
        assertEquals("essai", pack.manifest().id());
    }

    @Test
    void unLienSymboliqueSeRefuse() throws IOException {
        paquetValide();
        Files.createSymbolicLink(bac.resolve("clones"), bac.resolve("essai"));
        PackSource source = Packs.folder(bac);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                source::packs);
        assertTrue(String.valueOf(echec.getMessage()).contains("symbolic links"));
    }

    @Test
    void lesExtraitsDuFormatVsCodeSeLisent() throws IOException {
        Path dossier = bac.resolve("extraits");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json",
                "{\"id\": \"extraits\", \"version\": \"1\", \"schemaVersion\": 1,"
                        + " \"license\": \"X\", \"provenance\": \"banc\"}");
        ecrire(dossier, "language.json",
                "{\"id\": \"extraits\", \"name\": \"Extraits\","
                        + " \"extensions\": [\".e\"],"
                        + " \"syntax\": {\"preferred\": \"grammar\"}}");
        ecrire(dossier, "snippets.json", """
                {"boucle": {"prefix": "for", "body": ["for ($1) {", "  $0", "}"],
                            "description": "boucle générique"}}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        LanguagePack pack = chargeur.load(Packs.folder(bac).packs().get(0));
        assertEquals(1, pack.snippets().size());
        assertEquals("boucle", pack.snippets().get(0).name());
        assertEquals("for", pack.snippets().get(0).prefix());
        assertEquals(3, pack.snippets().get(0).body().size());
        assertEquals("boucle générique", pack.snippets().get(0).description());
    }

    @Test
    void uneCouleurDeBadgeInvalideSeRefuse() throws IOException {
        paquetValide();
        Path dossier = bac.resolve("essai");
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "badgeColor": "pas-une-couleur",
                 "syntax": {"preferred": "grammar"}}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("badgeColor"));
    }

    @Test
    void lePlancherDeConfigurationResteCoherent() throws IOException {
        paquetValide();
        LanguagePack pack = PackLoader.with(FABRIQUE)
                .load(Packs.folder(bac).packs().get(0));
        assertEquals(4, pack.configuration().indentSize());
        assertEquals(1, pack.configuration().brackets().size());
        assertInstanceOf(LanguagePack.SyntaxPreference.class,
                pack.syntaxPreference());
    }
}
