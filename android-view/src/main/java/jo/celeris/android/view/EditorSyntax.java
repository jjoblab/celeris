package jo.celeris.android.view;

import jo.celeris.android.view.internal.model.LanguageStructure;
import jo.celeris.android.view.internal.EnginePark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.EnginePathFactoryLoader;
import jo.celeris.core.syntax.AnalysisEngine;
import jo.celeris.core.syntax.SyntaxEngine;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.structure.TreeDiagnostics;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Pont session ↔ vue pour la syntaxe (M4) : le coordinateur d'analyse
 * suit la session sur la voie de transaction unique (PIEGE-07), la
 * source de rangées se pare des plages de jetons (B-01), les régions
 * pliables et les lignes en erreur nourrissent la coulisse de la
 * gouttière (A-15, D-02, D-07) et le pli manuel survit à l'éditon
 * voisine (A-15).
 *
 * <p>Pur et testable sans SDK : la vue le possède, l'auditeur de plis
 * et le coordinateur s'y attachent et s'y détachent — la fermeture
 * détache, la dernière analyse reste consultable. L'éditon remappe les
 * plis sur l'instantané d'avant, comme le coordinateur pour ses sites.</p>
 *
 * <p>Trois voies d'analyse : la source native du niveau 2 (ADR-020 —
 * le document ENTIER, borné) ; sans exécuteur, le coordinateur vit sur
 * le fil d'écriture ; avec exécuteur injecté (ADR-007), le moteur hors
 * fil publie par lots — la fenêtre visible d'abord (G-01) — et la vue
 * n'habille JAMAIS une ligne avec les jetons d'une autre révision sans
 * preuve d'identité (PIEGE-20) : les lignes épargnées se remappent,
 * les touchées passent à l'encre de base le temps du vol. Le rappel
 * {@code setSurAnalysePrete} part à chaque publication, sur le fil de
 * travail — l'hôte y invalide.</p>
 *
 * <p>Le plan consommateur (B-16, tranche 8) : les entrées du plan
 * sortent du MÊME arbre que la coloration — le consommateur déclaré
 * par {@code addPlanListener} reçoit les entrées de l'analyse habillée à
 * chaque changement de révision, jamais celles d'une autre révision.
 * Le rattachement d'une session et le retrait du langage poussent un
 * plan vide : le consommateur n'affiche jamais les entrées d'un
 * document parti. Depuis M7.6 (H-08), le plan vient des règles du
 * paquet quand il en porte et les plis suivent l'indentation déclarée
 * par la configuration : un langage sans analyseur dédié se replie et
 * se structure pareil.</p>
 */
public final class EditorSyntax {

    /** Taille maximale de la chaîne d'éditions en attente : au-delà, la chaîne se déclare rompue. */
    private static final int CHAINE_MAX = 256;

    /** Une édition en attente de publication : sa révision, son site en
     *  lignes, ses coordonnées PUBLIÉES — décalées dès l'enregistrement
     *  pour que le rattrapage les compose sans deviner — et son début
     *  RELATIF à la ligne publiée touchée, la matière de la chirurgie
     *  (PIEGE-26). */
    private record EditionEnAttente(int revision, EditSite site,
            int publieStart, int publieRemoved, int publieInserted,
            int lignePubliee, int debutDansLigne, boolean structurelle) {

        TokenCatchUp.PendingEdit versPendingEdit() {
            return new TokenCatchUp.PendingEdit(debutDansLigne, publieRemoved,
                    publieInserted, lignePubliee, structurelle);
        }
    }

    private final FoldTracker foldTracking;
    private final FoldModel folds;
    private final List<FoldSpan> regionsPliables = new ArrayList<>();
    private final EditListener foldListener = this::surEdition;
    private final @Nullable Executor executor;
    private final List<EditionEnAttente> editionsEnAttente = new ArrayList<>();
    /** La table des guides verticaux structurels (H-09, A-17 relue),
     *  remappée à l'édition, redérivée à la publication — VIDE sans
     *  analyse à jour. Visible du paquet : la couche des guides y lit
     *  la table courante, collée à la révision (PIEGE-20). */
    GuidesStructurels guides = GuidesStructurels.EMPTY;

