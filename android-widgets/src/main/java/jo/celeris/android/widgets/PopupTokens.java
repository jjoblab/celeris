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
    public static final float RAYON_CARTE_DP = 12f;

    /** Rayon d'un fond de rangée sélectionnée / d'une zone pressée. */
    public static final float RAYON_RANGEE_DP = 8f;

    /** Rayon d'un petit chip (badge de type, puce compacte). */
    public static final float RAYON_PUCE_DP = 6f;

    // ── Respirations (dp) ──────────────────────────────────────────

    /** La marge horizontale intérieure d'une carte — resserrée (l'audit
     *  de compacité du propriétaire du 2026-11-14 : les cartes trop
     *  respirées mangent la scène). */
    public static final float RESPIRATION_X_DP = 10f;

    /** La marge verticale intérieure d'une section. */
    public static final float RESPIRATION_Y_DP = 6f;

    /** L'écart visuel entre deux blocs voisins (filet compris). */
    public static final float ECART_DP = 5f;

    // ── Échelle typographique (× taille de texte de base) ─────────

    /** Titre de carte / libellé de rangée principal. */
    public static final float TEXTE_TITRE = 0.85f;

    /** Corps de texte (message, libellé de section). */
    public static final float TEXTE_CORPS = 0.80f;

    /** Méta (le détail d'un candidat, un conteneur). */
    public static final float TEXTE_META = 0.72f;

    /** Micro-typographie (compteur, badge, légende de filet). */
    public static final float TEXTE_MICRO = 0.68f;

    /** Le demi-rayon d'un pictogramme, en part de la taille de texte
     *  de sa rangée (l'audit du propriétaire du 2026-11-14 : « les
     *  icônes sont un peu trop grosses » — le glyphe suit le texte,
     *  il ne le domine plus). */
    public static final float FACTEUR_PICTO = 0.76f;

    /** La hauteur au plus d'un menu d'actions (étages compris) — la
     *  carte ne mange jamais la couche, l'ancre borne sa pose. */
    public static final float HAUTEUR_MENU_MAX_DP = 280f;

    /** La hauteur au plus d'une liste de complétion quand la place
     *  manque sous le caret — la liste se ramasse au lieu de marcher
     *  sur le texte (l'audit : les popups ne cachent pas la frappe). */
    public static final float HAUTEUR_COMPLETION_MIN_DP = 72f;

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

    // ── Feuilles de l'ADR-018 (maquettes du 2026-11-10) ─────────────

    /** Le rayon des coins OUVERTS d'une feuille (bas : coins hauts,
     *  haut : coins bas) — 18 px à la maquette de référence. */
    public static final float RAYON_FEUILLE_DP = 18f;

    /** La largeur au plus d'une feuille du bas (maquette : 660 px). */
    public static final float LARGEUR_MAX_FEUILLE_BAS_DP = 660f;

    /** La largeur au plus d'une feuille du haut (maquette : 760 px). */
    public static final float LARGEUR_MAX_FEUILLE_HAUT_DP = 760f;

    /** La poignée de glissement d'une feuille : sa largeur (dp). */
    public static final float POIGNEE_LARGEUR_DP = 44f;

    /** La poignée de glissement d'une feuille : son épaisseur (dp). */
    public static final float POIGNEE_EPAISSEUR_DP = 5f;

    /** L'aire tactile de la poignée — la cible minimale du kit (dp). */
    public static final float POIGNEE_AIRE_DP = 48f;

    /** La cible tactile minimale d'une commande de feuille, fermeture
     *  comprise (dp) — la maquette exige 44 px, le kit pose 48. */
    public static final float CIBLE_MIN_FEUILLE_DP = 48f;

    /** La part de la hauteur de l'hôte qu'une feuille occupe au plus
     *  (maquette : 76 %). */
    public static final float PART_HAUTEUR_FEUILLE = 0.76f;

    /** La durée du glissement d'une feuille, en millisecondes
     *  (maquette : 0,18 s). */
    public static final long DUREE_FEUILLE_MS = 180L;

    /** Le seuil de glissement qui emporte la feuille — au-delà, la
     *  fermeture suit le geste (dp). */
    public static final float SEUIL_GLISS_FEUILLE_DP = 120f;

    private PopupTokens() {
    }
}
