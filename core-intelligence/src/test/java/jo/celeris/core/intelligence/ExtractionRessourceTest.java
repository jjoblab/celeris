package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * L'extraction d'une chaîne vers une ressource (C-13) : le littéral
 * devient la référence, la ressource naît dans le fichier de valeurs —
 * le critère se prouve aux trois morceaux : ressource créée, référence
 * insérée, transaction annulable (un geste d'annulation par fichier).
 * Les refus se signalent par un plan nul, jamais par un plan faux.
 */
class ExtractionRessourceTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String ACTIVITE = """
            package demo;

            public class Accueil extends Activity {
                public void onCreate(Bundle b) {
                    setTitle("Bienvenue a bord");
                    log("Trace du lancement");
                }
            }
            """;

    private static final String VALEURS = """
            <resources>
                <string name="app_name">Celeris</string>
                <string name="titre">Accueil</string>
            </resources>
            """;

    private static final String URI_JAVA = "src/demo/Accueil.java";
    private static final String URI_VALEURS = "res/values/strings.xml";

    private Map<String, String> projet() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put(URI_JAVA, ACTIVITE);
        fichiers.put(URI_VALEURS, VALEURS);
        return fichiers;
    }

    private int auCurseurDe(String expression) {
        int index = ACTIVITE.indexOf(expression);
        assertTrue(index >= 0, "expression absente du corpus : " + expression);
        return index;
    }

    private ExtractionRessource.PlanRessource planSur(String expression, String nom) {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        ExtractionRessource.PlanRessource plan = ExtractionRessource.plan(
                URI_JAVA, doc, analyse, auCurseurDe(expression), nom, projet());
        assertNotNull(plan, "le plan doit naître sur le littéral « " + expression + " »");
        return plan;
    }

    /** Les éditions d'un fichier du plan : présentes, jamais nulles. */
    private List<TextEdit> editions(ExtractionRessource.PlanRessource plan, String uri) {
        List<TextEdit> liste = plan.editions().get(uri);
        assertNotNull(liste, "le plan doit porter l'édition de " + uri);
        return liste;
    }

    /** C-13 — le critère entier : ressource créée, référence insérée, annulable. */
    @Test
    void laRessourceNaitEtLaReferenceRemplace() {
        ExtractionRessource.PlanRessource plan =
                planSur("\"Bienvenue a bord\"", "message_bienvenue");
        assertEquals(2, plan.editions().size(), "deux fichiers touchés : le java et les valeurs");
        TextSession sessionJava = TextSession.of(ACTIVITE, System::nanoTime);
        TextSession sessionValeurs = TextSession.of(VALEURS, System::nanoTime);
        assertTrue(sessionJava.apply(
                Transaction.of(editions(plan, URI_JAVA))), "l'édition du document s'applique");
        assertTrue(sessionValeurs.apply(
                Transaction.of(editions(plan, URI_VALEURS))), "l'édition des valeurs s'applique");
        // la référence est insérée au point du littéral
        assertTrue(sessionJava.text().contains("setTitle(getString(R.string.message_bienvenue));"),
                "le littéral devient la référence : " + sessionJava.text());
        assertTrue(!sessionJava.text().contains("\"Bienvenue a bord\""),
                "le littéral remplacé ne reste pas");
        // la ressource naît dans le fichier de valeurs, au retrait des
        // entrées existantes, avant la fermeture
        assertTrue(sessionValeurs.text().contains(
                "    <string name=\"message_bienvenue\">Bienvenue a bord</string>\n</resources>"),
                "la ressource naît avant la fermeture, au retrait des siennes : "
                        + sessionValeurs.text());
        // annulable : l'original revient, un geste d'annulation par fichier
        sessionJava.undo();
        sessionValeurs.undo();
        assertEquals(ACTIVITE, sessionJava.text());
        assertEquals(VALEURS, sessionValeurs.text());
    }

    /** C-13 — le nom déjà défini dans le projet refuse : jamais de doublon. */
    @Test
    void leNomDejaDefiniRefuse() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                auCurseurDe("\"Bienvenue a bord\""), "titre", projet()));
    }

    /** C-13 — hors littéral de chaîne : curseur sur un identifiant, plan nul. */
    @Test
    void horsLitteralRefuse() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                auCurseurDe("setTitle"), "autre", projet()));
    }

    /** C-13 — le littéral apostrophé (un char) ne se ressource pas. */
    @Test
    void leLitteralApostropheRefuse() {
        String source = """
                class F {
                    char c = 'x';
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                source.indexOf("'x'") + 1, "lettre", projet()));
    }

    /** C-13 — sans fichier de valeurs dans le projet, plan nul. */
    @Test
    void sansFichierDeValeursRefuse() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                auCurseurDe("\"Bienvenue a bord\""), "message",
                Map.of(URI_JAVA, ACTIVITE)));
    }

    /** C-13 — le fichier de valeurs sans fermeture de ressources refuse. */
    @Test
    void leFichierDeValeursBriseRefuse() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put(URI_JAVA, ACTIVITE);
        fichiers.put(URI_VALEURS, "<resources><string name=\"a\">A");
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                auCurseurDe("\"Bienvenue a bord\""), "message", fichiers));
    }

    /** C-13 — le nom de ressource : camelCase et chiffre de tête refusent. */
    @Test
    void leNomInvalideRefuse() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        int curseur = auCurseurDe("\"Bienvenue a bord\"");
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                curseur, "MessageBienvenue", projet()));
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                curseur, "2message", projet()));
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                curseur, "message-titre", projet()));
    }

    /** C-13 — le nom blanc se refuse d'entrée, jamais par un plan faux. */
    @Test
    void leNomBlancRefuseDEntree() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        assertThrows(IllegalArgumentException.class, () -> ExtractionRessource.plan(
                URI_JAVA, doc, analyse, auCurseurDe("\"Bienvenue a bord\""), " ", projet()));
    }

    /** C-13 — la chaîne vide ne se ressource pas. */
    @Test
    void laChaineVideRefuse() {
        String source = """
                class F {
                    String s = "";
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        assertNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                source.indexOf("\"\"") + 1, "vide", projet()));
    }

    /** C-13 — les échappements se décodent, les entités XML se protègent. */
    @Test
    void lesEchappementsEtEntitesSeTraduisent() {
        String source = """
                class F {
                    String s = "Ligne1\\nLigne2 & <suite>";
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put(URI_VALEURS, VALEURS);
        ExtractionRessource.PlanRessource plan = ExtractionRessource.plan(
                URI_JAVA, doc, analyse, source.indexOf("Ligne1"), "texte_multi", fichiers);
        assertNotNull(plan);
        TextSession sessionValeurs = TextSession.of(VALEURS, System::nanoTime);
        sessionValeurs.apply(Transaction.of(editions(plan, URI_VALEURS)));
        assertTrue(sessionValeurs.text().contains(
                "<string name=\"texte_multi\">Ligne1\nLigne2 &amp; &lt;suite&gt;</string>"),
                "l'échappement devient une vraie ligne, les entités se protègent : "
                        + sessionValeurs.text());
        // la référence du document reste la forme d'appel
        assertEquals("getString(R.string.texte_multi)", editions(plan, URI_JAVA).get(0).inserted());
    }

    /** C-13 — le fichier de valeurs sans entrée : la ressource pose à un cran de la fermeture. */
    @Test
    void leFichierViergePoseACranDeLaFermeture() {
        String valeurs = "<resources>\n</resources>\n";
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put(URI_VALEURS, valeurs);
        ExtractionRessource.PlanRessource plan = ExtractionRessource.plan(
                URI_JAVA, doc, analyse, auCurseurDe("\"Bienvenue a bord\""),
                "message", fichiers);
        assertNotNull(plan);
        TextSession sessionValeurs = TextSession.of(valeurs, System::nanoTime);
        sessionValeurs.apply(Transaction.of(editions(plan, URI_VALEURS)));
        assertTrue(sessionValeurs.text().contains(
                        "    <string name=\"message\">Bienvenue a bord</string>\n</resources>"),
                "l'entrée vierge pose à un cran de la fermeture : " + sessionValeurs.text());
    }

    /** C-13 — le curseur aux bornes du littéral le touche : le plan naît. */
    @Test
    void leCurseurAuxBornesToucheLeLitteral() {
        Document doc = Document.of(ACTIVITE);
        Analysis analyse = java.analyseFull(doc);
        int debut = auCurseurDe("\"Trace du lancement\"");
        assertNotNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                debut, "trace_lancement", projet()));
        assertNotNull(ExtractionRessource.plan(URI_JAVA, doc, analyse,
                debut + "\"Trace du lancement\"".length(), "trace_lancement", projet()));
    }

    /** C-13 — les éditions descendent : l'ordre d'application reste le source. */
    @Test
    void lesEditionsDescendent() {
        ExtractionRessource.PlanRessource plan =
                planSur("\"Bienvenue a bord\"", "message_bienvenue");
        List<TextEdit> editionsJava = editions(plan, URI_JAVA);
        assertEquals(1, editionsJava.size());
        assertEquals(ACTIVITE.indexOf("\"Bienvenue a bord\""), editionsJava.get(0).start());
        assertEquals("\"Bienvenue a bord\"", editionsJava.get(0).removed());
    }
}
