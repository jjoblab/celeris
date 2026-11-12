package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * Les hiérarchies de types et d'appels (B-12) : les ancêtres et
 * descendants par les clauses lues à l'en-tête, les appelants par les
 * sites d'appel — la profondeur se prouve sur un corpus multi-fichiers,
 * les parents hors projet se signalent introuvables, le doute (récepteur
 * étranger, appel hors méthode) se signale sans arête, jamais ne se
 * tranche.
 */
class HierarchiesTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String ANIMAL = """
            package zoo;

            public class Animal {
                public void respire() {
                }
            }
            """;

    private static final String FAUVE = """
            package zoo;

            public class Fauve extends Animal {
            }
            """;

    private static final String CHAT = """
            package zoo;

            public class Chat extends Fauve {
                public void miaule() {
                    crie();
                }
            }
            """;

    private static final String CHIEN = """
            package zoo;

            public class Chien extends Animal implements Cri {
                public void aboie() {
                    crie();
                }
            }
            """;

    private static final String CRI = """
            package zoo;

            public interface Cri {
            }
            """;

    private static final String ENCLOS = """
            package zoo;

            public class Enclos<T extends Animal> implements Cri {
            }
            """;

    private static final String LICENCE = """
            package zoo;

            public class Licence extends java.lang.Exception {
            }
            """;

    private static final String SOIREE = """
            package zoo;

            public class Soiree {
                public void tour() {
                    accueil();
                }

                public void accueil() {
                    miaule();
                    aboie();
                }

                public void crie() {
                }
            }
            """;

    private static final String ORGANISATEUR = """
            package zoo;

            public class Organisateur {
                public void boucle() {
                    boucle();
                }
            }
            """;

    private static final String VUE = """
            package zoo;

            public class Vue extends View {
            }
            """;

    private Map<String, Document> projet() {
        Map<String, Document> fichiers = new LinkedHashMap<>();
        fichiers.put("zoo/Animal.java", Document.of(ANIMAL));
        fichiers.put("zoo/Chat.java", Document.of(CHAT));
        fichiers.put("zoo/Chien.java", Document.of(CHIEN));
        fichiers.put("zoo/Cri.java", Document.of(CRI));
        fichiers.put("zoo/Enclos.java", Document.of(ENCLOS));
        fichiers.put("zoo/Fauve.java", Document.of(FAUVE));
        fichiers.put("zoo/Licence.java", Document.of(LICENCE));
        fichiers.put("zoo/Organisateur.java", Document.of(ORGANISATEUR));
        fichiers.put("zoo/Soiree.java", Document.of(SOIREE));
        fichiers.put("zoo/Vue.java", Document.of(VUE));
        return fichiers;
    }

    /** B-12 — les ancêtres par générations : la profondeur trois sur le corpus. */
    @Test
    void lesAncetresParGenerations() {
        Hierarchies.HierarchieTypes hierarchie = Hierarchies.types("Chat", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("Chat", "zoo/Chat.java", 0),
                new Hierarchies.Sommet("Fauve", "zoo/Fauve.java", 1),
                new Hierarchies.Sommet("Animal", "zoo/Animal.java", 2)),
                hierarchie.ancetres(),
                "Chat s'étend sur Fauve puis Animal — trois générations");
        assertTrue(hierarchie.introuvables().isEmpty(),
                "tous les ancêtres vivent dans le projet embarqué");
    }

    /** B-12 — les descendants en profondeur : le parcours en largeur d'abord des sous-types. */
    @Test
    void lesDescendantsEnProfondeur() {
        Hierarchies.HierarchieTypes hierarchie = Hierarchies.types("Animal", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("Animal", "zoo/Animal.java", 0),
                new Hierarchies.Sommet("Chien", "zoo/Chien.java", 1),
                new Hierarchies.Sommet("Fauve", "zoo/Fauve.java", 1),
                new Hierarchies.Sommet("Chat", "zoo/Chat.java", 2)),
                hierarchie.descendants(),
                "Chien et Fauve étendent Animal, Chat étend Fauve — la profondeur deux");
    }

    /** B-12 — les interfaces comptent comme parents, la génération première les porte. */
    @Test
    void lesInterfacesComptentCommeParents() {
        Hierarchies.HierarchieTypes hierarchie = Hierarchies.types("Chien", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("Chien", "zoo/Chien.java", 0),
                new Hierarchies.Sommet("Animal", "zoo/Animal.java", 1),
                new Hierarchies.Sommet("Cri", "zoo/Cri.java", 1)),
                hierarchie.ancetres(),
                "extends et implements portent la même génération première");
    }

    /** B-12 — les parents hors du projet se signalent introuvables, la frontière s'arrête. */
    @Test
    void lesParentsHorsProjetSontIntrouvables() {
        Hierarchies.HierarchieTypes vue = Hierarchies.types("Vue", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("Vue", "zoo/Vue.java", 0),
                new Hierarchies.Sommet("View", "", 1)),
                vue.ancetres(), "View ne vit pas dans le projet embarqué");
        assertEquals(List.of("View"), vue.introuvables(), "la frontière se nomme, une fois");
        Hierarchies.HierarchieTypes licence = Hierarchies.types("Licence", projet(), java);
        assertEquals(List.of("Exception"), licence.introuvables(),
                "le nom qualifié prend son dernier segment — la frontière reste honnête");
    }

    /** B-12 — les bornes de généricité ne comptent pas de parents. */
    @Test
    void lesBornesDeGenericiteNeComptentPas() {
        Hierarchies.HierarchieTypes hierarchie = Hierarchies.types("Enclos", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("Enclos", "zoo/Enclos.java", 0),
                new Hierarchies.Sommet("Cri", "zoo/Cri.java", 1)),
                hierarchie.ancetres(),
                "le T extends Animal de l'en-tête est une borne, pas un parent");
    }

    /** B-12 — le nom inconnu reste la racine seule, sans mensonge. */
    @Test
    void leNomInconnuResteRacineSeule() {
        Hierarchies.HierarchieTypes hierarchie = Hierarchies.types("Inconnu", projet(), java);
        assertEquals(List.of(new Hierarchies.Sommet("Inconnu", "", 0)),
                hierarchie.ancetres());
        assertEquals(List.of(new Hierarchies.Sommet("Inconnu", "", 0)),
                hierarchie.descendants());
        assertTrue(hierarchie.introuvables().isEmpty());
    }

    /** B-12 — la profondeur d'appels sur le corpus : quatre crans jusqu'à tour. */
    @Test
    void laProfondeurDAppelsSurCorpus() {
        Hierarchies.HierarchieAppels hierarchie = Hierarchies.appelants("crie", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("crie", "zoo/Soiree.java", 0),
                new Hierarchies.Sommet("miaule", "zoo/Chat.java", 1),
                new Hierarchies.Sommet("accueil", "zoo/Soiree.java", 2),
                new Hierarchies.Sommet("tour", "zoo/Soiree.java", 3),
                new Hierarchies.Sommet("aboie", "zoo/Chien.java", 1)),
                hierarchie.appelants(),
                "crie se crie par miaule, aboié par accueil puis tour — la profondeur quatre");
        assertFalse(hierarchie.doute(), "tous les sites d'appel se résolvent, aucun doute");
    }

    /** B-12 — le récepteur étranger marque le doute, sans arête — jamais tranché. */
    @Test
    void leRecepteurEtrangerMarqueLeDoute() {
        Map<String, Document> fichiers = new LinkedHashMap<>();
        fichiers.put("zoo/Soiree.java", Document.of(SOIREE));
        fichiers.put("zoo/Organisateur.java", Document.of("""
                package zoo;

                public class Organisateur {
                    public void lance() {
                        Soiree soiree = new Soiree();
                        soiree.accueil();
                    }
                }
                """));
        Hierarchies.HierarchieAppels hierarchie = Hierarchies.appelants("accueil", fichiers, java);
        assertTrue(hierarchie.doute(),
                "l'appel sur soiree, récepteur étranger, se signale");
        assertTrue(hierarchie.appelants().stream().noneMatch(s -> s.nom().equals("lance")),
                "l'appel ambigu ne crée pas d'arête — jamais tranché");
        assertTrue(hierarchie.appelants().contains(
                        new Hierarchies.Sommet("tour", "zoo/Soiree.java", 1)),
                "l'appel nu de tour compte, lui");
    }

    /** B-12 — l'appel hors méthode (argument d'annotation de type) marque le doute, sans arête. */
    @Test
    void lAppelHorsMethodeMarqueLeDoute() {
        Map<String, Document> fichiers = new LinkedHashMap<>();
        fichiers.put("zoo/Champ.java", Document.of("""
                package zoo;

                @Journal(mode = crie())
                public class Champ {
                }
                """));
        Hierarchies.HierarchieAppels hierarchie = Hierarchies.appelants("crie", fichiers, java);
        assertTrue(hierarchie.doute(), "l'appel hors méthode ne se résout pas — signalé");
        assertEquals(List.of(new Hierarchies.Sommet("crie", "", 0)),
                hierarchie.appelants(), "aucune arête ne naît de l'appel hors méthode");
    }

    /** B-12 — la récursion directe se montre une fois, profondeur un, jamais en boucle. */
    @Test
    void laRecursionDirecteSeMontreUneFois() {
        Hierarchies.HierarchieAppels hierarchie = Hierarchies.appelants("boucle", projet(), java);
        assertEquals(List.of(
                new Hierarchies.Sommet("boucle", "zoo/Organisateur.java", 0),
                new Hierarchies.Sommet("boucle", "zoo/Organisateur.java", 1)),
                hierarchie.appelants(), "boucle s'appelle elle-même — montrée une fois");
    }

    /** B-12 — la construction ne compte pas : la hiérarchie d'appels sert les méthodes. */
    @Test
    void laConstructionNeComptePas() {
        Map<String, Document> fichiers = new LinkedHashMap<>();
        fichiers.put("zoo/Chien.java", Document.of("""
                package zoo;

                public class Chien {
                    public Chien() {
                    }

                    public void dresse() {
                        new Chien();
                    }
                }
                """));
        Hierarchies.HierarchieAppels hierarchie = Hierarchies.appelants("Chien", fichiers, java);
        assertEquals(List.of(new Hierarchies.Sommet("Chien", "zoo/Chien.java", 0)),
                hierarchie.appelants(),
                "new Chien() est une construction — la hiérarchie sert les méthodes");
    }

    /** B-12 — le nom blanc se refuse d'entrée, jamais par un arbre faux. */
    @Test
    void leNomBlancRefuseDEntree() {
        assertThrows(IllegalArgumentException.class,
                () -> Hierarchies.types(" ", projet(), java));
        assertThrows(IllegalArgumentException.class,
                () -> Hierarchies.appelants(" ", projet(), java));
    }
}
