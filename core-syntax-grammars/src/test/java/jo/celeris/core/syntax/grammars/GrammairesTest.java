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
class GrammairesTest {

    /** Une grammaire minimale se charge : nom, portée racine, motifs. */
    @Test
    void grammaireMinimale() {
        Grammar grammaire = Grammars.read("""
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
        assertEquals(2, grammaire.patterns().size());
    }

    /** Les inclusions du référentiel se résolvent en références parcourues. */
    @Test
    void inclusionsResolues() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "include": "#mots" } ],
                  "repository": {
                    "mots": { "patterns": [ { "match": "\\\\w+", "name": "keyword.essai" } ] }
                  }
                }
                """);
        assertEquals(1, grammaire.patterns().size());
        Rule regle = grammaire.patterns().get(0);
        assertTrue(regle instanceof ListRule, "l'inclusion devient une référence");
        ListRule liste = (ListRule) regle;
        assertEquals(1, liste.regles().size());
    }

    /** L'inclusion cyclique est permise quand elle traverse un bloc. */
    @Test
    void inclusionCycliqueATraversUnBloc() {
        Grammar grammaire = Grammars.read("""
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
        ListRule valeurs = (ListRule) grammaire.patterns().get(0);
        assertTrue(valeurs.regles().get(1) instanceof BlockRule, "le tableau est un bloc");
    }

    /** $self se résout en la racine : la grammaire s'inclut elle-même (TextMate réel). */
    @Test
    void selfSeResoutALaRacine() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [
                    { "match": "#.*$", "name": "comment.line.essai" },
                    { "begin": "\\\\{", "end": "\\\\}",
                      "patterns": [ { "include": "$self" } ] }
                  ]
                }
                """);
        assertTrue(grammaire.patterns().get(1) instanceof BlockRule, "le second motif est un bloc");
        BlockRule bloc = (BlockRule) grammaire.patterns().get(1);
        assertEquals(1, bloc.patterns().size(), "une inclusion");
        assertTrue(bloc.patterns().get(0) instanceof ListRule,
                "$self devient une référence de liste");
    }

    /** Une inclusion inconnue est refusée au chargement. */
    @Test
    void inclusionInconnueSauteComptee() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "include": "#absent" } ]
                }
                """);
        assertEquals(1, grammaire.report().inclusionsInternes(),
                "l'inclusion inconnue se saute et se compte — les grammaires"
                        + " du commerce s'en portent auteurs");
    }

    /**
     * Un motif invalide écarte sa règle et se compte au rapport —
     * comme la référence tolère les grammaires imparfaites, jamais en
     * silence : le taux d'acceptation le dit.
     */
    @Test
    void motifInvalideEcarteEtCompte() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [
                    { "match": "(", "name": "keyword.essai" },
                    { "match": "\\\\d+", "name": "constant.numeric.essai" }
                  ]
                }
                """);
        assertEquals(1, grammaire.patterns().size(), "la règle invalide est écartée");
        assertEquals(1, grammaire.report().refuses(), "un refus compté");
        assertEquals(2, grammaire.report().patterns(), "deux expressions rencontrées");
        assertEquals(0.5, grammaire.report().acceptation(), "le taux le dit");
    }

    /** Un bloc sans fermeture est refusé. */
    @Test
    void blocSansFinRefuse() {
        assertThrows(IllegalArgumentException.class, () -> Grammars.read("""
                {
                  "name": "Essai",
                  "scopeName": "source.essai",
                  "patterns": [ { "begin": "\\"", "name": "string.essai" } ]
                }
                """));
    }

    /** Les grammaires des paquets intégrés se chargent (M7.6 : déplacées
     * du module vers les paquets de core-language, lues ici par leur
     * ressource de paquet). */
    @Test
    void grammairesDesPaquetsEmbarques() {
        for (String nom : new String[] {"toml", "yaml", "properties", "markdown",
                "python", "shell", "sql", "c", "cpp", "proguard"}) {
            Grammar grammaire = Grammars.read(resourceText(nom));
            assertNotNull(grammaire, nom + " se charge");
            assertTrue(!grammaire.patterns().isEmpty(), nom + " a des patterns");
        }
    }

    /** Le texte JSON de la grammaire d'un paquet intégré. */
    private static String resourceText(String id) {
        var flux = GrammairesTest.class.getResourceAsStream(
                "/langages/" + id + "/grammar.json");
        assertNotNull(flux, "grammaire du paquet " + id);
        try (flux) {
            return new String(flux.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Le JSON mal formé est refusé avec la position. */
    @Test
    void jsonMalFormeRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> Grammars.read("{ \"name\": "));
    }
}
