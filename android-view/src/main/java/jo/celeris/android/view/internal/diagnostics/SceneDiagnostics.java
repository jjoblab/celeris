package jo.celeris.android.view.internal.diagnostics;

import jo.celeris.android.view.internal.exec.AnalysisExecutor;
import jo.celeris.android.view.internal.exec.RetainedExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import jo.celeris.android.view.internal.access.EditorAccess;
import jo.celeris.android.view.internal.accessibility.AccessibilityHost;

import jo.celeris.android.view.internal.exec.RetainedExecutor.Delayer;

import org.jspecify.annotations.Nullable;

/**
 * Le collaborateur de diagnostic de scène (M7.5, §9 — livré) : la
 * retenue d'analyse (le réglage de lenteur), le compteur d'images
 * décolorées et les lectures de vérité — taille de police, largeur de
 * gouttière — que la surimpression de l'hôte affiche en direct.
 *
 * <p>Vue-collaborateur par délégation (même famille qu'
 * {@link AccessibilityHost}) : la vue reste la racine de composition
 * et expose les accès publics minces, la mécanique vit ici.</p>
 *
 * <p>Les ressources du kit — le fil d'analyse et son minuteur —
 * s'éteignent à la libération (chantier C2) : l'exécuteur que l'hôte
 * a injecté reste le sien, jamais touché.</p>
 */
public final class SceneDiagnostics {

    private final EditorAccess acces;
    /** L'exécuteur RÉSOLU du travail d'analyse : la retenue par
     *  défaut, ou celui que l'hôte a injecté — tel quel. */
    private final Executor executor;
    /** L'exécuteur RETENU du travail d'analyse — null quand l'hôte a
     *  injecté le sien : le réglage ne détourne jamais un exécuteur
     *  étranger. */
    private final @Nullable RetainedExecutor retenue;
    /** Le fil d'analyse du kit (celeris-analyse) — null quand l'hôte
     *  a injecté le sien ; éteint à la libération (chantier C2). */
    private final @Nullable ExecutorService filAnalyse;
    /** Le minuteur de la retenue du kit (celeris-analyse-retenu) —
     *  null quand l'hôte a injecté le sien ; éteint pareillement. */
    private final @Nullable Delayer minuteur;
    /** Le compteur d'images décolorées : une image y entre quand une
     *  rangée qui AVAIT sa couleur trace à l'encre de base. */
    private long imagesDecolorees;

    /**
     * Construit le diagnostic de scène — l'exécuteur RETENU s'y crée
     * quand l'hôte n'a pas injecté le sien (M7.5, §9 : la retenue par
     * défaut, jamais un détour d'exécuteur étranger).
     *
     * @param vue              la vue propriétaire de la scène
     * @param executeurInjecte l'exécuteur du travail d'analyse injecté
     *                        par l'hôte, null pour la retenue par défaut
     */
    public SceneDiagnostics(EditorAccess acces, @Nullable Executor executeurInjecte) {
        this.acces = acces;
        if (executeurInjecte == null) {
            ExecutorService thread = AnalysisExecutor.parDefaut();
            Delayer horloge = RetainedExecutor.daemonDelayer();
            this.filAnalyse = thread;
            this.minuteur = horloge;
            this.retenue = new RetainedExecutor(thread, horloge,
                    EditorAccess.DELAI_RETENU_PAR_DEFAUT_MS);
            this.executor = retenue;
        } else {
            this.filAnalyse = null;
            this.minuteur = null;
            this.retenue = null;
            this.executor = executeurInjecte;
        }
    }

    /**
     * L'exécuteur du travail d'analyse : la retenue par défaut, ou
     * celui que l'hôte a injecté — tel quel.
     */
    public Executor executor() {
        return executor;
    }

    /**
     * Éteint les ressources du kit (chantier C2) : le fil d'analyse et
     * son minuteur meurent — l'exécuteur injecté par l'hôte n'est
     * jamais touché, la libération ne ferme que ce que le kit a ouvert.
     * Idempotent.
     */
    public void shutdown() {
        ExecutorService thread = filAnalyse;
        Delayer horloge = minuteur;
        if (thread != null) {
            thread.shutdownNow();
        }
        if (horloge != null) {
            horloge.shutdown();
        }
    }

    /**
     * Le réglage de lenteur d'analyse (M7.5, §9) : le délai de retenue
     * de la publication, en millisecondes — zéro rend la main
     * immédiate. L'hôte qui a injecté son propre exécuteur n'est
     * jamais détourné : l'appel s'ignore.
     *
     * @param delaiMs le délai de retenue, en millisecondes
     */
    public void setRetenueMs(long delaiMs) {
        RetainedExecutor locale = retenue;
        if (locale != null) {
            locale.setDelayMs(delaiMs);
        }
    }

    /** Le délai de retenue courant — zéro quand la main est immédiate. */
    public long retenueMs() {
        RetainedExecutor locale = retenue;
        return locale == null ? 0L : locale.delayMs();
    }

    /** La taille de police courante, en sp. */
    public float fontSizeSp() {
        return acces.policeLogique().size();
    }

    /** La largeur de gouttière à l'image courante, en pixels. */
    public float gutterWidth() {
        acces.preparerEtatImage();
        return acces.largeurGouttiere();
    }

    /**
     * Le nombre d'images décolorées depuis l'ouverture : le rattrapage
     * PIEGE-26 le garde à zéro, la surimpression le prouve en direct.
     */
    public long discoloredFrames() {
        return imagesDecolorees;
    }

    /**
     * L'image qui vient de se tracer : celle qui a tracé une rangée
     * sans couleur qu'elle possédait se compte (M7.5, §9).
     */
    public void surImageTracee() {
        if (acces.etatImage().imageDecoloree()) {
            imagesDecolorees++;
        }
    }

    /** Retombe à zéro : changement de session ou de langage. */
    public void reinitialiser() {
        imagesDecolorees = 0L;
    }
}
