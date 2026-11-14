package jo.celeris.android.widgets;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte des hiérarchies de types et d'appels (catégorie C de
 * l'inventaire — les moteurs vivaient dans core-intelligence, la
 * surface dormait) : l'arbre se rend en rangées indentées par
 * PROFONDEUR (une respiration par niveau), les sections annoncent —
 * supertypes, sous-types, appelants, frontière hors projet —, le
 * sommet prenable SAUTE au choix (le Runnable de l'hôte seul décide ce
 * que sauter veut dire : porter le curseur à la déclaration,
 * refermer la carte). PIEGE-27 : jamais de focus — la navigation
 * clavier s'y reçoit DÉLÉGUÉE ({@link PopupClavier} : flèches bornées
 * aux sommets PRENANTS, validation ; la page décline — les rangées
 * vivent toutes à découvert).
 *
 * <p>La frontière du projet se lit muette : le sommet introuvable
 * ({@code null} au saut) ne se prend jamais, ni au doigt ni à la
 * flèche — il s'annonce, il ne se visite pas. Le doute de la
 * hiérarchie d'appels se porte en légende (les sites non résolus se
 * signalent, jamais ne se tranchent).</p>
 */
public final class HierarchiesView extends TextRowsPopup implements PopupClavier {

    /** Un sommet de l'arbre : son libellé, sa profondeur (zéro à la
     *  racine), son saut — null pour l'introuvable (la frontière hors
     *  projet : muet, jamais pris). */
    public record Sommet(String libelle, int profondeur,
            @Nullable Runnable saut) {
    }

    /** Une section de l'arbre : son libellé (petites capitales muettes,
     *  jamais pris) et ses sommets. */
    public record Section(String libelle, List<Sommet> sommets) {

        /** Constructeur canonique : sommets figés. */
        public Section {
            sommets = List.copyOf(sommets);
        }
    }

    /** Les sections courantes de l'arbre — vides avant la première pose. */
    private List<Section> sections = List.of();
    /** L'en-tête qui nomme l'arbre — null : pas d'en-tête. */
    private @Nullable String titre;
    /** La légende du doute — null : pas de doute. */
    private @Nullable String doute;
    /** Le sommet surligné par le clavier délégué — —1 si aucun. */
    private int surligne = -1;

    /**
     * Construit la carte sur la palette du thème courant.
     *
     * @param context le contexte de la couche
     * @param palette la palette courante des popups
     */
    public HierarchiesView(Context context, PopupPalette palette) {
        super(context, palette);
        setOnPick(this::prendre);
    }

    /**
     * Pose l'arbre entier : l'en-tête qui le nomme, ses sections, sa
     * légende de doute éventuelle — la surbrillance s'efface, jamais de
     * sommet retenu d'un arbre d'avant.
     *
     * @param titre    le titre de la carte, null pour aucun
     * @param sections les sections de l'arbre, dans l'ordre du rendu
     * @param doute    la légende des sites non résolus, null pour aucune
     */
    public void setArbre(@Nullable String titre, List<Section> sections,
            @Nullable String doute) {
        this.titre = titre;
        this.sections = List.copyOf(sections);
        this.doute = doute;
        this.surligne = -1;
        rebatirRangees();
    }

    /** Le sommet d'index donné de l'arbre aplati, null s'il n'existe
     *  pas — les bancs y lisent, l'atelier y transpose. */
    public @Nullable Sommet sommetAt(int index) {
        int reste = index;
        for (Section section : sections) {
            if (reste < section.sommets().size()) {
                return section.sommets().get(reste);
            }
            reste -= section.sommets().size();
        }
        return null;
    }

    /** Le sommet surligné par le clavier délégué, —1 si aucun. */
    public int surbrillance() {
        return surligne;
    }

    /** Rebâtit les rangées : l'en-tête, chaque section et son arbre —
     *  la légende du doute clôt. */
    private void rebatirRangees() {
        List<Row> rangees = new ArrayList<>();
        if (titre != null && !titre.isEmpty()) {
            rangees.add(Row.section(titre));
        }
        for (Section section : sections) {
            if (!rangees.isEmpty()) {
                rangees.add(Row.apresFilet(Row.section(section.libelle())));
            } else {
                rangees.add(Row.section(section.libelle()));
            }
            for (Sommet sommet : section.sommets()) {
                rangees.add(Row.arbre(sommet.libelle(),
                        sommet.saut() == null ? palette.muted() : -1,
                        sommet.saut() == null ? Tier.META : Tier.CORPS,
                        sommet.profondeur()));
            }
        }
        if (doute != null && !doute.isEmpty()) {
            rangees.add(Row.of(doute, palette.muted()));
        }
        setRows(rangees);
    }