    private @Nullable TextSession session;
    private @Nullable LanguageSupport language;
    private @Nullable LanguageSyntax syntaxeDuLangage;
    private @Nullable SyntaxCoordinator coordinateur;
    private @Nullable AnalysisEngine engine;
    private @Nullable SyntaxEngine nativePathEngine;
    /** Le parc des moteurs détachés, un par session ouverte (le
     *  retour terrain du 2026-11-19 : la tokenisation appartient AU
     *  DOCUMENT — le moteur de la session quittée s'y parque, la
     *  session revenue le retrouve avec sa publication). */
    private final EnginePark park = new EnginePark();

    /** L'auditeur de publication du moteur — UN seul, posé et retiré
     *  proprement : jamais dupliqué au retour du parc. */
    private final java.util.function.Consumer<jo.celeris.core.syntax.Analysis> auditeurPublication =
            analyse -> notifyAnalysisReady();
    private @Nullable Runnable surAnalysePrete;

    /** Les consommateurs 2.0 du plan — multiples (ADR-0022, phase
     *  2, T3) : le kit et l'hôte reçoivent le même arbre. */
    private final java.util.List<Consumer<List<OutlineEntry>>> consommateursPlan =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private int windowFirst;
    private int windowLast = Integer.MAX_VALUE / 2;
    private @Nullable Document documentSuivi;
    private long revisionAnalysee = -1L;
    private GutterMarks marks = GutterMarks.EMPTY;

    /** Les diagnostics courants, plages et sévérité (D-05, D-06) —
     *  la couche de squiggles y lit, la gouttière garde ses lignes. */
    private java.util.List<jo.celeris.core.syntax.SyntaxIssue> issues =
            java.util.List.of();

    /**
     * La mémoire des plis PAR SESSION (D-07, l'analyse croisée) :
     * l'archive et la restauration vivent dans le porteur, à clés
     * faibles — une session morte n'y retient plus rien.
     */
    private final FoldsBySession foldMemory;
    private boolean chaineRompue;

    /**
     * Construit le pont synchrone sur le modèle de plis de la projection
     * — l'analyse se paie à la première demande, sur le fil d'appel.
     *
     * @param folds le modèle de plis, lu en direct par la projection et
     *             par les marques
     */
    public EditorSyntax(FoldModel folds) {
        this(folds, null);
    }

    /**
     * Construit le pont sur le modèle de plis, avec exécuteur injecté :
     * l'analyse vit hors fil d'appel (ADR-007), la fenêtre visible se
     * paie d'abord (G-01).
     *
     * @param folds      le modèle de plis, lu en direct par la projection
     *                  et par les marques
     * @param executor l'exécuteur du travail d'analyse, null pour la
     *                  voie synchrone
     */
    public EditorSyntax(FoldModel folds, @Nullable Executor executor) {
        this.folds = folds;
        this.foldTracking = new FoldTracker(folds);
        this.foldMemory = new FoldsBySession(folds, foldTracking);
        this.executor = executor;
    }

    /** Attache la session suivie : les plis de la précédente
     *  s'oublient — un pli appartient à son document. La langue
     *  RESTE celle posée (la voie historique : la vue rattache la
     *  session seule, l'hôte pose la langue ensuite). */
    public void attach(TextSession nouvelleSession) {
        attach(nouvelleSession, language);
    }

