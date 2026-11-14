package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jo.celeris.core.intelligence.LecteurTypes;
import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * La lecture du type du symbole au caret (catégorie A de l'inventaire
 * — le fil rouge du guide du développeur, la seconde surface réveillée
 * du signal du propriétaire) : le lecteur lit la DÉCLARATION du symbole
 * dans le document — « Type symbole », le mot précédent le nom, hors
 * mots réservés — la déclaration introuvable rend null, jamais un type
 * deviné (la carte ne se pose pas sans type : aucune fausse oracle).
 */
class TypeInfoTest {

    /** La déclaration se lit sur la ligne du caret : le mot précédent
     *  le symbole porte le type. */
    @Test
    void laDeclarationSeLitSurLaLigne() {
        Document document = Document.of("int total = 1;\ntotal += 2;\n");
        assertEquals("int",
                LecteurTypes.typeDe(document, 5),
                "la déclaration porte son type");
        assertEquals("int",
                LecteurTypes.typeDe(document, document.text().indexOf("total", 12) + 1),
                "l'usage renvoie à SA déclaration, où qu'elle vive");
    }

    /** Les formes riches se lisent : génériques et tableaux suivent le
     *  mot de type, la déclaration qualifiée reste entière. */
    @Test
    void lesFormesRichesSeLisent() {
        Document document = Document.of(
                "List<String> noms = new ArrayList<>();\n"
                + "final String salut = \"bon\";\n"
                + "int[] compteurs = null;\n");
        assertEquals("List<String>",
                LecteurTypes.typeDe(document, document.text().indexOf("noms") + 1));
        assertEquals("String",
                LecteurTypes.typeDe(document, document.text().indexOf("salut") + 1));
        assertEquals("int[]",
                LecteurTypes.typeDe(document, document.text().indexOf("compteurs") + 1));
    }

    /** Le mot réservé ne se prend jamais pour un type : « return
     *  total » ne déclare rien, le doute rend null. */
    @Test
    void leMotReserveNeSePrendPasPourUnType() {
        Document conduite = Document.of("return total;\n");
        assertNull(LecteurTypes.typeDe(conduite, conduite.text().indexOf("total") + 1),
                "le mot réservé ne déclare pas — jamais de type deviné");
        Document usageSeul = Document.of("total += 1;\n");
        assertNull(LecteurTypes.typeDe(usageSeul, 1),
                "sans déclaration dans le document, rien ne se devine");
        Document tableau = Document.of("for (int total : liste) {\n}\n");
        assertEquals("int",
                LecteurTypes.typeDe(tableau, tableau.text().indexOf("total") + 1),
                "la boucle for-each DÉCLARE sa variable — son type se lit");
    }
}