    /** L'appui d'une rangée : l'indice du sommet y compte l'en-tête
     *  et les sections éventuels. */
    private void prendre(int rang) {
        sauter(indiceDe(rang));
    }

    /** L'indice du sommet à la rangée d'affichage — l'en-tête et les
     *  sections comptent leurs rangées, —1 hors des sommets. */
    private int indiceDe(int rang) {
        int reste = rang - decalage();
        for (Section section : sections) {
            reste--;
            if (reste < 0) {
                return -1;
            }
            if (reste < section.sommets().size()) {
                return reste;
            }
            reste -= section.sommets().size();
        }
        return -1;
    }

    /** Le saut du sommet d'index donné — le Runnable de l'hôte seul
     *  décide (refermer la carte y vit, l'atelier l'exécute). */
    private void sauter(int index) {
        Sommet sommet = sommetAt(index);
        if (sommet != null && sommet.saut() != null) {
            sommet.saut().run();
        }
    }

    @Override
    public boolean surFleche(int pas) {
        int taille = nombreDeSommets();
        if (taille == 0) {
            return false;
        }
        // la flèche part de la tête prenable, saute les introuvables et
        // ne déborde jamais des bornes
        int cible = surligne < 0 ? premierPrenable(0, pas)
                : bornePrenable(surligne, pas);
        if (cible < 0) {
            return false;
        }
        surligne = cible;
        setHighlighted(rangDe(cible));
        return true;
    }

    @Override
    public boolean surPage(int pas) {
        // la carte décline les pages : ses rangées vivent toutes à
        // découvert — la touche retourne à l'éditeur, le document y défile
        return false;
    }

    @Override
    public boolean surValidation() {
        Sommet vise = sommetAt(surligne);
        if (vise == null || vise.saut() == null) {
            return false;
        }
        // la validation clavier prend le MÊME chemin que le doigt : le
        // Runnable du sommet seul décide ce que sauter veut dire
        vise.saut().run();
        performClick();
        return true;
    }

    /** Le nombre de sommets de l'arbre aplati. */
    private int nombreDeSommets() {
        int nombre = 0;
        for (Section section : sections) {
            nombre += section.sommets().size();
        }
        return nombre;
    }

    /** Le premier sommet prenable depuis le départ, dans la direction. */
    private int premierPrenable(int depart, int pas) {
        int taille = nombreDeSommets();
        int cible = pas >= 0 ? depart : taille - 1;
        while (cible >= 0 && cible < taille) {
            if (estPrenable(cible)) {
                return cible;
            }
            cible += pas >= 0 ? 1 : -1;
        }
        return -1;
    }

    /** Le sommet prenable depuis l'index donné, borné aux extrêmes. */
    private int bornePrenable(int courant, int pas) {
        int taille = nombreDeSommets();
        int cible = Math.min(taille - 1, Math.max(0, courant + pas));
        while (cible >= 0 && cible < taille && !estPrenable(cible)) {
            cible += pas >= 0 ? 1 : -1;
        }
        if (cible < 0 || cible >= taille) {
            return courant;
        }
        return cible;
    }

    /** Vrai quand le sommet d'index donné se prend (l'introuvable
     *  muet ne se prend jamais). */
    private boolean estPrenable(int index) {
        Sommet sommet = sommetAt(index);
        return sommet != null && sommet.saut() != null;
    }

    /** Le rang d'affichage du sommet d'index donné — l'en-tête et les
     *  sections éventuels comptent leurs rangées. */
    private int rangDe(int index) {
        int rang = decalage();
        int reste = index;
        for (Section section : sections) {
            rang += 1;
            if (reste < section.sommets().size()) {
                return rang + reste;
            }
            reste -= section.sommets().size();
            rang += section.sommets().size();
        }
        return rang + reste;
    }

    /** Le décalage des rangées de sommets — l'en-tête éventuel compte
     *  une rangée. */
    private int decalage() {
        return titre != null && !titre.isEmpty() ? 1 : 0;
    }
}
