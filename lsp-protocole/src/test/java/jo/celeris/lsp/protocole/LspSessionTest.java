package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.CorrectifLsp;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionTexteLsp;
import jo.celeris.core.lsp.IndiceLsp;
import jo.celeris.core.lsp.SemanticTokensLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.OptionsFormatageLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PliLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.RequeteAnnuleeException;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SurvolLsp;
import jo.celeris.core.lsp.SymboleLsp;

/**
 * La session de la frontière exercée de bout en bout contre un serveur
 * factice EN PROCESSUS (ADR-015) : le protocole circule pour de vrai —
 * cadrage, sérialisation, fils de lecture — sans qu'aucun type de la
 * bibliothèque ne paraisse dans les réponses, qui sont du domaine.
 */
public final class LspSessionTest {

    private static final String URI = "file:///banc/exemple.xml";

    /** Une session démarrée avec son serveur factice et son état observable. */
    private static final class Banc implements AutoCloseable {
        private final LspSession session;
        private final ServeurFactice.Etat etat;

        private Banc(LspSession session, ServeurFactice.Etat etat) {
            this.session = session;
            this.etat = etat;
        }

        private LspSession session() {
            return session;
        }

        private ServeurFactice.Etat etat() {
            return etat;
        }

        @Override
        public void close() {
            session.close();
        }
    }

    private static Banc demarrer() throws Exception {
        // tube à file, le transport de la démo et des bancs (ADR-015) :
        // les tubes du JDK couplent la lecture à la vivacité du fil
        // écrivain — les fils de piscine du serveur sont éphémères
        TubesMemoire.Direction versServeur = TubesMemoire.creer();
        TubesMemoire.Direction duServeur = TubesMemoire.creer();
        ServeurFactice.Etat etat = ServeurFactice.demarrer(
                versServeur.entree(), duServeur.sortie());
        LspSession session = LspSession.sur(
                duServeur.entree(), versServeur.sortie(), "celeris-banc");
        session.initialiser();
        return new Banc(session, etat);
    }

    private static <T> List<T> attendreNonVide(Supplier<List<T>> sonde)
            throws Exception {
        long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        List<T> vu = sonde.get();
        while (vu.isEmpty() && System.nanoTime() < borne) {
            Thread.sleep(10);
            vu = sonde.get();
        }
        return vu;
    }

