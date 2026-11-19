package jo.celeris.android.view;

import java.util.EnumSet;
import java.util.Set;


/**
 * Les réglages de l'éditeur (l'audit du propriétaire du 2026-11-14 :
 * « ajoute un système central editor settings pour pouvoir activer
 * désactiver, etc. certaines fonctionnalités ») : UNE table de
 * commutateurs, UN point de vérité — la vue, l'orchestrateur des
 * popups et l'hôte y lisent les mêmes portes. Pur, sans appareil :
 * chaque bascule s'y pose, s'y lit et s'y écoute ; l'application des
 * portes appartient aux porteurs (la vue retire ses indices en
 * ligne, l'orchestrateur retient sa barre de sélection, etc.).
 *
 * <p>Les portes par défaut suivent l'IDE de référence : tout ce qui
 * aide la lecture vit, tout ce qui distrait attend son tour. Les
 * clés restent STABLES — les préférences persistées de l'hôte s'y
 * branchent sans renégocier le monde.</p>
 */
public final class EditorSettingsPanel {

    /** Une porte de l'éditeur — sa clé STABLE (les préférences de
     *  l'hôte s'y branchent) et son état par défaut. */
    public enum Porte {

        /** Les indices en ligne (inlay hints) : le type des « var »,
         *  les noms de paramètres — la lecture assistée du code. */
        INDICES_EN_LIGNE("editeur.inlays", true),

        /** La barre de sélection : la pilule des actions au-dessus de
         *  la sélection. */
        BARRE_SELECTION("editeur.barreSelection", true),

        /** L'ampoule d'intentions de la gouttière : le petit icône
         *  jaune des lignes à diagnostic. */
        AMPOULE_INTENTIONS("editeur.ampoule", true),

        /** La complétion automatique à la frappe — la fermer rend la
         *  saisie silencieuse (la complétion manuelle reste). */
        COMPLETION_AUTO("editeur.completionAuto", true),

        /** Les valeurs de pause du débogage — la valeur des variables
         *  en fin de ligne, à l'arrêt du débogueur. */
        VALEURS_PAUSE("editeur.valeursPause", true),

        /** Le texte fantôme de l'IA — la suggestion fantôme en fin de
         *  frappe (F-01). */
        FANTOME_IA("editeur.fantomeIa", true),

        /** La minimap du bord droit (C9). */
        MINIMAP("editeur.minimap", true),

        /** Le défilement rapide du bord droit (C9). */
        DEFILEMENT_RAPIDE("editeur.defilementRapide", true),

        /** Les numéros de lignes de la gouttière (D-01). */
        NUMEROS_LIGNES("editeur.numeros", true),

        /** Le retour à la ligne souple (A-18). */
        RETOUR_LIGNE("editeur.retourLigne", false);

        /** La clé STABLE de la porte — les préférences persistées de
         *  l'hôte s'y branchent. */
        private final String key;

        /** L'état par défaut de la porte. */
        private final boolean defaut;

        Porte(String key, boolean defaut) {
            this.key = key;
            this.defaut = defaut;
        }

        /** La clé stable de la porte. */
        public String key() {
            return key;
        }

        /** L'état par défaut de la porte. */
        public boolean parDefaut() {
            return defaut;
        }
    }

    /** Les états courants des portes. */
    private final Set<Porte> ouvertes = EnumSet.noneOf(Porte.class);

    /** L'écouteur des bascules — l'hôte y applique ses portes. */

    /** Construit la table aux états PAR DÉFAUT — l'hôte y pose ensuite
     *  ses préférences persistées. */
    public EditorSettingsPanel() {
        for (Porte porte : Porte.values()) {
            if (porte.parDefaut()) {
                ouvertes.add(porte);
            }
        }
    }

    /**
     * Pose l'état d'une porte — silencieux : la bascule n'avertit pas,
     * la pose en masse des préférences de l'hôte n'égrène pas ses
     * écouteurs.
     *
     * @param porte  la porte à poser
     * @param ouverte vrai pour ouvrir (activer), faux pour fermer
     */
    public void place(Porte porte, boolean ouverte) {
        if (ouverte) {
            ouvertes.add(porte);
        } else {
            ouvertes.remove(porte);
        }
    }

    /**
     * Bascule une porte — le geste vif de la carte de réglages. Les
     * abonnés de la source unique (EditorSettings, T2) y apprennent
     * la bascule : plus d'écouteur dédié depuis la 2.0 (ADR-0024).
     *
     * @param porte la porte à basculer
     * @return le nouvel état de la porte
     */
    public boolean toggle(Porte porte) {
        boolean ouverte = !ouvertes.contains(porte);
        place(porte, ouverte);
        return ouverte;
    }

    /**
     * Vrai quand la porte est ouverte — le point de lecture UNIQUE :
     * la vue, l'orchestrateur et l'hôte y voient le même monde.
     *
     * @param porte la porte lue
     * @return vrai quand la fonctionnalité vit
     */
    public boolean ouverte(Porte porte) {
        return ouvertes.contains(porte);
    }

}
