package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jo.celeris.android.widgets.HierarchiesView;
import jo.celeris.core.intelligence.Hierarchies;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * La transpose des hiérarchies du moteur à la carte (catégorie C de
 * l'inventaire — les moteurs vivaient, la surface dormait) : le
 * MOTEUR réel sert l'arbre sur le projet embarqué (le document
 * ouvert), la transpose pure en fait les sections de la carte — le
 * sommet CONNU se prend et son saut court, l'introuvable vit muet,
 * la racine d'abord. Pur, sans appareil — les régimes de la carte
 * vivent au banc du kit ({@code PopupControllerTest},
 * {@code HierarchiesViewTest}).
 */
class HierarchiesTransposeTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String TERRAIN = """
            package zoo;

            public class Animal {
                public void respire() {
                }
            }

            public class Chat extends Animal implements Perroquet {
                public void miaule() {
                    respire();
                }
            }
            """;

    /** Les sections de types : le connu se prend, l'introuvable vit
     *  muet — le saut du sommet choisi court chez le sauteur. */
    @Test
    void lesSectionsDeTypesServentLeConnuEtLaFrontiere() {
        Document document = Document.of(TERRAIN);
        Hierarchies.HierarchieTypes arbre = Hierarchies.types("Chat",
                Map.of("Exemple.java", document), java);
        List<String> courses = new ArrayList<>();
        List<HierarchiesView.Section> sections =
                AtelierHierarchies.Transpose.deTypes(arbre,
                        Set.of("Animal", "Chat", "respire", "miaule"),
                        courses::add, "Supertypes", "Sous-types",
                        "Hors projet");
        assertEquals(3, sections.size(),
                "supertypes, sous-types et la frontière");
        assertEquals("Supertypes", sections.get(0).libelle());
        assertEquals("Chat", sections.get(0).sommets().get(0).libelle());
        assertEquals(0, sections.get(0).sommets().get(0).profondeur(),
                "la racine vit à découvert");
        assertEquals("Animal", sections.get(0).sommets().get(1).libelle());
        assertEquals(1, sections.get(0).sommets().get(1).profondeur());
        // la frontière : Perroquet ne vit pas dans le projet — muet,
        // jamais pris
        HierarchiesView.Sommet frontiere = sections.get(2).sommets().get(0);
        assertEquals("Perroquet", frontiere.libelle());
        assertNull(frontiere.saut(),
                "le parent hors projet ne se prend jamais");
        // le sommet connu saute : le sauteur reçoit SON nom
        sections.get(0).sommets().get(1).saut().run();
        assertEquals(List.of("Animal"), courses);
    }

    /** Les sections d'appels : la racine d'abord, les appelants en
     *  profondeur — chaque sommet connu porte son saut. */
    @Test
    void lesSectionsDAppelsServentLaRacinePuisLesAppelants() {
        Document document = Document.of(TERRAIN);
        Hierarchies.HierarchieAppels arbre = Hierarchies.appelants("respire",
                Map.of("Exemple.java", document), java);
        List<String> courses = new ArrayList<>();
        List<HierarchiesView.Section> sections =
                AtelierHierarchies.Transpose.dAppels(arbre,
                        Set.of("Animal", "Chat", "respire", "miaule"),
                        courses::add, "Callers");
        assertEquals(1, sections.size());
        HierarchiesView.Sommet racine = sections.get(0).sommets().get(0);
        assertEquals("respire", racine.libelle());
        assertEquals(0, racine.profondeur(), "la racine d'abord");
        assertNotNull(racine.saut(), "la racine déclarée se prend");
        HierarchiesView.Sommet appelant = sections.get(0).sommets().get(1);
        assertEquals("miaule", appelant.libelle(),
                "l'appelant porte la méthode");
        assertEquals(1, appelant.profondeur());
        appelant.saut().run();
        assertEquals(List.of("miaule"), courses);
        assertTrue(sections.get(0).sommets().size() >= 2,
                "la racine et son appelant au moins");
    }
}
