package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les jetons sémantiques du protocole (B-02) : le delta encodé du serveur
 * — ligne, colonne, longueur, type, modificateurs, chacun relatif au
 * jeton précédent — devient une liste absolue. Le décodage est une
 * fonction pure de la charge utile : aucun autre flux (diagnostics
 * compris, PIEGE-18) n'y entre.
 */
class JetonsSemantiquesTest {

    /** Le décodage : les deltas deviennent absolus, ligne par ligne. */
    @Test
    void leDecodageDesDeltas() {
        // jeton 1 : ligne 2, colonne 4, 5 caractères, type 3, modificateur 1
        // jeton 2 : même ligne, colonne +3 (7), 2 caractères, type 0
        // jeton 3 : ligne +1 (3), colonne 0, 8 caractères, type 42
        JetonsSemantiques jetons = JetonsSemantiques.de(JsonLecteur.lire(
                "{\"data\": [2, 4, 5, 3, 1, 0, 3, 2, 0, 0, 1, 0, 8, 42, 0]}"));
        List<JetonSemantique> absolus = jetons.jetons();
        assertEquals(3, absolus.size());
        assertEquals(new JetonSemantique(2, 4, 5, 3, 1), absolus.get(0));
        assertEquals(new JetonSemantique(2, 7, 2, 0, 0), absolus.get(1));
        assertEquals(new JetonSemantique(3, 0, 8, 42, 0), absolus.get(2));
    }

    /** Le résultat porte son identifiant quand le serveur en donne un — nul sinon. */
    @Test
    void lidentifiantDeResultat() {
        assertEquals("resultat-1", JetonsSemantiques.de(JsonLecteur.lire(
                "{\"resultId\": \"resultat-1\", \"data\": []}"))
                .identifiantResultat());
        assertNull(JetonsSemantiques.de(JsonLecteur.lire("{\"data\": []}"))
                .identifiantResultat());
    }

    /**
     * La fabrique typée (ADR-015, la voie de la frontière protocolaire) :
     * la charge d'entiers décode comme la voie JSON — les deux voies
     * partagent le décodage relatif-à-absolu du domaine.
     */
    @Test
    void laFabriqueTypeeDecodeCommeLaVoieJson() {
        JetonsSemantiques jetons = JetonsSemantiques.de("resultat-2",
                List.of(2, 4, 5, 3, 1, 0, 3, 2, 0, 0, 1, 0, 8, 42, 0));
        List<JetonSemantique> absolus = jetons.jetons();
        assertEquals("resultat-2", jetons.identifiantResultat());
        assertEquals(3, absolus.size());
        assertEquals(new JetonSemantique(2, 4, 5, 3, 1), absolus.get(0));
        assertEquals(new JetonSemantique(2, 7, 2, 0, 0), absolus.get(1));
        assertEquals(new JetonSemantique(3, 0, 8, 42, 0), absolus.get(2));
        assertEquals(List.of(), JetonsSemantiques.de(null, List.of()).jetons());
    }

    /** Les formes vides : data absent, data null, data vide — jamais d'exception. */
    @Test
    void lesFormesVides() {
        assertTrue(JetonsSemantiques.de(JsonLecteur.lire("{}")).jetons().isEmpty());
        assertTrue(JetonsSemantiques.de(JsonLecteur.lire(
                "{\"data\": null}")).jetons().isEmpty());
        assertTrue(JetonsSemantiques.de(JsonLecteur.lire(
                "{\"data\": []}")).jetons().isEmpty());
    }

    /** Le rejet : un tableau de taille non multiple de cinq est une erreur de protocole. */
    @Test
    void leRejetDesTailles() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> JetonsSemantiques.de(JsonLecteur.lire(
                        "{\"data\": [1, 2, 3]}")));
    }

    /** Un delta de ligne non nul ramène la colonne au delta donné — pas à un cumul. */
    @Test
    void laColonneSeReinitialiseParLigne() {
        JetonsSemantiques jetons = JetonsSemantiques.de(JsonLecteur.lire(
                "{\"data\": [0, 10, 2, 1, 0, 2, 5, 3, 2, 0]}"));
        assertEquals(0, jetons.jetons().get(0).ligne());
        assertEquals(10, jetons.jetons().get(0).colonne());
        assertEquals(2, jetons.jetons().get(1).ligne());
        assertEquals(5, jetons.jetons().get(1).colonne());
    }
}
