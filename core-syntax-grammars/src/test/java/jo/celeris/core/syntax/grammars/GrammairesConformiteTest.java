package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;

/**
 * Le banc de conformité des grammaires (ADR-014) : chaque grammaire
 * livrée se rejoue sur son corpus, et son taux d'acceptation est un
 * SEUIL BLOQUANT — une montée de version de joni qui rejetterait un
 * motif que les grammaires du dépôt emploient échoue ici, avant
 * jamais d'atteindre l'utilisateur. La relecture vérifie le
 * déterminisme (même texte, même état d'entrée, mêmes jetons) et
 * l'absence d'incident de budget : le bornage ne se déclenche pas sur
 * les corpus du mandat.
 */
class GrammairesConformiteTest {

    /** Les grammaires livrées et leur corpus, dans l'ordre du catalogue. */
    private static final String[][] CATALOGUE = {
        {"toml", "exemple.toml"}, {"yaml", "exemple.yaml"},
        {"properties", "exemple.properties"}, {"markdown", "exemple.md"},
        {"python", "exemple.py"}, {"shell", "exemple.sh"},
        {"sql", "exemple.sql"}, {"c", "exemple.c"},
        {"cpp", "exemple.cpp"}, {"proguard", "exemple.pro"}};

    /** Chaque grammaire livrée accepte la totalité de ses expressions. */
    @Test
    void acceptationCompleteParGrammaire() {
        for (String[] entree : CATALOGUE) {
            Grammaire grammaire = grammaireDePaquet(entree[0]);
            assertEquals(1.0, grammaire.rapport().acceptation(),
                    entree[0] + " : le taux d'acceptation est un seuil bloquant ("
                            + grammaire.rapport().acceptes() + "/"
                            + grammaire.rapport().motifs() + ")");
            assertEquals(0, grammaire.rapport().refuses(),
                    entree[0] + " : aucune règle écartée");
            assertEquals(0, grammaire.rapport().inclusionsInternes(),
                    entree[0] + " : aucune inclusion de référentiel inconnue"
                            + " — les paquets écrits ici restent stricts");
        }
    }

    /** Chaque grammaire rejoue son corpus : des jetons, le déterminisme, aucun incident. */
    @Test
    void corpusRejoues() {
        for (String[] entree : CATALOGUE) {
            Grammaire grammaire = grammaireDePaquet(entree[0]);
            String[] lignes = corpus(entree[1]).split("\n", -1);
            GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
            LexState etat = lexeur.initialState();
            int jetons = 0;
            for (String ligne : lignes) {
                LexedLine premiere = lexeur.lex(ligne, etat);
                jetons += premiere.tokens().length;
                LexedLine seconde = lexeur.lex(ligne, etat);
                assertArrayEquals(premiere.tokens(), seconde.tokens(),
                        entree[0] + " : le lexeur est déterministe");
                etat = premiere.exitState();
            }
            assertTrue(jetons > 0, entree[0] + " : le corpus produit des jetons");
            assertEquals(0, lexeur.incidents(),
                    entree[0] + " : aucun incident de budget sur le corpus");
        }
    }

    private static String corpus(String nom) {
        String chemin = "corpus/" + nom;
        try (InputStream flux = GrammairesConformiteTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("corpus absent : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("corpus illisible : " + chemin, e);
        }
    }

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammaire grammaireDePaquet(String id) {
        var flux = GrammairesConformiteTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return Grammaires.lire(new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
