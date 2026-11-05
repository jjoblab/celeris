package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le chargeur de grammaires (ADR-004, voie de grammaires) : le JSON
 * déclaratif se lit strictement — motifs compilés, inclusions du
 * référentiel résolues en références, cycles et noms inconnus refusés
 * au chargement, jamais découverts à la lexicographie.
 */
class LecteurJsonGrammaireTest {

    /** Une grammaire minimale se charge : nom, portée racine, motifs. */
    @Test
    void grammaireMinimale() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [
                    { "match": "#.*$", "name": "comment.line.essai" },
                    { "match": "\\\\d+", "name": "constant.numeric.essai" }
                  ]
                }
                """);
        assertEquals("Essai", grammaire.nom());
        assertEquals("source.essai", grammaire.porteeRacine());
        assertEquals(2, grammaire.motifs().size());
    }

    /** Les inclusions du référentiel se résolvent en références parcourues. */
    @Test
    void inclusionsResolues() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "include": "#mots" } ],
                  "repository": {
                    "mots": { "patterns": [ { "match": "\\\\w+", "name": "keyword.essai" } ] }
                  }
                }
                """);
        assertEquals(1, grammaire.motifs().size());
        Regle regle = grammaire.motifs().get(0);
        assertTrue(regle instanceof RegleListe, "l'inclusion devient une référence");
        RegleListe liste = (RegleListe) regle;
        assertEquals(1, liste.regles().size());
    }

    /** L'inclusion cyclique est permise quand elle traverse un bloc. */
    @Test
    void inclusionCycliqueATraversUnBloc() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "include": "#valeurs" } ],
                  "repository": {
                    "valeurs": { "patterns": [
                      { "match": "\\\\d+", "name": "constant.numeric.essai" },
                      { "begin": "\\\\[", "end": "\\\\]",
                        "patterns": [ { "include": "#valeurs" } ] }
                    ] }
                  }
                }
                """);
        // la référence du tableau pointe vers la liste en cours de
        // construction — la lexicographie la parcourt sans fin d'aplatissement
        RegleListe valeurs = (RegleListe) grammaire.motifs().get(0);
        assertTrue(valeurs.regles().get(1) instanceof RegleBloc, "le tableau est un bloc");
    }

    /** $self est refusé : l'aplatissement n'y trouverait jamais son compte. */
    @Test
    void selfEstRefuse() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> Grammaires.lire("""
                        {
                          "name": "Essai",
                          "scopeName": "source.essai",
                          "patterns": [ { "include": "$self" } ]
                        }
                        """));
        assertTrue(erreur.getMessage().contains("unsupported include"),
                "message explicite : " + erreur.getMessage());
    }

    /** Une inclusion inconnue est refusée au chargement. */
    @Test
    void inclusionInconnueRefusee() {
        assertThrows(IllegalArgumentException.class, () -> Grammaires.lire("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "include": "#absent" } ]
                }
                """));
    }

    /** Un motif invalide est refusé avec l'expression en cause. */
    @Test
    void motifInvalideRefuse() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> Grammaires.lire("""
                        {
                          "name": "Essai",
                          "scopeName": "source.essai",
                          "patterns": [ { "match": "(", "name": "keyword.essai" } ]
                        }
                        """));
        assertTrue(erreur.getMessage().contains("bad pattern"),
                "message explicite : " + erreur.getMessage());
    }

    /** Un bloc sans fermeture est refusé. */
    @Test
    void blocSansFinRefuse() {
        assertThrows(IllegalArgumentException.class, () -> Grammaires.lire("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "begin": "\\"", "name": "string.essai" } ]
                }
                """));
    }

    /** Les grammaires embarquées du module se chargent. */
    @Test
    void grammairesEmbarquees() {
        for (String nom : new String[] {"toml", "yaml", "properties", "markdown",
                "python", "shell"}) {
            Grammaire grammaire = Grammaires.ressource(nom);
            assertNotNull(grammaire, nom + " se charge");
            assertTrue(!grammaire.motifs().isEmpty(), nom + " a des motifs");
        }
    }

    /** Une ressource inconnue est refusée. */
    @Test
    void ressourceInconnueRefusee() {
        assertThrows(IllegalArgumentException.class, () -> Grammaires.ressource("inconnu"));
    }

    /** Le JSON mal formé est refusé avec la position. */
    @Test
    void jsonMalFormeRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> Grammaires.lire("{ \"name\": "));
    }
}
