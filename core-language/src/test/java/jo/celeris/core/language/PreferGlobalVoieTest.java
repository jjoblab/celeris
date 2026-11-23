package jo.celeris.core.language;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.TreeSitterLanguages;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.json.JsonSyntax;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Le banc de la préférence GLOBALE de la voie tree-sitter (ADR-0033 —
 * la généralisation demandée par le propriétaire le 2026-11-23 :
 * « toujours tree-sitter quand il est servi » doit DÉROGER aux
 * préférences déclarées des paquets). Le paquet java déclare
 * « handwritten » : la préférence globale posée, sa source devient
 * CELLE de l'enregistrement tree-sitter ; la préférence retirée,
 * la préférence déclarée reprend.
 *
 * <p>La source de banc n'est pas une vraie langue tree-sitter (la
 * liaison ne vit pas sur JVM) : le registre se pose par l'API
 * publique d'enregistrement, le mécanisme de la préférence se
 * banc par le CHOIX de la source, pas par sa nature.</p>
 */
final class PreferGlobalVoieTest {

    /** La source de banc : DISTINCTE de la source écrite java (la
     *  préférence se banc par le choix — l'égalité par défaut n'y
     *  prouverait rien, java écrit vit déjà JavaSyntax). */
    private static final LanguageSyntax SOURCE_BANC = JsonSyntax.INSTANCE;

    /** Le nettoyage : la préférence et l'enregistrement ne fuient
     *  pas entre les bancs. */
    @AfterEach
    void nettoyer() {
        TreeSitterLanguages.preferTreeSitter(false);
        TreeSitterLanguages.forget("java");
    }

    /** La préférence globale déroge à la préférence déclarée : le
     *  paquet java (déclaré handwritten) sert la source
     *  tree-sitter enregistrée quand l'hôte pose le choix global. */
    @Test
    void laPreferenceGlobaleDerogeAuxPaquets() {
        TreeSitterLanguages.enregistrer("java", SOURCE_BANC);
        TreeSitterLanguages.preferTreeSitter(true);
        LanguageSupport support = java.util.Objects.requireNonNull(
                registreIntegre().byId("java"));
        assertSame(SOURCE_BANC, support.syntax(),
                "la préférence globale déroge : java servirait la voie");
    }

    /** Sans la préférence globale, la préférence déclarée du paquet
     *  reprend ses droits — java sert sa source écrite. */
    @Test
    void sansLaPreferenceGlobaleLaDeclareeReprend() {
        TreeSitterLanguages.enregistrer("java", SOURCE_BANC);
        TreeSitterLanguages.preferTreeSitter(false);
        LanguageSupport support = java.util.Objects.requireNonNull(
                registreIntegre().byId("java"));
        assertTrue(support.syntax() != SOURCE_BANC,
                "sans le choix global, la préférence déclarée (handwritten) reprend");
    }

    /** La préférence globale ne FORCE pas l'inconnu : un langage sans
     *  enregistrement tree-sitter garde sa préférence déclarée (le
     *  repli sert — personne ne change). */
    @Test
    void laPreferenceGlobaleNeForcePasLinconnu() {
        TreeSitterLanguages.preferTreeSitter(true);
        LanguageSupport support = java.util.Objects.requireNonNull(
                registreIntegre().byId("java"));
        assertTrue(support.syntax() != SOURCE_BANC,
                "sans enregistrement, le repli déclaré sert");
    }

    private static LanguageRegistry registreIntegre() {
        return LanguagePlatform.with((id, json) -> JavaSyntax.INSTANCE)
                .builtinPacks().build();
    }
}
