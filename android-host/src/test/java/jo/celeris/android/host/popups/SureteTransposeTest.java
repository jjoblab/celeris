package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.widgets.ReferencesView;
import jo.celeris.core.intelligence.SafeDelete;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * La transpose de sûreté chez l'orchestrateur (le déménagement du
 * 2026-11-14, la sixième surface portée — reprise de
 * {@code AtelierSureteTest} de la démonstration) : les types candidats
 * au déménagement s'y lisent du plan (HORS la classe qui enclot le
 * caret — les conditions de sûreté du moteur refuseront ce qui pend,
 * jamais la carte ne préjuge), les usages qui barrent y deviennent des
 * occurrences cliquables (le libellé lu du document, le saut vers
 * l'offset). Pur, sans appareil — les régimes des cartes vivent au
 * banc du kit ({@code PopupControllerTest}).
 */
final class SureteTransposeTest {

    private static final String TERRAIN = """
            class Source {
                void corps() {
                    aide();
                }
                void aide() {
                }
            }

            class Cible {
                void autre() {
                }
            }
            """;

    /** Les types candidats excluent la classe qui enclot le caret —
     *  les autres types du fichier, dans l'ordre du plan. */
    @Test
    void lesCiblesExcluentLaClasseEnglobante() {
        TextSession session = TextSession.of(TERRAIN, () -> 0L);
        List<OutlineEntry> plan = planDe(session);
        int corps = TERRAIN.indexOf("aide();");
        assertEquals(List.of("Cible"), Surete.cibles(plan, corps),
                "la classe du caret ne se propose jamais à elle-même");
        int horsClasse = TERRAIN.indexOf("class Cible") - 1;
        assertEquals(List.of("Source", "Cible"),
                Surete.cibles(plan, horsClasse),
                "hors classe, tous les types se proposent");
    }

    /** Les usages qui barrent deviennent des occurrences cliquables :
     *  le libellé lu du document (« L12 · l'extrait »), le saut porte
     *  à l'offset exact de l'usage. */
    @Test
    void lesUsagesBarrantsDeviennentDesOccurrences() {
        TextSession session = TextSession.of(TERRAIN, () -> 0L);
        int aide = TERRAIN.indexOf("aide();");
        List<Integer> sautes = new ArrayList<>();
        List<ReferencesView.Occurrence> occurrences = Surete.occurrences(
                session.document(), List.of(new SafeDelete.Usage(aide, "aide")),
                sautes::add);
        assertEquals(1, occurrences.size());
        assertEquals("L3 · aide();", occurrences.get(0).libelle(),
                "le libellé se lit du document, ligne comprise");
        occurrences.get(0).saut().run();
        assertEquals(List.of(aide), sautes,
                "le saut porte à l'offset exact de l'usage");
    }

    /** Le plan du terrain — lecture fraîche, coordinateur fermé. */
    private List<OutlineEntry> planDe(TextSession session) {
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            return TreeOutline.entries(
                    session.document(), coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
    }
}
