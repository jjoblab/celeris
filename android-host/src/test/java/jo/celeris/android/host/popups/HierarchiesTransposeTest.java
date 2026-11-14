package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * La transpose des hiérarchies chez l'orchestrateur (le déménagement
 * du 2026-11-14, la quatrième surface portée) : le MOTEUR réel sert
 * l'arbre sur le projet embarqué (le document ouvert), la transpose
 * pure en fait les sections de la carte — l'audit Android Studio du
 * 2026-11-14 y aligne l'arbre sur l'IDE de référence : la RACINE vit
 * UNE fois (l'en-tête la nomme — l'arbre de l'IDE porte le type
 * interrogé à sa racine, jamais il ne se répète dans les sections),
 * l'introuvable vit muet à la FRONTIÈRE seule, la section vide ne se
 * pose pas. Pur, sans appareil — les régimes de la carte vivent au
 * banc du kit ({@code PopupControllerTest},
 * {@code HierarchiesViewTest}).
 */
final class HierarchiesTransposeTest {

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

            public class Chaton extends Chat {
            }
            """;

    /** Les sections de types : la racine ne s'y répète jamais (elle
     *  vit à l'en-tête — l'arbre de l'IDE), le connu se prend,
     *  l'introuvable vit muet à la frontière SEULE. */
    @Test
    void lesSectionsDeTypesServentLeConnuEtLaFrontiere() {
        Document document = Document.of(TERRAIN);
        Hierarchies.HierarchieTypes arbre = Hierarchies.types("Chat",
                Map.of("Exemple.java", document), java);
        List<String> courses = new ArrayList<>();
        List<HierarchiesView.Section> sections =
                CarteHierarchies.Transpose.deTypes(arbre,
                        Set.of("Animal", "Chat", "Chaton", "respire",
                                "miaule"),
                        courses::add, "Supertypes", "Sous-types",
                        "Hors projet");
        assertEquals(3, sections.size(),
                "supertypes, sous-types et la frontière");
        assertEquals("Supertypes", sections.get(0).libelle());
        assertEquals("Animal", sections.get(0).sommets().get(0).libelle());
        assertEquals(1, sections.get(0).sommets().get(0).profondeur(),
                "le parent vit à sa génération — la racine est à l'en-tête");
        assertEquals("Sous-types", sections.get(1).libelle());
        assertEquals("Chaton", sections.get(1).sommets().get(0).libelle());
        // la frontière : Perroquet ne vit pas dans le projet — muet à
        // la frontière SEULE, jamais deux fois
        HierarchiesView.Sommet frontiere = sections.get(2).sommets().get(0);
        assertEquals("Perroquet", frontiere.libelle());
        assertNull(frontiere.saut(),
                "le parent hors projet ne se prend jamais");
        // l'audit Android Studio : la racine « Chat » ne vit dans
        // AUCUNE section — l'en-tête seul la nomme
        for (HierarchiesView.Section section : sections) {
            for (HierarchiesView.Sommet sommet : section.sommets()) {
                assertTrue(!sommet.libelle().equals("Chat"),
                        "la racine vit à l'en-tête, jamais en section : "
                                + sommet.libelle());
            }
        }
        // le sommet connu saute : le sauteur reçoit SON nom
        sections.get(0).sommets().get(0).saut().run();
        assertEquals(List.of("Animal"), courses);
    }

    /** Les sections d'appels : la racine vit à l'en-tête (jamais en
     *  section), les appelants en profondeur — chaque sommet connu
     *  porte son saut. */
    @Test
    void lesSectionsDAppelsServentLesAppelantsSansLaRacine() {
        Document document = Document.of(TERRAIN);
        Hierarchies.HierarchieAppels arbre = Hierarchies.appelants("respire",
                Map.of("Exemple.java", document), java);
        List<String> courses = new ArrayList<>();
        List<HierarchiesView.Section> sections =
                CarteHierarchies.Transpose.dAppels(arbre,
                        Set.of("Animal", "Chat", "Chaton", "respire",
                                "miaule"),
                        courses::add, "Callers");
        assertEquals(1, sections.size());
        HierarchiesView.Sommet appelant = sections.get(0).sommets().get(0);
        assertEquals("miaule", appelant.libelle(),
                "l'appelant porte la méthode — la racine vit à l'en-tête");
        assertEquals(1, appelant.profondeur());
        appelant.saut().run();
        assertEquals(List.of("miaule"), courses);
        for (HierarchiesView.Sommet sommet : sections.get(0).sommets()) {
            assertTrue(!sommet.libelle().equals("respire"),
                    "la racine vit à l'en-tête, jamais en section : "
                            + sommet.libelle());
        }
    }

    /** La section VIDE ne se pose pas (l'arbre de l'IDE ne montre que
     *  les nœuds qui existent) — un type sans parent déclaré sert une
     *  seule section. */
    @Test
    void laSectionVideNeSePosePas() {
        Document document = Document.of(TERRAIN);
        Hierarchies.HierarchieTypes arbre = Hierarchies.types("Animal",
                Map.of("Exemple.java", document), java);
        List<HierarchiesView.Section> sections =
                CarteHierarchies.Transpose.deTypes(arbre,
                        Set.of("Animal", "Chat", "Chaton", "respire",
                                "miaule"),
                        nom -> { }, "Supertypes", "Sous-types",
                        "Hors projet");
        // Animal : sans parent ni frontière — seule la section des
        // sous-types vit (Chaton... par Chat ? NON : les descendants
        // d'Animal sont Chat puis Chaton)
        assertTrue(sections.size() >= 1,
                "les sections existantes seules se posent");
        for (HierarchiesView.Section section : sections) {
            assertTrue(!section.sommets().isEmpty(),
                    "une section vide ne se pose jamais : "
                            + section.libelle());
        }
    }
}
