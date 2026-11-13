package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.widgets.ExceptionView;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * La trace d'exécution de la démonstration (catégorie E de
 * l'inventaire — « Exception Details », la surface dormait) : l'échec
 * SIMULÉ naît du document réel — le type englobant porte la classe,
 * la méthode englobante porte le sommet À LA LIGNE DU CARET, les
 * cadres suivent à leurs lignes de déclaration ; la trace se rend au
 * format des piles puis se relit par l'analyseur (E-06) ; la
 * transpose sert les cadres de la carte — le cadre du fichier ouvert
 * saute, l'étranger vit muet. Pur, sans appareil.
 */
class TraceExecutionTest {

    private static final String TERRAIN = """
            class Service {
                void inscrire() {
                    aide();
                }
                void aide() {
                }
            }
            """;

    private List<OutlineEntry> planDe(TextSession session) {
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            return TreeOutline.entries(session.document(),
                    coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
    }

    /** L'échec simulé : le sommet vit à la ligne du caret dans sa
     *  méthode, les cadres suivent à leurs lignes de déclaration — la
     *  classe porte le type englobant, la trace relue par
     *  l'analyseur rend ses cadres exacts. */
    @Test
    void lechecPorteLeSommetALaLigneDuCaret() {
        TextSession session = TextSession.of(TERRAIN, () -> 0L);
        int aide = TERRAIN.indexOf("aide();");
        TraceExecution.Echec echec = TraceExecution.echec(
                "java.lang.Exception", "l'échec simulé", "Service.java",
                session.document(), planDe(session), aide);
        assertNotNull(echec, "le fichier porte type et méthodes");
        assertEquals("java.lang.Exception", echec.type());
        assertEquals("l'échec simulé", echec.message());
        assertEquals(2, echec.cadres().size(),
                "la méthode englobante au sommet, l'autre suit");
        assertEquals("Service", echec.cadres().get(0).classe());
        assertEquals("inscrire", echec.cadres().get(0).methode());
        assertEquals(3, echec.cadres().get(0).ligne(),
                "le sommet vit à la ligne du caret — jamais deviné");
        assertEquals("aide", echec.cadres().get(1).methode());
        assertEquals(5, echec.cadres().get(1).ligne(),
                "le cadre suivant vit à sa ligne de déclaration");
    }

    /** La transpose en cadres de la carte : le cadre du fichier ouvert
     *  saute à sa ligne, l'étranger vit muet — jamais deviné. */
    @Test
    void laTransposeSauteAuFichierOuvertSeul() {
        TextSession session = TextSession.of(TERRAIN, () -> 0L);
        int aide = TERRAIN.indexOf("aide();");
        TraceExecution.Echec echec = TraceExecution.echec(
                "java.lang.Exception", "l'échec simulé", "Service.java",
                session.document(), planDe(session), aide);
        List<Integer> sautes = new ArrayList<>();
        List<ExceptionView.Cadre> cadres = TraceExecution.cadres(
                new TraceExecution.Echec(echec.type(), echec.message(),
                        echec.cadres()),
                "Autre.java", sautes::add);
        assertEquals(2, cadres.size());
        assertNull(cadres.get(0).saut(),
                "le cadre étranger au fichier ouvert vit muet");
        assertEquals("L3 · Service.inscrire", cadres.get(0).libelle());
        List<ExceptionView.Cadre> ouverts = TraceExecution.cadres(echec,
                "Service.java", sautes::add);
        ouverts.get(0).saut().run();
        assertEquals(List.of(3), sautes,
                "le cadre du fichier ouvert saute à sa ligne");
    }

    /** Le fichier sans méthode ne peut échouer — rien à faire échouer
     *  ne se devine jamais. */
    @Test
    void leFichierSansMethodeNePeutEchouer() {
        TextSession session = TextSession.of("class Vide {\n}\n", () -> 0L);
        assertNull(TraceExecution.echec("java.lang.Exception",
                        "l'échec simulé", "Vide.java", session.document(),
                        planDe(session), 0),
                "sans méthode, l'exécution s'annonce terminée");
    }
}
