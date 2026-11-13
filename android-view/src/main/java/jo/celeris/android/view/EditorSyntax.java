package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.MoteurAnalyse;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.syntax.SyntaxIssue;
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
 * <p>Deux voies d'analyse (tranche 6) : sans exécuteur, le coordinateur
 * vit sur le fil d'écriture de la session — l'analyse se paie à la
 * première demande ; avec exécuteur injecté (ADR-007), le moteur hors
 * fil publie par lots — la fenêtre visible d'abord (G-01) — et la vue
 * n'habille JAMAIS une ligne avec les jetons d'une autre révision sans
 * preuve d'identité : sans publication à jour, les éditions en attente
 * remappent les lignes épargnées (contenu démontré identique,
 * RemapLignes), les lignes touchées passent à l'encre de base le temps
 * du vol (PIEGE-20, facette vue — plus un mot ne clignote). Le rappel
 * {@code setSurAnalysePrete} part à chaque publication, sur le fil de
 * travail — l'hôte y invalide.</p>
 *
 * <p>Le plan consommateur (B-16, tranche 8) : les entrées du plan
 * sortent du MÊME arbre que la coloration — le consommateur déclaré
 * par {@code setSurPlan} reçoit les entrées de l'analyse habillée à
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

    private final FoldTracker suiviPlis;
    private final FoldModel plis;
    private final List<FoldSpan> regionsPliables = new ArrayList<>();
    private final EditListener ecouteurPlis = this::surEdition;
    private final @Nullable Executor executeur;
    private final List<EditionEnAttente> editionsEnAttente = new ArrayList<>();
    /** La table des guides verticaux structurels (H-09, A-17 relue),
     *  remappée à l'édition, redérivée à la publication — VIDE sans
     *  analyse à jour. Visible du paquet : la couche des guides y lit
     *  la table courante, collée à la révision (PIEGE-20). */
    GuidesStructurels guides = GuidesStructurels.VIDE;

    private @Nullable TextSession session;
    private @Nullable LanguageSupport langue;
    private @Nullable LanguageSyntax syntaxeDuLangage;
    private @Nullable SyntaxCoordinator coordinateur;
    private @Nullable MoteurAnalyse moteur;
    private @Nullable Runnable surAnalysePrete;
    private @Nullable Consumer<List<OutlineEntry>> surPlan;
    private int fenetrePremiere;
    private int fenetreDerniere = Integer.MAX_VALUE / 2;
    private @Nullable Document documentSuivi;
    private long revisionAnalysee = -1L;
    private GutterMarks marques = GutterMarks.VIDE;
    private boolean chaineRompue;

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
        editionsEnAttente.clear();
        chaineRompue = false;
        guides = GuidesStructurels.VIDE;
        rattacher();
        suiviPlis.clear();
        pousserPlan(List.of());
    }

    /**
     * Change le langage de l'éditeur (M7.6 : le support complet —
     * descripteur, configuration, source syntaxique, capacités) ;
     * null coupe l'analyse et vide la coulisse de marqueurs. La
     * source syntaxique du support nourrit l'analyse, sa
     * configuration nourrit la frappe.
     *
     * @param nouvelleLangue le support du langage, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSupport nouvelleLangue) {
        langue = nouvelleLangue;
        syntaxeDuLangage = nouvelleLangue == null ? null : nouvelleLangue.syntax();
        editionsEnAttente.clear();
        chaineRompue = false;
        guides = GuidesStructurels.VIDE;
        rattacher();
        revisionAnalysee = -1L;
        if (langue == null) {
            pousserPlan(List.of());
        }
    }

    /** Le support du langage analysé, null sans analyse. */
    public @Nullable LanguageSupport language() {
        return langue;
    }

    /** Vrai quand l'analyse est branchée. */
    public boolean active() {
        return coordinateur != null || moteur != null;
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
    public boolean couleurAttendue() {
        MoteurAnalyse moteurLocal = moteur;
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

    /**
     * Le consommateur du plan (B-16) : il reçoit les entrées du plan de
     * l'analyse habillée à chaque changement de révision — le même arbre
     * que la coloration — et un plan vide au rattachement d'une session
     * ou au retrait du langage. Le rappel part sur le fil qui habille :
     * l'hôte y poste vers le sien.
     *
     * @param consommateur le consommateur des entrées, null pour le retirer
     */
    public void setSurPlan(@Nullable Consumer<List<OutlineEntry>> consommateur) {
        surPlan = consommateur;
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
     * la marge au plancher — nulle sans analyse. La coulisse se réserve
     * dès l'attachement d'un langage, AVANT la première publication :
     * jamais elle n'arrive avec un résultat (PIEGE-25).
     *
     * @param charWidth la largeur d'un caractère, en pixels
     * @param plancher  la marge minimale, en pixels
     * @return la largeur de coulisse, en pixels
     */
    public float coulisse(float charWidth, float plancher) {
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
    public RowSource habiller(RowSource source, Document document) {
        Analysis analyse = analyseCourante(document);
        if (analyse == null) {
            return habillerPerimee(source, document);
        }
        if (analyse.revision() != revisionAnalysee) {
            revisionAnalysee = analyse.revision();
            regionsPliables.clear();
            regionsPliables.addAll(
                    LanguageStructure.plis(langue, document, analyse));
            marques = GutterMarks.of(regionsPliables, plis,
                    lignesEnErreur(document, analyse));
            guides = GuidesStructurels.de(langue, document, analyse);
            pousserPlan(LanguageStructure.plan(langue, document, analyse,
                    regionsPliables));
        }
        editionsEnAttente.clear();
        chaineRompue = false;
        return new SyntaxRowSource(source, analyse.lex());
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
     */
    private RowSource habillerPerimee(RowSource source, Document document) {
        MoteurAnalyse moteurLocal = moteur;
        if (moteurLocal == null) {
            return source;
        }
        Analysis publiee = moteurLocal.analysePubliee();
        if (publiee == null || chaineRompue) {
            return source;
        }
        List<EditionEnAttente> enAttente = editionsDepuis(publiee.revision());
        if (enAttente.isEmpty()) {
            return source;
        }
        List<EditSite> sites = new ArrayList<>(enAttente.size());
        List<TokenCatchUp.PendingEdit> editions =
                new ArrayList<>(enAttente.size());
        for (EditionEnAttente edition : enAttente) {
            sites.add(edition.site());
            editions.add(edition.versPendingEdit());
        }
        return new SyntaxRowSource(source, publiee.lex(),
                TokenCatchUp.of(publiee.lex(), editions, sites, document,
                        syntaxeDuLangage == null ? null : syntaxeDuLangage.lexer()));
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
     * dans la BANDE DE PLIS — le bord droit de la gouttière, contre le
     * code — déplie une ligne repliée, replie une région pliable sur
     * l'étendue de l'arbre.
     *
     * @param x          l'abscisse de l'appui, en pixels
     * @param y          l'ordonnée de l'appui, en pixels
     * @param scrollY    le défilement vertical courant, en pixels
     * @param hauteurLigne la hauteur d'une rangée, en pixels
     * @param largeurGouttiere la largeur totale de la gouttière, en pixels
     * @param bandePli   la largeur de la bande de plis, en pixels
     * @param projection la projection rangées ↔ lignes courante
     * @return vrai quand l'appui est consommé par la bande de plis
     */
    public boolean basculerPliAuPoint(float x, float y, float scrollY,
            float hauteurLigne, float largeurGouttiere, float bandePli,
            VisualProjection projection) {
        // D-07 : la bande vit pour l'analyse, qu'elle soit servie par le
        // coordinateur synchrone OU par le moteur hors fil — la voie de
        // production démo embarque TOUJOURS un moteur (SceneDiagnostics
        // fabrique l'exécuteur quand l'hôte n'en fournit pas), l'ancienne
        // garde y tuait l'appui avant même le test d'appartenance.
        if (!active()
                || !GutterGeometry.dansLaBande(x, largeurGouttiere, bandePli)) {
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
        LanguageSyntax syntaxe = syntaxeDuLangage;
        if (executeur == null) {
            coordinateur = syntaxe == null || session == null
                    ? null
                    : SyntaxCoordinator.on(session, syntaxe);
            return;
        }
        if (syntaxe != null && session != null) {
            moteur = MoteurAnalyse.sur(session, syntaxe, executeur,
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

    /** Pousse le plan au consommateur déclaré, s'il y en a un. */
    private void pousserPlan(List<OutlineEntry> entrees) {
        Consumer<List<OutlineEntry>> consommateur = surPlan;
        if (consommateur != null) {
            consommateur.accept(entrees);
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
            suiviPlis.surEdition(avant, start, end, removed, inserted);
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
        int ligne = apres.lineForOffset(start);
        editionsEnAttente.add(new EditionEnAttente(apres.revision(),
                EditSite.of(avant, apres, start, end, insere),
                publieStart, end - start, insere.length(), ligne,
                publieStart - apres.lineStart(ligne), structurelle));
    }

    /**
     * Le décalage d'une position courante vers le document publié :
     * les éditions en attente se défont de la plus récente à la plus
     * ancienne — position après le texte inséré, recule du delta ;
     * position DANS le texte inséré, retombe au point d'édition.
     */
    private int versPublie(int position) {
        int x = position;
        for (int i = editionsEnAttente.size() - 1; i >= 0; i--) {
            EditionEnAttente edition = editionsEnAttente.get(i);
            if (x >= edition.publieStart() + edition.publieInserted()) {
                x = x - edition.publieInserted() + edition.publieRemoved();
            } else if (x > edition.publieStart()) {
                x = edition.publieStart();
            }
        }
        return x;
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
