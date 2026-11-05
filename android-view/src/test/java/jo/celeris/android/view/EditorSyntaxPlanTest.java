package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le plan consommateur côté pont (B-16, tranche 8 M4) : les entrées du
 * plan sortent du MÊME arbre que la coloration — le pont pousse les
 * entrées de l'analyse qu'il habille, jamais d'une autre révision — et
 * se mettent à jour à l'édition. Le retrait du langage vide le plan :
 * le consommateur ne garde jamais les entrées d'un document parti.
 */
class EditorSyntaxPlanTest {

    private static final String SOURCE = """
            package demo;

            class Exemple {
                private int champ;

                void premiere() {
                }

                void seconde() {
                }
            }
            """;

    /** Une source qui ne rend rien : habiller l'enveloppe, jamais ne la consulte ici. */
    private static final RowSource SOURCE_MUETTE = cle -> {
        throw new UnsupportedOperationException("pas consultée par le pont");
    };

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /**
     * Le plan poussé est exactement celui du même arbre que la
     * coloration : une analyse indépendante du même instantané produit
     * les mêmes entrées, à la valeur près.
     */
    @Test
    void planPousseDuMemeArbreQueLaColoration() {
        List<List<OutlineEntry>> plans = new ArrayList<>();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel());
        syntaxe.setSurPlan(plans::add);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);

        Document document = session.document();
        syntaxe.habiller(SOURCE_MUETTE, document);

        assertEquals(2, plans.size(), "le rattachement vide, l'analyse habille");
        assertTrue(plans.get(0).isEmpty(), "un document sans analyse n'a pas de plan");
        List<OutlineEntry> pousse = plans.get(1);
        List<OutlineEntry> attendu = TreeOutline.entries(document,
                SyntaxService.of(JavaSyntax.INSTANCE).analyseFull(document));
        assertEquals(attendu, pousse, "le plan poussé sort du même arbre");
        assertTrue(pousse.stream().anyMatch(e -> "Exemple".equals(e.name())),
                "la classe figure au plan");
        assertEquals(NodeCategory.TYPE,
                pousse.stream().filter(e -> "Exemple".equals(e.name()))
                        .findFirst().orElseThrow().category());
        assertTrue(pousse.stream().anyMatch(e -> "premiere".equals(e.name())),
                "la méthode figure au plan");
        syntaxe.fermer();
    }

    /** L'édition renomme une déclaration : le plan poussé la suit. */
    @Test
    void planSeMetAJourALaEdition() {
        List<List<OutlineEntry>> plans = new ArrayList<>();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel());
        syntaxe.setSurPlan(plans::add);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        int avant = plans.size();

        int cible = SOURCE.indexOf("premiere");
        session.replaceRange(cible, cible + 8, "troisieme");
        syntaxe.habiller(SOURCE_MUETTE, session.document());

        assertEquals(avant + 1, plans.size(), "l'édition republie le plan");
        List<OutlineEntry> pousse = plans.get(plans.size() - 1);
        assertTrue(pousse.stream().anyMatch(e -> "troisieme".equals(e.name())),
                "le renommé figure au plan");
        assertTrue(pousse.stream().noneMatch(e -> "premiere".equals(e.name())),
                "l'ancien nom a disparu du plan");
        syntaxe.fermer();
    }

    /**
     * La voie hors fil pousse le plan de la publication à jour — jamais
     * le lexique d'une autre révision ne colore, jamais le plan d'une
     * autre révision ne s'affiche.
     */
    @Test
    void planPousseHorsFilApresPublication() {
        List<List<OutlineEntry>> plans = new ArrayList<>();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        syntaxe.setSurPlan(plans::add);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        int avant = plans.size();
        assertTrue(plans.get(avant - 1).stream()
                .anyMatch(e -> "Exemple".equals(e.name())), "le plan initial est là");

        int cible = SOURCE.indexOf("seconde");
        session.replaceRange(cible, cible + 7, "quatrieme");
        syntaxe.habiller(SOURCE_MUETTE, session.document());

        List<OutlineEntry> pousse = plans.get(plans.size() - 1);
        assertTrue(pousse.stream().anyMatch(e -> "quatrieme".equals(e.name())),
                "le plan suit la publication de la nouvelle révision");
        syntaxe.fermer();
    }

    /** Le retrait du langage vide le plan : le consommateur n'affiche rien. */
    @Test
    void retraitDuLangageVideLePlan() {
        List<List<OutlineEntry>> plans = new ArrayList<>();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        syntaxe.setSurPlan(plans::add);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        assertTrue(plans.get(plans.size() - 1).size() > 0, "un plan est affiché");

        syntaxe.setLanguage(null);

        assertTrue(plans.get(plans.size() - 1).isEmpty(),
                "le retrait du langage vide le plan");
        syntaxe.fermer();
    }
}