    private static void attendreCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < borne) {
            Thread.sleep(10);
        }
    }

    @Test
    void laPoigneeDeMainNegocieLesCapacites() throws Exception {
        try (Banc banc = demarrer()) {
            assertTrue(banc.session().capacites().sertCompletion());
            assertTrue(banc.session().capacites().sertSurvol());
            assertTrue(banc.session().capacites().sertSignature());
            assertTrue(banc.session().capacites().sertDefinition());
            assertEquals(2, banc.session().capacites().genreSynchro());
            assertEquals(List.of("type", "fonction"),
                    banc.session().capacites().typesJetons());
            assertEquals(List.of("declaration"),
                    banc.session().capacites().modificateursJetons());
        }
    }

    @Test
    void leDocumentValidePublieSesDiagnostics() throws Exception {
        try (Banc banc = demarrer()) {
            List<DiagnosticLsp> entendues = new ArrayList<>();
            banc.session().ajouterAuditeurDiagnostics((uri, diagnostics) ->
                    entendues.addAll(diagnostics));
            banc.session().ouvrir(URI, "xml", "<ex/>");
            List<DiagnosticLsp> publies = attendreNonVide(
                    () -> banc.session().diagnosticsDe(URI));
            assertEquals(1, publies.size());
            assertEquals(4, publies.get(0).gravite());
            assertEquals("document ouvert", publies.get(0).message());
            assertEquals("factice", publies.get(0).source());
            assertEquals(1, banc.session().versionDe(URI));
            List<DiagnosticLsp> entendu = attendreNonVide(() -> entendues);
            assertEquals(1, entendu.size());
        }
    }

    @Test
    void lEditionSuitLeDocumentEtRevalide() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex>");
            banc.session().changer(URI, 2, "<ex>");
            List<DiagnosticLsp> publies = attendreNonVide(() ->
                    banc.session().diagnosticsDe(URI).stream()
                            .filter(d -> d.gravite() == DiagnosticLsp.ERREUR)
                            .toList());
            assertEquals(1, publies.size());
            assertEquals("l'élément **n'est pas déclaré**",
                    publies.get(0).message());
            assertEquals("42", publies.get(0).code());
            assertEquals(2, banc.session().versionDe(URI));
            assertEquals("<ex>", banc.etat().dernierTexte.get());
        }
    }

    @Test
    void laCompletionSertLesElementsDocumentes() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            ListeCompletion liste = banc.session().completion(
                    URI, new PositionLsp(0, 3)).get(5, TimeUnit.SECONDS);
            assertTrue(liste.estIncomplete());
            assertEquals(1, liste.elements().size());
            assertEquals("ex:exemplaire", liste.elements().get(0).libelle());
            assertEquals(5, liste.elements().get(0).genre());
            assertEquals("l'élément exemplaire",
                    liste.elements().get(0).detail());
            assertEquals("La documentation de l'élément exemplaire.",
                    liste.elements().get(0).documentation());
        }
    }

    @Test
    void leSurvolSertLaDocumentationBalisee() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            SurvolLsp survol = banc.session().survol(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals("La documentation survolée.", survol.texte());
            assertEquals("markdown", survol.genre());
            PlageLsp plageSurvol = survol.plage();
            assertNotNull(plageSurvol);
            assertEquals(new PositionLsp(0, 0), plageSurvol.debut());
        }
    }

    @Test
    void laSignatureSertLesParametresADecalages() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            SignatureLsp aide = banc.session().signature(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, aide.entrees().size());
            assertEquals("f(a, b)", aide.entrees().get(0).libelle());
            assertEquals(2, aide.entrees().get(0).parametres().size());
            assertEquals("a", aide.entrees().get(0).parametres().get(0).libelle());
            assertEquals("b", aide.entrees().get(0).parametres().get(1).libelle());
            assertEquals(0, aide.signatureActiveIndex());
            assertEquals(1, aide.parametreCourant());
        }
    }

    @Test
    void laDefinitionSertLesLieux() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            List<LieuLsp> lieux = banc.session().definition(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, lieux.size());
            assertEquals(URI, lieux.get(0).uri());
            PlageLsp plageLieu = lieux.get(0).plage();
            assertNotNull(plageLieu);
            assertEquals(new PositionLsp(0, 0), plageLieu.debut());
        }
    }

    @Test
    void laFermetureDemandeArretJamaisSortie() throws Exception {
        Banc banc = demarrer();
        banc.session().close();
        banc.session().close();
        assertTrue(banc.etat().arretDemande.get());
        assertFalse(banc.etat().sortieDemandee.get());
        assertThrows(IllegalStateException.class,
                () -> banc.session().ouvrir(URI, "xml", "<ex/>"));
    }

    @Test
    void laSessionRefuseSansInitialisation() throws Exception {
        TubesMemoire.Direction versServeur = TubesMemoire.creer();
        TubesMemoire.Direction duServeur = TubesMemoire.creer();
        ServeurFactice.demarrer(versServeur.entree(), duServeur.sortie());
        try (LspSession session = LspSession.sur(
                duServeur.entree(), versServeur.sortie(), "celeris-banc")) {
            assertThrows(IllegalStateException.class,
                    () -> session.ouvrir(URI, "xml", "<ex/>"));
        }
    }

    /**
     * PIEGE-18 — les frappes rapides, la preuve au fil : deux
     * complétions de suite, la seconde remplace la première en vol —
     * le futur remplacé échoue REMPLACÉE, l'annulation TRAVERSE le
     * fil ({@code $/cancelRequest} : le futur tenu côté serveur est
     * annulé par le cadre de la bibliothèque), le dernier mot gagne.
     */
    @Test
    void lesFrappesRapidesRemplacentEnVolAuFil() throws Exception {
        try (Banc banc = demarrer()) {
            banc.etat().tenirCompletion.set(true);
            banc.session().ouvrir(URI, "xml", "<ex");
            CompletableFuture<ListeCompletion> vieille = banc.session()
                    .completion(URI, new PositionLsp(0, 2));
            CompletableFuture<ListeCompletion> fraiche = banc.session()
                    .completion(URI, new PositionLsp(0, 3));
            attendreCondition(() -> banc.etat().volsCompletion.size() >= 2);
            assertEquals(2, banc.etat().volsCompletion.size(),
                    "les deux demandes ont volé");
            ExecutionException remplacee = assertThrows(ExecutionException.class,
                    () -> vieille.get(5, TimeUnit.SECONDS));
            assertTrue(remplacee.getCause() instanceof RequeteAnnuleeException annulee
                    && annulee.raison() == RequeteAnnuleeException.Raison.REMPLACEE,
                    "le futur remplacé échoue REMPLACÉE : " + remplacee.getCause());
            attendreCondition(() -> banc.etat().volsCompletion.get(0).isCancelled());
            assertTrue(banc.etat().volsCompletion.get(0).isCancelled(),
                    "l'annulation traverse le fil : le vol tenu côté serveur"
                            + " est annulé par $/cancelRequest");
            // le dernier mot gagne : le banc achève le second vol, la fraîche est servie
            banc.etat().volsCompletion.get(1).complete(Either.forRight(
                    new CompletionList(false, List.of())));
            ListeCompletion servie = fraiche.get(5, TimeUnit.SECONDS);
            assertFalse(servie.estIncomplete(),
                    "la demande fraîche est servie, jamais empilée");
        }
    }

    // ── La couverture du jalon (H-12) : chaque méthode, la forme du
    //    protocole lue en domaine — le serveur factice les sert toutes. ──

    /** Les références : les lieux plats du protocole. */
    @Test
    void lesReferencesServentLesLieux() throws Exception {
        try (Banc banc = demarrer()) {
            List<LieuLsp> lieux = banc.session().references(URI,
                    new PositionLsp(0, 4)).get(5, TimeUnit.SECONDS);
            assertEquals(1, lieux.size());
            assertEquals(URI, lieux.get(0).uri());
            assertNotNull(lieux.get(0).plage());
            assertEquals(new PositionLsp(3, 1), lieux.get(0).plage().debut());
        }
    }

    /** Les symboles : la hiérarchie du serveur suit, la profondeur entière. */
    @Test
    void lesSymbolesServentLaHierarchie() throws Exception {
        try (Banc banc = demarrer()) {
            List<SymboleLsp> symboles = banc.session().symboles(URI)
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, symboles.size());
            SymboleLsp racine = symboles.get(0);
            assertEquals("exemplaire", racine.nom());
            assertEquals(8, racine.genre());
            assertEquals(1, racine.enfants().size());
            assertEquals("imbrique", racine.enfants().get(0).nom());
        }
    }

    /** Les surlignages : la plage et son genre, lus du protocole. */
    @Test
    void lesSurlignagesServentLesOccurrences() throws Exception {
        try (Banc banc = demarrer()) {
            List<SurlignageLsp> surlignages = banc.session().surlignages(URI,
                    new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
            assertEquals(1, surlignages.size());
            assertEquals(new PlageLsp(new PositionLsp(0, 4),
                    new PositionLsp(0, 9)), surlignages.get(0).plage());
            assertEquals(2, surlignages.get(0).genre());
        }
    }

    /** La préparation de renommage : la plage et son amorce. */
    @Test
    void laPreparationDeRennomageSertPlageEtAmorce() throws Exception {
        try (Banc banc = demarrer()) {
            PreparationRennomageLsp preparation =
                    banc.session().preparerRennomage(URI,
                            new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
            assertNotNull(preparation);
            assertEquals(new PlageLsp(new PositionLsp(0, 4),
                    new PositionLsp(0, 9)), preparation.plage());
            assertEquals("ancien", preparation.texteAmorce());
        }
    }

    /** Le renommage : l'édition d'espace porte le nouveau nom. */
    @Test
    void leRennomageSertLEditionDEspace() throws Exception {
        try (Banc banc = demarrer()) {
            EditionEspaceLsp espace = banc.session().rennomage(URI,
                    new PositionLsp(0, 5), "neuf").get(5, TimeUnit.SECONDS);
            assertEquals(1, espace.documents().size());
            assertEquals("neuf", espace.documents().get(0).editions().get(0)
                    .nouveauTexte());
        }
    }

    /** Les correctifs : le titre et son édition — la carte héritée se plie. */
    @Test
    void lesCorrectifsServentTitreEtEdition() throws Exception {
        try (Banc banc = demarrer()) {
            List<CorrectifLsp> correctifs = banc.session().correctifs(URI,
                    new PlageLsp(new PositionLsp(0, 0), new PositionLsp(0, 4)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, correctifs.size());
            assertEquals("Entourer d'accolades", correctifs.get(0).titre());
            assertEquals(1, correctifs.get(0).edition().documents().size());
        }
    }

    /** Le formatage : les éditions de texte, l'ordre du serveur. */
    @Test
    void leFormatageSertSesEditions() throws Exception {
        try (Banc banc = demarrer()) {
            List<EditionTexteLsp> editions = banc.session().formatage(URI,
                    OptionsFormatageLsp.DEFAUT).get(5, TimeUnit.SECONDS);
            assertEquals(1, editions.size());
            assertEquals("  ", editions.get(0).nouveauTexte());
            List<EditionTexteLsp> plage = banc.session().formatagePlage(URI,
                    new PlageLsp(new PositionLsp(0, 0), new PositionLsp(1, 0)),
                    OptionsFormatageLsp.DEFAUT).get(5, TimeUnit.SECONDS);
            assertEquals("formate", plage.get(0).nouveauTexte());
        }
    }

    /** Les plis : la fin inclusive du protocole devient EXCLUSIVE au domaine. */
    @Test
    void lesPlisDeviennentAFinExclusive() throws Exception {
        try (Banc banc = demarrer()) {
            List<PliLsp> plis = banc.session().plis(URI).get(5, TimeUnit.SECONDS);
            assertEquals(1, plis.size());
            assertEquals(new PliLsp(1, 5), plis.get(0),
                    "la ligne 4 inclusive du protocole vaut 5 exclusive");
        }
    }

    /** Les plages de sélection : la chaîne des englobantes, de l'intime à la large. */
    @Test
    void lesPlagesDeSelectionDescendentLesPaliers() throws Exception {
        try (Banc banc = demarrer()) {
            List<PlageLsp> plages = banc.session().plagesSelection(URI,
                    new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
            assertEquals(2, plages.size());
            assertEquals(new PlageLsp(new PositionLsp(0, 4),
                    new PositionLsp(0, 9)), plages.get(0));
            assertEquals(new PlageLsp(new PositionLsp(0, 0),
                    new PositionLsp(0, 20)), plages.get(1));
        }
    }

    /** Les jetons : la charge encodée traverse, le décodage reste au domaine. */
    @Test
    void lesJetonsServentLaChargeEncodee() throws Exception {
        try (Banc banc = demarrer()) {
            SemanticTokensLsp jetons = banc.session().jetons(URI)
                    .get(5, TimeUnit.SECONDS);
            assertEquals("factice-1", jetons.identifiantResultat());
            assertEquals(1, jetons.jetons().size());
            assertEquals(2, jetons.jetons().get(0).ligne());
            SemanticTokensLsp plage = banc.session().jetonsPlage(URI,
                    new PlageLsp(new PositionLsp(0, 0), new PositionLsp(1, 0)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, plage.jetons().size());
        }
    }

    /** Les indices : les parties typées et leur cible de navigation. */
    @Test
    void lesIndicesServentPartiesEtCible() throws Exception {
        try (Banc banc = demarrer()) {
            List<IndiceLsp> indices = banc.session().indices(URI,
                    new PlageLsp(new PositionLsp(0, 0), new PositionLsp(9, 0)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, indices.size());
            IndiceLsp indice = indices.get(0);
            assertEquals(1, indice.genre());
            assertTrue(indice.remplissageGauche());
            assertEquals(2, indice.parties().size());
            assertEquals("Double", indice.parties().get(0).valeur());
            assertNotNull(indice.parties().get(0).cible());
            assertEquals("total", indice.parties().get(1).valeur());
        }
    }
}
