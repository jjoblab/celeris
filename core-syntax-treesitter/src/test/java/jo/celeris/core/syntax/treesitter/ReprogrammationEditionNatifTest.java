package jo.celeris.core.syntax.treesitter;

import jo.celeris.core.syntax.Analysis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * Le banc de la REPROGRAMMATION à l'édition (feuille de route A1.1) :
 * la voie native ne pouvait vivre que par un appel externe de
 * reanalyser() — l'auditeur d'édition enregistrait les bornes et
 * attendait. Le pipeline réel ne l'appelle jamais : la publication
 * périmait à la première frappe, pour toujours. Le banc verrouille le
 * contrat nouveau : l'édition programme la passe, la publication suit
 * la révision, la rafale de frappes finit publiée à la dernière.
 *
 * <p>Vit sous {@code assumeTrue} : sans la liaison chargée (JVM sans
 * Android), il se saute — même discipline que NatifBancTest.</p>
 */
class ReprogrammationEditionNatifTest {

    /** Le nettoyage : l'enregistrement de la grammaire python ne
     *  fuit pas entre les bancs (le registre de la plateforme est
     *  STATIQUE — leçon de la session : sans AfterEach ici, la
     *  fabrique d'un autre banc voyait python s'inscrire). */
    @org.junit.jupiter.api.AfterEach
    void nettoyer() {
        TreeSitterGrammars.oublier("python");
    }

    /** La source python — le saut du banc sans liaison. La grammaire
     *  s'enregistre par le registre (ADR-0033) : le fournisseur rend
     *  la classe de langue de la liaison. */
    private static SyntaxeNatif python() {
        if (TreeSitterGrammars.grammaireDe("python") == null) {
            TreeSitterGrammars.enregistrer("python",
                    com.itsaky.androidide.treesitter.python.TSLanguagePython::getInstance,
                    "");
        }
        SyntaxeNatif source = (SyntaxeNatif) jo.celeris.core.language.packs
                .TreeSitterLanguages.syntaxOf("python");
        assumeTrue(source != null);
        return source;
    }

    /**
     * L'édition programme la passe : la publication suit la révision
     * courante sans AUCUN appel externe de reanalyser() — le chemin du
     * pont de vue (attacher → frapper → lire) suffit.
     */
    @Test
    void leditionProgrammeLaPasse() {
        SyntaxeNatif python = python();
        TextSession session = TextSession.of(
                "def f():\n    return 1\n", () -> 0L);
        NativeEngine moteur = NativeEngine.on(session, python, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        assertNotNull(moteur.analysePubliee());
        // LA frappe — aucun reanalyser() derriere : le moteur suit seul
        assertTrue(session.replaceRange(21, 21, " + 2"));
        Analysis apres = moteur.analysePubliee();
        assertNotNull(apres,
                "l'edition doit reprogrammer la passe sans appel externe");
        assertEquals(session.document().revision(), apres.revision(),
                "la publication suit la revision courante");
        moteur.close();
    }

    /**
     * La rafale de frappes finit publiée à la dernière révision : les
     * passes se coalescent, chacune sert le document courant au moment
     * où elle tourne — la dernière révision finit toujours servie.
     */
    @Test
    void laRafaleFinitPublieeALaDerniere() {
        SyntaxeNatif python = python();
        TextSession session = TextSession.of("x = 1\n", () -> 0L);
        NativeEngine moteur = NativeEngine.on(session, python, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        assertNotNull(moteur.analysePubliee());
        for (int i = 0; i < 20; i++) {
            assertTrue(session.replaceRange(0, 0, "y"));
        }
        Analysis apres = moteur.analysePubliee();
        assertNotNull(apres);
        assertEquals(session.document().revision(), apres.revision(),
                "20 frappes : la publication rattrape la derniere revision");
        moteur.close();
    }

    /**
     * L'auditeur part à chaque publication programmée par l'édition :
     * le pont de vue y invalide — la rafale ne le quitte jamais.
     */
    @Test
    void lauditeurSuivitLesPublicationsDEdition() {
        SyntaxeNatif python = python();
        TextSession session = TextSession.of("x = 1\n", () -> 0L);
        NativeEngine moteur = NativeEngine.on(session, python, null);
        assumeTrue(moteur != null);
        AtomicInteger publications = new AtomicInteger();
        List<Integer> revisions = new ArrayList<>();
        moteur.addListener(analyse -> {
            publications.incrementAndGet();
            revisions.add(analyse.revision());
        });
        moteur.reanalyser();
        assertTrue(session.replaceRange(5, 5, " + 1"));
        assertTrue(session.replaceRange(0, 0, "# "));
        assertTrue(publications.get() >= 2,
                "l'edition publie au moins une fois par frappe servie");
        assertEquals(Integer.valueOf(session.document().revision()),
                revisions.get(revisions.size() - 1),
                "la derniere publication servie est la revision courante");
        moteur.close();
    }

    /**
     * La fermeture coupe la reprogrammation : une édition après close()
     * ne poste rien, ne leve rien — le moteur est mort, il ne travaille
     * plus (la discipline C2 : aucun fil d'analyse vivant derrière).
     */
    @Test
    void laFermetureCoupeLaReprogrammation() {
        SyntaxeNatif python = python();
        TextSession session = TextSession.of("x = 1\n", () -> 0L);
        NativeEngine moteur = NativeEngine.on(session, python, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        Analysis avant = moteur.analysePubliee();
        assertNotNull(avant);
        moteur.close();
        assertTrue(session.replaceRange(0, 0, "z"));
        assertNull(moteur.analyseDeRevision(session.document().revision()),
                "ferme : aucune publication nouvelle apres l'edition");
    }
}