    /**
     * Attache la session ET sa langue d'un seul geste (le retour
     * terrain du 2026-11-19 : « la coloration se refait encore et
     * encore à chaque affichage » — la pose séparée rattachait le
     * document avec la langue de l'onglet PRÉCÉDENT, une tokenisation
     * complète pour rien, aussitôt remplacée par la vraie). Le
     * parquage fait le reste : une session revenue retrouve SON
     * moteur et SA publication — la tokenisation vit avec le
     * document (le modèle du dépôt de départ), jamais refaite.
     *
     * @param nouvelleSession la session d'édition à afficher
     * @param nouvelleLangue  le support du langage du document, null
     *                        pour aucun
     */
    public void attach(TextSession nouvelleSession,
            @Nullable LanguageSupport nouvelleLangue) {
        TextSession precedente = session;
        if (precedente != null) {
            precedente.removeEditListener(foldListener);
        }
        // D-07 : l'état replié de la session quittée s'archive,
        // celui de la session qui arrive se restaure — le pli
        // manuel appartient à SON document.
        foldMemory.archiver(precedente);
        session = nouvelleSession;
        session.addEditListener(foldListener);
        documentSuivi = session.document();
        foldMemory.restaurer(session);
        // Le parquage par session (le retour terrain du 2026-11-19) :
        // la tokenisation appartient AU DOCUMENT. Le moteur de la
        // session quittée se PARQUE sous ELLE avec sa publication —
        // une session revenue le retrouve et ne re-tokenise RIEN (le
        // dépôt de départ y gardait les styledLines dans la session).
        engine = park.toggle(precedente, engine, nouvelleSession,
                windowFirst, windowLast);
        language = nouvelleLangue;
        syntaxeDuLangage =
                nouvelleLangue == null ? null : nouvelleLangue.syntax();
        editionsEnAttente.clear();
        chaineRompue = false;
        guides = GuidesStructurels.EMPTY;
        rattacher();
        // suiviPlis.clear() vit dans restaurerPlis() (D-07) : la
        // restauration vide PUIS repose les plis du document qui
        // arrive — clear ici effacerait la restauration.
        // Le plan de l'attachement : vide tant que la publication
        // n'est pas relue (habiller pousse le vrai plan à la première
        // rangée habillée) — le consommateur n'affiche jamais les
        // entrées d'un document parti.
        pushOutline(List.of());
    }

    /**
     * Change le langage de l'éditeur (M7.6 : le support complet —
     * descripteur, configuration, source syntaxique, capacités) ;
     * null coupe l'analyse et vide la coulisse de marqueurs. La
     * source syntaxique du support nourrit l'analyse, sa
     * configuration nourrit la frappe. La LANGUE inchangée ne
     * re-rattache rien (le retour terrain du 2026-11-19 : chaque
     * pose re-tokenisait le document entier — même langue, même
     * document, même travail refait).
     *
     * @param nouvelleLangue le support du langage, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSupport nouvelleLangue) {
        language = nouvelleLangue;
        syntaxeDuLangage = nouvelleLangue == null ? null : nouvelleLangue.syntax();
        editionsEnAttente.clear();
        chaineRompue = false;
        guides = GuidesStructurels.EMPTY;
        rattacher();
        revisionAnalysee = -1L;
        if (language == null) {
            pushOutline(List.of());
        }
    }

    /** Le support du langage analysé, null sans analyse. */
    public @Nullable LanguageSupport language() {
        return language;
    }

    /** Vrai quand l'analyse est branchée. */
    public boolean active() {
        return coordinateur != null || engine != null || nativePathEngine != null;
    }

    /**
     * Une publication d'analyse existe-t-elle ? (M7.5, §9) : OUI dès
     * la première publication — une rangée qui trace sans couleur
     * ensuite se comptera comme décolorée ; avant, rien n'avait de
     * couleur à perdre. La voie synchrone publie à la première
     * demande : elle n'attend jamais.
     *
     * @return vrai quand une publication existe
     */
    public boolean expectedColor() {
        if (nativePathEngine != null) {
            return nativePathEngine.analysePubliee() != null;
        }
        AnalysisEngine moteurLocal = engine;
        if (moteurLocal != null) {
            return moteurLocal.analysePubliee() != null;
        }
        return coordinateur != null;
    }

    /**
     * Indique la fenêtre visible : la voie hors fil y paie l'arbre
     * d'abord (G-01). Le travail en vol ne s'annule pas — le défilement
     * ne coûte rien.
     *
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     */
    public void window(int premiereLigne, int derniereLigne) {
        windowFirst = premiereLigne;
        windowLast = derniereLigne;
        if (engine != null) {
            engine.window(premiereLigne, derniereLigne);
        }
    }

