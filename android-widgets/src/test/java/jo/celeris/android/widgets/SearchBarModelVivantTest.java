package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

/**
 * C10 (l'analyse croisée) : la barre VIT avec le document — le
 * recalcul part à l'édition (plus de résultats périmés après la
 * première frappe), l'occurrence courante se remplace, et les
 * signaux du moteur ({@code truncated} / {@code tooCostly})
 * remontent à l'hôte au lieu de se perdre dans le calcul.
 */
public class SearchBarModelVivantTest {

    /** Le recalcul vivant : une édition du document rafraîchit les
     *  occurrences sans repasser par setQuery. */
    @Test
    public void leRecalculPartALaEdition() {
        TextSession session = TextSession.of("mot un\nmot deux\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(session, () -> 0L);
        barre.setQuery("mot");
        assertEquals(2, barre.matches().size());
        // une frappe AJOUTE une occurrence : la barre le voit SEULE
        session.replaceRange(7, 7, "mot");
        assertEquals("le troisime mot arrive dans la barre",
                3, barre.matches().size());
    }

    /** Le replaceCurrent : l'occurrence courante se remplace, la
     *  suivante devient courante, le compte suit. */
    @Test
    public void leRemplacementCourantRemplaceEtAvance() {
        TextSession session = TextSession.of("aaa bbb aaa bbb\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(session, () -> 0L);
        barre.setQuery("bbb");
        assertEquals(2, barre.matches().size());
        barre.setCurrentNear(0);
        assertNotNull(barre.current());
        boolean remplace = barre.replaceCurrent("ccc");
        assertTrue(remplace);
        // setCurrentNear(0) prend le premier « bbb » (offset 4)
        assertEquals("aaa ccc aaa bbb\n", session.document().text());
        assertEquals("le compte suit le remplacement",
                1, barre.matches().size());
        int courante = barre.currentIndex();
        assertTrue("une occurrence reste courante", courante >= 0);
        assertEquals("la suivante devient courante", "bbb",
                session.document().text().substring(
                        barre.matches().get(courante).start(),
                        barre.matches().get(courante).end()));
    }

    /** Sans occurrence courante, le remplacement rend la main. */
    @Test
    public void leRemplacementSansOccurrenceRendLaMain() {
        TextSession session = TextSession.of("rien\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(session, () -> 0L);
        assertFalse(barre.replaceCurrent("x"));
    }

    /** Le remplacement survit à l'annulation : la passe passe par la
     *  voie de transaction de la session (PIEGE-07), pas par un
     *  contournement. */
    @Test
    public void leRemplacementSannule() {
        TextSession session = TextSession.of("old old\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(session, () -> 0L);
        barre.setQuery("old");
        barre.setCurrentNear(0);
        barre.replaceCurrent("new");
        session.undo();
        assertEquals("old old\n", session.document().text());
    }

    /** Les signaux du moteur remontent : une requête regex
     *  catastrophique porte {@code tooCostly}, une collecte plafonnée
     *  porte {@code truncated}. */
    @Test
    public void lesSignauxRemontent() {
        // 60 000 « a » sans le « b » : backtracking exponentiel de
        // (a+)+b — l'horloge RÉELLE fait courir le garde-fou (le banc
        // du moteur, FindReplaceSafetyTest, suit la même voie).
        String adverse = "a".repeat(60_000);
        TextSession lent = TextSession.of(adverse, System::nanoTime);
        SearchBarModel barre = new SearchBarModel(lent, System::currentTimeMillis);
        barre.setRegex(true);
        barre.setQuery("(a+)+b");
        assertTrue("le signal tooCostly remonte", barre.isTooCostly());
        assertFalse(barre.isTruncated());

        StringBuilder plafond = new StringBuilder();
        for (int i = 0; i < 12_000; i++) {
            plafond.append("ab ");
        }
        TextSession dense = TextSession.of(plafond.toString(), () -> 0L);
        SearchBarModel barreDense = new SearchBarModel(dense, () -> 0L);
        barreDense.setQuery("ab");
        assertEquals("la collecte s'arrête au plafond",
                jo.celeris.core.text.FindReplace.MAX_MATCHES,
                barreDense.matches().size());
        assertTrue("le signal truncated remonte", barreDense.isTruncated());
        assertFalse(barreDense.isTooCostly());
    }

    /** Le détachement : une barre fermée ne retient pas le document
     *  (l'écouteur part, plus de fuite de l'ancien). */
    @Test
    public void laBarreFermeSeDetache() {
        TextSession session = TextSession.of("mot\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(session, () -> 0L);
        barre.setQuery("mot");
        AtomicInteger notifications = new AtomicInteger();
        session.addEditListener(new EditListener() {
            @Override
            public void onTextEdit(int start, int end, String removed, String inserted) {
                notifications.incrementAndGet();
            }
        });
        barre.close();
        // la barre morte n'écoute plus : éditer ne la réveille pas
        session.replaceRange(0, 3, "xyz");
        assertEquals(1, notifications.get());
    }

    /** Le changement de document détache l'ancien : l'ancienne
     *  session n'est plus retenue par la barre. */
    @Test
    public void leChangementDeDocumentDetacheLancien() {
        TextSession premiere = TextSession.of("mot\n", () -> 0L);
        SearchBarModel barre = new SearchBarModel(premiere, () -> 0L);
        barre.setQuery("mot");
        barre.setDocument(Document.of("autre\n"));
        // l'ancienne session éditée ne touche plus la barre
        premiere.replaceRange(0, 4, "mot mot\n");
        assertEquals(0, barre.matches().size());
    }

}
