package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.MoteurAnalyse;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.syntax.TreeDiagnostics;
import jo.celeris.core.syntax.TreeFolds;
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
 * <p>Deux voies d'analyse (tranche 6) : sans exécuteur, le coordinateur
 * vit sur le fil d'écriture de la session — l'analyse se paie à la
 * première demande ; avec exécuteur injecté (ADR-007), le moteur hors
 * fil publie par lots — la fenêtre visible d'abord (G-01) — et la vue
 * n'habille JAMAIS un document avec le lexique d'une autre révision :
 * sans publication à jour, la source passe sans couleur, la première
 * image ne paie pas l'arbre (PIEGE-20, facette vue). Le rappel
 * {@code setSurAnalysePrete} part à chaque publication, sur le fil de
 * travail — l'hôte y invalide.</p>
 */
public final class EditorSyntax {

    private final FoldTracker suiviPlis;
    private final FoldModel plis;
    private final List<FoldSpan> regionsPliables = new ArrayList<>();
    private final EditListener ecouteurPlis = this::surEdition;
    private final @Nullable Executor executeur;

    private @Nullable TextSession session;
    private @Nullable LanguageSyntax langue;
    private @Nullable SyntaxCoordinator coordinateur;
    private @Nullable MoteurAnalyse moteur;
    private @Nullable Runnable surAnalysePrete;
    private int fenetrePremiere;
    private int fenetreDerniere = Integer.MAX_VALUE / 2;
    private @Nullable Document documentSuivi;
    private long revisionAnalysee = -1L;
    private GutterMarks marques = GutterMarks.VIDE;

    /**
     * Construit le pont synchrone sur le modèle de plis de la projection
     * — l'analyse se paie à la première demande, sur le fil d'appel.
     *
     * @param plis le modèle de plis, lu en direct par la projection et
     *             par les marques
     */
    public EditorSyntax(FoldModel plis) {
        this(plis, null);
    }

    /**
     * Construit le pont sur le modèle de plis, avec exécuteur injecté :
     * l'analyse vit hors fil d'appel (ADR-007), la fenêtre visible se
     * paie d'abord (G-01).
     *
     * @param plis      le modèle de plis, lu en direct par la projection
     *                  et par les marques
     * @param executeur l'exécuteur du travail d'analyse, null pour la
     *                  voie synchrone
     */
    public EditorSyntax(FoldModel plis, @Nullable Executor executeur) {
        this.plis = plis;
        this.suiviPlis = new FoldTracker(plis);
        this.executeur = executeur;
    }

    /**
     * Attache la session suivie : l'auditeur de plis s'y abonne, le
     * coordinateur s'y rattache, les plis de la session précédente
     * s'oublient — un pli appartient à son document.
     *
     * @param nouvelleSession la session d'éditon affichée
     */
    public void attacher(TextSession nouvelleSession) {
        if (session != null) {
            session.removeEditListener(ecouteurPlis);
        }
        session = nouvelleSession;
        session.addEditListener(ecouteurPlis);
        documentSuivi = session.document();
        rattacher();
        suiviPlis.clear();
    }

    /**
     * Change le langage analysé ; null coupe l'analyse et vide la
     * coulisse de marqueurs.
     *
     * @param nouvelleLangue le langage à analyser, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSyntax nouvelleLangue) {
        langue = nouvelleLangue;
        rattacher();
        revisionAnalysee = -1L;
    }

    /** Le langage analysé, null sans analyse. */
    public @Nullable LanguageSyntax language() {
        return langue;
    }

    /** Vrai quand l'analyse est branchée. */
    public boolean active() {
        return coordinateur != null || moteur != null;
    }

