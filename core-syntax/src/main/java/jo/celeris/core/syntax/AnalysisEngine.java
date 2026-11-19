package jo.celeris.core.syntax;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

/**
 * Moteur d'analyse hors fil d'appel (ADR-007, G-01) : l'exécuteur
 * s'injecte — jamais ne se déclare au cœur —, le travail d'un document
 * part par la fenêtre visible puis se complète par lots, chaque lot
 * publié à l'auditeur sur le fil de travail ; l'édition supprime le
 * travail en vol par son jeton et reprogramme sur l'instantané le plus
 * récent — une tâche supplantée ne publie jamais (PIEGE-20 : la
 * publication vérifie son jeton sous moniteur).
 *
 * <p>L'édition après une publication suit la voie incrémentale — un seul
 * site en attente ; une seconde édition avant que l'incrément n'ait tourné
 * repart par la voie de fenêtre, la chaîne de sites ne se plie pas. Le
 * rejeu de l'annulation emprunte la même voie que toute édition
 * (PIEGE-07 : l'auditeur de session notifie le rejeu, le moteur suit).</p>
 *
 * <p>Tout état mutable vit sous le moniteur du moteur ; le travail
 * lisible — documents et analyses — est immuable, capturé sous moniteur
 * puis travaillé hors verrou. Sans volatile ni atomique : la règle
 * ADR-007 est vérifiée par le test d'architecture du module.</p>
 */
public final class AnalysisEngine implements SyntaxEngine {

    /** Taille d'un lot de complétion entre deux publications. */
    private static final int LOT = 32;

    private final TextSession session;
    private final SyntaxService service;
    private final Executor executor;
    private final EditListener ecouteur = this::surEdition;

    /** Auditeurs, sous moniteur. */
    private final List<java.util.function.Consumer<Analysis>> auditeurs =
            new ArrayList<>();

    /** Sites d'édition en attente d'être rejoués par incrément (au plus un). */
    private final Deque<EditSite> sites = new ArrayDeque<>();

    private @Nullable TokenCancellation enCours;
    private @Nullable Document target;
    private @Nullable Analysis published;
    private @Nullable Document publishedDoc;
    private @Nullable Document documentSuivi;
    private int windowFirst;
    private int windowLast;
    private boolean cheminComplet;
    private boolean ferme;

    private AnalysisEngine(TextSession session, LanguageSyntax language, Executor executor,
                          int windowFirst, int windowLast) {
        this.session = session;
        this.service = SyntaxService.of(language);
        this.executor = executor;
        this.windowFirst = windowFirst;
        this.windowLast = windowLast;
        this.documentSuivi = session.document();
        session.addEditListener(ecouteur);
    }

    /**
     * Le moteur d'une session, fenêtre couvrant tout le document.
     *
     * @param session   la session suivie (fil d'écriture)
     * @param language    le langage à analyser
     * @param executor l'exécuteur du travail d'analyse (injecté, ADR-007)
     * @return le moteur abonné, travail non encore démarré
     */
    public static AnalysisEngine on(TextSession session, LanguageSyntax language,
                                    Executor executor) {
        return new AnalysisEngine(session, language, executor, 0, Integer.MAX_VALUE / 2);
    }

    /**
     * Le moteur d'une session, fenêtre initiale donnée.
     *
     * @param session           la session suivie (fil d'écriture)
     * @param language            le langage à analyser
     * @param executor         l'exécuteur du travail d'analyse (injecté, ADR-007)
     * @param windowFirst   la première ligne visible au démarrage
     * @param windowLast   la dernière ligne visible au démarrage
     * @return le moteur abonné, travail non encore démarré
     */
    public static AnalysisEngine on(TextSession session, LanguageSyntax language,
                                    Executor executor, int windowFirst,
                                    int windowLast) {
        return new AnalysisEngine(session, language, executor,
                windowFirst, windowLast);
    }

    /**
     * Indique la fenêtre visible au prochain passage complet : le
     * travail en vol ne s'annule pas, le défilement ne coûte rien.
     *
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     */
    @Override
    public synchronized void window(int premiereLigne, int derniereLigne) {
        this.windowFirst = premiereLigne;
        this.windowLast = derniereLigne;
    }

    /**
     * Programme l'analyse de l'instantané courant par la voie complète :
     * fenêtre d'abord, complétion par lots ensuite.
     */
    @Override
    public void reanalyser() {
        Runnable tache;
        synchronized (this) {
            if (ferme) {
                return;
            }
            target = session.document();
            documentSuivi = target;
            cheminComplet = true;
            sites.clear();
            tache = programmer();
        }
        executor.execute(tache);
    }

    /**
     * Le dernier instantané publié — null tant que rien n'est publié.
     *
     * @return l'analyse publiée, complète ou partielle
     */
    @Override
    public synchronized @Nullable Analysis analysePubliee() {
        return published;
    }

    /**
     * La session suivie par le moteur — le parquage de la vue s'y
     * reconnaît (un moteur DETACHÉ reste vivant pour son document,
     * la vue le retrouve au retour de sa session ; le retour terrain
     * du 2026-11-19 : la coloration ne se refait jamais).
     *
     * @return la session que le moteur analyse
     */
    public TextSession session() {
        return session;
    }

    /**
     * Le langage que le moteur analyse — la vue y reconnaît le
     * moteur de la LANGUE courante : un moteur parqué ne revient du
     * parc que pour le document ET la langue qui sont les siens (le
     * retrait ou le changement de langue referme le moteur, la
     * re-tokenisation ne se fait que là où elle est due).
     *
     * @return la syntaxe que le moteur sert
     */
    public LanguageSyntax language() {
        return service.language();
    }

