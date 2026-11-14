package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Le correctif d'import local (catégorie B de l'inventaire — « Import
 * Resolution », la surface dormait derrière l'action d'import) : le
 * classpath du terrain résout l'identifiant au curseur, la PREMIÈRE
 * candidate (l'ordre de préférence décide, jamais l'aléa), l'import
 * déjà présent ne se re-propose pas. Pur, sans appareil — la feuille
 * d'aperçu (B-18) vit dans l'atelier du correctif.
 */
class CorrectifImportTest {

    /** La résolution sert la première candidate : l'ordre de préférence
     *  du terrain décide — l'édition s'applique, la ligne d'import se
     *  pose en tête du bloc. */
    @Test
    void laResolutionSertLaPremiereCandidate() {
        Document document = Document.of("package zoo;\n\nList noms;\n");
        CorrectifImport.Candidat candidat = CorrectifImport.local(document,
                document.text().indexOf("List") + 1,
                AtelierRefactor.CLASSPATH);
        assertNotNull(candidat, "le classpath connaît List");
        assertEquals("java.util.List", candidat.fqn(),
                "la première candidate gagne, jamais l'aléa");
        TextSession epreuve = TextSession.of(document.text(), () -> 0L);
        epreuve.apply(candidat.edition());
        assertEquals("package zoo;\nimport java.util.List;\n\nList noms;\n",
                epreuve.text(),
                "la ligne d'import se pose en tête du bloc, derrière le paquet");
    }

    /** Sans résolution ni identifiant, rien ne se propose — jamais
     *  d'import deviné ; l'import déjà présent ne se re-propose pas. */
    @Test
    void sansResolutionRienNeSePropose() {
        Document inconnu = Document.of("Inconnu nom;\n");
        assertNull(CorrectifImport.local(inconnu, 1,
                AtelierRefactor.CLASSPATH), "le classpath ne devine pas");
        Document sansMot = Document.of("  \n");
        assertNull(CorrectifImport.local(sansMot, 1,
                AtelierRefactor.CLASSPATH), "sans identifiant, aucun candidat");
        Document dejaImporte = Document.of(
                "package zoo;\n\nimport java.util.List;\nList noms;\n");
        // l'import DÉJÀ PRÉSENT ne se re-propose pas — les autres
        // candidats du même nom restent proposés : le choix reste à
        // l'utilisateur (la sémantique du moteur, documentée)
        CorrectifImport.Candidat reste = CorrectifImport.local(dejaImporte,
                dejaImporte.text().lastIndexOf("List") + 1,
                AtelierRefactor.CLASSPATH);
        assertNotNull(reste, "l'autre candidate du même nom reste proposée");
        assertEquals("java.awt.List", reste.fqn(),
                "jamais la candidate déjà importée");
    }
}
