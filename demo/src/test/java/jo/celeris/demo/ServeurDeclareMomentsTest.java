package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.text.TextSession;

/**
 * Le BON MOMENT des popups (H-13, le signalement du propriétaire) : le
 * serveur déclaré rend chaque fonctionnalité à l'instant où la popup
 * qui la sert doit vivre — la signature NON VIDE au caret DANS un
 * appel, null dès la sortie (la popup se ferme, jamais elle ne
 * s'accroche) ; le survol au MOT, null au blanc ; les diagnostics
 * POUSSÉS à l'ouverture puis au changement du texte ; la complétion au
 * préfixe tapé. Les popups de la démonstration s'alimentent de ces
 * seules réponses — le moment juste se prouve à la source.
 */
class ServeurDeclareMomentsTest {

    private static final String URI_D = "file:///moments.java";

    private static final String TEXTE = "class Moments {\n"
            + "    int ajouter(int a, int b) {\n"
            + "        return a + b;\n"
            + "    }\n"
            + "    void corps() {\n"
            + "        int total = ajouter(1, 2);\n"
            + "    }\n"
            + "}\n";

    /** Ouvre la session déclarée sur le texte — null si repli. */
    private static LanguageSession sessionSur(String texte) {
        LanguageSupport langue = LangagesDemo.langageDe("moments.java",
                null, texte);
        if (langue == null) {
            return null;
        }
        LanguageSession session = DeclaredServers.open(langue);
        if (!(session instanceof DeclaredServerSession)) {
            return null;
        }
        session.ouvrir(URI_D, "java", texte);
        return session;
    }

    /** La signature vit au caret DANS l'appel, meurt à la sortie. */
    @Test
    void laSignatureNaitDansLappelEtMeurtALaSortie() throws Exception {
        try (LanguageSession session = sessionSur(TEXTE)) {
            assertNotNull(session, "le serveur déclaré monte");
            var document = TextSession.of(TEXTE, () -> 0L).document();
            int dansLappel = TEXTE.indexOf("ajouter(1,")
                    + "ajouter(1,".length();
            var aide = session.signature(URI_D, document, dansLappel)
                    .get(2, TimeUnit.SECONDS);
            assertNotNull(aide, "dans l'appel ouvert, la signature vit");
            assertEquals("ajouter(int a, int b)", aide.libelle());
            assertEquals(1, aide.parametreCourant());
            // la parenthèse fermée atteinte : la popup se ferme
            int apresLappel = TEXTE.indexOf("ajouter(1, 2);")
                    + "ajouter(1, 2);".length();
            assertNull(session.signature(URI_D, document, apresLappel)
                            .get(2, TimeUnit.SECONDS),
                    "après l'appel fermé, la signature meurt");
            // le caret sur le nom appelé, AVANT l'ouvrante : rien ne vit
            assertNull(session.signature(URI_D, document,
                            TEXTE.indexOf("ajouter(1")).get(2, TimeUnit.SECONDS),
                    "avant l'appel, la signature ne naît pas");
        }
    }

    /** Le survol vit au mot, meurt au blanc et au signe. */
    @Test
    void leSurvolVitAuMotMeurtAuBlancEtAuSigne() throws Exception {
        try (LanguageSession session = sessionSur(TEXTE)) {
            assertNotNull(session);
            var document = TextSession.of(TEXTE, () -> 0L).document();
            int auNom = TEXTE.indexOf("ajouter(1,") + 2;
            assertNotNull(session.survol(URI_D, document, "ajouter", auNom)
                            .get(2, TimeUnit.SECONDS),
                    "le nom appelé se survole — sa déclaration répond");
            // le blanc entre « total » et « = » : aucun mot, aucun survol
            int auBlanc = TEXTE.indexOf("int total") + "int total".length();
            assertNull(session.survol(URI_D, document, "", auBlanc)
                            .get(2, TimeUnit.SECONDS),
                    "le blanc ne se survole pas");
            // le point-virgule : aucun mot, aucun survol
            int auPoint = TEXTE.indexOf("ajouter(1, 2);")
                    + "ajouter(1, 2".length();
            assertNull(session.survol(URI_D, document, "", auPoint)
                            .get(2, TimeUnit.SECONDS),
                    "le signe ne se survole pas");
        }
    }

    /** Les diagnostics se POUSSENT à l'ouverture et au changement. */
    @Test
    void lesDiagnosticsSePoussentALouverturePuisAuChangement() throws Exception {
        try (LanguageSession session = sessionSur(TEXTE)) {
            assertNotNull(session);
            List<DiagnosticLsp> premiere = new java.util.ArrayList<>();
            java.util.concurrent.CountDownLatch pousseeNonVide =
                    new java.util.concurrent.CountDownLatch(1);
            session.poserAuditeurDiagnostics((uri, constats) -> {
                // la poussée de l'OUVERTURE peut voyager en retard —
                // seule la poussée NON VIDE du changement réveille :
                // le document fautif porte son constat, jamais un vent
                if (!constats.isEmpty()) {
                    premiere.addAll(constats);
                    pousseeNonVide.countDown();
                }
            });
            // la re-poussée part du changement : l'accolade de « corps »
            // disparaît — le constat d'équilibre devient vivant
            String mute = TEXTE.replace("    void corps() {\n",
                    "    void corps()\n");
            session.synchroniser(URI_D, mute);
            assertTrue(pousseeNonVide.await(2, TimeUnit.SECONDS),
                    "le changement pousse ses diagnostics");
            assertFalse(premiere.isEmpty(),
                    "le document fautif porte un constat réel");
        }
    }

    /** La complétion répond au préfixe tapé, jamais au blanc. */
    @Test
    void laCompletionRepondAuPrefixePasAuBlanc() throws Exception {
        try (LanguageSession session = sessionSur(TEXTE)) {
            assertNotNull(session);
            var document = TextSession.of(TEXTE, () -> 0L).document();
            int auPrefixe = TEXTE.indexOf("int total = a")
                    + "int total = a".length();
            var elements = session.completion(URI_D, document, auPrefixe)
                    .get(2, TimeUnit.SECONDS).elements();
            assertTrue(elements.stream()
                            .anyMatch(c -> "ajouter".equals(c.libelle())),
                    "le préfixe tapé propose la méthode déclarée");
            int auBlanc = TEXTE.indexOf("int total") + "int total".length();
            var auBlancListe = session.completion(URI_D, document, auBlanc)
                    .get(2, TimeUnit.SECONDS).elements();
            assertFalse(auBlancListe.stream()
                            .anyMatch(c -> "ajouter".equals(c.libelle())),
                    "le blanc ne propose pas le mot collé au caret");
        }
    }
}
