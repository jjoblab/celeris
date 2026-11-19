package jo.celeris.sample;

import android.app.Activity;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.core.text.TextSession;
import jo.celeris.sample.lsp.MiniJavaServeur;
import jo.celeris.sample.lsp.SessionLspAdaptee;
import jo.celeris.lsp.protocole.ServeurEmbarque;


/**
 * La poseuse de session LSP de l'exemple : monte le mini serveur
 * Java EN PROCESSUS (vrai protocole sur tubes à mémoire), l'initialise,
 * le branche aux popups de l'éditeur — puis le synchronise à chaque
 * frappe. La pose entière vit sur le fil démon : le fil de
 * l'interface ne l'attend jamais, l'éditeur reste fluide pendant que
 * la poignée de main se joue.
 *
 * <p>Le branchement ne sert QUE la pose TOUJOURS courante (le retour
 * terrain du 2026-11-19 : « quand j'appuie sur l'onglet inactif ça
 * bug » — deux poses rapides se coursaient sur le champ statique, un
 * serveur fuyait, l'autre se branchait sur le document d'un AUTRE
 * onglet) : la session texte est capturée À LA POSE, le branchement
 * se vérifie contre elle sur le fil de l'interface — un changement
 * d'onglet pendant la poignée de main annule proprement la pose
 * dépassée.</p>
 */
public final class SessionLspPoseuse {

    private SessionLspPoseuse() {
    }

    /** La session courante — une seule vit à la fois. */
    private static SessionLspAdaptee courante;
    /** Le numéro de pose courant : une pose dépassée ne se branche
     *  pas (le changement d'onglet pendant la poignée de main). */
    private static int numeroPose;

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
        final int maPose = ++numeroPose;
        final TextSession textePose = editeur.session();
        if (textePose == null) {
            return;
        }
        Thread poseur = new Thread(() -> {
            try {
                monterEtBrancher(activite, editeur, nomFichier, maPose,
                        textePose, branche);
            } catch (Exception echec) {
                // L'échec se DIT (l'ancien catch vide tuait le
                // serveur sans trace — le journal restait muet sur un
                // montage LSP cassé) : le message part au fil de
                // l'interface, l'éditeur reste éditable sans serveur.
                activite.runOnUiThread(() -> {
                    if (maPose == numeroPose && branche instanceof Journal) {
                        ((Journal) branche).echec(echec.getMessage());
                    }
                });
            }
        }, "celeris-poseuse-lsp");
        poseur.setDaemon(true);
        poseur.start();
    }

    /** Monte le mini serveur et le branche — sur le fil du poseur,
     *  le branchement lui se joue au fil de l'interface. Une pose
     *  plus récente a dépassé celle-ci : le fil de l'interface a
     *  changé d'onglet pendant la poignée de main, celle-ci s'annule
     *  sans rien monter ; le branchement ne sert que la pose TOUJOURS
     *  courante ET la session TOUJOURS affichée : le serveur du
     *  fichier A ne se branche jamais sur le document du fichier B
     *  (le retour terrain du 2026-11-19 : le diagnostic arrivait sur
     *  le mauvais document, l'éditeur « buggait »).
     */
    private static void monterEtBrancher(Activity activite,
            CelerisEditor editeur, String nomFichier, int maPose,
            TextSession textePose, Runnable branche) throws Exception {
        if (maPose != numeroPose) {
            return;
        }
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
            if (maPose != numeroPose || editeur.popups() == null
                    || editeur.session() != textePose) {
                return;
            }
            editeur.popups().brancherServeur(session,
                    "file:///" + nomFichier, editeur.language());
            branche.run();
        });
    }

    /** Le geste de pose qui SAIT journaliser son échec : l'exemple y
     *  branche le journal, la pose y dit pourquoi elle a échoué. */
    public interface Journal extends Runnable {

        /** L'échec de la pose — la cause, telle quelle. */
        void echec(String cause);
    }
}
