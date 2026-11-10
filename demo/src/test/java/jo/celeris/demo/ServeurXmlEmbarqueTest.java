package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le serveur XML RÉEL embarqué (M8.6 — LemMinX, identité au NOTICE) :
 * lancé EN PROCESSUS sur les tubes mémoire par sa DÉCLARATION (H-13 —
 * le fichier servers.json de la démonstration, le montage générique
 * de la frontière), rejoint par le client du cœur et son transport à
 * flux — le test À FOND de la fonctionnalité LSP contre un serveur du
 * monde réel : cadrage Content-Length pour de vrai, poignée de main,
 * synchronisation complète versionnée, complétion documentée, survol,
 * diagnostics de validation poussés, et l'ordonnanceur qui remplace
 * les demandes en vol (PIEGE-18).
 */
class ServeurXmlEmbarqueTest {

    private static final String URI = "file:///exemple.xml";

    private static final String VALIDE = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE racine [
                <!-- Un catalogue d'exemples. -->
                <!ELEMENT racine (exemple*)>
                <!-- Un exemple nommé, avec sa valeur. -->
                <!ELEMENT exemple (nom, valeur)>
                <!-- Le nom de l'exemple. -->
                <!ELEMENT nom (#PCDATA)>
                <!-- La valeur mesurée de l'exemple. -->
                <!ELEMENT valeur (#PCDATA)>
            ]>
            <racine>
                <exemple>
                    <nom>premier</nom>
                    <valeur>1</valeur>
                </exemple>
            </racine>
            """;

    /** Le document au milieu d'une frappe : un « ex » inachevé. */
    private static final String AVEC_FRAPPE = VALIDE.replace("</racine>",
            "    <ex\n</racine>");

    private LanguageSession serveur;
    private final CopyOnWriteArrayList<List<DiagnosticLsp>> publications =
            new CopyOnWriteArrayList<>();
    private CountDownLatch verrou;

    @BeforeEach
    void demarrer() {
        // le chemin réel de la démonstration (H-13) : le registre
        // détecte le langage, la déclaration servers.json monte le
        // serveur disponible — aucun nom de serveur dans le code
        serveur = DeclaredServers.open(
                LangagesDemo.langageDe("exemple.xml", null, null));
        verrou = new CountDownLatch(1);
        serveur.poserAuditeurDiagnostics((uri, diagnostics) -> {
            publications.add(List.copyOf(diagnostics));
            verrou.countDown();
        });
    }

    @AfterEach
    void arreter() {
        serveur.close();
    }

    private static <T> T joindre(CompletableFuture<T> futur) throws Exception {
        return futur.get(20, TimeUnit.SECONDS);
    }

    /** Les diagnostics du document valide : le serveur valide et publie. */
    @Test
    void leDocumentValidePublieSesDiagnostics() throws Exception {
        serveur.ouvrir(URI, "xml", VALIDE);
        assertTrue(verrou.await(20, TimeUnit.SECONDS),
                "le serveur réel publie ses diagnostics à l'ouverture");
        List<DiagnosticLsp> derniers = publications.get(
                publications.size() - 1);
        assertTrue(derniers.stream().noneMatch(d ->
                        d.gravite() == DiagnosticLsp.ERREUR),
                "le document valide ne publie aucune erreur : "
                        + derniers);
    }

    /** La violation de la DTD se publie en erreur de validation. */
    @Test
    void laViolationDeDtdSePublie() throws Exception {
        serveur.ouvrir(URI, "xml", VALIDE.replace("<valeur>1</valeur>",
                "<valeur>1</valeur><machin/>"));
        assertTrue(verrou.await(20, TimeUnit.SECONDS),
                "la violation publie ses diagnostics");
        List<DiagnosticLsp> derniers = publications.get(
                publications.size() - 1);
        assertTrue(derniers.stream().anyMatch(d -> d.message()
                        .contains("machin")),
                "l'élément inconnu se diagnostique : "
                        + derniers.stream().map(DiagnosticLsp::message)
                        .toList());
    }

    /** L'édition suivie revalide : la synchronisation complète vit. */
    @Test
    void lEditionSuitLeDocumentEtRevalide() throws Exception {
        serveur.ouvrir(URI, "xml", VALIDE);
        assertTrue(verrou.await(20, TimeUnit.SECONDS));
        verrou = new CountDownLatch(1);
        serveur.synchroniser(URI, VALIDE.replace("<valeur>1</valeur>",
                "<valeur>1</valeur><machin/>"));
        assertTrue(verrou.await(20, TimeUnit.SECONDS),
                "le changement déclenche une nouvelle validation");
        List<DiagnosticLsp> derniers = publications.get(
                publications.size() - 1);
        assertFalse(derniers.isEmpty(),
                "le document édité viole la DTD : l'erreur arrive");
    }

    /** La complétion sert l'élément de la DTD, SA DOCUMENTATION comprise. */
    @Test
    void laCompletionSertLesElementsDocumentes() throws Exception {
        serveur.ouvrir(URI, "xml", AVEC_FRAPPE);
        int caret = AVEC_FRAPPE.indexOf("<ex\n") + 3;
        ListeCompletion reponse = joindre(serveur.completion(URI,
                TextSession.of(AVEC_FRAPPE, () -> 0L).document(), caret));
        assertFalse(reponse.elements().isEmpty(),
                "le serveur propose l'élément qui matche le préfixe");
        assertTrue(reponse.elements().stream()
                .anyMatch(e -> "exemple".equals(e.libelle())),
                "l'élément déclaré se propose");
        int documente = 0;
        while (documente < reponse.elements().size()
                && reponse.elements().get(documente).documentation() == null) {
            documente++;
        }
        assertTrue(documente < reponse.elements().size(),
                "un candidat porte sa documentation de DTD");
        CompletionDocModel doc = PopupCoach.docDe(reponse, documente);
        assertNotNull(doc, "la documentation se convertit en modèle");
    }

    /** Le survol sert la documentation du commentaire de déclaration. */
    @Test
    void leSurvolSertLaDocumentationDtd() throws Exception {
        serveur.ouvrir(URI, "xml", VALIDE);
        int caret = VALIDE.indexOf("<exemple>") + 3;
        CompletionDocModel doc = joindre(serveur.survol(URI,
                TextSession.of(VALIDE, () -> 0L).document(), "exemple",
                caret));
        assertNotNull(doc, "l'élément déclaré sert sa documentation");
        assertTrue(doc.texteLibre().contains("Un exemple nommé"),
                "la documentation vient du commentaire de la DTD : "
                        + doc.texteLibre());
    }

    /** La signature : le serveur XML n'en sert jamais — null au futur. */
    @Test
    void laSignatureNeSertRienEnXml() throws Exception {
        serveur.ouvrir(URI, "xml", VALIDE);
        assertNull(joindre(serveur.signature(URI,
                TextSession.of(VALIDE, () -> 0L).document(), 3)));
    }

    /**
     * PIEGE-18 — les frappes rapides : deux complétions de suite, la
     * seconde remplace la première en vol — le futur remplacé échoue,
     * le dernier mot gagne, jamais d'empilement.
     */
    @Test
    void lesFrappesRapidesRemplacentEnVol() throws Exception {
        serveur.ouvrir(URI, "xml", AVEC_FRAPPE);
        int frappe = AVEC_FRAPPE.indexOf("<ex\n") + 2;
        CompletableFuture<ListeCompletion> vieille = serveur.completion(
                URI, TextSession.of(AVEC_FRAPPE, () -> 0L).document(),
                frappe);
        CompletableFuture<ListeCompletion> fraiche = serveur.completion(
                URI, TextSession.of(AVEC_FRAPPE, () -> 0L).document(),
                frappe + 1);
        assertNotNull(joindre(fraiche), "la demande fraîche gagne");
        AtomicReference<Throwable> remplacee = new AtomicReference<>();
        vieille.whenComplete((reponse, echec) -> remplacee.set(echec));
        Thread.sleep(500);
        assertTrue(vieille.isDone(), "la demande remplacée se termine");
        assertTrue(remplacee.get() instanceof Exception,
                "le futur remplacé échoue : " + remplacee.get());
    }
}
