package jo.celeris.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.FeuilleHaut;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.ai.AppliquerPrediction;
import jo.celeris.core.ai.ExclusionsContexte;
import jo.celeris.core.ai.FauxModeleLocal;
import jo.celeris.core.ai.GardeContexte;
import jo.celeris.core.ai.PredictionEdition;
import jo.celeris.core.ai.RequeteModele;
import jo.celeris.core.ai.SessionTexteFantome;
import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.syntax.JetonAnnulation;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier IA de la démonstration (F-01 interface, F-02, F-03, F-07,
 * F-10) : les actions IA vivent au même titre que les intentions
 * classiques — le menu de l'atelier, rien d'ailleurs. Le modèle est le
 * faux local, déterministe et sans réseau, gardé par le contexte : le
 * consentement se relit à chaque départ (coupé par défaut, F-10) et le
 * fichier d'exclusions de la démonstration refuse les secrets (F-07).
 *
 * <p>ADR-018 (chantier C12) : le rappel de consentement et la décision
 * de prédiction vivent en feuilles du haut ; les verdicts — prédiction
 * appliquée, refusée, ou aucune à prédire — se publient à la barre
 * d'état de l'hôte (le canal Verdict), plus aucun dialogue système ne
 * reste.</p>
 */
final class AtelierIA {

    /** La clé de persistance du consentement IA (F-10) — coupé par défaut. */
    static final String CLE_CONSENTEMENT = "ia.consentement";

    /** Le fichier d'exclusions de la démonstration (F-07). */
    static final String EXCLUSIONS = """
            # Les secrets de la demonstration ne partent jamais
            /secrets/
            *.jks
            *.pem
            *.keystore
            """;

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final TextSession session;
    private final SharedPreferences prefs;
    private final Supplier<PopupPalette> palettes;
    private final AtelierRefactor.Verdict verdict;
    private final Runnable ouvrirReglages;
    private final String chemin;
    private final ExclusionsContexte exclusions = ExclusionsContexte.de(EXCLUSIONS);
    private final FauxModeleLocal modele = new FauxModeleLocal();
    private final PiloteFantome pilote;

    /** La feuille de consentement — née à son premier refus. */
    private @Nullable FeuilleHaut feuilleConsentement;
    /** La feuille de prédiction — née à sa première décision. */
    private @Nullable FeuilleHaut feuillePrediction;

