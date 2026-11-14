package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jo.celeris.android.host.R;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.HierarchiesView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

import jo.celeris.core.intelligence.Hierarchies;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Les hiérarchies de types et d'appels (la quatrième surface portée de
 * la démonstration à l'orchestrateur, 2026-11-14) : la hiérarchie de
 * TYPES du symbole au curseur quand il nomme un type, la hiérarchie
 * d'APPELS quand il nomme une méthode — le plan local décide la forme,
 * le moteur du cœur sert l'arbre sur le projet embarqué, le satellite
 * transpose. L'audit Android Studio du 2026-11-14 y aligne l'arbre sur
 * l'IDE de référence : la racine y vit UNE fois — l'EN-TÊTE la nomme
 * (l'arbre de l'IDE porte le type interrogé à sa racine, jamais il ne
 * se répète dans les sections), l'introuvable vit à la FRONTIÈRE
 * seule (jamais deux fois muet). Le sommet déclaré se prend et SAUTE
 * à sa déclaration, l'introuvable se lit muet, le doute se porte en
 * légende.
 *
 * <p>La frontière tient : la carte ne connaît NI le moteur, NI
 * l'éditeur — le satellite transpose via la classe pure
 * {@link Transpose}, prouvée au banc.</p>
 */
final class CarteHierarchies {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final HierarchiesView carte;
    /** Le plan courant du fichier — la forme de l'arbre s'y décide. */
    private final Supplier<List<OutlineEntry>> plan;
    /** Le nom du fichier ouvert — la clé du projet embarqué. */
    private final Supplier<String> nomFichier;
    /** Le langage du fichier ouvert — la syntaxe s'y lit. */
    private final Supplier<@Nullable LanguageSupport> langue;

