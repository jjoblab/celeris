package jo.celeris.android.view;

import java.util.concurrent.Executor;

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
 */
final class SceneDiagnostics {

    private final EditorView vue;
    /** L'exécuteur RÉSOLU du travail d'analyse : la retenue par
     *  défaut, ou celui que l'hôte a injecté — tel quel. */
    private final Executor executeur;
    /** L'exécuteur RETENU du travail d'analyse — null quand l'hôte a
     *  injecté le sien : le réglage ne détourne jamais un exécuteur
     *  étranger. */
    private final @Nullable RetainedExecutor retenue;
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
    SceneDiagnostics(EditorView vue, @Nullable Executor executeurInjecte) {
        this.vue = vue;
        if (executeurInjecte == null) {
            this.retenue = new RetainedExecutor(ExecuteurAnalyse.parDefaut(),
                    RetainedExecutor.daemonDelayer(),
                    EditorView.DELAI_RETENU_PAR_DEFAUT_MS);
            this.executeur = retenue;
        } else {
            this.retenue = null;
            this.executeur = executeurInjecte;
        }
    }

    /**
     * L'exécuteur du travail d'analyse : la retenue par défaut, ou
     * celui que l'hôte a injecté — tel quel.
     */
    Executor executor() {
        return executeur;
    }

    /**
     * Le réglage de lenteur d'analyse (M7.5, §9) : le délai de retenue
     * de la publication, en millisecondes — zéro rend la main
     * immédiate. L'hôte qui a injecté son propre exécuteur n'est
     * jamais détourné : l'appel s'ignore.
     *
     * @param delaiMs le délai de retenue, en millisecondes
     */
    void setRetenueMs(long delaiMs) {
        RetainedExecutor locale = retenue;
        if (locale != null) {
            locale.setDelayMs(delaiMs);
        }
    }

    /** Le délai de retenue courant — zéro quand la main est immédiate. */
    long retenueMs() {
        RetainedExecutor locale = retenue;
        return locale == null ? 0L : locale.delayMs();
    }

    /** La taille de police courante, en sp. */
    float fontSizeSp() {
        return vue.policeLogique.size();
    }

    /** La largeur de gouttière à l'image courante, en pixels. */
    float gutterWidth() {
        vue.preparerEtatImage();
        return vue.largeurGouttiere;
    }

    /**
     * Le nombre d'images décolorées depuis l'ouverture : le rattrapage
     * PIEGE-26 le garde à zéro, la surimpression le prouve en direct.
     */
    long discoloredFrames() {
        return imagesDecolorees;
    }

    /**
     * L'image qui vient de se tracer : celle qui a tracé une rangée
     * sans couleur qu'elle possédait se compte (M7.5, §9).
     */
    void surImageTracee() {
        if (vue.etatImage.imageDecoloree()) {
            imagesDecolorees++;
        }
    }

    /** Retombe à zéro : changement de session ou de langage. */
    void reinitialiser() {
        imagesDecolorees = 0L;
    }
}