    /**
     * Indique la fenêtre visible en rangées : la projection résout les
     * lignes, la voie hors fil y paie l'arbre d'abord (G-01).
     *
     * @param projection   la projection rangées ↔ lignes courante
     * @param premiereRangee la première rangée visible
     * @param nombreRangees  le nombre de rangées visibles
     */
    public void window(VisualProjection projection, int premiereRangee, int nombreRangees) {
        int derniere = Math.min(premiereRangee + nombreRangees, projection.rowCount() - 1);
        window(projection.firstLineOfRow(premiereRangee), projection.firstLineOfRow(derniere));
    }

    /**
     * Le rappel d'invalidation : il part à chaque publication d'analyse,
     * sur le fil de travail — l'hôte y poste son invalidation (PIEGE-20 :
     * la vue recolore à la bonne révision, jamais avant).
     *
     * @param rappel le rappel à prévenir, null pour le retirer
     */
    public void setSurAnalysePrete(@Nullable Runnable rappel) {
        surAnalysePrete = rappel;
    }

    /**
     * Abonne un consommateur du plan (voie 2.0, ADR-0022 phase 2
     * T3) : il reçoit les mêmes entrées, au même moment — le kit et
     * l'hôte ensemble.
     *
     * @param consommateur le consommateur des entrées, jamais null
     */
    public void addPlanListener(Consumer<List<OutlineEntry>> consommateur) {
        consommateursPlan.add(java.util.Objects.requireNonNull(consommateur));
    }

    /**
     * Désabonne un consommateur du plan (voie 2.0).
     *
     * @param consommateur le consommateur retiré
     */
    public void removePlanListener(Consumer<List<OutlineEntry>> consommateur) {
        consommateursPlan.remove(consommateur);
    }

    /** Referme le pont : les moteurs se détachent, le parc se vide,
     *  la gouttière se vide. */
    public void close() {
        closeEngine();
        // Le parc suit : les moteurs parqués des sessions détachées
        // meurent tous — la vue qui ferme ne laisse AUCUN fil d'analyse
        // vivant derrière elle (C2).
        park.closeAll();
        if (session != null) {
            session.removeEditListener(foldListener);
        }
    }

    /**
     * La largeur de la coulisse de marqueurs : un caractère au moins,
     * la marge au plancher — nulle sans analyse. La coulisse se réserve
     * dès l'attachement d'un langage, AVANT la première publication :
     * jamais elle n'arrive avec un résultat (PIEGE-25).
     *
     * @param charWidth la largeur d'un caractère, en pixels
     * @param plancher  la marge minimale, en pixels
     * @return la largeur de coulisse, en pixels
     */
    public float lane(float charWidth, float plancher) {
        if (!active()) {
            return 0f;
        }
        return Math.max(plancher, charWidth);
    }

    /**
     * Habille la source de rangées des plages de jetons (B-01) et
     * rafraîchit les régions pliables et les lignes en erreur quand
     * l'analyse change de révision — l'état replié s'interroge au
     * vivant sur le modèle de plis. Sans analyse à jour, la publication
     * périmée rattrape par remappage ; sans publication du tout, la
     * source passe telle quelle.
     *
     * @param source   la source de rangées non colorées
     * @param document l'instantané affiché
     * @return la source habillée, ou la source telle quelle
     */
    public RowSource wrap(RowSource source, Document document) {
        Analysis analyse = analyseCourante(document);
        if (analyse == null) {
            return wrapStale(source, document);
        }
        if (analyse.revision() != revisionAnalysee) {
            revisionAnalysee = analyse.revision();
            regionsPliables.clear();
            regionsPliables.addAll(
                    LanguageStructure.folds(language, document, analyse));
            java.util.List<jo.celeris.core.syntax.SyntaxIssue> issuesNeuves =
                    fusionner(TreeDiagnostics.issues(document, analyse),
                            fromBench);
            marks = GutterMarks.of(regionsPliables, folds,
                    linesOf(issuesNeuves, document));
            issues = issuesNeuves;
            guides = GuidesStructurels.of(language, document, analyse);
            pushOutline(LanguageStructure.plan(language, document, analyse,
                    regionsPliables));
        }
        editionsEnAttente.clear();
        chaineRompue = false;
        return new FoldedRowSource(new SyntaxRowSource(source, analyse.lex()),
                document, folds, analyse.lex());
    }

