package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Le fournisseur d'intelligence de la démonstration (M7.5, §8) : la
 * complétion par les mots du CODE — l'analyseur décide, les chaînes et
 * les commentaires ne proposent rien (B-03) —, le balayage du coach en
 * repli tant que la publication n'a pas rattrapé la frappe. Pur, sans
 * appareil.
 */
class DemoIntelligenceTest {

    private static final String DOCUMENT = """
            int nombreLu = 3;
            String secret = "nombreCacheDansLaChaine";
            void mesurer() {
                int nombreLu2 = nombreLu;
            }
            """;

    private final DemoIntelligence fournisseur = new DemoIntelligence(
            () -> LangagesDemo.langageDe("Banc.java", null, DOCUMENT));

    /**
     * B-03 — la voie analytique : les mots du code répondent au
     * préfixe, ceux des CHAÎNES jamais — le lexeur décide, pas le
     * balayage aveugle.
     */
    @Test
    void completionServesCodeWordsOnly() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        // le curseur juste après l'USAGE « nombreLu » — le préfixe tapé
        int caret = DOCUMENT.indexOf("nombreLu;") + "nombreLu".length();
        List<String> candidats = fournisseur.completionCandidates(
                session.document(), coordinateur.analysis(), caret,
                PopupCoach.motsReserves());
        assertTrue(candidats.contains("nombreLu"),
                "le mot du code est candidat de son préfixe");
        assertTrue(candidats.contains("nombreLu2"),
                "l'autre mot du code, même préfixe, se joint");
        assertFalse(candidats.contains("nombreCacheDansLaChaine"),
                "le mot de la chaîne ne propose rien — B-03");
    }

    /**
     * Le repli : la publication en retard ne ferme pas la complétion —
     * le coach sert le balayage textuel, préfixe respecté.
     */
    @Test
    void completionFallsBackWhileAnalysisInFlight() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        int caret = DOCUMENT.indexOf("nombreLu;") + "nombreLu".length();
        List<String> candidats = fournisseur.completionCandidates(
                session.document(), null, caret, PopupCoach.motsReserves());
        assertTrue(candidats.contains("nombreLu"),
                "le repli du coach sert pendant le vol d'analyse");
        assertTrue(candidats.size() <= PopupCoach.MAX_CANDIDATS,
                "la liste du repli reste capée");
    }

    /** Sans préfixe tapé, le repli du coach ne propose rien. */
    @Test
    void fallbackWithoutPrefixProposesNothing() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        int finDeLigne = DOCUMENT.indexOf('\n');
        List<String> candidats = fournisseur.completionCandidates(
                session.document(), null, finDeLigne,
                PopupCoach.motsReserves());
        assertTrue(candidats.isEmpty(),
                "sans préfixe, le repli ne propose rien");
    }

    /**
     * C8 — l'Entrée d'un fichier Java porte le découpage des chaînes :
     * le fournisseur délègue aux gestes dédiés quand la langue est
     * Java.
     */
    @Test
    void entreeJavaDecoupeLaChaine() {
        String document = "String s = \"abcdef\";\n";
        TextSession session = TextSession.of(document, () -> 0L);
        DemoIntelligence java = new DemoIntelligence(
                () -> LangagesDemo.langageDe("Banc.java", null, document));
        jo.celeris.core.intelligence.EditPlan plan = java.enter(
                session.document(), null,
                jo.celeris.core.text.MultiSelection.caretsAt(15),
                jo.celeris.core.intelligence.IndentStyle.ESPACES_4);
        assertTrue(plan.transaction().edits().get(0).inserted()
                .contains(" +\n    \""),
                "la chaîne se ferme, se concatène et se rouvre — C8");
    }

    /**
     * C8 — l'Entrée d'un fichier d'une autre langue ne découpe rien :
     * les citations d'un attribut XML ne se coupent jamais.
     */
    @Test
    void entreeXmlNeDecoupePas() {
        String document = "<a attr=\"abcdef\"/>\n";
        TextSession session = TextSession.of(document, () -> 0L);
        DemoIntelligence xml = new DemoIntelligence(
                () -> LangagesDemo.langageDe("Banc.xml", null, document));
        jo.celeris.core.intelligence.EditPlan plan = xml.enter(
                session.document(), null,
                jo.celeris.core.text.MultiSelection.caretsAt(10),
                jo.celeris.core.intelligence.IndentStyle.ESPACES_4);
        assertFalse(plan.transaction().edits().get(0).inserted()
                .contains(" +"),
                "les citations XML ne se découpent jamais — C8");
    }
}
