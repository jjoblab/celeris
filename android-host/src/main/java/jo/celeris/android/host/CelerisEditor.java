package jo.celeris.android.host;


import jo.celeris.android.host.popups.PopupsEditeur;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorSettings;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.IntelligenceHub;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La façade de montage (ADR-0022, phase 1) : UN objet compose la vue,
 * les popups, la saisie intelligente et la langue détectée — le
 * développeur hôte n'écrit plus les quinze démarches de la revue, il
 * écrit le montage et vit avec.
 *
 * <pre>{@code
 * CelerisEditor editor = CelerisEditor.attach(editorView, popupLayer, kit);
 * editor.open("Main.java", text);                    // session + langue
 * editor.settings().setTheme(EditorThemes.SOMBRE);   // vue ET popups
 * editor.settings().setFontSizeSp(14f);              // idem, suit le pincement
 * editor.close();                                    // libération entière
 * }</pre>
 *
 * <p>Les valeurs par défaut sont utiles : la frappe intelligente
 * (`IntelligenceHub.builtins()`) vit dès le montage — l'hôte qui n'en
 * veut pas appelle {@link Builder#withoutSmartTyping()} ; le coach
 * des popups est muet ; les déclencheurs du survol (double appui) et
 * la délégation clavier des popups sont branchés. Le thème et la
 * taille se posent en UN endroit ({@link Settings}) : la façade
 * synchronise la vue et l'orchestrateur — la phase 2 (ADR-0022)
 * remplacera cette compensation par une source unique dans les
 * composants eux-mêmes.</p>
 *
 * <p>Fin de vie : {@link #close()} détache l'orchestrateur (ses
 * écouteurs de session se retirent, chantier C1), libère la vue (ses
 * écouteurs partent, ses fils d'analyse s'éteignent, chantiers C1/C2)
 * et rend la façade muette — idempotent. Le détachement de la fenêtre
 * n'est pas une libération ; l'hôte appelle close() quand la scène
 * meurt.</p>
 */
public final class CelerisEditor {

    /** Le monte-charge de la façade : les choix utiles s'y posent
     *  avant le montage. */
    public static final class Builder {

        private final EditorView view;
        private final PopupLayer layer;
        private final LanguageKit languages;
        private boolean smartTyping = true;

        private Builder(EditorView view, PopupLayer layer,
                LanguageKit languages) {
            this.view = view;
            this.layer = layer;
            this.languages = languages;
        }

        /**
         * Coupe la frappe intelligente par défaut (T4) : l'hôte qui
         * posera son propre fournisseur plus tard, ou qui n'en veut
         * pas, monte la façade muette.
         *
         * @return le monte-charge
         */
        public Builder withoutSmartTyping() {
            this.smartTyping = false;
            return this;
        }

        /**
         * Monte la façade : l'intelligence, les déclencheurs et les
         * réglages se posent, l'orchestrateur naît muet au coach près.
         *
         * @return la façade prête à ouvrir
         */
        public CelerisEditor build() {
            return new CelerisEditor(view, layer, languages, smartTyping);
        }
    }

    /**
     * Monte la façade sur la vue et la couche posées (le gabarit § 4.1
     * d'USAGE) avec le chargeur de langues du kit.
     *
     * @param view      la vue d'éditeur du gabarit
     * @param layer     la couche porte-popups du gabarit
     * @param languages le chargeur de langues partagé (T5) — le même
     *                  sert plusieurs éditeurs
     * @return la façade prête à ouvrir
     */
    public static CelerisEditor attach(EditorView view, PopupLayer layer,
            LanguageKit languages) {
        return new Builder(view, layer, languages).build();
    }

    /** Les réglages de l'éditeur (ADR-0022, phase 2, T2) : une
     *  VUE MINCE sur la source unique réelle de la vue —
     *  {@link EditorSettings}. Chaque pose y passe, la vue applique
     *  (thème, métriques), l'orchestrateur des popups suit par son
     *  abonnement à la même source : plus de compensation à double
     *  appel, la façade ne fait que déléguer. */
    public final class Settings {

        private final EditorSettings source;

        private Settings(EditorSettings source) {
            this.source = source;
        }

        /**
         * Pose le thème : la source unique l'entend, la vue applique,
         * l'orchestrateur redérive sa palette — UN appel, les deux
         * suivent, plus de double synchronisation.
         *
         * @param nouveau le thème résolu
         */
        public void setTheme(EditorTheme nouveau) {
            source.setTheme(nouveau);
        }

        /** Le thème courant. */
        public EditorTheme theme() {
            return source.theme();
        }

        /**
         * Pose la taille de police, en sp : la source unique
         * l'entend, la vue applique, l'orchestrateur redérive sa
         * typographie — le pincement sur la vue y rabat aussi (la
         * source reste la vérité, le geste écrit dedans).
         *
         * @param tailleSp la taille, en sp
         */
        public void setFontSizeSp(float tailleSp) {
            source.setFontSizeSp(tailleSp);
        }

        /** La taille de police courante, en sp. */
        public float fontSizeSp() {
            return source.fontSizeSp();
        }

        /** La source réelle — pour qui pose plus que le thème et la
         *  taille (retour de ligne, ligatures, portes).
         *
         * @return les réglages vivants de la vue
         */
        public EditorSettings source() {
            return source;
        }
    }

    private final EditorView vue;
    private final PopupLayer couche;
    private final LanguageKit langues;
    private final Settings settings;
    private @Nullable PopupsEditeur popups;
    private @Nullable TextSession session;
    private @Nullable LanguageSupport langue;
    private boolean fermee;
    // T4 : les déclencheurs posés par le kit — mémorisés pour le
    // retrait à close() (le montage de l'orchestrateur les pose, le
    // démontage les retire : jamais d'écouteur fantôme).
    private final java.util.function.IntPredicate verdictTouche =
            code -> {
                PopupsEditeur orchestrateur = popups;
                return orchestrateur != null && orchestrateur.surTouche(code);
            };
    private final java.util.function.IntPredicate verdictDoubleTap =
            offset -> {
                PopupsEditeur orchestrateur = popups;
                return orchestrateur != null
                        && (orchestrateur.ouvrirColorPicker(offset)
                                || orchestrateur.ouvrirSurvolAuDoubleTap(offset));
            };

    private CelerisEditor(EditorView vue, PopupLayer couche,
            LanguageKit langues, boolean frappeIntelligente) {
        this.vue = vue;
        this.couche = couche;
        this.langues = langues;
        // T4 : la frappe intelligente vit dès le montage — l'opt-out
        // existe (withoutSmartTyping), le fournisseur se remplace.
        if (frappeIntelligente) {
            vue.intelligence().setProvider(IntelligenceHub.builtins());
        }
        // T2 (ADR-0022, phase 2) : la source unique est celle de la
        // vue — la façade n'a plus de compensation à faire. Le
        // pincement y écrit aussi (ViewFonts notifie, l'orchestrateur
        // s'y abonne) : la vérité ne se dédouble jamais.
        settings = new Settings(vue.settings());
    }

    /**
     * Ouvre le document : la session naît du texte, la langue se
     * résout du nom de fichier (null sans langage : l'édition reste
     * entière, la coloration seule se tait) et se pose sur la vue ;
     * l'orchestrateur des popups se (re)branche sur la session vivante.
     * Sans effet après {@link #close()}.
     *
     * @param fileName le nom du fichier ouvert (langue détectée)
     * @param text     le texte initial du document
     */
    public void open(String fileName, String text) {
        if (fermee) {
            return;
        }
        TextSession precedente = session;
        session = TextSession.of(text, System::nanoTime);
        langue = langues.languageFor(fileName);
        vue.setSession(session);
        vue.setLanguage(langue);
        if (precedente != null) {
            // Le remplacement d'un document referme l'orchestrateur
            // courant : ses écouteurs de session partent (C1).
            if (popups != null) {
                popups.detacher();
                popups = null;
            }
        }
        if (popups == null) {
            // Le coach muet par défaut (T4, point 9 de la revue) ; le
            // constructeur court de l'orchestrateur suffit.
            // Le thème et la taille de la source (T2) : l'orchestrateur
            // les lit de toute façon à son abonnement — ici la palette
            // initiale en naît.
            popups = new PopupsEditeur(vue, session, couche,
                    settings.theme(), settings.fontSizeSp());
            // T4 : les déclencheurs du kit — la délégation clavier des
            // popups (PIEGE-27) et le survol au double appui (le color
            // picker d'abord). L'hôte n'écrit plus rien : ce que
            // l'intégration branchait à la main vit dans le montage.
            vue.addPopupKeyListener(verdictTouche);
            vue.addDoubleTapListener(verdictDoubleTap);
        }
        // Le nom du fichier : le filtre des diagnostics s'y fait.
        popups.setNomFichier(fileName);
    }

    /**
     * La session ouverte — null après {@link #close()} et avant le
     * premier {@link #open(String, String)}. L'hôte qui veut la
     * garder (ViewModel) la lit ici.
     *
     * @return la session vivante, ou null
     */
    public @Nullable TextSession session() {
        return session;
    }

    /**
     * La langue résolue du dernier fichier ouvert — null sans
     * langage.
     *
     * @return le support de langage posé, ou null
     */
    public @Nullable LanguageSupport language() {
        return langue;
    }

    /**
     * Les réglages de l'éditeur : LE point de pose du thème et de la
     * taille (vue et popups ensemble).
     *
     * @return les réglages vivants de la façade
     */
    public Settings settings() {
        return settings;
    }

    /**
     * L'orchestrateur des popups — l'accès de repli pour les réglages
     * avancés (serveur de langage, barre de sélection, § 9 d'USAGE) ;
     * la façade en garde la vie.
     *
     * @return l'orchestrateur, null après close()
     */
    public @Nullable PopupsEditeur popups() {
        return popups;
    }

    /**
     * Ferme l'éditeur : l'orchestrateur se détache (ses écouteurs de
     * session partent, C1), la vue se libère (ses écouteurs partent,
     * ses fils d'analyse s'éteignent, C1/C2), la façade devient
     * muette. Idempotent — le second appel ne fait rien.
     */
    public void close() {
        if (fermee) {
            return;
        }
        fermee = true;
        vue.removePopupKeyListener(verdictTouche);
        vue.removeDoubleTapListener(verdictDoubleTap);
        PopupsEditeur orchestrateur = popups;
        if (orchestrateur != null) {
            orchestrateur.detacher();
        }
        popups = null;
        vue.release();
        session = null;
        langue = null;
    }
}
