package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * La minimap (chantier C9) : la silhouette du document — chaque rangée
 * de la carte porte la charge de son bouquet de lignes, la longueur de
 * la plus longue ligne plafonnée ; le cran d'une ligne se calcule au
 * prorata, la dernière ligne tombe toujours dans le dernier cran.
 * Pur et testable sans SDK.
 */
class MinimapModelTest {

    private static Document document() {
        return TextSession.of("courte\n\nune ligne tres longue ici\nfin", () -> 0L)
                .document();
    }

    /** Chaque rangée porte la charge de son bouquet — la plus longue ligne plafonnée. */
    @Test
    void laChargeDuBouquet() {
        Document doc = document();
        MinimapModel carte = MinimapModel.of(doc, 2);
        assertEquals(2, carte.taille());
        // le bouquet de la rangée 0 tient « courte » et la ligne vide :
        // la charge est celle de « courte », 6 caractères
        assertEquals(6, carte.rangees().get(0).load());
        // le bouquet de la rangée 1 tient la longue et « fin » : 25
        assertEquals(25, carte.rangees().get(1).load());
    }

    /** La ligne vide charge zéro — le trait ne se rend pas. */
    @Test
    void laLigneVideChargeZero() {
        Document doc = TextSession.of("\n\n", () -> 0L).document();
        MinimapModel carte = MinimapModel.of(doc, 2);
        assertEquals(0, carte.rangees().get(0).load());
        assertEquals(0, carte.rangees().get(1).load());
    }

    /** La charge plafonne — au-delà de CHARGE_MAX, le trait sature. */
    @Test
    void laChargePlafonne() {
        StringBuilder longue = new StringBuilder();
        for (int i = 0; i < MinimapModel.CHARGE_MAX + 50; i++) {
            longue.append('x');
        }
        Document doc = TextSession.of(longue.toString(), () -> 0L).document();
        MinimapModel carte = MinimapModel.of(doc, 1);
        assertEquals(MinimapModel.CHARGE_MAX, carte.rangees().get(0).load());
    }

    /** Le cran d'une ligne : au prorata, la dernière tombe dans le dernier cran. */
    @Test
    void leCranSeCalculeAuProrata() {
        assertEquals(0, MinimapModel.cranDeLigne(0, 100, 10));
        assertEquals(5, MinimapModel.cranDeLigne(50, 100, 10));
        assertEquals(9, MinimapModel.cranDeLigne(99, 100, 10));
        // la dernière ligne tombe toujours dans le dernier cran
        assertEquals(9, MinimapModel.cranDeLigne(99, 99, 10));
        // les bornes tiennent
        assertEquals(0, MinimapModel.cranDeLigne(0, 1, 1));
        assertEquals(0, MinimapModel.cranDeLigne(0, 10, 1));
    }

    /** La ligne d'un cran : la premiere ligne de son bouquet — le saut retombe vrai. */
    @Test
    void laLigneDuCran() {
        Document doc = document();
        MinimapModel carte = MinimapModel.of(doc, 2);
        assertEquals(0, carte.ligneDeCran(0, doc.lineCount()));
        assertEquals(2, carte.ligneDeCran(1, doc.lineCount()));
        // le cran hors des bornes retombe aux crans extrêmes
        assertEquals(2, carte.ligneDeCran(9, doc.lineCount()));
        assertEquals(0, carte.ligneDeCran(-3, doc.lineCount()));
    }

    /** Chaque ligne compte une fois : une rangée par cran, jamais plus. */
    @Test
    void uneRangeeParCran() {
        Document doc = document();
        for (int hauteur : List.of(1, 2, 7, 200)) {
            MinimapModel carte = MinimapModel.of(doc, hauteur);
            assertEquals(hauteur, carte.taille());
            assertEquals(hauteur, carte.rangees().size());
        }
    }

    /** Le document d'une ligne vit dans sa rangée unique. */
    @Test
    void documentDUneLigne() {
        MinimapModel carte = MinimapModel.of(
                TextSession.of("seule", () -> 0L).document(), 200);
        assertEquals(200, carte.taille());
        assertEquals(5, carte.rangees().get(0).load());
        assertEquals(0, carte.rangees().get(199).load());
        assertTrue(carte.ligneDeCran(199, 1) == 0);
    }

    // ── Les segments encrés de la ligne représentative (le rendu du
    // dépôt de départ : la silhouette COLORÉE, chaque token à son encre)

