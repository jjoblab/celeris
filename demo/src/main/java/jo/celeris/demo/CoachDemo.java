package jo.celeris.demo;

import java.util.List;

import jo.celeris.android.host.popups.CoachPopups;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.SignatureAideModel;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Le coach de la démonstration : les tables du terrain servies à
 * l'orchestrateur de l'éditeur (le déménagement du 2026-11-14 —
 * {@link PopupCoach} garde les tables, l'éditeur porte les
 * algorithmes). Le terrain de test y gagne ses mots réservés Java,
 * ses signatures connues et sa documentation factice ; toute
 * application hôte ferait de même avec SES tables.
 */
final class CoachDemo implements CoachPopups {

    @Override
    public List<String> motsReserves() {
        return PopupCoach.motsReserves();
    }

    @Override
    public List<CompletionListView.Candidat> candidats(List<String> bruts) {
        return PopupCoach.candidats(bruts);
    }

    @Override
    public @Nullable CompletionDocModel docDe(String symbole) {
        return PopupCoach.docFor(symbole);
    }

    @Override
    public @Nullable SignatureAideModel signatureDe(Document document,
            int caret) {
        return PopupCoach.signatureFor(document, caret);
    }
}
