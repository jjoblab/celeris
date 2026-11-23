package jo.celeris.core.language;

import jo.celeris.core.language.packs.Packs;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Le banc de la COHÉRENCE déclarative des paquets intégrés (ADR-0033,
 * l'incohérence § 3.5 du prompt du 2026-11-22) : le dépôt ne publie
 * AUCUNE grammaire tree-sitter (elles vivent chez l'hôte par
 * dépendance, ADR-0033) — les paquets qui déclaraient
 * {@code "preferred": "tree-sitter"} (json, python, javascript)
 * le déclaraient SANS grammaire au dépôt : le repli TextMate servait
 * en silence, la préférence déclarée mentait. La cohérence
 * désormais : les paquets intégrés déclarent leurs sources du DÉPÔT
 * (grammar ou handwritten) ; l'hôte qui veut la voie native pose
 * {@link jo.celeris.core.language.packs.TreeSitterLanguages#preferTreeSitter(boolean)}
 * et enregistre ses grammaires — javascript n'était qu'une
 * incohérence de plus (aucune grammaire native n'existe pour lui
 * chez la liaison).
 */
final class DeclarationTreeSitterCoherenceTest {

    /** Aucun paquet intégré ne déclare la préférence tree-sitter :
     *  les grammaires vivent chez l'hôte (ADR-0033) — la préférence
     *  déclarée d'un paquet intégré décrit ce que le DÉPÔT sert.
     *  L'ancien trio fautif : json, python, javascript. */
    @Test
    void aucunPaquetIntegreNeDeclareTreeSitter() {
        assertNull(jo.celeris.core.language.packs.TreeSitterLanguages
                .syntaxOf("json"),
                "le registre intégré reste vide au chargement du module");
        for (String id : new String[] {"json", "python", "javascript"}) {
            String json = contenu(id);
            assertTrue(json.contains("\"preferred\": \"" + attendu() + "\""),
                    id + " déclare " + attendu() + " : " + json);
        }
    }

    /** La préférence attendue d'un paquet intégré : grammar — le
     *  dépôt la sert. */
    private static String attendu() {
        return "grammar";
    }

    /** Le contenu d'un language.json du classpath. */
    private static String contenu(String dossier) {
        try (InputStream flux = Packs.class.getResourceAsStream(
                "/langages/" + dossier + "/language.json")) {
            assertNotNull(flux, dossier + "/language.json vit au classpath");
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException echec) {
            throw new IllegalStateException(echec);
        }
    }
}