    /**
     * Indique la fenêtre visible : la voie hors fil y paie l'arbre
     * d'abord (G-01). Le travail en vol ne s'annule pas — le défilement
     * ne coûte rien.
     *
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     */
    public void fenetre(int premiereLigne, int derniereLigne) {
        fenetrePremiere = premiereLigne;
        fenetreDerniere = derniereLigne;
        if (moteur != null) {
            moteur.fenetre(premiereLigne, derniereLigne);
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
    public void fenetre(VisualProjection projection, int premiereRangee, int nombreRangees) {
        int derniere = Math.min(premiereRangee + nombreRangees, projection.rowCount() - 1);
        fenetre(projection.firstLineOfRow(premiereRangee), projection.firstLineOfRow(derniere));
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

    /** Referme le pont : le moteur et le coordinateur se détachent, la gouttière se vide. */
    public void fermer() {
        fermerMoteur();
        if (coordinateur != null) {
            coordinateur.close();
            coordinateur = null;
        }
        if (session != null) {
            session.removeEditListener(ecouteurPlis);
        }
    }

    /**
     * La largeur de la coulisse de marqueurs : un caractère au moins,
     * la marge au plancher — nulle sans analyse.
     *
     * @param charWidth la largeur d'un caractère, en pixels
     * @param plancher  la marge minimale, en pixels
     * @return la largeur de coulisse, en pixels
     */
    public float coulisse(float charWidth, float plancher) {
        if (coordinateur == null) {
            return 0f;
        }
        return Math.max(plancher, charWidth);
    }

    /**
     * Habille la source de rangées des plages de jetons (B-01) et
     * rafraîchit les régions pliables et les lignes en erreur quand
     * l'analyse change de révision — l'état replié s'interroge au
     * vivant sur le modèle de plis. Sans analyse, la source passe
     * telle quelle.
     *
     * @param source   la source de rangées non colorées
     * @param document l'instantané affiché
     * @return la source habillée, ou la source telle quelle
     */
    public RowSource habiller(RowSource source, Document document) {
        Analysis analyse = analyseCourante(document);
        if (analyse == null) {
            return source;
        }
        if (analyse.revision() != revisionAnalysee) {
            revisionAnalysee = analyse.revision();
            regionsPliables.clear();
            regionsPliables.addAll(TreeFolds.regions(document, analyse));
            marques = GutterMarks.of(regionsPliables, plis,
                    lignesEnErreur(document, analyse));
        }
        return new SyntaxRowSource(source, analyse.lex());
    }

    /**
     * L'analyse courante : la voie synchrone la calcule à la demande,
     * la voie hors fil ne rend QUE la publication à jour de l'instantané
     * affiché — jamais le lexique d'une autre révision ne colore un
     * document (PIEGE-20).
     */
    private @Nullable Analysis analyseCourante(Document document) {
        if (moteur != null) {
            Analysis publiee = moteur.analysePubliee();
            if (publiee == null || publiee.revision() != document.revision()) {
                return null;
            }
            return publiee;
        }
        if (coordinateur == null) {
            return null;
        }
        return coordinateur.analysis();
    }

    /** Les marques de gouttière courantes — {@link GutterMarks#VIDE} sans analyse. */
    public GutterMarks marques() {
        return marques;
    }

    /**
     * Bascule le pli de la ligne sous le point donné (D-07) : l'appui
     * dans la coulisse déplie une ligne repliée, replie une région
     * pliable sur l'étendue de l'arbre.
     *
     * @param x          l'abscisse de l'appui, en pixels
     * @param y          l'ordonnée de l'appui, en pixels
     * @param scrollY    le défilement vertical courant, en pixels
     * @param hauteurLigne la hauteur d'une rangée, en pixels
     * @param coulisse   la largeur de la coulisse des marqueurs, en pixels
     * @param projection la projection rangées ↔ lignes courante
     * @return vrai quand l'appui est consommé par la coulisse
     */
    public boolean basculerPliAuPoint(float x, float y, float scrollY,
            float hauteurLigne, float coulisse, VisualProjection projection) {
        if (coordinateur == null || x >= coulisse) {
            return false;
        }
        int rangee = ScrollGeometry.firstRow(scrollY + y, hauteurLigne);
        int ligne = projection.firstLineOfRow(rangee);
        return basculerPli(ligne);
    }

    /**
     * Bascule le pli porté par la ligne d'en-tête donnée : repliée, elle
     * se déplie ; pliable, elle se replie sur l'étendue de sa région —
     * un pli voisin qui chevauche déjà bloque le repli, l'appui se
     * consomme sans effet.
     *
     * @param ligne la ligne d'en-tête visée
     * @return vrai quand la ligne porte une région pliable
     */
    public boolean basculerPli(int ligne) {
        if (!marques.pliable(ligne)) {
            return false;
        }
        if (marques.replie(ligne)) {
            suiviPlis.unfold(ligne);
            return true;
        }
        for (FoldSpan region : regionsPliables) {
            if (region.startLine() == ligne) {
                if (region.endLine() > ligne
                        && !chevaucheUnPli(ligne, region.endLine())) {
                    suiviPlis.fold(ligne, region.endLine());
                }
                return true;
            }
        }
        return true;
    }

    /** Vrai quand la plage donnée chevauche un pli déjà replié. */
    private boolean chevaucheUnPli(int debut, int fin) {
        for (FoldModel.Pli pli : plis.plis()) {
            if (debut <= pli.fin() && pli.debut() <= fin) {
                return true;
            }
        }
        return false;
    }

    private void rattacher() {
        fermerMoteur();
        if (coordinateur != null) {
            coordinateur.close();
            coordinateur = null;
        }
        if (executeur == null) {
            coordinateur = langue == null || session == null
                    ? null
                    : SyntaxCoordinator.on(session, langue);
            return;
        }
        if (langue != null && session != null) {
            moteur = MoteurAnalyse.sur(session, langue, executeur,
                    fenetrePremiere, fenetreDerniere);
            moteur.ajouterAuditeur(analyse -> notifierAnalysePrete());
            moteur.reanalyser();
        }
    }

    private void fermerMoteur() {
        if (moteur != null) {
            moteur.close();
            moteur = null;
        }
    }

    private void notifierAnalysePrete() {
        Runnable rappel = surAnalysePrete;
        if (rappel != null) {
            rappel.run();
        }
    }

    /**
     * Le remappage des plis à l'éditon (A-15) : l'instantané d'avant
     * accompagne la notification, le suivi décale les bornes, jamais
     * l'état replié.
     */
    private void surEdition(int start, int end, String removed, String inserted) {
        Document avant = documentSuivi;
        if (avant != null && session != null) {
            suiviPlis.surEdition(avant, start, end, removed, inserted);
            documentSuivi = session.document();
        }
    }

    /** Les lignes porteuses d'un diagnostic, dédoublées et triées par GutterMarks. */
    private static int[] lignesEnErreur(Document document, Analysis analyse) {
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(document, analyse);
        int[] lignes = new int[problemes.size()];
        for (int i = 0; i < problemes.size(); i++) {
            lignes[i] = document.lineForOffset(problemes.get(i).startOffset());
        }
        return lignes;
    }
}
