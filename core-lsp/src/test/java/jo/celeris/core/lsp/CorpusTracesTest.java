package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les traces de contrat du jalon (sortie M6) : les réponses enregistrées
 * des serveurs — l'initialisation, la complétion tronquée, le survol
 * balisé, la signature aux paramètres par bornes, la définition
 * multiple, les indices à parties, l'édition d'espace avec édit de
 * gabarit — forment le corpus de la parade au risque « serveurs réels
 * divergents » (feuille de route) : chaque décodeur tient la forme
 * enregistrée, toute divergence future casse la construction. Contenu
 * original, aucune pièce tierce (neutralité, ADR-011).
 */
class CorpusTracesTest {

    /** L'initialisation : la surface bornée se lit des capacités annoncées. */
    @Test
    void laTraceInitialisation() {
        CapacitesServeur capacites = CapacitesServeur.de(trace("initialisation"));
        assertEquals(1, capacites.genreSynchro());
        assertTrue(capacites.sertCompletion());
        assertTrue(capacites.sertSurvol());
        assertTrue(capacites.sertSignature());
        assertTrue(capacites.sertDefinition());
        assertTrue(capacites.sertReferences());
        assertTrue(capacites.sertFormatage());
        assertTrue(capacites.sertIndices(), "inlayHintProvider lu");
        assertEquals(List.of("keyword", "variable", "parameter", "function", "type"),
                capacites.typesJetons());
        assertEquals(List.of("declaration", "static", "deprecated"),
                capacites.modificateursJetons());
    }

    /** La complétion tronquée : la coupure honorée, l'ordre du serveur tenu (PIEGE-17). */
    @Test
    void laTraceCompletionTronquee() {
        ListeCompletion liste = ListeCompletion.de(trace("completion-tronquee"));
        assertTrue(liste.estIncomplete());
        assertEquals(3, liste.elements().size());
        assertEquals("ajouter", liste.elements().get(0).libelle());
        // la liste tronquée ne se trie jamais — la même revient
        assertEquals(liste, liste.trier());
        // le filtrage garde l'ordre relatif, n'ajoute rien
        assertEquals(List.of("ajouter", "ajuster"),
                liste.filtrer("aj").elements().stream()
                        .map(ElementCompletion::libelle).toList());
    }

    /** Le survol : le contenu balisé et la plage, résolus à la demande. */
    @Test
    void laTraceSurvol() {
        SurvolLsp survol = SurvolLsp.de(trace("survol"));
        assertEquals("markdown", survol.genre());
        assertTrue(survol.texte().startsWith("fun ajouter"));
        assertTrue(survol.texte().contains("Ajoute deux entiers."));
        PlageLsp plage = survol.plage();
        assertNotNull(plage);
        assertEquals(new PositionLsp(2, 10), plage.debut());
        assertEquals(new PositionLsp(2, 17), plage.fin());
    }

    /** La signature : paramètres par bornes, l'actif au-delà de l'arité retombe au dernier. */
    @Test
    void laTraceSignature() {
        SignatureLsp aide = SignatureLsp.de(trace("signature"));
        assertEquals(1, aide.entrees().size());
        SignatureLsp.Entree entree = aide.entrees().get(0);
        assertEquals("ajouter(a: Int, b: Int, c: Int): Int", entree.libelle());
        assertEquals(List.of("a", "b", "c"), entree.parametres().stream()
                .map(SignatureLsp.Parametre::libelle).toList());
        assertEquals("le premier terme", entree.parametres().get(0).documentation());
        assertEquals(2, aide.parametreCourant(),
                "l'actif 5 au-dela de l'arite 3 retombe au dernier");
    }

    /** La définition : les lieux multiples dans l'ordre du serveur. */
    @Test
    void laTraceDefinition() {
        List<LieuLsp> lieux = LieuLsp.liste(trace("definition"));
        assertEquals(2, lieux.size());
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt", lieux.get(0).uri());
        PlageLsp seconde = lieux.get(1).plage();
        assertNotNull(seconde);
        assertEquals(new PositionLsp(40, 2), seconde.debut());
    }

    /** Les indices : parties cliquables, la chaîne nue, les remplissages. */
    @Test
    void laTraceIndices() {
        List<IndiceLsp> indices = IndiceLsp.liste(trace("indices"));
        assertEquals(2, indices.size());
        IndiceLsp premier = indices.get(0);
        assertEquals(3, premier.parties().size());
        assertEquals(1, premier.genre());
        assertTrue(premier.remplissageGauche());
        assertFalse(premier.remplissageDroite());
        LieuLsp cible = premier.parties().get(1).cible();
        assertNotNull(cible, "la partie « : » porte sa cible de navigation");
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt", cible.uri());
        IndiceLsp second = indices.get(1);
        assertEquals(1, second.parties().size());
        assertEquals("a =", second.parties().get(0).valeur());
        assertTrue(second.remplissageDroite());
    }

    /** L'édition d'espace : le document édité ET l'édit de gabarit — la forme celeris tenue. */
    @Test
    void laTraceEditionEspace() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(trace("edition-espace"));
        assertEquals(1, espace.documents().size());
        assertEquals(7, espace.documents().get(0).version());
        assertEquals("additionner", espace.documents().get(0).editions().get(0).nouveauTexte());
        assertEquals(1, espace.gabarits().size(), "l'edit de gabarit vit dans la trace");
        EditionGabaritLsp gabarit = espace.gabarits().get(0);
        assertEquals(new PositionLsp(20, 4), gabarit.position());
        assertTrue(gabarit.corps().contains("${nom:soustraire}"));
    }

    private static ValeurJson trace(String nom) {
        String chemin = "traces/" + nom + ".json";
        try (InputStream flux = CorpusTracesTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("trace absente : " + chemin);
            }
            return JsonLecteur.lire(
                    new String(flux.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("trace illisible : " + chemin, e);
        }
    }
}
