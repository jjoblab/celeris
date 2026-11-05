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
public final class MoteurAnalyse {

    /** Auditeur de publication : prévenu sur le fil de travail, par lot publié. */
    public interface Auditeur {

        /**
         * Un instantané d'analyse vient d'être publié.
         *
         * @param analyse l'analyse publiée, complète ou partielle
         */
        void analysePubliee(Analysis analyse);
    }

    /** Taille d'un lot de complétion entre deux publications. */
    private static final int LOT = 32;

    private final TextSession session;
    private final SyntaxService service;
    private final Executor executeur;
    private final EditListener ecouteur = this::surEdition;

    /** Auditeurs, sous moniteur. */
    private final List<Auditeur> auditeurs = new ArrayList<>();

    /** Sites d'édition en attente d'être rejoués par incrément (au plus un). */
    private final Deque<EditSite> sites = new ArrayDeque<>();

    private @Nullable JetonAnnulation enCours;
    private @Nullable Document cible;
    private @Nullable Analysis publiee;
    private @Nullable Document publieDoc;
    private @Nullable Document documentSuivi;
    private int fenetrePremiere;
    private int fenetreDerniere;
    private boolean cheminComplet;
    private boolean ferme;

    private MoteurAnalyse(TextSession session, LanguageSyntax langue, Executor executeur,
                          int fenetrePremiere, int fenetreDerniere) {
        this.session = session;
        this.service = SyntaxService.of(langue);
        this.executeur = executeur;
        this.fenetrePremiere = fenetrePremiere;
        this.fenetreDerniere = fenetreDerniere;
        this.documentSuivi = session.document();
        session.addEditListener(ecouteur);
    }

    /**
     * Le moteur d'une session, fenêtre couvrant tout le document.
     *
     * @param session   la session suivie (fil d'écriture)
     * @param langue    le langage à analyser
     * @param executeur l'exécuteur du travail d'analyse (injecté, ADR-007)
     * @return le moteur abonné, travail non encore démarré
     */
    public static MoteurAnalyse sur(TextSession session, LanguageSyntax langue,
                                    Executor executeur) {
        return new MoteurAnalyse(session, langue, executeur, 0, Integer.MAX_VALUE / 2);
    }

    /**
     * Le moteur d'une session, fenêtre initiale donnée.
     *
     * @param session           la session suivie (fil d'écriture)
     * @param langue            le langage à analyser
     * @param executeur         l'exécuteur du travail d'analyse (injecté, ADR-007)
     * @param fenetrePremiere   la première ligne visible au démarrage
     * @param fenetreDerniere   la dernière ligne visible au démarrage
     * @return le moteur abonné, travail non encore démarré
     */
    public static MoteurAnalyse sur(TextSession session, LanguageSyntax langue,
                                    Executor executeur, int fenetrePremiere,
                                    int fenetreDerniere) {
        return new MoteurAnalyse(session, langue, executeur,
                fenetrePremiere, fenetreDerniere);
    }

    /**
     * Indique la fenêtre visible au prochain passage complet : le
     * travail en vol ne s'annule pas, le défilement ne coûte rien.
     *
     * @param premiereLigne la première ligne visible
     * @param derniereLigne la dernière ligne visible
     */
    public synchronized void fenetre(int premiereLigne, int derniereLigne) {
        this.fenetrePremiere = premiereLigne;
        this.fenetreDerniere = derniereLigne;
    }

    /**
     * Programme l'analyse de l'instantané courant par la voie complète :
     * fenêtre d'abord, complétion par lots ensuite.
     */
    public void reanalyser() {
        Runnable tache;
        synchronized (this) {
            if (ferme) {
                return;
            }
            cible = session.document();
            documentSuivi = cible;
            cheminComplet = true;
            sites.clear();
            tache = programmer();
        }
        executeur.execute(tache);
    }

    /**
     * Le dernier instantané publié — null tant que rien n'est publié.
     *
     * @return l'analyse publiée, complète ou partielle
     */
    public synchronized @Nullable Analysis analysePubliee() {
        return publiee;
    }

    /**
     * S'abonne aux publications.
     *
     * @param auditeur l'auditeur à prévenir sur le fil de travail
     */
    public synchronized void ajouterAuditeur(Auditeur auditeur) {
        auditeurs.add(auditeur);
    }

    /** Se désabonne des publications. */
    public synchronized void retirerAuditeur(Auditeur auditeur) {
        auditeurs.remove(auditeur);
    }

    /** Détache l'auditeur de session, annule le travail en vol : tout s'arrête. */
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
            Document avant = documentSuivi;
            Document apres = session.document();
            if (publieDoc != null && publieDoc == avant && !cheminComplet
                    && sites.isEmpty()) {
                sites.add(EditSite.of(avant, apres, start, end, inserted));
            } else {
                cheminComplet = true;
                sites.clear();
            }
            documentSuivi = apres;
            cible = apres;
            tache = programmer();
        }
        executeur.execute(tache);
    }

    /** Annule le jeton en vol et en pose un neuf ; appelé sous moniteur. */
    private Runnable programmer() {
        if (enCours != null) {
            enCours.annuler();
        }
        JetonAnnulation jeton = new JetonAnnulation();
        enCours = jeton;
        return new Tache(jeton);
    }

    /** Publication : ne remplace que si le jeton est encore le courant (PIEGE-20). */
    private void publierSiCourant(JetonAnnulation jeton, Analysis analyse) {
        Auditeur[] aPrevenir;
        synchronized (this) {
            if (ferme || enCours != jeton) {
                return;
            }
            publiee = analyse;
            publieDoc = cible;
            cheminComplet = false;
            sites.clear();
            aPrevenir = auditeurs.toArray(new Auditeur[0]);
        }
        for (Auditeur auditeur : aPrevenir) {
            auditeur.analysePubliee(analyse);
        }
    }

    /** Le travail d'un passage : incrément, fenêtre, puis complétion par lots. */
    private final class Tache implements Runnable {

        private final JetonAnnulation jeton;

        private Tache(JetonAnnulation jeton) {
            this.jeton = jeton;
        }

        @Override
        public void run() {
            Document doc;
            Analysis base;
            EditSite site;
            int premiere;
            int derniere;
            synchronized (MoteurAnalyse.this) {
                if (ferme || enCours != jeton) {
                    return;
                }
                doc = cible;
                base = cheminComplet ? null : publiee;
                site = cheminComplet ? null : sites.poll();
                premiere = fenetrePremiere;
                derniere = fenetreDerniere;
            }
            if (doc == null || (base != null && site == null)) {
                return;
            }
            try {
                Analysis resultat;
                if (base != null && site != null) {
                    resultat = service.analyseIncremental(doc, base, site);
                } else {
                    resultat = service.analyseFenetre(doc, premiere, derniere, jeton);
                }
                publierSiCourant(jeton, resultat);
                while (!resultat.estComplet()) {
                    jeton.verifier();
                    resultat = service.completer(doc, resultat, jeton, LOT);
                    publierSiCourant(jeton, resultat);
                }
            } catch (AnalyseAnnulee annulation) {
                // Supplantée ou fermée : le silence est la réponse — le
                // jeton a cédé sa place, la dernière vérification le dit.
                jeton.estAnnule();
            }
        }
    }
}