    /**
     * L'analyse publiée pour la révision demandée (MoteurSyntaxe,
     * ADR-0024) — null si la publication courante ne la sert pas
     * exactement : jamais le lexique d'une autre révision ne colore
     * un document (PIEGE-20).
     *
     * @param revision la révision voulue
     * @return l'analyse à jour, ou null
     */
    @Override
    public synchronized @Nullable Analysis analyseDeRevision(long revision) {
        Analysis courante = published;
        if (courante == null || courante.revision() != revision) {
            return null;
        }
        return courante;
    }

    /**
     * S'abonne aux publications (MoteurSyntaxe, ADR-0024) — le pont
     * y branche sa remise en couleur.
     *
     * @param auditeur l'auditeur à prévenir sur le fil de travail
     */
    @Override
    public synchronized void addListener(
            java.util.function.Consumer<Analysis> auditeur) {
        auditeurs.add(auditeur);
    }

    /** Se désabonne des publications. */
    public synchronized void removeListener(
            java.util.function.Consumer<Analysis> auditeur) {
        auditeurs.remove(auditeur);
    }

    /** Détache l'auditeur de session, annule le travail en vol : tout s'arrête. */
    @Override
    public void close() {
        synchronized (this) {
            ferme = true;
            if (enCours != null) {
                enCours.annuler();
                enCours = null;
            }
            sites.clear();
            auditeurs.clear();
        }
        session.removeEditListener(ecouteur);
    }

    /** L'édition supprime le travail en vol et reprogramme sur le plus récent. */
    private void surEdition(int start, int end, String removed, String inserted) {
        Runnable tache;
        synchronized (this) {
            if (ferme) {
                return;
            }
            Document apres = session.document();
            Document avant = documentSuivi == null ? apres : documentSuivi;
            // B-06 : la base reste publieDoc tant qu'aucune publication
            // n'a eu lieu (publierSiCourant seul la change, en vidant
            // les sites) — les sites en attente chaînent de publieDoc
            // au document courant, chacun dérivé de son avant, le
            // documentSuivi qui avance à chaque frappe. La chaîne se
            // coupe seulement par cheminComplet (rejeu multi-pas).
            if (publishedDoc != null && !cheminComplet) {
                EditSite site = EditSite.of(avant, apres, start, end, inserted);
                // B-06 : la frappe rapide FUSIONNE ses sites — le
                // lexing déjà fait sert de base, la plage cumulée se
                // relit en une passe. La voie complète ne se reprend
                // QUE si la base publiée a changé de document (rejeu
                // d'annulation multi-pas) — jamais pour des sites qui
                // s'enveloppent proprement.
                EditSite dernier = sites.peekLast();
                if (dernier == null) {
                    sites.add(site);
                } else {
                    sites.add(dernier.enveloppe(site));
                }
            } else {
                cheminComplet = true;
                sites.clear();
            }
            documentSuivi = apres;
            target = apres;
            tache = programmer();
        }
        executor.execute(tache);
    }

    /** Annule le jeton en vol et en pose un neuf ; appelé sous moniteur. */
    private Runnable programmer() {
        if (enCours != null) {
            enCours.annuler();
        }
        TokenCancellation token = new TokenCancellation();
        enCours = token;
        return new Task(token);
    }

    /** Publication : ne remplace que si le jeton est encore le courant (PIEGE-20). */
    private void publierSiCourant(TokenCancellation token, Analysis analyse) {
        java.util.List<java.util.function.Consumer<Analysis>> aPrevenir;
        synchronized (this) {
            if (ferme || enCours != token) {
                return;
            }
            published = analyse;
            publishedDoc = target;
            cheminComplet = false;
            sites.clear();
            aPrevenir = new ArrayList<>(auditeurs);
        }
        for (java.util.function.Consumer<Analysis> auditeur : aPrevenir) {
            auditeur.accept(analyse);
        }
    }

    /** Le travail d'un passage : incrément, fenêtre, puis complétion par lots. */
    private final class Task implements Runnable {

        private final TokenCancellation token;

        private Task(TokenCancellation token) {
            this.token = token;
        }

        @Override
        public void run() {
            Document doc;
            Analysis base;
            EditSite site;
            int premiere;
            int derniere;
            synchronized (AnalysisEngine.this) {
                if (ferme || enCours != token) {
                    return;
                }
                doc = target;
                base = cheminComplet ? null : published;
                // B-06 : les sites cumulés s'enveloppent — la
                // DERNIÈRE couvre toutes les frappes en attente, les
                // précédentes n'en sont que des préfixes.
                site = cheminComplet ? null : sites.pollLast();
                premiere = windowFirst;
                derniere = windowLast;
            }
            if (doc == null || (base != null && site == null)) {
                return;
            }
            try {
                Analysis resultat;
                if (base != null && site != null) {
                    resultat = service.analyseIncremental(doc, base, site);
                } else {
                    resultat = service.analyzeWindow(doc, premiere, derniere, token);
                }
                publierSiCourant(token, resultat);
                while (!resultat.isComplete()) {
                    token.check();
                    resultat = service.completer(doc, resultat, token, LOT);
                    publierSiCourant(token, resultat);
                }
            } catch (AnalysisCancelled annulation) {
                // Supplantée ou fermée : le silence est la réponse — le
                // jeton a cédé sa place, la dernière vérification le dit.
                token.isCancelled();
            }
        }
    }
}
