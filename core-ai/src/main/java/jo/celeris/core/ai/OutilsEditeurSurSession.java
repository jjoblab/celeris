package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * L'implémentation de référence de l'API d'outils (F-05, ADR-012) :
 * les outils servent la session donnée, les symboles et les
 * diagnostics viennent des sources injectées par l'hôte — le plan du
 * fichier et l'analyseur de l'éditeur, rien de caché.
 *
 * <p>Le modèle d'exécution (ADR-007) : chaque appel se prend sur le
 * fil d'écriture de la session ; l'hôte qui expose les outils au
 * réseau ramène ses appels à ce fil, le cœur ne connaît pas le sien.
 * L'application passe par la voie de transaction ordinaire — une étape
 * d'annulation, jamais de mutation directe (PIEGE-07).</p>
 */
public final class OutilsEditeurSurSession implements OutilsEditeur {

    private final TextSession session;
    private final Function<Document, List<OutlineEntry>> sourceSymboles;
    private final Supplier<List<SyntaxIssue>> sourceDiagnostics;

    /**
     * Les outils sur la session donnée.
     *
     * @param session           la session servie, sur son fil d'écriture
     * @param sourceSymboles    le plan du document (B-16), calculé par
     *                          l'hôte
     * @param sourceDiagnostics les diagnostics courants, servis par
     *                          l'hôte
     */
    public OutilsEditeurSurSession(TextSession session,
            Function<Document, List<OutlineEntry>> sourceSymboles,
            Supplier<List<SyntaxIssue>> sourceDiagnostics) {
        this.session = session;
        this.sourceSymboles = sourceSymboles;
        this.sourceDiagnostics = sourceDiagnostics;
    }

    @Override
    public PlageLue lire(int debut, int fin) {
        Document document = session.document();
        return new PlageLue(document.revision(), document.subText(debut, fin));
    }

    @Override
    public PlageLue instantane() {
        return lire(0, session.document().length());
    }

    @Override
    public List<OutlineEntry> symboles() {
        return List.copyOf(sourceSymboles.apply(session.document()));
    }

    @Override
    public List<SyntaxIssue> diagnostics() {
        return List.copyOf(sourceDiagnostics.get());
    }

    @Override
    public TransactionOutilReponse appliquer(TransactionOutil transaction) {
        Document document = session.document();
        if (transaction.edits().isEmpty()) {
            return refus(TransactionOutilReponse.Sortie.REFUSEE_VIDE,
                    "la transaction ne porte aucune edition", document);
        }
        if (transaction.revisionAttendue() != document.revision()) {
            return refus(TransactionOutilReponse.Sortie.REFUSEE_CONFLIT,
                    "le document a bouge depuis l'instantane lu (attendu "
                            + transaction.revisionAttendue() + ", courant "
                            + document.revision() + ") — relire, recalculer, recommencer",
                    document);
        }
        if (session.isReadOnly()) {
            return refus(TransactionOutilReponse.Sortie.REFUSEE_PROTECTION,
                    "la session est protegee en ecriture", document);
        }
        if (!PropositionAgent.editsFideles(document, transaction.edits())) {
            return refus(TransactionOutilReponse.Sortie.REFUSEE_EDITS,
                    "les editions ne collent pas au document lu", document);
        }
        List<TextEdit> sequentielles = new ArrayList<>(transaction.edits().size());
        int delta = 0;
        for (TextEdit edition : transaction.edits()) {
            sequentielles.add(new TextEdit(edition.start() + delta,
                    edition.removed(), edition.inserted()));
            delta += edition.inserted().length() - edition.removed().length();
        }
        boolean appliquee = session.apply(Transaction.of(sequentielles));
        if (!appliquee) {
            return refus(TransactionOutilReponse.Sortie.REFUSEE_PROTECTION,
                    "la session a refuse la transaction", document);
        }
        return new TransactionOutilReponse(TransactionOutilReponse.Sortie.APPLIQUEE,
                "transaction appliquee — annulable d'un geste",
                session.document().revision());
    }

    private static TransactionOutilReponse refus(
            TransactionOutilReponse.Sortie sortie, String message,
            Document document) {
        return new TransactionOutilReponse(sortie, message, document.revision());
    }
}
