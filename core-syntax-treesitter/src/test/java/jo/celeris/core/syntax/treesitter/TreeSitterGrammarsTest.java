package jo.celeris.core.syntax.treesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jo.celeris.core.language.packs.TreeSitterLanguages;
import jo.celeris.core.syntax.LanguageSyntax;

/**
 * Le banc du registre des grammaires (ADR-0033 — la généralisation
 * demandée par le propriétaire le 2026-11-23 : « l'utilisateur a le
 * choix, il ajoute la dépendance tree-sitter de même pour son
 * application »). L'hôte enregistre SA grammaire (le fournisseur de
 * langue + SA requête highlights.scm) : la source se pose au
 * registre de la plateforme, la préférence « tree-sitter » du paquet
 * du langage se met à servir. L'oubli retire tout, proprement.
 *
 * <p>Le banc vit sur JVM (le registre est pur — la langue ne se
 * charge qu'à la première vue, jamais à l'enregistrement : le
 * fournisseur paresseux n'y veut pas la liaison).</p>
 */
class TreeSitterGrammarsTest {

    /** Le nettoyage : les enregistrements du banc ne fuient pas. */
    @AfterEach
    void nettoyer() {
        TreeSitterGrammars.oublier("banc");
        TreeSitterGrammars.oublier("banc2");
    }

    /** L'enregistrement pose la grammaire ET la source au registre
     *  de la plateforme — la langue ne se charge PAS à
     *  l'enregistrement (paresse : la grammaire se paie à sa
     *  première vue, jamais avant). */
    @Test
    void lenregistrementPoseLaSourceSansChargerLaLangue() {
        boolean[] charge = {false};
        TreeSitterGrammars.enregistrer("banc", () -> {
            charge[0] = true;
            throw new IllegalStateException("the lazy supplier is never called");
        }, "");
        assertNotNull(TreeSitterGrammars.grammaireDe("banc"));
        LanguageSyntax source = TreeSitterLanguages.syntaxOf("banc");
        assertNotNull(source, "la source du langage vit au registre");
        assertEquals("banc", source.id());
        // la langue ne s'est JAMAIS chargée — l'enregistrement ne
        // paie pas la grammaire (le fournisseur paresseux).
        assertTrue(!charge[0],
                "l'enregistrement ne charge pas la langue de la liaison");
    }

    /** L'oubli retire la grammaire ET la source — le symétrique
     *  propre : le repli déclaré du paquet reprend. */
    @Test
    void loubliRetireTout() {
        TreeSitterGrammars.enregistrer("banc2", () -> {
            throw new IllegalStateException("never called");
        }, "");
        assertNotNull(TreeSitterGrammars.grammaireDe("banc2"));
        TreeSitterGrammars.oublier("banc2");
        assertNull(TreeSitterGrammars.grammaireDe("banc2"));
        assertNull(TreeSitterLanguages.syntaxOf("banc2"));
    }

    /** Les identifiants suivent les enregistrements — le recensement
     *  du registre vit. */
    @Test
    void lesIdsSuiventLesEnregistrements() {
        TreeSitterGrammars.enregistrer("banc", () -> {
            throw new IllegalStateException("never called");
        }, "");
        Set<String> ids = TreeSitterGrammars.ids();
        assertTrue(ids.contains("banc"), "l'enregistrement y vit : " + ids);
        TreeSitterGrammars.oublier("banc");
        assertTrue(!TreeSitterGrammars.ids().contains("banc"));
    }

    /** La grammaire sans identifiant se refuse — l'identifiant du
     *  paquet fait foi, vide il n'y a pas de case. */
    @Test
    void laGrammaireSansIdSeRefuse() {
        assertThrows(IllegalArgumentException.class, () -> {
            TreeSitterGrammars.enregistrer("", () -> null, "");
        });
    }
}