    /**
     * L'habillage de rattrapage : la publication date d'avant
     * l'instantané affiché — les lignes dont le contenu a survécu
     * gardent leurs couleurs publiées, la ligne touchée reçoit ses
     * jetons recalés sur l'édition en attente (chirurgie fine, puis
     * relecture quand la structure a changé) : jamais un mot ne
     * retombe à l'encre de base en attendant la publication
     * (PIEGE-26, TokenCatchUp). Sans publication ou chaîne rompue
     * (rafale au-delà du plafond), la source reste sans couleur :
     * jamais une ligne au contenu non prouvé ne se colore à devinant.
     *
     * <p>La publication lu est celle de la voie qui SERT (feuille de
     * route A1.2) : la voie native la publie aussi — le rattrapeur ne
     * lit qu'un lexique document, jamais le moteur qui l'a produit ;
     * sa publication périmée y recolle exactement pareil.</p>
     */
    private RowSource wrapStale(RowSource source, Document document) {
        Analysis published = publicationPerimee();
        if (published == null || chaineRompue) {
            return source;
        }
        List<EditionEnAttente> enAttente = editionsDepuis(published.revision());
        if (enAttente.isEmpty()) {
            return source;
        }
        List<EditSite> sites = new ArrayList<>(enAttente.size());
        List<TokenCatchUp.PendingEdit> edits =
                new ArrayList<>(enAttente.size());
        for (EditionEnAttente edit : enAttente) {
            sites.add(edit.site());
            edits.add(edit.versPendingEdit());
        }
        return new SyntaxRowSource(source, published.lex(),
                TokenCatchUp.of(published.lex(), edits, sites, document,
                        syntaxeDuLangage == null ? null : syntaxeDuLangage.lexer()));
    }

    /**
     * La publication périmée de la voie qui sert — la voie hors fil du
     * socle ou la voie native, la première non nulle : le rattrapage
     * leur est commun, seule la publication change.
     */
    private @Nullable Analysis publicationPerimee() {
        SyntaxEngine voieNative = nativePathEngine;
        if (voieNative != null) {
            return voieNative.analysePubliee();
        }
        AnalysisEngine moteurLocal = engine;
        return moteurLocal == null ? null : moteurLocal.analysePubliee();
    }

    /**
     * Les éditions en attente postérieures à la révision publiée, en
     * ordre chronologique — les révisions croissent strictement, les
     * éditions absorbées par la publication se retirent de la tête.
     */
    private List<EditionEnAttente> editionsDepuis(int revisionPubliee) {
        while (!editionsEnAttente.isEmpty()
                && editionsEnAttente.get(0).revision() <= revisionPubliee) {
            editionsEnAttente.remove(0);
        }
        return new ArrayList<>(editionsEnAttente);
    }

    /**
     * L'analyse courante : la voie synchrone la calcule à la demande,
     * la voie hors fil ne rend QUE la publication à jour de l'instantané
     * affiché — jamais le lexique d'une autre révision ne colore un
     * document (PIEGE-20). Visible du paquet : le concentrateur
     * d'intelligence (M7.5, §8) y lit l'analyse de ses gestes.
     */
    @Nullable Analysis analyseCourante(Document document) {
        if (nativePathEngine != null) {
            return nativePathEngine.analyseDeRevision(document.revision());
        }
        if (engine != null) {
            Analysis published = engine.analysePubliee();
            if (published == null || published.revision() != document.revision()) {
                return null;
            }
            return published;
        }
        if (coordinateur == null) {
            return null;
        }
        return coordinateur.analysis();
    }

