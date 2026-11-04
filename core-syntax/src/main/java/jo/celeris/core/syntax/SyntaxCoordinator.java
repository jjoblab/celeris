package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

/**
 * Coordinateur session ↔ analyse (ADR-004, ADR-007) : écoute la voie de
 * transaction unique, analyse paresseusement à la première demande puis
 * par incrément à chaque édition — le rejeu de l'annulation compris
 * (PIEGE-07 : l'auditeur est notifié du rejeu, l'analyse suit la même voie
 * que tous les consommateurs). Vit sur le fil d'écriture de la session,
 * sans verrou ; l'{@link Analysis instantané} produit est immuable et
 * lisible depuis tout fil.
 *
 * <p>La fermeture détache l'auditeur : la dernière analyse reste
 * consultable, le suivi s'arrête.</p>
 */
public final class SyntaxCoordinator {

    private final TextSession session;
    private final SyntaxService service;
    private final EditListener listener;
    private @Nullable Analysis analysis;
    private @Nullable Document analysedDoc;

    private SyntaxCoordinator(TextSession session, SyntaxService service) {
        this.session = session;
        this.service = service;
        this.listener = this::surEdition;
        session.addEditListener(listener);
    }

    /**
     * Branche le coordinateur sur une session.
     *
     * @param session  la session à suivre (fil d'écriture)
     * @param language le langage à analyser
     * @return le coordinateur abonné
     */
    public static SyntaxCoordinator on(TextSession session, LanguageSyntax language) {
        return new SyntaxCoordinator(session, SyntaxService.of(language));
    }

    /**
     * L'analyse courante — calculée à la demande si elle manque, puis
     * tenue à jour par les éditions.
     *
     * @return l'analyse de l'instantané courant
     */
    public Analysis analysis() {
        if (analysis == null || analysedDoc == null) {
            analysis = service.analyseFull(session.document());
            analysedDoc = session.document();
        }
        return analysis;
    }

    /** Détache l'auditeur : le suivi s'arrête, la dernière analyse reste. */
    public void close() {
        session.removeEditListener(listener);
    }

    private void surEdition(int start, int end, String removed, String inserted) {
        if (analysis == null || analysedDoc == null) {
            return;
        }
        Document avant = analysedDoc;
        Document apres = session.document();
        analysis = service.analyseIncremental(apres, analysis,
                EditSite.of(avant, apres, start, end, inserted));
        analysedDoc = apres;
    }
}
