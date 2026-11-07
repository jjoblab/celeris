package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La session de langage de la démonstration (M7.5, §8) : le client
 * LSP branché au serveur de référence EN PROCESSUS — l'aide aux
 * paramètres et le survol passent par le fil du protocole (B-07,
 * B-09), la synchronisation complète suit les éditions. Pur, sans
 * appareil — le transport mémoire livre sans fil, les futurs se
 * rejoignent bornés.
 */
class ServeurLangageTest {

    private static final String URI = "file:///exemple.java";

    private static final String DOCUMENT = """
            int ajouter(int a, int b) {
                return a + b;
            }
            int total = ajouter(1, 2);
            """;

    private ServeurLangage serveur;

    @BeforeEach
    void demarrer() {
        serveur = ServeurLangage.demarrer();
        serveur.ouvrir(URI, "java", DOCUMENT);
    }

    @AfterEach
    void arreter() {
        serveur.close();
    }

    private static <T> T joindre(CompletableFuture<T> futur) throws Exception {
        return futur.get(5, TimeUnit.SECONDS);
    }

    /** B-07 — la signature sert par le fil : libellé, paramètres, paramètre courant. */
    @Test
    void signatureServedOverTheWire() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // le curseur dans l'appel « ajouter(1, » après la virgule
        int caret = DOCUMENT.indexOf("ajouter(1,") + "ajouter(1,".length();
        SignatureAideModel modele = joindre(serveur.signature(URI,
                session.document(), caret));
        assertTrue(modele != null, "l'appel ouvert sert sa signature");
        assertEquals("ajouter(int a, int b)", modele.libelle(),
                "le libellé de la table, par le protocole");
        assertEquals(1, modele.parametreCourant(),
                "après la virgule : le deuxième paramètre");
    }

    /** B-07 — hors de tout appel, le serveur ne dit rien : le futur rend null. */
    @Test
    void signatureOutsideCallServesNothing() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        SignatureAideModel modele = joindre(serveur.signature(URI,
                session.document(), 3));
        assertNull(modele, "hors appel, pas de signature");
    }

    /** B-09 — le survol sert la documentation du symbole par le fil. */
    @Test
    void hoverServesTheDocTable() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        int caret = DOCUMENT.indexOf("ajouter(1,") + 3;
        CompletionDocModel doc = joindre(serveur.survol(URI,
                session.document(), "ajouter", caret));
        assertTrue(doc != null, "le symbole connu sert sa documentation");
        assertEquals("ajouter", doc.titre(),
                "le titre est le symbole, jamais fondu (PIEGE-16)");
    }

    /** B-09 — le symbole inconnu ne sert rien : null dans le futur. */
    @Test
    void hoverOfUnknownSymbolServesNothing() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        int caret = DOCUMENT.indexOf("total") + 2;
        CompletionDocModel doc = joindre(serveur.survol(URI,
                session.document(), "total", caret));
        assertNull(doc, "la table ne connaît pas « total »");
    }

    /** La synchronisation complète : l'édition suivie, le serveur répond du neuf. */
    @Test
    void didChangeKeepsTheServerCurrent() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // l'appel disparaît : le texte suivi change par la voie du protocole
        String sansAppel = DOCUMENT.replace("ajouter(1, 2)", "0");
        serveur.synchroniser(URI, sansAppel);
        int caret = DOCUMENT.indexOf("ajouter(1,") + "ajouter(1,".length();
        assertNull(joindre(serveur.signature(URI,
                        TextSession.of(sansAppel, () -> 0L).document(), caret)),
                "l'appel suivi a disparu : plus de signature");
        // l'appel revient : le serveur suit encore
        serveur.synchroniser(URI, DOCUMENT);
        SignatureAideModel modele = joindre(serveur.signature(URI,
                session.document(), caret));
        assertEquals("ajouter(int a, int b)", modele.libelle(),
                "le retour de l'appel ressait sa signature");
    }
}
