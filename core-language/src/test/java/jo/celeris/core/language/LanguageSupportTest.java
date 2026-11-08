package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Les tests du support de langage (M7.6) : l'agrégat, la passerelle de
 * migration et la chaîne de résolution des capacités — le fournisseur
 * du langage, celui de sa famille, puis le générique.
 */
final class LanguageSupportTest {

    /** Une capacité de banc : rend une liste fixe. */
    private record Compteur(String valeur) implements Completion {
        @Override
        public List<String> candidates(Document document,
                @Nullable Analysis analysis, int caretOffset) {
            return List.of(valeur);
        }
    }

    private static LanguageDescriptor descripteur(String id,
            @Nullable String parentId) {
        return new LanguageDescriptor(id, id.toUpperCase(Locale.ROOT), List.of(),
                List.of(id), List.of(), List.of(), null, List.of(), parentId,
                null, id.substring(0, 2), 0xFF123456);
    }

    @Test
    void laCapaciteDuLangageGagne() {
        Compteur fournisseur = new Compteur("java");
        LanguageSupport java = LanguageSupport.builder(descripteur("java", null))
                .capability(Completion.class, fournisseur)
                .build();
        assertEquals(fournisseur, java.capability(Completion.class));
    }

    @Test
    void laFamilleSertSansCapacitePropre() {
        Compteur duKotlin = new Compteur("kotlin");
        LanguageSupport kotlin = LanguageSupport.builder(descripteur("kotlin", null))
                .capability(Completion.class, duKotlin)
                .build();
        LanguageSupport kts = LanguageSupport.builder(descripteur("kts", "kotlin"))
                .parent(kotlin)
                .build();
        assertEquals(duKotlin, kts.capability(Completion.class));
    }

    @Test
    void leGeneriqueSertEnBoutDeChaine() {
        Compteur generique = new Compteur("mots");
        LanguageSupport java = LanguageSupport.builder(descripteur("java", null))
                .build();
        LanguageSupport avecRepli = java.withGenericFallbacks(
                Map.of(Completion.class, generique));
        assertEquals(generique, avecRepli.capability(Completion.class));
    }

    @Test
    void laCapacitePropreBatLaFamilleEtLeGenerique() {
        LanguageSupport groovy = LanguageSupport.builder(descripteur("groovy", null))
                .capability(Completion.class, new Compteur("groovy"))
                .build();
        Compteur duKts = new Compteur("kts");
        LanguageSupport kts = LanguageSupport.builder(descripteur("kts", "groovy"))
                .parent(groovy)
                .capability(Completion.class, duKts)
                .build();
        LanguageSupport avecRepli = kts.withGenericFallbacks(
                Map.of(Completion.class, new Compteur("mots")));
        assertEquals(duKts, avecRepli.capability(Completion.class));
    }

    @Test
    void laChaineSArreteSansFournisseur() {
        LanguageSupport texte = LanguageSupport.builder(descripteur("text", null))
                .build();
        assertNull(texte.capability(Completion.class));
    }

    @Test
    void laPasserelleSynthetiseDescripteurEtConfiguration() {
        LanguageSupport support = LanguageSupport.of(JavaSyntax.INSTANCE);
        assertEquals("java", support.id());
        assertEquals("Java", support.displayName());
        assertSame(JavaSyntax.INSTANCE, support.syntax());
        assertTrue(support.configuration().effectiveAutoClosingPairs().size() >= 3);
        assertFalse(support.configuration().comments().commentable());
        assertNull(support.parent());
    }

    @Test
    void laConfigurationVideNormaliseSesSections() {
        LanguageSupport texte = LanguageSupport.builder(descripteur("text", null))
                .build();
        assertTrue(texte.configuration().brackets().isEmpty());
        assertTrue(texte.injections().isEmpty());
        assertEquals(4, texte.configuration().indentSize());
        assertFalse(texte.configuration().folding().byIndentation());
    }

    @Test
    void lesIncorporationsSePortentAvecLeSupport() {
        LanguageSupport html = LanguageSupport.builder(descripteur("html", null))
                .injections(List.of(
                        new LanguageInjection("text.html source", "javascript"),
                        new LanguageInjection("text.html style", "css")))
                .build();
        assertEquals(2, html.injections().size());
        assertEquals("javascript", html.injections().get(0).injectedLanguageId());
        assertEquals("text.html source", html.injections().get(0).scopeSelector());
    }

    @Test
    void laConfigurationGeneriqueDitLesPairesEtCitations() {
        LanguageConfiguration generique = LanguageConfiguration.generic();
        assertEquals(5, generique.effectiveAutoClosingPairs().size());
        assertFalse(generique.comments().commentable());
        assertTrue(generique.surroundingPairs().size() >= 3);
    }

    @Test
    void lautoFermetureSeDeriveDesCrochets() {
        LanguageConfiguration crochets = new LanguageConfiguration(
                new LanguageConfiguration.Comments(null, null, null),
                List.of(new LanguageConfiguration.BracketPair('(', ')')),
                List.of(), List.of(), null,
                new LanguageConfiguration.Folding(false, false, null, null),
                new LanguageConfiguration.IndentRules(null, null, null, null),
                4, false);
        List<LanguageConfiguration.AutoClosePair> paires =
                crochets.effectiveAutoClosingPairs();
        assertEquals(1, paires.size());
        assertEquals(')', paires.get(0).close());
        assertTrue(paires.get(0).notIn().isEmpty());
    }
}
