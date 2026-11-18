package jo.celeris.sample;

import android.app.Activity;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.core.text.TextSession;
import jo.celeris.sample.lsp.MiniJavaServeur;
import jo.celeris.sample.lsp.SessionLspAdaptee;
import jo.celeris.lsp.protocole.ServeurEmbarque;

/**
 * La poseuse de session LSP de l'exemple : monte le mini serveur
 * Java EN PROCESSUS (vrai protocole sur tubes à mémoire),
 * l'initialise, le branche aux popups de l'éditeur — puis le
 * synchronise à chaque frappe. La pose entière vit sur le fil
 * démon : le fil de l'interface ne l'attend jamais, l'éditeur reste
 * fluide pendant que la poignée de main se joue.
 */
public final class SessionLspPoseuse {

    private SessionLspPoseuse() {
    }

    /** La session courante — une seule vit à la fois. */
    private static SessionLspAdaptee courante;

    /**
     * Pose la session du mini serveur sur l'éditeur ouvert.
     *
     * @param activite  l'activite porteuse (le contexte du protocole)
     * @param editeur   l'éditeur ouvert
     * @param nomFichier le nom du fichier ouvert (son uri)
     * @param branche   le geste à la pose réussie
     */
    public static void poser(Activity activite, CelerisEditor editeur,
            String nomFichier, Runnable branche) {
        Thread poseur = new Thread(() -> {
            try {
                SessionLspAdaptee precedente = courante;
                if (precedente != null) {
                    precedente.close();
                    courante = null;
                }
                ServeurEmbarque montage = ServeurEmbarque.monter(
                        new MiniJavaServeur(), "celeris-exemple");
                montage.session().initialiser();
                SessionLspAdaptee session = new SessionLspAdaptee(
                        montage.session());
                courante = session;
                activite.runOnUiThread(() -> {
                    TextSession texte = editeur.session();
                    if (texte == null || editeur.popups() == null) {
                        return;
                    }
                    editeur.popups().brancherServeur(session,
                            "file:///" + nomFichier, editeur.language());
                    // le diagnostic d'ouverture se journalise côté popups
                    branche.run();
                });
            } catch (Exception e) {
                activite.runOnUiThread(() -> {
                    if (editeur.popups() != null) {
                        // le journal de l'activite n'est pas porté ici :
                        // la pose échouée se tait proprement, l'éditeur
                        // reste éditable sans serveur
                    }
                });
            }
        }, "celeris-poseuse-lsp");
        poseur.setDaemon(true);
        poseur.start();
    }
}