    /**
     * Construit les hiérarchies sur le coordinateur des popups : la
     * carte naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur    la vue d'éditeur ouverte
     * @param session    la session d'édition courante
     * @param popups     le coordinateur des popups de la scène
     * @param margePx    la respiration des ancres aux bords, en pixels
     * @param palette    la palette courante des popups
     * @param plan       le plan courant du fichier ouvert
     * @param nomFichier le nom du fichier ouvert, lu au déclenchement
     * @param langue     le langage du fichier ouvert, null sans syntaxe
     */
    CarteHierarchies(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette,
            Supplier<List<OutlineEntry>> plan,
            Supplier<String> nomFichier,
            Supplier<@Nullable LanguageSupport> langue) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.plan = plan;
        this.nomFichier = nomFichier;
        this.langue = langue;
        this.carte = new HierarchiesView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.HIERARCHY, carte);
    }

    /** La palette tourne : la carte des hiérarchies la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la hiérarchie du symbole au curseur : le plan décide la
     * forme — TYPE rend la hiérarchie de types, FONCTION la
     * hiérarchie d'appels, rien d'autre ne se sert (le moteur sert
     * les types et les méthodes). Sans symbole au curseur, sans
     * syntaxe du fichier, la carte ne se pose pas.
     */
    void ouvrir() {
        Document document = session.document();
        String symbole = ContenuPopups.symboleA(document, caret());
        if (symbole == null) {
            return;
        }
        LanguageSupport support = langue.get();
        if (support == null || support.syntax() == null) {
            return;
        }
        SyntaxService syntaxe = SyntaxService.of(support.syntax());
        List<OutlineEntry> entrees = plan.get();
        if (estMethode(entrees, symbole)) {
            poserAppels(symbole, document, syntaxe, entrees);
        } else if (estType(entrees, symbole)) {
            poserTypes(symbole, document, syntaxe, entrees);
        }
    }

    /** Pose la hiérarchie de types du nom — les sections suivent (la
     *  racine vit à l'en-tête, l'introuvable à la frontière), la
     *  carte se montre sous le curseur. */
    private void poserTypes(String symbole, Document document,
            SyntaxService syntaxe, List<OutlineEntry> entrees) {
        Hierarchies.HierarchieTypes arbre = Hierarchies.types(symbole,
                Map.of(nomFichier.get(), document), syntaxe);
        if (arbre.introuvables().isEmpty() && arbre.ancetres().size() == 1
                && arbre.descendants().size() == 1) {
            // la racine seule, sans parent ni enfant : rien à montrer
            return;
        }
        carte.setArbre(popups.couche().getContext().getString(
                        R.string.titre_hierarchie_types, symbole),
                Transpose.deTypes(arbre, connusDe(entrees), this::sauterVers,
                        libelle(R.string.section_supertypes),
                        libelle(R.string.section_soustypes),
                        libelle(R.string.section_hors_projet)),
                null);
        montrer();
    }

    /** Pose la hiérarchie d'appels du nom — les appelants suivent (la
     *  racine vit à l'en-tête), le doute se porte en légende, la
     *  carte se montre sous le curseur. */
    private void poserAppels(String symbole, Document document,
            SyntaxService syntaxe, List<OutlineEntry> entrees) {
        Hierarchies.HierarchieAppels arbre = Hierarchies.appelants(symbole,
                Map.of(nomFichier.get(), document), syntaxe);
        if (arbre.appelants().size() == 1 && !arbre.doute()) {
            // la racine seule, sans appelant ni doute : rien à montrer
            return;
        }
        carte.setArbre(popups.couche().getContext().getString(
                        R.string.titre_hierarchie_appels, symbole),
                Transpose.dAppels(arbre, connusDe(entrees), this::sauterVers,
                        libelle(R.string.section_appelants)),
                arbre.doute() ? popups.couche().getContext().getString(
                        R.string.legende_doute_hierarchie) : null);
        montrer();
    }

    /** Montre la carte mesurée sous le curseur, bornée. */
    private void montrer() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom, taille(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.HIERARCHY,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.HIERARCHY);
    }

    /** Le saut d'un sommet : la carte se referme, le curseur porte à
     *  la DÉCLARATION du nom — le plan seul la connaît. */
    private void sauterVers(String nom) {
        popups.fermer(PopupController.PopupKind.HIERARCHY);
        for (OutlineEntry entree : plan.get()) {
            if (entree.name().equals(nom)) {
                NavigationCommands.placeCaret(editeur, entree.startOffset());
                return;
            }
        }
    }

    /** Les noms déclarés du plan — les sommets prenants s'y lisent. */
    private static Set<String> connusDe(List<OutlineEntry> entrees) {
        Set<String> connus = new HashSet<>();
        for (OutlineEntry entree : entrees) {
            connus.add(entree.name());
        }
        return connus;
    }

    /** Vrai quand le plan déclare le nom en TYPE. */
    private static boolean estType(List<OutlineEntry> entrees, String nom) {
        return estCategorie(entrees, nom, NodeCategory.TYPE);
    }

    /** Vrai quand le plan déclare le nom en FONCTION. */
    private static boolean estMethode(List<OutlineEntry> entrees, String nom) {
        return estCategorie(entrees, nom, NodeCategory.FUNCTION);
    }

    /** Vrai quand le plan déclare le nom à la catégorie donnée. */
    private static boolean estCategorie(List<OutlineEntry> entrees,
            String nom, NodeCategory categorie) {
        for (OutlineEntry entree : entrees) {
            if (entree.name().equals(nom)
                    && entree.category() == categorie) {
                return true;
            }
        }
        return false;
    }

    /** Le libellé localisé de la ressource — la carte reste pure. */
    private String libelle(int ressource) {
        return popups.couche().getContext().getString(ressource);
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }

    /**
     * La transpose du moteur à la carte — pure et prouvée au banc
     * (l'audit Android Studio du 2026-11-14 y aligne l'arbre sur
     * l'IDE de référence) : la RACINE vit UNE fois — l'en-tête de la
     * carte la nomme (l'arbre de l'IDE porte le type interrogé à sa
     * racine, jamais il ne se répète dans les sections) ; le sommet
     * CONNU se prend (le sauteur y court), l'INTROUVABLE vit muet à
     * la frontière SEULE (jamais deux fois muet).
     */
    static final class Transpose {

        private Transpose() {
        }

        /**
         * Les sections de la hiérarchie de types : les SUPERTYPES par
         * générations, les SOUS-TYPES en profondeur, la FRONTIÈRE hors
         * projet en introuvables muets — la racine et les introuvables
         * des sections en ligne n'y vivent pas.
         *
         * @param arbre        la hiérarchie servie par le moteur
         * @param connus       les noms déclarés du plan — seuls ils se prennent
         * @param sauteur      le saut du sommet choisi (le satellite l'exécute)
         * @param libelleSuper le libellé de section des supertypes
         * @param libelleSous  le libellé de section des sous-types
         * @param libelleHors  le libellé de section de la frontière
         * @return les sections, dans l'ordre du rendu
         */
        static List<HierarchiesView.Section> deTypes(
                Hierarchies.HierarchieTypes arbre, Set<String> connus,
                Consumer<String> sauteur, String libelleSuper,
                String libelleSous, String libelleHors) {
            List<HierarchiesView.Section> sections = new ArrayList<>();
            List<HierarchiesView.Sommet> supertypes = sommetsDe(
                    arbre.ancetres(), arbre.introuvables(), connus, sauteur);
            if (!supertypes.isEmpty()) {
                sections.add(new HierarchiesView.Section(libelleSuper,
                        supertypes));
            }
            List<HierarchiesView.Sommet> soustypes = sommetsDe(
                    arbre.descendants(), List.of(), connus, sauteur);
            if (!soustypes.isEmpty()) {
                sections.add(new HierarchiesView.Section(libelleSous,
                        soustypes));
            }
            if (!arbre.introuvables().isEmpty()) {
                List<HierarchiesView.Sommet> frontiere = new ArrayList<>();
                for (String introuvable : arbre.introuvables()) {
                    frontiere.add(new HierarchiesView.Sommet(
                            introuvable, 0, null));
                }
                sections.add(new HierarchiesView.Section(libelleHors,
                        frontiere));
            }
            return sections;
        }

        /**
         * Les sections de la hiérarchie d'appels : les APPELANTS en
         * arbre, la racine vit à l'en-tête — chaque sommet porte sa
         * profondeur.
         *
         * @param arbre            la hiérarchie servie par le moteur
         * @param connus           les noms déclarés du plan — seuls ils se prennent
         * @param sauteur          le saut du sommet choisi (le satellite l'exécute)
         * @param libelleAppelants le libellé de section des appelants
         * @return la section unique des appelants
         */
        static List<HierarchiesView.Section> dAppels(
                Hierarchies.HierarchieAppels arbre, Set<String> connus,
                Consumer<String> sauteur, String libelleAppelants) {
            return List.of(new HierarchiesView.Section(libelleAppelants,
                    sommetsDe(arbre.appelants(), List.of(),
                            connus, sauteur)));
        }

        /** Les sommets de l'arbre — le connu se prend (le sauteur y
         *  court) ; la RACINE (profondeur zéro) vit à l'en-tête, le
         *  HORS PROJET vit muet à la frontière : ni l'un ni l'autre
         *  ne se répète en ligne. */
        private static List<HierarchiesView.Sommet> sommetsDe(
                List<Hierarchies.Sommet> arbre, List<String> introuvables,
                Set<String> connus, Consumer<String> sauteur) {
            List<HierarchiesView.Sommet> sommets = new ArrayList<>();
            for (Hierarchies.Sommet sommet : arbre) {
                if (sommet.profondeur() == 0
                        || introuvables.contains(sommet.nom())) {
                    continue;
                }
                sommets.add(new HierarchiesView.Sommet(sommet.nom(),
                        sommet.profondeur(), connus.contains(sommet.nom())
                                ? () -> sauteur.accept(sommet.nom()) : null));
            }
            return sommets;
        }
    }
}
