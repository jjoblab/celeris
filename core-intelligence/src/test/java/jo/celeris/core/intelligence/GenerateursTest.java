package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * La génération de code (B-24) : constructeur, accesseurs,
 * equals/hashCode/toString — générés depuis les champs que l'arbre
 * déclare, conformes aux conventions. La position d'insertion vit
 * avant l'accolade fermante de la classe.
 */
class GenerateursTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String SOURCE = """
            package demo;

            public class Personne {
                private final String nom;
                private int age;
                private boolean actif;

                public Personne() {
                }
            }
            """;

    private List<Generateurs.Champ> champs() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        return Generateurs.champs(doc, analyse, "Personne");
    }

    /** Les champs lus dans l'arbre : type et nom, dans l'ordre du source. */
    @Test
    void lesChampsLus() {
        List<Generateurs.Champ> champs = champs();
        assertEquals(3, champs.size());
        assertEquals("String", champs.get(0).type());
        assertEquals("nom", champs.get(0).nom());
        assertEquals("int", champs.get(1).type());
        assertEquals("age", champs.get(1).nom());
        assertEquals("boolean", champs.get(2).type());
        assertEquals("actif", champs.get(2).nom());
    }

    /** Le constructeur : paramètres et affectations, un par champ. */
    @Test
    void leConstructeur() {
        String texte = Generateurs.constructeur("Personne", champs());
        assertEquals("""
                public Personne(String nom, int age, boolean actif) {
                    this.nom = nom;
                    this.age = age;
                    this.actif = actif;
                }
                """.stripTrailing(), texte.stripTrailing());
    }

    /** Les accesseurs : getter et setter par champ, conventions de nommage. */
    @Test
    void lesAccesseurs() {
        String texte = Generateurs.accesseurs(champs());
        assertTrue(texte.contains("public String getNom()"));
        assertTrue(texte.contains("return nom;"));
        assertTrue(texte.contains("public void setAge(int age)"));
        assertTrue(texte.contains("this.age = age;"));
        // le booléen : isActif, pas getActif — la convention Java
        assertTrue(texte.contains("public boolean isActif()"));
        assertTrue(!texte.contains("getActif"));
    }

    /** equals/hashCode/toString : tous les champs, les conventions tenues. */
    @Test
    void equalsHashCodeToString() {
        String texte = Generateurs.equalsHashCodeToString("Personne", champs());
        assertTrue(texte.contains("@Override"));
        assertTrue(texte.contains("public boolean equals(Object o)"));
        assertTrue(texte.contains("public int hashCode()"));
        assertTrue(texte.contains("Objects.hash(nom, age, actif)"));
        assertTrue(texte.contains("public String toString()"));
        assertTrue(texte.contains("return \"Personne{nom=\" + nom"));
    }

    /** La position d'insertion : avant l'accolade fermante de la classe. */
    @Test
    void laPositionInsertion() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        Generateurs.Champ ignored = Generateurs.champs(doc, analyse, "Personne").get(0);
        Symbole classe = Generateurs.classeDe(doc, analyse, "Personne");
        org.junit.jupiter.api.Assertions.assertNotNull(classe);
        int position = Generateurs.positionInsertion(classe);
        // juste avant le « } » qui ferme la classe
        assertEquals('}', doc.charAt(position));
        assertEquals('}', doc.charAt(doc.length() - 2));
        assertTrue(position < doc.length());
    }

    /** Sans champ : le constructeur vide, aucun accesseur, les trois méthodes tiennent. */
    @Test
    void sansChamp() {
        String constructeur = Generateurs.constructeur("Vide", List.of());
        assertTrue(constructeur.contains("public Vide()"));
        assertEquals("", Generateurs.accesseurs(List.of()));
        String eht = Generateurs.equalsHashCodeToString("Vide", List.of());
        assertTrue(eht.contains("Objects.hash()"));
    }
}
