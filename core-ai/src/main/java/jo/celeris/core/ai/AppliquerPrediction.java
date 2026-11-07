package jo.celeris.core.ai;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * L'application explicite d'une prédiction (F-02, ADR-012) : la voie de
 * transaction ordinaire, jamais une application silencieuse.
 *
 * <p>Le modèle ne touche aucune session — la prédiction qui arrive est
 * une donnée, possiblement fausse. L'hôte décide, puis applique ici :
 * les éditions se prévalident d'abord — bornes séquentielles et textes
 * retirés rendus exacts — car la session, elle, fait confiance à son
 * appelant : une exception en cours d'application y laisserait le
 * document muté sans étape d'annulation. Prévalidée, la prédiction
 * devient une transaction comme une autre, donc annulable et diffable.
 * L'instantané de référence doit correspondre au document courant — un
 * document qui a bougé depuis la prédiction refuse le transfert,
 * explicitement : jamais de fusion silencieuse, la discipline de l'API
 * d'outils (ADR-012) appliquée à la prédiction.</p>
 */
public final class AppliquerPrediction {

    private AppliquerPrediction() {
    }

    /** Le sort d'une application de prédiction. */
    public enum Resultat {

        /** Appliquée en transaction ordinaire — annulable, diffable. */
        APPLIQUEE,

        /** Refusée : la prédiction vise un instantané qui n'est plus courant. */
        REFUSEE_REVISION,

        /** Refusée : la session est protégée en écriture. */
        REFUSEE_PROTECTION,

        /** Refusée : les éditions prévues ne collent pas au document —
         *  bornes ou textes retirés. */
        REFUSEE_EDITS
    }

    /**
     * Applique la prédiction sur la session, par la voie de transaction
     * ordinaire — le refus est un résultat, jamais une exception.
     *
     * @param session    la session à muter, sur son fil d'écriture
     * @param prediction la prédiction à appliquer
     * @return le sort de l'application
     */
    public static Resultat appliquer(TextSession session, PredictionEdition prediction) {
        if (prediction.revision() != session.document().revision()) {
            return Resultat.REFUSEE_REVISION;
        }
        if (session.isReadOnly()) {
            return Resultat.REFUSEE_PROTECTION;
        }
        if (!editsFideles(session.document(), prediction)) {
            return Resultat.REFUSEE_EDITS;
        }
        boolean appliquee = session.apply(Transaction.of(prediction.edits()));
        return appliquee ? Resultat.APPLIQUEE : Resultat.REFUSEE_PROTECTION;
    }

    /**
     * Les éditions prévues collent au document : la simulation
     * séquentielle — chaque édition vit dans le document que les
     * précédentes ont laissé — trouve chaque plage dans les bornes et
     * chaque texte retiré exact. Les instantanés immuables rendent la
     * simulation bon marché, et la session rejouera la même séquence
     * sans lever.
     */
    private static boolean editsFideles(Document document, PredictionEdition prediction) {
        Document etat = document;
        for (TextEdit edition : prediction.edits()) {
            if (edition.end() > etat.length()) {
                return false;
            }
            if (!etat.subText(edition.start(), edition.end()).equals(edition.removed())) {
                return false;
            }
            etat = etat.replace(edition.start(), edition.end(), edition.inserted());
        }
        return true;
    }
}
