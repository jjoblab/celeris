package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;

import org.junit.jupiter.api.Test;

/**
 * Les résultats de l'aller à (catégorie C de l'inventaire, « Go to
 * File / Class / Symbol ») : la recherche floue (B-13) mélange les
 * fichiers du catalogue et les symboles du plan par score, les
 * fichiers d'abord à égalité — la requête muette sert le catalogue en
 * raccourci, le plafond ne déborde jamais. Pur, sans appareil — la
 * feuille n'y ajoute que les vues et le saut.
 */
class ResultatsAllerTest {

    private static final List<OutlineEntry> PLAN = List.of(
            new OutlineEntry(NodeCategory.TYPE, "ServiceUtilisateur",
                    33, 400, 2, 12),
            new OutlineEntry(NodeCategory.FUNCTION, "inscrire",
                    77, 140, 4, 6));

    /** La requête muette sert le catalogue en raccourci — les fichiers
     *  seuls, dans l'ordre, le plafond compris. */
    @Test
    void laRequeteMuetteSertLeCatalogue() {
        List<ResultatsAller.Resultat> resultats = ResultatsAller.recherche("",
                List.of("Exemple.java", "MainActivity.kt", "Styles.xml"),
                PLAN, 8);
        assertEquals(3, resultats.size());
        assertEquals(ResultatsAller.GENRE_FICHIER, resultats.get(0).genre());
        assertEquals("Exemple.java", resultats.get(0).libelle());
        assertEquals("java", resultats.get(0).detail());
        List<ResultatsAller.Resultat> borne = ResultatsAller.recherche(" ",
                List.of("A.java", "B.kt", "C.xml"), List.of(), 2);
        assertEquals(2, borne.size(), "le plafond ne déborde jamais");
    }

    /** La requête répond par le score : le préfixe gagne, les fichiers
     *  d'abord à égalité — les symboles portent leur ligne. */
    @Test
    void laRequeteRepondParLeScore() {
        List<ResultatsAller.Resultat> resultats = ResultatsAller.recherche(
                "Ser", List.of("ServiceUtilisateur.java", "Autre.kt"),
                PLAN, 8);
        assertEquals("ServiceUtilisateur.java", resultats.get(0).libelle(),
                "le préfixe du fichier gagne");
        assertEquals("ServiceUtilisateur", resultats.get(1).libelle(),
                "le symbole préfixé suit");
        assertEquals("L3", resultats.get(1).detail(),
                "le symbole porte SA ligne, numérotée à partir de un");
        assertEquals(ResultatsAller.GENRE_SYMBOLE, resultats.get(1).genre());
        assertTrue(resultats.size() == 2,
                "Autre.kt ne répond pas à la requête");
    }
}
