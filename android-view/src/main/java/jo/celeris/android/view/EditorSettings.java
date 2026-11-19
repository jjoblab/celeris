package jo.celeris.android.view;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Les réglages de l'éditeur, UNE source (ADR-0022, phase 2, T2 — le
 * point 14 de la revue du 2026-11-17 : les portes vivaient dans
 * {@link EditorSettingsPanel}, le thème et la taille dans la vue et le
 * constructeur de l'orchestrateur, jamais ensemble). Le thème, la
 * police, la taille, le retour à la ligne, les ligatures et les
 * portes de fonctionnalités y vivent : la vue applique, les popups
 * suivent, l'hôte écoute — plus de double pose, plus de
 * synchronisation à la main.
 *
 * <p>La table des portes {@link EditorSettingsPanel} reste le cœur des
 * commutateurs (ses clés STABLES servent les préférences persistées
 * de l'hôte) : {@link #gates()} la rend, les poseurs historiques
 * ({@link EditorSettingsPanel#place}) continuent d'y vivre — la source
 * unique les lit au même titre.</p>
 *
 * <p>Observable par PLUSIEURS abonnés (T3 le même geste) :
 * {@link #addListener} / {@link #removeListener} notifient chaque
 * abonné de CHAQUE changement — l'orchestrateur des popups, la carte
 * de réglages de l'hôte et sa persistance ensemble. La persistance
 * reste à l'hôte (via écouteur) ; la vue applique ce qui se pose
 * ici.</p>
 */
public final class EditorSettings {

    /** L'écouteur des changements de réglages — un changement, un
     *  avis, CHAQUE abonné. */
    public interface ChangeListener {

        /**
         * Un réglage a changé — l'abonné relit ce qui l'intéresse
         * (thème, taille, portes) sur la source.
         *
         * @param source les réglages changés
         */
        void onSettingsChanged(EditorSettings source);
    }

    /** La table des portes — le cœur des commutateurs. */
    private final EditorSettingsPanel portes;

    /** Le thème courant. */
    private EditorTheme theme = EditorThemes.CLAIR;

    /** La police logique, en sp. */
    private FontSpec font = EditorView.POLICE_PAR_DEFAUT;

    /** Le retour à la ligne souple. */
    private boolean lineWrap;

    /** Les ligatures du texte. */
    private boolean ligatures;

    /** Les abonnés des changements — multiples (T2/T3). */
    private final List<ChangeListener> abonnes =
            new CopyOnWriteArrayList<>();

    /** Construit la source aux valeurs PAR DÉFAUT — la vue la pose,
     *  l'hôte y écrit ensuite ses préférences. */
    public EditorSettings() {
        this.portes = new EditorSettingsPanel();
    }

    /**
     * La table des portes de fonctionnalités — les clés STABLES y
     * vivent, les poseurs historiques y continuent (la source unique
     * les lit).
     *
     * @return la table des portes, vivante
     */
    public EditorSettingsPanel gates() {
        return portes;
    }

    /**
     * Pose le thème — la vue l'applique, les abonnés en sont avisés.
     *
     * @param nouveau le thème résolu, jamais null
     */
    public void setTheme(EditorTheme nouveau) {
        Objects.requireNonNull(nouveau, "theme");
        this.theme = nouveau;
        notifyListeners();
    }

    /** Le thème courant. */
    public EditorTheme theme() {
        return theme;
    }

    /**
     * Pose la police (famille et taille, en sp) — la vue l'applique,
     * les abonnés en sont avisés.
     *
     * @param nouvelle la spécification de police, jamais null
     */
    public void setFont(FontSpec nouvelle) {
        Objects.requireNonNull(nouvelle, "police");
        this.font = nouvelle;
        notifyListeners();
    }

    /**
     * Pose la taille de police, en sp — la famille courante garde.
     *
     * @param tailleSp la taille, en sp
     */
    public void setFontSizeSp(float tailleSp) {
        setFont(new FontSpec(font.family(), tailleSp));
    }

    /** La police courante (famille et taille, en sp). */
    public FontSpec font() {
        return font;
    }

    /** La taille de police courante, en sp. */
    public float fontSizeSp() {
        return font.size();
    }

    /**
     * Pose le retour à la ligne souple — la vue l'applique, les
     * abonnés en sont avisés.
     *
     * @param actif vrai pour ouvrir le retour à la ligne
     */
    public void setSoftWrap(boolean actif) {
        this.lineWrap = actif;
        notifyListeners();
    }

    /** Le retour à la ligne souple courant. */
    public boolean softWrap() {
        return lineWrap;
    }

    /**
     * Pose les ligatures du texte — la vue les applique, les abonnés
     * en sont avisés.
     *
     * @param actives vrai pour ouvrir les ligatures
     */
    public void setLigatures(boolean actives) {
        this.ligatures = actives;
        notifyListeners();
    }

    /** Les ligatures courantes. */
    public boolean ligatures() {
        return ligatures;
    }

    /**
     * Bascule une porte de fonctionnalités ET avise les abonnés — le
     * geste de la carte de réglages sur la source unique (la voie
     * historique {@link EditorSettingsPanel#toggle} reste, elle
     * n'avise que l'écouteur 1.x).
     *
     * @param porte la porte à basculer
     * @return le nouvel état de la porte
     */
    public boolean toggleGate(EditorSettingsPanel.Porte porte) {
        boolean ouverte = portes.toggle(porte);
        notifyListeners();
        return ouverte;
    }

    /**
     * Abonne un écouteur des changements — chaque pose et chaque
     * bascule l'atteint.
     *
     * @param ecouteur l'écouteur, jamais null
     */
    public void addListener(ChangeListener ecouteur) {
        abonnes.add(Objects.requireNonNull(ecouteur));
    }

    /**
     * Désabonne un écouteur des changements.
     *
     * @param ecouteur l'écouteur retiré
     */
    public void removeListener(ChangeListener ecouteur) {
        abonnes.remove(ecouteur);
    }

    /** Avise CHAQUE abonné — un changement, un avis. */
    private void notifyListeners() {
        for (ChangeListener abonne : abonnes) {
            abonne.onSettingsChanged(this);
        }
    }
}
