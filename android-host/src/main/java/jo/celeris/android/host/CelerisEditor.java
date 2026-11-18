package jo.celeris.android.host;

import java.util.Objects;

import jo.celeris.android.host.popups.PopupsEditeur;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
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

    /** Les réglages de l'éditeur (T2, phase 1) : le thème et la
     *  taille s'y posent une fois, la vue et les popups suivent. */
    public final class Settings {

        private EditorTheme theme = EditorThemes.CLAIR;
        private float fontSizeSp = 14f;

        private Settings() {
        }

        /**
         * Pose le thème : la vue le porte, l'orchestrateur redérive
         * sa palette — UN appel, les deux suivent.
         *
         * @param nouveau le thème résolu
         */
        public void setTheme(EditorTheme nouveau) {
            this.theme = Objects.requireNonNull(nouveau);
            vue.setTheme(nouveau);
            PopupsEditeur orchestrateur = popups;
            if (orchestrateur != null) {
                orchestrateur.surTheme(nouveau);
            }
        }

        /** Le thème courant. */
        public EditorTheme theme() {
            return theme;
        }

        /**
         * Pose la taille de police, en sp : la vue l'applique, les
         * popups redérivent leur typographie — le pincement sur la
         * vue rabat les popups ici aussi (l'écouteur de la façade).
         *
         * @param tailleSp la taille, en sp
         */
        public void setFontSizeSp(float tailleSp) {
            this.fontSizeSp = tailleSp;
            vue.setFont(new FontSpec("mono", tailleSp));
            PopupsEditeur orchestrateur = popups;
            if (orchestrateur != null) {
                orchestrateur.surTailleTexte(tailleSp);
            }
        }

        /** La taille de police courante, en sp. */
        public float fontSizeSp() {
            return fontSizeSp;
        }
    }

    private final EditorView vue;
    private final PopupLayer couche;
    private final LanguageKit langues;
    private final Settings settings = new Settings();
    private @Nullable PopupsEditeur popups;
    private @Nullable TextSession session;
    private @Nullable LanguageSupport langue;
    private boolean fermee;

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
        // Le pincement de la vue rabat les popups : la taille reste
        // UNE source (Settings), le geste suit.
        vue.setOnFontSizeChanged(taille -> {
            settings.fontSizeSp = taille;
            PopupsEditeur orchestrateur = popups;
            if (orchestrateur != null) {
                orchestrateur.surTailleTexte(taille);
            }
        });
        // Les réglages posés dès le montage : la vue ET l'orchestrateur
        // à venir lisent la même source.
        settings.setTheme(settings.theme);
        settings.setFontSizeSp(settings.fontSizeSp);
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
            popups = new PopupsEditeur(vue, session, couche,
                    settings.theme(), settings.fontSizeSp());
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
