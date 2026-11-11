package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * Les raffinements de complétion (C6) : l'apparieur unifié à six
 * paliers (l'oracle : l'apparieur du dépôt de départ), l'apprentissage
 * du classement par l'historique d'acceptation, la complétion
 * contextuelle (constantes d'énumération après le point, attributs
 * d'annotation dans la parenthèse).
 */
class RaffinementsCompletionTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    /** C6 — les six paliers, du meilleur au pire, sensibles à la casse d'abord. */
    @Test
    void lesSixPaliers() {
        assertEquals(CompletionMatcher.TIER_EXACT,
                CompletionMatcher.tier("total", "", "total"));
        assertEquals(CompletionMatcher.TIER_EXACT_CI,
                CompletionMatcher.tier("Total", "", "total"));
        assertEquals(CompletionMatcher.TIER_PREFIX,
                CompletionMatcher.tier("totalisateur", "", "total"));
        assertEquals(CompletionMatcher.TIER_PREFIX_CI,
                CompletionMatcher.tier("Totalisateur", "", "total"));
        assertEquals(CompletionMatcher.TIER_CAMEL_HUMP,
                CompletionMatcher.tier("getString", "", "gS"));
        assertEquals(CompletionMatcher.TIER_SUBSEQUENCE,
                CompletionMatcher.tier("column", "", "lm"));
        assertEquals(CompletionMatcher.TIER_NONE,
                CompletionMatcher.tier("total", "", "xyz"));
        assertEquals(CompletionMatcher.TIER_EXACT,
                CompletionMatcher.tier("total", "", ""), "le préfixe vide correspond à tout");
    }

    /** C6 — les bosses de chameau : initiales de mots et runs contigus, jamais les autres. */
    @Test
    void lesBossesDeChameau() {
        assertTrue(CompletionMatcher.camelHumpSubsequence("getString", "gS"),
                "les initiales de mots");
        assertTrue(CompletionMatcher.camelHumpSubsequence("getString", "get"),
                "le run contigu");
        assertTrue(CompletionMatcher.camelHumpSubsequence("android_layout", "al"),
                "les séparateurs comptent");
        assertTrue(!CompletionMatcher.camelHumpSubsequence("column", "lm"),
                "la sous-séquence quelconque n'est pas une bosse");
    }

    /** C6 — l'identifiant de tête : la signature ne participe jamais au flou. */
    @Test
    void lIdentifiantDeTete() {
        assertEquals("Text", CompletionMatcher.leadingIdentifier("Text(text: String)"));
        assertEquals("foo", CompletionMatcher.leadingIdentifier("foo()"));
        assertEquals("android:layout_width",
                CompletionMatcher.leadingIdentifier("android:layout_width"));
        assertEquals("x", CompletionMatcher.leadingIdentifier("x: Int"));
        assertEquals("foo", CompletionMatcher.leadingIdentifier("foo."));
        assertEquals(CompletionMatcher.TIER_NONE,
                CompletionMatcher.tier("Text(text: String)", "", "text:"),
                "la signature ne participe jamais au flou");
    }

    /** C6 — les groupes départagent : résolu, mot-clé, gabarit, mot du tampon. */
    @Test
    void lesGroupes() {
        assertEquals(CompletionMatcher.GROUP_RESOLVED,
                CompletionMatcher.group(true, false, false));
        assertEquals(CompletionMatcher.GROUP_KEYWORD,
                CompletionMatcher.group(false, true, false));
        assertEquals(CompletionMatcher.GROUP_SNIPPET,
                CompletionMatcher.group(false, false, true));
        assertEquals(CompletionMatcher.GROUP_BUFFER_WORD,
                CompletionMatcher.group(false, false, false));
    }

    /** C6 — les positions en gras : exactement les caractères qui ont fait correspondre. */
    @Test
    void lesPositionsEnGras() {
        assertEquals(List.of(0, 1, 2, 3),
                CompletionMatcher.matchPositions("total", "tota"));
        assertEquals(List.of(0, 3),
                CompletionMatcher.matchPositions("getString", "gS"));
        assertEquals(List.of(0, 7, 8),
                CompletionMatcher.matchPositions("layout_width", "lwi"),
                "la bosse mène : l en tête, w après le séparateur, i en run contigu");
        assertEquals(List.of(0, 7, 9),
                CompletionMatcher.matchPositions("layout_width", "lwd"),
                "la sous-séquence gloutonne quand la bosse ne mène pas");
        assertEquals(List.of(),
                CompletionMatcher.matchPositions("total", "xyz"));
    }

    /** C6 — l'apprentissage : fréquence puis fraîcheur, compartiments étanches, éviction. */
    @Test
    void lApprentissageDuClassement() {
        AcceptanceHistory historique = new AcceptanceHistory();
        assertEquals(0, historique.boost("java", "total"), "l'inconnu vaut 0");

        historique.record("java", "total");
        long premiere = historique.boost("java", "total");
        assertTrue(premiere > 0, "l'accepté remonte");

        historique.record("java", "total");
        assertTrue(historique.boost("java", "total") > premiere,
                "la fréquence monte le score");

        historique.record("java", "prix");
        historique.record("java", "prix");
        assertTrue(historique.boost("java", "prix") > historique.boost("java", "total"),
                "à fréquence égale, le plus récent passe devant");
        assertEquals(0, historique.boost("xml", "total"),
                "les langages sont des compartiments étanches");
        assertEquals(2, historique.size());
    }

    /** C6 — la bascule, la purge, les compteurs. */
    @Test
    void laBasculeEtLaPurge() {
        AcceptanceHistory historique = new AcceptanceHistory();
        historique.record("java", "total");
        historique.setEnabled(false);
        historique.record("java", "prix");
        assertEquals(0, historique.boost("java", "total"), "la bascule coupée ne booste pas");
        assertEquals(1, historique.size(), "la bascule coupée n'enregistre pas");
        historique.setEnabled(true);
        assertEquals(1, historique.recordedAcceptanceCount());
        historique.boost("java", "total");
        assertEquals(2, historique.boostedAcceptanceCount());

        historique.clear("java");
        assertEquals(0, historique.size());
        historique.record("java", "total");
        historique.clear();
        assertEquals(0, historique.size());
        assertEquals(0, historique.recordedAcceptanceCount(), "la purge totale efface les compteurs");
    }

    /** C6 — la persistance : la photographie fait l'aller-retour, la version inconnue refuse. */
    @Test
    void laPersistance() {
        AcceptanceHistory historique = new AcceptanceHistory();
        historique.record("java", "total");
        historique.record("java", "total");
        AcceptanceHistory.Snapshot photographie = historique.snapshot();
        assertEquals(AcceptanceHistory.Snapshot.FORMAT_VERSION, photographie.version());

        AcceptanceHistory relue = new AcceptanceHistory();
        relue.record("xml", "element");
        relue.restore(photographie);
        assertEquals(historique.boost("java", "total"), relue.boost("java", "total"));
        assertEquals(0, relue.boost("xml", "element"), "la restauration remplace intégralement");

        relue.record("java", "prix");
        relue.record("java", "prix");
        assertTrue(relue.boost("java", "prix") > relue.boost("java", "total"),
                "la fraîcheur rebasse : jamais plus vieille qu'une entrée restaurée");
        assertThrows(IllegalArgumentException.class,
                () -> relue.restore(new AcceptanceHistory.Snapshot(Map.of(), 99)));
    }

    /** C6 — le contexte d'énumération : après le point, les constantes déclarées. */
    @Test
    void leContexteDenumeration() {
        String source = """
                enum Couleur {
                    ROUGE,
                    VERT,
                    BLEU
                }

                class Achat {
                    Couleur c = Couleur.
                }
                """;
        ContextualCompletion.Context contexte = contexte(source, source.indexOf("Couleur.") + 8);
        assertEquals(ContextualCompletion.Kind.ENUM_CONSTANTS, contexte.kind());
        assertEquals(List.of("ROUGE", "VERT", "BLEU"), contexte.candidates());
    }

    /** C6 — le contexte d'annotation : dans la parenthèse, les attributs déclarés. */
    @Test
    void leContexteDannotation() {
        String source = """
                @interface Anno {
                    String nom();
                    int valeur();
                }

                class Achat {
                    @Anno()
                    void facture() {
                    }
                }
                """;
        int caret = source.indexOf("@Anno()") + "@Anno(".length();
        ContextualCompletion.Context contexte = contexte(source, caret);
        assertEquals(ContextualCompletion.Kind.ANNOTATION_ATTRIBUTES, contexte.kind());
        assertEquals(List.of("nom", "valeur"), contexte.candidates());
    }

    /** C6 — sans contexte, rien : jamais un contexte deviné. */
    @Test
    void sansContexte() {
        String source = """
                class Achat {
                    void facture() {
                        total.
                    }
                }
                """;
        int caret = source.indexOf("total.") + "total.".length();
        assertEquals(ContextualCompletion.Kind.NONE, contexte(source, caret).kind());

        Document doc = Document.of("class A {\n}\n");
        Analysis analyse = java.analyseFull(doc);
        assertThrows(IllegalArgumentException.class,
                () -> ContextualCompletion.context(doc, analyse, -1));
    }

    /** C6 — le plancher fusionné : brancher un fournisseur ne retire ni les mots-clés ni les gabarits. */
    @Test
    void lePlancherFusionne() {
        FusedFloor.Floor plancher = new FusedFloor.Floor(
                List.of("to", "toString"),
                List.of("tostring"),
                List.of("totaux", "totalement"));
        List<String> fusionnes = FusedFloor.merged(
                List.of("total(int)", "toString()"), plancher, "to");
        assertEquals(List.of("to", "total(int)", "toString()", "tostring",
                "totaux", "totalement"), fusionnes,
                "le mot-clé exact précède, les résolus au palier préfixe dans l'ordre du fournisseur, "
                        + "le gabarit et les mots du tampon suivent — rien ne disparaît");
        assertEquals(List.of(), FusedFloor.merged(List.of("prix"), plancher, "xyz"),
                "le préfixe qui ne correspond à rien rend une liste vide");
        assertEquals(List.of("to", "total(int)", "toString", "tostring",
                "totaux", "totalement"),
                FusedFloor.merged(List.of("total(int)", "prix"), plancher, "to"),
                "le mot-clé homonyme du résolu se dédie, le reste du plancher suit");
        FusedFloor.Floor avecDoublon = new FusedFloor.Floor(List.of(), List.of(),
                List.of("total"));
        assertEquals(List.of("total(int)"),
                FusedFloor.merged(List.of("total(int)"), avecDoublon, "to"),
                "le doublon se dédie : le résolu du fournisseur précède le mot du tampon homonyme");
    }

    /** Le contexte au curseur, sur l'analyse Java du source. */
    private ContextualCompletion.Context contexte(String source, int caret) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        return ContextualCompletion.context(doc, analyse, caret);
    }
}
