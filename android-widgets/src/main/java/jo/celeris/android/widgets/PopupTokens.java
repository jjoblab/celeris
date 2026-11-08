package jo.celeris.android.widgets;

/**
 * Les jetons de design des popups (portage du kit du dépôt de départ)
 * : rayons, respirations, échelle typographique — un seul endroit où
 * changer l'apparence d'une carte flottante. Avant le kit, huit rayons
 * coexistaient (6, 8, 12, 14, 16, 18 dp) et chaque popup portait ses
 * propres respirations et facteurs de taille de texte ; les voici
 * unifiés, aux valeurs du dépôt de départ.
 *
 * <p>Toutes les valeurs sont en <b>dp</b> sauf mention contraire — les
 * consommateurs multiplient par la densité. L'échelle typographique est
 * exprimée en <b>facteur</b> de la taille de texte de base des popups
 * (la taille de texte de l'éditeur, portée par
 * {@link PopupPalette#textBaseSp()}).</p>
 */
public final class PopupTokens {

    // ── Rayons (dp) ────────────────────────────────────────────────

    /** Rayon d'une carte de popup flottante (complétion, signature…). */
    public static final float RAYON_CARTE_DP = 14f;

    /** Rayon d'un fond de rangée sélectionnée / d'une zone pressée. */
    public static final float RAYON_RANGEE_DP = 8f;

    /** Rayon d'un petit chip (badge de type, puce compacte). */
    public static final float RAYON_PUCE_DP = 6f;

    // ── Respirations (dp) ──────────────────────────────────────────

    /** La marge horizontale intérieure d'une carte. */
    public static final float RESPIRATION_X_DP = 12f;

    /** La marge verticale intérieure d'une section. */
    public static final float RESPIRATION_Y_DP = 8f;

    /** L'écart visuel entre deux blocs voisins (filet compris). */
    public static final float ECART_DP = 6f;

    // ── Échelle typographique (× taille de texte de base) ──────────

    /** Titre de carte / libellé de rangée principal. */
    public static final float TEXTE_TITRE = 0.90f;

    /** Corps de texte (message, libellé de section). */
    public static final float TEXTE_CORPS = 0.85f;

    /** Méta (le détail d'un candidat, un conteneur). */
    public static final float TEXTE_META = 0.75f;

    /** Micro-typographie (compteur, badge, légende de filet). */
    public static final float TEXTE_MICRO = 0.70f;

    /** Le plafond de largeur d'une carte de complétion (le dépôt de
     *  départ fixait la largeur de la liste à 280 dp). */
    public static final float LARGEUR_MAX_LISTE_DP = 280f;

    /** Le PLANCHER de largeur d'une carte de complétion : la carte
     *  garde une taille raisonnable même à libellés courts — le
     *  redressement du propriétaire (2026-11-08 : « la largeur du
     *  popup de complétion trop petite, il n'a pas une taille
     *  raisonnable »). La rangée deux lignes — badge, libellé,
     *  identité qualifiée — y respire. */
    public static final float LARGEUR_MIN_LISTE_DP = 240f;

    /** La hauteur d'une rangée de complétion, deux lignes comprises. */
    public static final float HAUTEUR_RANGEE_LISTE_DP = 40f;

    /** Le badge de genre d'un candidat, au plus (dp). */
    public static final float BADGE_MAX_DP = 20f;

    private PopupTokens() {
    }
}