    /**
     * Construit l'atelier : le pilote du fantôme s'attache à la vue —
     * Tab et l'appui sur le fantôme acceptent le premier mot — et le
     * garde du contexte enveloppe le modèle (consentement relu à chaque
     * départ, exclusions comprises).
     *
     * @param activite       l'activité hôte
     * @param editeur        la vue d'éditeur ouverte
     * @param session        la session d'édition courante
     * @param nomFichier     le nom du fichier ouvert (chemin servi au garde)
     * @param palettes       la palette courante, dérivée à chaque ouverture
     * @param verdict        le canal des verdicts (barre d'état de l'hôte)
     * @param ouvrirReglages ouvre la feuille des réglages (consentement)
     */
    AtelierIA(AppCompatActivity activite, EditorView editeur,
            TextSession session, String nomFichier,
            Supplier<PopupPalette> palettes,
            AtelierRefactor.Verdict verdict, Runnable ouvrirReglages) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.palettes = palettes;
        this.verdict = verdict;
        this.ouvrirReglages = ouvrirReglages;
        this.chemin = "/" + nomFichier;
        this.prefs = activite.getSharedPreferences("celeris",
                Context.MODE_PRIVATE);
        GardeContexte garde = new GardeContexte(modele, modele, chemin,
                exclusions, this::consentementAccorde);
        this.pilote = new PiloteFantome(session,
                new SessionTexteFantome(garde, Runnable::run));
        editeur.setSurGesteFantome(geste -> {
            pilote.surGeste(geste);
            rafraichir();
        });
    }

    /**
     * F-10 — l'état de consentement, relu à chaque départ : coupé par
     * défaut, l'activation explicite de l'utilisateur reste maîtresse.
     */
    boolean consentementAccorde() {
        return prefs.getBoolean(CLE_CONSENTEMENT, false);
    }

    /**
     * F-01 — demande une complétion au modèle local : sans
     * consentement, la demande ne part pas et la feuille du haut le
     * dit ; consentie, la suggestion se dessine fantôme à l'ancre,
     * l'acceptation se prend par mot (Tab, appui), par ligne ou en
     * bloc (menu).
     */
    void demanderFantome() {
        if (!consentementAccorde()) {
            montrerConsentementRefuse();
            return;
        }
        Document document = session.document();
        int curseur = curseur();
        modele.fantome(new SuggestionFantome(
                fragmentSuggere(document, curseur), curseur,
                document.revision()));
        pilote.demander(document, curseur);
        rafraichir();
    }

    /**
     * F-02 — demande une prédiction d'édition : le modèle prédit une
     * opération, l'application reste explicite — la feuille du haut
     * montre la description, l'hôte applique en transaction annulable,
     * le refus est un résultat qui se lit à la barre d'état. Sans
     * consentement, la demande ne part pas.
     */
    void demanderPrediction() {
        if (!consentementAccorde()) {
            montrerConsentementRefuse();
            return;
        }
        Document document = session.document();
        int fin = document.length();
        modele.prediction(new PredictionEdition(
                activite.getString(R.string.prediction_description),
                List.of(new TextEdit(fin, "",
                        "\n// ligne prédite par le modèle local")),
                document.revision()));
        modele.predire(new RequeteModele(document, curseur(), new JetonAnnulation()),
                new jo.celeris.core.ai.ModelePrediction.Recepteur() {
                    @Override
                    public void prediction(PredictionEdition predite) {
                        activite.runOnUiThread(() ->
                                montrerPrediction(predite));
                    }

                    @Override
                    public void aucune() {
                        activite.runOnUiThread(() -> verdict.publier(
                                activite.getString(R.string.prediction_aucune)));
                    }
                });
    }

    /** F-01 — le fantôme visible suit le pilote, ré-ancré. */
    void rafraichir() {
        editeur.setFantome(pilote.suggestionVue());
    }

    /** La frappe suivante refuse le fantôme et annule le vol (F-09). */
    void surEdition() {
        pilote.surEdition();
        rafraichir();
    }

    /**
     * Le rappel de consentement (F-10) en feuille du haut : rien ne
     * part sans consentement, l'action ouvre la feuille des réglages.
     */
    private void montrerConsentementRefuse() {
        FeuilleHaut feuille = feuilleConsentement;
        if (feuille == null) {
            FeuilleHaut neuve = new FeuilleHaut(activite, palettes.get());
            neuve.libelleFermeture(
                    activite.getString(R.string.action_fermer));
            neuve.contenu(activite.getLayoutInflater()
                    .inflate(R.layout.feuille_consentement, null));
            neuve.findViewById(R.id.bouton_consentement_annuler)
                    .setOnClickListener(vue -> neuve.cacher());
            neuve.findViewById(R.id.bouton_consentement_ouvrir)
                    .setOnClickListener(vue -> {
                        neuve.cacher();
                        ouvrirReglages.run();
                    });
            feuilleConsentement = neuve;
            feuille = neuve;
        }
        feuille.titre(activite.getString(R.string.consentement_titre));
        feuille.setPalette(palettes.get());
        feuille.montrer(hote());
    }

    /** La décision de prédiction (F-02) : la feuille du haut porte la
     *  description, l'application reste explicite. */
    private void montrerPrediction(PredictionEdition predite) {
        FeuilleHaut feuille = feuillePrediction;
        if (feuille == null) {
            FeuilleHaut neuve = new FeuilleHaut(activite, palettes.get());
            neuve.libelleFermeture(
                    activite.getString(R.string.action_fermer));
            neuve.contenu(activite.getLayoutInflater()
                    .inflate(R.layout.feuille_prediction, null));
            neuve.findViewById(R.id.bouton_prediction_refuser)
                    .setOnClickListener(vue -> neuve.cacher());
            neuve.findViewById(R.id.bouton_prediction_appliquer)
                    .setOnClickListener(vue -> {
                        neuve.cacher();
                        appliquerPrediction(predite);
                    });
            feuillePrediction = neuve;
            feuille = neuve;
        }
        feuille.titre(activite.getString(R.string.prediction_titre));
        ((android.widget.TextView) feuille.findViewById(
                R.id.description_prediction)).setText(predite.description());
        feuille.setPalette(palettes.get());
        feuille.montrer(hote());
    }

    private void appliquerPrediction(PredictionEdition predite) {
        AppliquerPrediction.Resultat resultat =
                AppliquerPrediction.appliquer(session, predite);
        int message = resultat == AppliquerPrediction.Resultat.APPLIQUEE
                ? R.string.prediction_appliquee
                : R.string.prediction_refusee;
        verdict.publier(activite.getString(message));
        editeur.invalidate();
    }

    /** Le fragment suggéré par le faux modèle, déterministe au contenu. */
    private static String fragmentSuggere(Document document, int curseur) {
        String texte = document.text();
        char avant = curseur > 0 ? texte.charAt(curseur - 1) : ' ';
        if (avant == '.') {
            return "taille()";
        }
        if (avant == '(') {
            return "\")";
        }
        return "suite()";
    }

    /** La touche retour rencontre une feuille IA — ouverte, elle se referme. */
    boolean surRetour() {
        boolean consentement = feuilleConsentement != null
                && feuilleConsentement.surRetour();
        if (consentement) {
            return true;
        }
        FeuilleHaut feuille = feuillePrediction;
        return feuille != null && feuille.surRetour();
    }

    private int curseur() {
        var plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
