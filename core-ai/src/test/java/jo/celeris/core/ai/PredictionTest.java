package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.JetonAnnulation;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;

/**
 * La prédiction d'édition (F-02, ADR-012) : le modèle prédit une
 * opération, l'hôte l'applique par la voie de transaction ordinaire —
 * annulable, diffable, jamais silencieuse. Le refus est un résultat :
 * révision périmée, protection en écriture, éditions qui ne collent pas
 * au document.
 */
class PredictionTest {

    /** Le récepteur de test : la dernière réponse du modèle, ou rien. */
    private static final class Enregistreur implements ModelePrediction.Recepteur {
        private @Nullable PredictionEdition recue;
        private boolean aucune;

        @Override
        public void prediction(PredictionEdition arrivee) {
            recue = arrivee;
        }

        @Override
        public void aucune() {
            aucune = true;
        }
    }

    /** Une prédiction sans description ni éditions ne passe pas. */
    @Test
    void predictionVideRejetee() {
        assertThrows(IllegalArgumentException.class,
                () -> new PredictionEdition("", List.of(new TextEdit(0, "a", "b")), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PredictionEdition("remplace", List.of(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PredictionEdition("remplace",
                        List.of(new TextEdit(0, "a", "b")), -1));
    }

    /**
     * F-02 — la prédiction s'applique en transaction ordinaire : une
     * étape d'annulation, le défaire restaure le document.
     */
    @Test
    void predictionSappliqueEnTransactionAnnulable() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        PredictionEdition prediction = new PredictionEdition(
                "initialise a 42", List.of(new TextEdit(8, "0", "42")), 0);
        assertEquals(AppliquerPrediction.Resultat.APPLIQUEE,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("int x = 42;", session.text());
        assertTrue(session.undo(), "la prédiction est annulable");
        assertEquals("int x = 0;", session.text(), "l'annulation restaure l'original");
    }

    /**
     * Les éditions multiples vivent dans l'espace séquentiel : la
     * seconde se prédit contre le document que la première a laissé.
     */
    @Test
    void predictionMultiEditionsSequentialles() {
        TextSession session = TextSession.of("int x = 0;\nint y = 1;", () -> 0L);
        PredictionEdition prediction = new PredictionEdition("final partout",
                List.of(new TextEdit(0, "int", "final var"),
                        new TextEdit(17, "int", "var")),
                0);
        assertEquals(AppliquerPrediction.Resultat.APPLIQUEE,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("final var x = 0;\nvar y = 1;", session.text());
        assertTrue(session.undo());
        assertEquals("int x = 0;\nint y = 1;", session.text());
    }

    /**
     * Jamais de fusion silencieuse : la prédiction d'un instantané qui
     * n'est plus courant refuse, le document ne bouge pas.
     */
    @Test
    void predictionPerimeeRefusee() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        assertTrue(session.replaceRange(session.length(), session.length(), " "));
        PredictionEdition prediction = new PredictionEdition(
                "initialise a 42", List.of(new TextEdit(8, "0", "42")), 0);
        assertEquals(AppliquerPrediction.Resultat.REFUSEE_REVISION,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("int x = 0; ", session.text(),
                "le document périmé ne bouge pas d'un refus");
    }

    /** La session protégée en écriture refuse, le document ne bouge pas. */
    @Test
    void predictionRefuseeEnProtection() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        session.setReadOnly(true);
        PredictionEdition prediction = new PredictionEdition(
                "initialise a 42", List.of(new TextEdit(8, "0", "42")), 0);
        assertEquals(AppliquerPrediction.Resultat.REFUSEE_PROTECTION,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("int x = 0;", session.text());
    }

    /** Les éditions hors bornes refusent — jamais d'exception du modèle. */
    @Test
    void predictionHorsBornesRefusee() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        PredictionEdition prediction = new PredictionEdition(
                "remplace loin", List.of(new TextEdit(20, "0", "42")), 0);
        assertEquals(AppliquerPrediction.Resultat.REFUSEE_EDITS,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("int x = 0;", session.text());
    }

    /**
     * Les éditions incohérentes refusent AVANT toute application : le
     * texte retiré ne colle pas, le document ne doit rien — la session
     * ferait confiance à son appelant, la prédiction ne la mérite pas.
     */
    @Test
    void predictionIncoherenteRefuseeSansApplicationPartielle() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        PredictionEdition prediction = new PredictionEdition("remplace faux",
                List.of(new TextEdit(0, "int", "final var"),
                        new TextEdit(18, "float", "var")),
                0);
        assertEquals(AppliquerPrediction.Resultat.REFUSEE_EDITS,
                AppliquerPrediction.appliquer(session, prediction));
        assertEquals("int x = 0;", session.text(),
                "aucune édition partielle ne fuite d'un refus");
        assertFalse(session.canUndo(), "aucune étape d'annulation ne se pose");
    }

    /**
     * F-02 — jamais d'application silencieuse : le modèle rend sa
     * prédiction au récepteur, la session ne bouge pas ; seule
     * l'application explicite de l'hôte écrit.
     */
    @Test
    void leModeleNeToucheJamaisLaSession() {
        TextSession session = TextSession.of("int x = 0;", () -> 0L);
        FauxModeleLocal modele = new FauxModeleLocal();
        modele.prediction(new PredictionEdition(
                "initialise a 42", List.of(new TextEdit(8, "0", "42")), 0));
        Enregistreur recepteur = new Enregistreur();
        modele.predire(new RequeteModele(session.document(), 8, new JetonAnnulation()),
                recepteur);
        assertEquals(new PredictionEdition("initialise a 42",
                List.of(new TextEdit(8, "0", "42")), 0), recepteur.recue,
                "le modèle rend sa prédiction au récepteur");
        assertEquals("int x = 0;", session.text(), "rendre ne touche pas la session");
        assertFalse(session.canUndo());
        PredictionEdition recue = Objects.requireNonNull(recepteur.recue);
        assertEquals(AppliquerPrediction.Resultat.APPLIQUEE,
                AppliquerPrediction.appliquer(session, recue));
        assertEquals("int x = 42;", session.text(),
                "seule l'application explicite de l'hôte écrit");
    }

    /** Le modèle sans avis rend l'aveu qu'il n'y a rien à prédire. */
    @Test
    void modeleSansAvisRepondAucune() {
        FauxModeleLocal modele = new FauxModeleLocal();
        Enregistreur recepteur = new Enregistreur();
        modele.predire(new RequeteModele(Document.of("x"), 0,
                new JetonAnnulation()), recepteur);
        assertNull(recepteur.recue);
        assertTrue(recepteur.aucune);
    }
}
