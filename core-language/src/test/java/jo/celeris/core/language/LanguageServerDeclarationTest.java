package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Les tests de la déclaration de serveur (M7.6, H-13) : le fichier
 * {@code servers.json} se lit et se valide entier — chaque transport
 * exige ses champs, la première faute nomme le sien — le paquet qui
 * en porte un ne déclare QUE son langage, les paquets intégrés n'en
 * déclarent AUCUN (l'éditeur ne connaît pas de serveur), et la
 * plateforme assemble les déclarations du paquet comme celles de
 * l'hôte.
 */
final class LanguageServerDeclarationTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE = (id, json) ->
            jo.celeris.core.syntax.JavaSyntax.INSTANCE;

    @TempDir
    Path bac;

    @Test
    void uneDeclarationEmbarqueeSeLitEntiere() {
        List<LanguageServerDeclaration> lues = LanguageServerDeclaration.parse("""
                {"servers": [
                  {"language": "xml", "transport": "embedded",
                   "serverClass": "org.exemple.Serveur",
                   "clientInterface": "org.exemple.Client",
                   "clientName": "banc",
                   "initializationOptions": {"catalogue": true}}
                ]}
                """);
        assertEquals(1, lues.size());
        LanguageServerDeclaration declaration = lues.get(0);
        assertEquals("xml", declaration.language());
        assertEquals(LanguageServerDeclaration.EMBEDDED, declaration.transport());
        assertEquals("org.exemple.Serveur", declaration.serverClass());
        assertEquals("org.exemple.Client", declaration.clientInterface());
        assertEquals("banc", declaration.clientName());
        assertEquals(Map.of("catalogue", true),
                declaration.initializationOptions());
        assertTrue(declaration.command().isEmpty());
        assertNull(declaration.host());
    }

    @Test
    void uneDeclarationDeProcessusSeLitAvecSaCommande() {
        List<LanguageServerDeclaration> lues = LanguageServerDeclaration.parse("""
                {"servers": [
                  {"language": "go", "transport": "stdio",
                   "command": ["/usr/local/bin/gopls", "serve"],
                   "clientName": "banc"}
                ]}
                """);
        LanguageServerDeclaration declaration = lues.get(0);
        assertEquals(LanguageServerDeclaration.STDIO, declaration.transport());
        assertEquals(List.of("/usr/local/bin/gopls", "serve"),
                declaration.command());
        assertNull(declaration.serverClass());
    }

    @Test
    void uneDeclarationDePriseSeLitAvecSonAdresse() {
        List<LanguageServerDeclaration> lues = LanguageServerDeclaration.parse("""
                {"servers": [
                  {"language": "rust", "transport": "tcp",
                   "host": "127.0.0.1", "port": 9257,
                   "clientName": "banc"}
                ]}
                """);
        LanguageServerDeclaration declaration = lues.get(0);
        assertEquals(LanguageServerDeclaration.TCP, declaration.transport());
        assertEquals("127.0.0.1", declaration.host());
        assertEquals(9257, declaration.port());
    }

    @Test
    void unFichierSansServeursRendLaListeVide() {
        assertTrue(LanguageServerDeclaration.parse("{}").isEmpty());
    }

    @Test
    void plusieursServeursGardentLeurOrdre() {
        List<LanguageServerDeclaration> lues = LanguageServerDeclaration.parse("""
                {"servers": [
                  {"language": "xml", "transport": "embedded",
                   "serverClass": "a.Serveur", "clientInterface": "a.Client",
                   "clientName": "banc"},
                  {"language": "go", "transport": "stdio",
                   "command": ["gopls"], "clientName": "banc"}
                ]}
                """);
        assertEquals(2, lues.size());
        assertEquals("xml", lues.get(0).language());
        assertEquals("go", lues.get(1).language());
    }

    @Test
    void lesFabriquesBatisentLesTroisTransports() {
        LanguageServerDeclaration embarquee = LanguageServerDeclaration.embedded(
                "xml", "a.Serveur", "a.Client", "banc", Map.of());
        assertEquals(LanguageServerDeclaration.EMBEDDED, embarquee.transport());
        LanguageServerDeclaration processus = LanguageServerDeclaration.stdio(
                "go", List.of("gopls"), "banc", Map.of());
        assertEquals(LanguageServerDeclaration.STDIO, processus.transport());
        LanguageServerDeclaration prise = LanguageServerDeclaration.tcp(
                "rust", "127.0.0.1", 9257, "banc", Map.of());
        assertEquals(LanguageServerDeclaration.TCP, prise.transport());
    }

    @Test
    void unTransportInconnuSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "xml", "transport": "fume",
                           "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("transport"));
    }

    @Test
    void unEmbarqueSansClasseSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "xml", "transport": "embedded",
                           "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("serverClass"));
    }

    @Test
    void unProcessusSansCommandeSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "go", "transport": "stdio",
                           "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("command"));
    }

    @Test
    void unePriseSansPortSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "rust", "transport": "tcp",
                           "host": "127.0.0.1", "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("port"));
    }

    @Test
    void unePriseAuPortFouSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "rust", "transport": "tcp",
                           "host": "127.0.0.1", "port": 99999,
                           "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("port"));
    }

    @Test
    void uneEntreeSansLangageSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"transport": "stdio", "command": ["x"],
                           "clientName": "banc"}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("language"));
    }

    @Test
    void desOptionsQuiNeSontPasUnObjetSeRefusent() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> LanguageServerDeclaration.parse("""
                        {"servers": [
                          {"language": "xml", "transport": "embedded",
                           "serverClass": "a.Serveur",
                           "clientInterface": "a.Client",
                           "clientName": "banc",
                           "initializationOptions": 3}
                        ]}
                        """));
        assertTrue(String.valueOf(echec.getMessage())
                .contains("initializationOptions"));
    }

    @Test
    void unPaquetPorteSesServeursEtPasCeuxDesAutres() throws IOException {
        Path dossier = bac.resolve("essai");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json", """
                {"id": "essai", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc de test"}
                """);
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "syntax": {"preferred": "grammar"},
                 "comments": {"lineComment": "# "}}
                """);
        ecrire(dossier, "grammar.json",
                "{\"name\": \"Essai\", \"scopeName\": \"source.essai\","
                        + " \"patterns\": []}");
        ecrire(dossier, "servers.json", """
                {"servers": [
                  {"language": "essai", "transport": "embedded",
                   "serverClass": "org.exemple.Serveur",
                   "clientInterface": "org.exemple.Client",
                   "clientName": "banc"}
                ]}
                """);
        LanguagePack pack = PackLoader.with(FABRIQUE).load(
                Packs.folder(bac).packs().get(0));
        assertEquals(1, pack.servers().size());
        assertEquals("essai", pack.servers().get(0).language());

        ecrire(dossier, "servers.json", """
                {"servers": [
                  {"language": "autre", "transport": "embedded",
                   "serverClass": "org.exemple.Serveur",
                   "clientInterface": "org.exemple.Client",
                   "clientName": "banc"}
                ]}
                """);
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class,
                () -> chargeur.load(Packs.folder(bac).packs().get(0)));
        assertTrue(String.valueOf(echec.getMessage()).contains("autre"),
                "le refus nomme le langage étranger : " + echec.getMessage());
    }

    @Test
    void lesPaquetsIntegresNeDeclarentAucunServeur() throws IOException {
        PackLoader chargeur = PackLoader.with(FABRIQUE);
        for (PackSource.PackFiles fichiers : Packs.classpath("langages")
                .packs()) {
            LanguagePack pack = chargeur.load(fichiers);
            assertTrue(pack.servers().isEmpty(),
                    pack.manifest().id() + " ne doit déclarer aucun serveur : "
                            + "l'éditeur ne connaît pas les serveurs de ses"
                            + " hôtes (H-13)");
        }
    }

    @Test
    void laPlateformeAssembleLesDeclarationsDuPaquetEtDeLhote()
            throws IOException {
        Path dossier = bac.resolve("essai");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json", """
                {"id": "essai", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc de test"}
                """);
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "syntax": {"preferred": "grammar"},
                 "comments": {"lineComment": "# "}}
                """);
        ecrire(dossier, "grammar.json",
                "{\"name\": \"Essai\", \"scopeName\": \"source.essai\","
                        + " \"patterns\": []}");
        ecrire(dossier, "servers.json", """
                {"servers": [
                  {"language": "essai", "transport": "embedded",
                   "serverClass": "org.exemple.Serveur",
                   "clientInterface": "org.exemple.Client",
                   "clientName": "banc"}
                ]}
                """);
        LanguageServerDeclaration deLhote = LanguageServerDeclaration.stdio(
                "essai", List.of("essai-lsp"), "banc", Map.of());
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .addPackSource(Packs.folder(bac))
                .servers(List.of(deLhote))
                .build();
        LanguageSupport support = registre.byId("essai");
        assertNotNull(support, "le langage déclaré se résout");
        List<LanguageServerDeclaration> declarees =
                support.serverDeclarations();
        assertEquals(2, declarees.size());
        assertEquals("org.exemple.Serveur",
                declarees.get(0).serverClass());
        assertEquals(List.of("essai-lsp"), declarees.get(1).command());
    }

    private static void ecrire(Path dossier, String fichier, String contenu)
            throws IOException {
        Files.writeString(dossier.resolve(fichier), contenu,
                StandardCharsets.UTF_8);
    }
}