    /** Les segments classent la ligne : mot-clé, code, nombre, chaîne. */
    @Test
    void lesSegmentsClassentLaLigne() {
        List<MinimapModel.MapSegment> segments =
                MinimapModel.segmentsDe("int x = 42");
        // « int » mot-clé en tête, « 42 » nombre en queue — le code nu
        // vit entre les deux, le point-virgule final est code soudé
        assertEquals(MinimapModel.ENCRE_MOTCLE, segments.get(0).encre());
        assertEquals(0, segments.get(0).colonne());
        assertEquals(3, segments.get(0).colonnes());
        MinimapModel.MapSegment nombre = segments.stream()
                .filter(s -> s.encre() == MinimapModel.ENCRE_NOMBRE)
                .findFirst().orElseThrow();
        assertEquals(8, nombre.colonne());
        assertEquals(2, nombre.colonnes());
        // le code nu vit entre les deux
        assertTrue(segments.stream()
                .anyMatch(s -> s.encre() == MinimapModel.ENCRE_CODE));
    }

    /** La chaîne littérale porte son encre, guillemets compris. */
    @Test
    void laChainePorteSonEncre() {
        List<MinimapModel.MapSegment> segments =
                MinimapModel.segmentsDe("mesurer(\"cm\", 3)");
        MinimapModel.MapSegment chaine = segments.stream()
                .filter(s -> s.encre() == MinimapModel.ENCRE_CHAINE)
                .findFirst().orElseThrow();
        assertEquals(8, chaine.colonne());
        assertEquals(4, chaine.colonnes());
    }

    /** La chaîne non fermée vit jusqu'à la fin de sa ligne. */
    @Test
    void laChaineNonFermeeVitJusquauBout() {
        List<MinimapModel.MapSegment> segments =
                MinimapModel.segmentsDe("x = \"inacheve");
        MinimapModel.MapSegment chaine = segments.stream()
                .filter(s -> s.encre() == MinimapModel.ENCRE_CHAINE)
                .findFirst().orElseThrow();
        assertEquals(4, chaine.colonne());
        assertEquals(9, chaine.colonnes());
    }

    /** Le commentaire de fin de ligne : du « // » à la fin. */
    @Test
    void leCommentaireDeFinDeLigne() {
        List<MinimapModel.MapSegment> segments =
                MinimapModel.segmentsDe("int x = 1; // le reste se tait");
        MinimapModel.MapSegment commentaire = segments.stream()
                .filter(s -> s.encre() == MinimapModel.ENCRE_COMMENTAIRE)
                .findFirst().orElseThrow();
        assertEquals(11, commentaire.colonne());
    }

    /** La ligne entière de commentaire ou de bloc : une seule plage. */
    @Test
    void laLigneDeCommentementEntiere() {
        assertEquals(1, MinimapModel.segmentsDe("// tout se tait").size());
        assertEquals(MinimapModel.ENCRE_COMMENTAIRE,
                MinimapModel.segmentsDe("// tout se tait").get(0).encre());
        assertEquals(MinimapModel.ENCRE_COMMENTAIRE,
                MinimapModel.segmentsDe(" * la suite d'un bloc").get(0).encre());
        assertEquals(MinimapModel.ENCRE_COMMENTAIRE,
                MinimapModel.segmentsDe("/* l'ouverture").get(0).encre());
    }

    /** Les plages voisines de même encre se soudent — le code nu ne se miette pas. */
    @Test
    void lesPlagesVoisinesSeSoudent() {
        List<MinimapModel.MapSegment> segments =
                MinimapModel.segmentsDe("a + b - c");
        // « a », « + », « b », « - », « c » : les identifiants, les
        // signes et les blancs portent tous le code — UNE plage continue
        assertEquals(1, segments.size());
        assertEquals(MinimapModel.ENCRE_CODE, segments.get(0).encre());
        assertEquals(9, segments.get(0).colonnes());
    }

    /** La rangée porte les segments de sa ligne REPRÉSENTATIVE — la plus longue du bouquet. */
    @Test
    void laRangeePorteLesSegmentsDeSaRepresentative() {
        Document doc = TextSession.of(
                "int a = 1;\nreturn \"texte\";", () -> 0L).document();
        MinimapModel carte = MinimapModel.of(doc, 1);
        List<MinimapModel.MapSegment> segments =
                carte.rangees().get(0).segments();
        // la représentative est la ligne du retour : le mot-clé « return »
        // y vit, la chaîne aussi
        assertTrue(segments.stream()
                .anyMatch(s -> s.encre() == MinimapModel.ENCRE_MOTCLE));
        assertTrue(segments.stream()
                .anyMatch(s -> s.encre() == MinimapModel.ENCRE_CHAINE));
        // la rangée vide ne rend rien
        assertTrue(MinimapModel.of(TextSession.of("\n", () -> 0L)
                .document(), 4).rangees().get(0).segments().isEmpty());
    }
}
