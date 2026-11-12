package jo.celeris.core.language;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.Packs;
import jo.celeris.core.language.packs.TreeSitterLanguages;
import jo.celeris.core.syntax.clike.KotlinSyntax;
import jo.celeris.core.syntax.json.JsonSyntax;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;



import jo.celeris.core.syntax.LanguageSyntax;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'option tree-sitter de la plateforme (M7.6, la demande du
 * propriétaire) : la case déclarée du niveau 2 — un paquet peut
 * préférer {@code tree-sitter}, la plateforme consulte
 * {@link TreeSitterLanguages} et retombe sur le repli déclaré quand la
 * case est vide (l'arrière natif ne rentre pas au dépôt, ADR-016) ;
 * l'enregistrement vivant, la préférence sert.
 */
final class TreeSitterOptionTest {

    /** La fabrique de banc : toute grammaire devient la façade JSON. */
    private static final GrammarFactory FABRIQUE = (id, json) -> JsonSyntax.INSTANCE;

    /** La source tree-sitter de banc : la façade Kotlin tient le rôle. */
    private static final LanguageSyntax SOURCE_NIVEAU_DEUX = KotlinSyntax.INSTANCE;

    @TempDir
    Path bac;

    @BeforeEach
    @AfterEach
    void laCaseRevientAVide() {
        TreeSitterLanguages.oublier("essai");
    }

    /** Un paquet qui préfère tree-sitter, le repli grammaire déclaré. */
    private Path paquetTreeSitter() throws IOException {
        Path dossier = bac.resolve("essai");
        Files.createDirectories(dossier);
        ecrire(dossier, "pack.json", """
                {"id": "essai", "version": "1.0.0", "schemaVersion": 1,
                 "license": "Apache-2.0", "provenance": "banc de test"}
                """);
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "syntax": {"preferred": "tree-sitter", "fallback": "grammar"}}
                """);
        ecrire(dossier, "grammar.json",
                "{\"name\": \"Essai\", \"scopeName\": \"source.essai\", \"patterns\": []}");
        return dossier;
    }

    /** La case vide : la préférence tree-sitter retombe sur le repli déclaré. */
    @Test
    void laCaseVideRetombeSurLeRepli() throws IOException {
        paquetTreeSitter();
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .addPackSource(Packs.folder(bac)).build();
        LanguageSupport support = supportDe(registre);
        assertSame(JsonSyntax.INSTANCE, support.syntax(),
                "sans arrière natif, le repli grammar sert (ADR-016)");
    }

    /** L'enregistrement vivant : la préférence tree-sitter sert. */
    @Test
    void lenregistrementVivantSert() throws IOException {
        paquetTreeSitter();
        TreeSitterLanguages.enregistrer("essai", SOURCE_NIVEAU_DEUX);
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .addPackSource(Packs.folder(bac)).build();
        assertSame(SOURCE_NIVEAU_DEUX, supportDe(registre).syntax(),
                "l'hôte qui embarque l'arrière natif obtient le niveau 2");
    }

    /** La case vide sans repli : le refus reste précis. */
    @Test
    void laCaseVideSansRepliSeRefuse() throws IOException {
        Path dossier = paquetTreeSitter();
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "syntax": {"preferred": "tree-sitter"}}
                """);
        LanguagePlatform plateforme = LanguagePlatform.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> plateforme.addPackSource(Packs.folder(bac)));
        assertTrue(String.valueOf(echec.getMessage()).contains("has no source"),
                "message précis : " + echec.getMessage());
    }

    /** Un niveau inconnu reste refusé — tree-sitter n'ouvre pas la porte. */
    @Test
    void unNiveauInconnuResteRefuse() throws IOException {
        Path dossier = paquetTreeSitter();
        ecrire(dossier, "language.json", """
                {"id": "essai", "name": "Essai", "extensions": [".essai"],
                 "syntax": {"preferred": "telepathie"}}
                """);
        LanguagePlatform plateforme = LanguagePlatform.with(FABRIQUE);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> plateforme.addPackSource(Packs.folder(bac)));
        assertTrue(String.valueOf(echec.getMessage()).contains("unknown syntax preference"),
                "message précis : " + echec.getMessage());
    }

    /** Le support du paquet de banc — le banc meurt s'il manque. */
    private static LanguageSupport supportDe(LanguageRegistry registre) {
        LanguageSupport support = registre.byId("essai");
        if (support == null) {
            throw new IllegalStateException("le paquet essai manque");
        }
        return support;
    }

    private static void ecrire(Path dossier, String fichier, String contenu)
            throws IOException {
        Files.writeString(dossier.resolve(fichier), contenu, StandardCharsets.UTF_8);
    }
}