    /**
     * L'analyse de REPLI de la complétion : la DERNIÈRE publication,
     * même périmée — la complétion de base ne dépend JAMAIS d'une
     * analyse à jour de la révision (la frappe change la révision à
     * chaque caractère, la publication la suit de loin : attendre
     * l'à-jour rend la complétion muette à la première frappe — la
     * revue du propriétaire du 2026-11-19 : « j'ai tapé des mots,
     * aucune suggestion »). Les mots du document s'y relisent au
     * lexique de la dernière publication ; les mots réservés du
     * langage arrivent de toute façon (le coach de la langue) — le
     * pire cas (publication jamais venue) rend les mots réservés
     * seuls, jamais le silence.
     *
     * @return la dernière analyse publiée, null sans aucune publication
     */
    @Nullable Analysis analyseDeRepli() {
        AnalysisEngine moteurLocal = engine;
        return moteurLocal == null ? null : moteurLocal.analysePubliee();
    }

    /** Les marques de gouttière courantes — {@link GutterMarks#EMPTY} sans analyse. */
    public GutterMarks marks() {
        return marks;
    }

    /**
     * Les diagnostics courants, plage source et sévérité (D-05,
     * D-06) — la couche de squiggles y lit. Vide sans analyse ; les
     * plages suivent la dernière publication, comme les marques.
     *
     * @return les diagnostics, jamais null
     */
    public java.util.List<jo.celeris.core.syntax.SyntaxIssue> issues() {
        return issues;
    }

    /** Les issues de banc posées (D-05) — les tests de vue y
     *  injectent les diagnostics EXTERNES (lint du build, LSP) ; la
     *  FUSION se fait au moment de l'habillage (les issues de l'arbre
     *  se re-posent à chaque publication, celles du banc survivent —
     *  la même sémantique que les lignes fusionnées de la gouttière,
     *  C-12).
     */
    private java.util.List<jo.celeris.core.syntax.SyntaxIssue> fromBench =
            java.util.List.of();

    /** Pose des issues de banc (D-05) — jamais appelé hors banc.
     *
     * @param nouvelles les issues externes à fusionner aux issues d'arbre
     */
    void poserIssuesDeBanc(
            java.util.List<jo.celeris.core.syntax.SyntaxIssue> nouvelles) {
        fromBench = java.util.List.copyOf(nouvelles);
        // re-fusion immédiate : la pose suit les issues courantes
        issues = fusionner(issues, fromBench);
    }

    /** La fusion : les issues de l'arbre puis celles du banc —
     *  l'ordre du document n'est pas exigé (chaque issue porte sa
     *  plage ; la couche trace dans l'ordre donné). */
    private static java.util.List<jo.celeris.core.syntax.SyntaxIssue> fusionner(
            java.util.List<jo.celeris.core.syntax.SyntaxIssue> arbre,
            java.util.List<jo.celeris.core.syntax.SyntaxIssue> banc) {
        if (banc.isEmpty()) {
            return arbre;
        }
        java.util.List<jo.celeris.core.syntax.SyntaxIssue> fondues =
                new java.util.ArrayList<>(arbre.size() + banc.size());
        fondues.addAll(arbre);
        fondues.addAll(banc);
        return java.util.List.copyOf(fondues);
    }

    /** Les lignes des issues données — la voie historique de la
     *  gouttière (D-02), nourrie par les issues complètes (D-06). */
    private static int[] linesOf(
            java.util.List<jo.celeris.core.syntax.SyntaxIssue> issues,
            Document document) {
        int[] lines = new int[issues.size()];
        for (int i = 0; i < issues.size(); i++) {
            lines[i] = document.lineForOffset(issues.get(i).startOffset());
        }
        return lines;
    }

    /**
     * Bascule le pli de la ligne sous le point donné (D-07) : l'appui
     * dans la BANDE DE PLIS — le bord droit de la gouttière, contre le
     * code — déplie une ligne repliée, replie une région pliable sur
     * l'étendue de l'arbre.
     *
     * @param x          l'abscisse de l'appui, en pixels
     * @param y          l'ordonnée de l'appui, en pixels
     * @param scrollY    le défilement vertical courant, en pixels
     * @param hauteurLigne la hauteur d'une rangée, en pixels
     * @param largeurGouttiere la largeur totale de la gouttière, en pixels
     * @param foldBand   la largeur de la bande de plis, en pixels
     * @param projection la projection rangées ↔ lignes courante
     * @return vrai quand l'appui est consommé par la bande de plis
     */
    public boolean toggleFoldAt(float x, float y, float scrollY,
            float hauteurLigne, float largeurGouttiere, float foldBand,
            VisualProjection projection) {
        // D-07 : la bande vit pour l'analyse, qu'elle soit servie par le
        // coordinateur synchrone OU par le moteur hors fil — la voie de
        // l'hôte d'intégration embarque TOUJOURS un moteur (SceneDiagnostics
        // fabrique l'exécuteur quand l'hôte n'en fournit pas), l'ancienne
        // garde y tuait l'appui avant même le test d'appartenance.
        if (!active()
                || !GutterGeometry.inBand(x, largeurGouttiere, foldBand)) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int line = projection.firstLineOfRow(rangee);
        return toggleFold(line);
    }

    /**
     * Bascule le pli porté par la ligne d'en-tête donnée : repliée, elle
     * se déplie ; pliable, elle se replie sur l'étendue de sa région —
     * le pli IMBRIQUÉ s'y garde (le parent d'un bloc déjà replié se
     * replie PAR-DESSUS, l'enfant survit replié sous lui, le
     * redressement du 2026-11-13) ; seul un pli voisin qui CROISE la
     * région bloque le repli, l'appui se consomme sans effet.
     *
     * @param line la ligne d'en-tête visée
     * @return vrai quand la ligne porte une région pliable
     */
    public boolean toggleFold(int line) {
        if (!marks.pliable(line)) {
            return false;
        }
        if (marks.replie(line)) {
            foldTracking.unfold(line);
            return true;
        }
        for (FoldSpan region : regionsPliables) {
            if (region.startLine() == line) {
                if (region.endLine() > line
                        && !croiseUnPli(line, region.endLine())) {
                    foldTracking.fold(line, region.endLine());
                }
                return true;
            }
        }
        return true;
    }

    /**
     * Vrai quand la plage donnée CROISE un pli déjà replié : la règle
     * vit dans {@link FoldModel#croise} — LA source unique.
     */
    private boolean croiseUnPli(int start, int end) {
        for (FoldModel.Pli pli : folds.folds()) {
            if (FoldModel.croise(start, end, pli)) {
                return true;
            }
        }
        return false;
    }

    private void rattacher() {
        // Le moteur revenu du PARC ne se referme pas (le retour
        // terrain du 2026-11-19 : sa publication survit au
        // détachement — la vue ne re-tokenise JAMAIS un document
        // déjà analysé). La garde précède fermerMoteur : le moteur
        // courant sert TOUJOURS la session courante ici (attacher a
        // parqué l'ancien et repris le bon avant l'appel).
        if (catchUpParkedEngine()) {
            return;
        }
        closeEngine();
        LanguageSyntax syntaxe = syntaxeDuLangage;
        // La voie tree-sitter (ADR-0024) : le pont ne connaît pas le
        // moteur concret — le module opt-in expose sa fabrique par
        // ServiceLoader (charge si présent, rien sinon) et la voie
        // des grammaires sert par défaut.
        nativePathEngine =
                EnginePathFactoryLoader.create(syntaxe, session, executor);
        if (nativePathEngine != null) {
            nativePathEngine.addListener(auditeurPublication);
            nativePathEngine.reanalyser();
            return;
        }
        if (executor == null) {
            coordinateur = syntaxe == null || session == null
                    ? null
                    : SyntaxCoordinator.on(session, syntaxe);
            return;
        }
        if (syntaxe != null && session != null) {
            engine = AnalysisEngine.on(session, syntaxe, executor,
                    windowFirst, windowLast);
            engine.addListener(auditeurPublication);
            engine.reanalyser();
        }
    }

    /** Le moteur courant ressert la session et la langue courantes
     *  (le retour terrain du 2026-11-19) : sa publication à jour ne
     *  se rejoue PAS — le document inchangé depuis le détachement
     *  revient coloré sans AUCUNE nouvelle passe ; seules les
     *  éditions manquées pendant l'absence demandent un rattrapage.
     *  Vrai quand le moteur a servi, faux quand il faut le refaire.
     */
    private boolean catchUpParkedEngine() {
        boolean duDocument = engine != null
                && engine.session() == session
                && engine.language() == syntaxeDuLangage;
        if (!duDocument) {
            return false;
        }
        // L'auditeur ne se DUPLIQUE pas au retour du parc (l'ancien chemin
        // ré-additionnait l'auditeur de la création à chaque retour — N
        // allers-retours d'onglets = N invalidate par publication).
        engine.removeListener(auditeurPublication);
        engine.addListener(auditeurPublication);
        Analysis published = engine.analysePubliee();
        boolean aJour = published != null
                && published.revision() == session.document().revision();
        if (!aJour) {
            engine.reanalyser();
        }
        return true;
    }

    private void closeEngine() {
        if (nativePathEngine != null) {
            nativePathEngine.close();
            nativePathEngine = null;
        }
        if (engine != null) {
            engine.close();
            engine = null;
        }
        if (coordinateur != null) {
            coordinateur.close();
            coordinateur = null;
        }
    }

    private void notifyAnalysisReady() {
        Runnable rappel = surAnalysePrete;
        if (rappel != null) {
            rappel.run();
        }
    }

    /** Pousse le plan à CHAQUE abonné (voie 2.0 seule). */
    private void pushOutline(List<OutlineEntry> entrees) {
        for (Consumer<List<OutlineEntry>> abonne : consommateursPlan) {
            abonne.accept(entrees);
        }
    }

    /**
     * Le remappage des plis à l'éditon (A-15) : l'instantané d'avant
     * accompagne la notification, le suivi décale les bornes, jamais
     * l'état replié. Le site de l'édition rejoint la chaîne des
     * éditions en attente — le rattrapage de coloration s'y servira
     * tant que la publication n'est pas à jour.
     */
    private void surEdition(int start, int end, String removed, String inserted) {
        Document avant = documentSuivi;
        if (avant != null && session != null) {
            Document apres = session.document();
            foldTracking.surEdition(avant, start, end, removed, inserted);
            documentSuivi = apres;
            guides = guides.remapper(apres.revision(), avant, start, end,
                    removed, inserted);
            enregistrerEdition(avant, apres, start, end, removed, inserted);
        }
    }

    /**
     * Enregistre le site d'une édition en attente : ses coordonnées
     * se décalent vers le document PUBLIÉ au passage — chaque édition
     * suivante se transforme contre les précédentes, la chaîne se
     * borne ; une rafale au-delà du plafond la déclare rompue — le
     * rattrapage cède, jamais ne devine.
     */
    private void enregistrerEdition(Document avant, Document apres, int start,
            int end, String retire, String insere) {
        if (editionsEnAttente.size() >= CHAINE_MAX) {
            editionsEnAttente.clear();
            chaineRompue = true;
            return;
        }
        boolean structurelle = retire.indexOf('\n') >= 0
                || insere.indexOf('\n') >= 0;
        for (EditionEnAttente ancienne : editionsEnAttente) {
            structurelle |= ancienne.structurelle();
        }
        int publieStart = versPublie(start);
        int line = apres.lineForOffset(start);
        editionsEnAttente.add(new EditionEnAttente(apres.revision(),
                EditSite.of(avant, apres, start, end, insere),
                publieStart, end - start, insere.length(), line,
                publieStart - apres.lineStart(line), structurelle));
    }

    /**
     * Le décalage d'une position courante vers le document publié :
     * les éditions en attente se défont de la plus récente à la plus
     * ancienne — position après le texte inséré, recule du delta ;
     * position DANS le texte inséré, retombe au point d'édition ; avant
     * le point, elle ne bouge pas.
     */
    private int versPublie(int position) {
        int x = position;
        for (int i = editionsEnAttente.size() - 1; i >= 0; i--) {
            EditionEnAttente edit = editionsEnAttente.get(i);
            x = x >= edit.publieStart() + edit.publieInserted()
                    ? x - edit.publieInserted() + edit.publieRemoved()
                    : Math.min(x, edit.publieStart());
        }
        return x;
    }

}
