package jo.celeris.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.sample.LspSessionMount.Journal;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.grammars.PackGrammars;

/**
 * L'activité unique de l'exemple : le montage du § 4.2 d'USAGE sur
 * une scène VRAIE — le retour terrain du 2026-11-19 : « utiliser de
 * vrais layout pour le demo, qu'il soit plus moderne ». Tout le
 * chrome vit en XML Material 3 (activite_principale.xml) : le
 * tiroir DrawerLayout + NavigationView porte les échantillons du
 * dépôt, la MaterialToolbar suit l'onglet actif, la TabLayout porte
 * les fichiers ouverts (fermables par leur croix), le journal vit
 * en panneau bas repliable. L'activité gonfle, branche, vit — le
 * code ne monte plus une seule vue à la main, il ne reste que le
 * montage du § 4.2 : la façade attachée à la scène.
 */
public final class MainActivity extends Activity {

    /** Le chargeur partagé — un seul assemblage par processus. */
    private static volatile LanguageKit langages;

    private CelerisEditor editor;
    private JournalPanneau journal;
    /** Les fichiers ouverts, dans l'ordre des onglets. */
    private final List<Samples.Sample> ouverts = new ArrayList<>();
    private int ongletActif = -1;
    private DrawerLayout drawerRoot;
    private NavigationView navigationDrawer;
    private MaterialToolbar barreOutils;
    private TabLayout barreOnglets;
    private boolean journalReplie;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activite_principale);

        drawerRoot = findViewById(R.id.tiroir_racine);
        navigationDrawer = findViewById(R.id.tiroir_navigation);
        barreOutils = findViewById(R.id.barre_outils);
        barreOnglets = findViewById(R.id.barre_onglets);
        EditorView vue = findViewById(R.id.vue_editeur);
        PopupLayer couche = findViewById(R.id.couche_popups);

        journal = new JournalPanneau(
                findViewById(R.id.lignes_journal),
                findViewById(R.id.defileur_journal),
                findViewById(R.id.panneau_journal));
        LanguageKit kit = mountKit();
        editor = CelerisEditor.attach(vue, couche, kit);
        editor.settings().setTheme(EditorThemes.DARK);
        editor.settings().setFontSizeSp(14f);

        mountDrawer();
        mountJournal();
        journal.line("montage prêt — l'assemblage court en fil démon");
        // L'ouverture de départ : le premier échantillon du drawer.
        open(Samples.tous().get(0));
        probeLanguage(kit);
    }

    /** Monte le chargeur de paquets — l'assemblage ENTIER part sur
     *  le fil démon, le fil de l'interface ne l'attend jamais. */
    private LanguageKit mountKit() {
        LanguageKit kit = langages;
        if (kit == null) {
            kit = LanguageKit.builder(LanguageKit.sharedDaemon())
                    .grammarFactory(PackGrammars.INSTANCE)
                    .builtinPacks()
                    .onError(erreur -> journal.line(
                            "PAQUET FAUTIF : " + erreur.getMessage()))
                    .build();
            langages = kit;
            kit.preload();
        }
        return kit;
    }

    /** Journalise l'arrivée de la langue du fichier ouvert — la
     *  sonde HONNÊTE : tant que le registre s'assemble, elle PATIENTE
     *  (les 33 paquets prennent jusqu'à une minute sur un appareil
     *  lent — l'abandon à trois secondes du retour terrain du
     *  2026-11-19 journalisait un mensonge : « le registre n'a pas
     *  résolu Salut.java » alors que l'assemblage courait encore) ;
     *  le verdict ne tombe QUE quand le registre vit vraiment. */
    private void probeLanguage(LanguageKit kit) {
        String nom = ouverts.get(ongletActif).nomFichier();
        if (!kit.estPret()) {
            getWindow().getDecorView().postDelayed(
                    () -> probeLanguage(kit), 500L);
            return;
        }
        LanguageSupport language = kit.languageForSiPret(nom);
        journal.line(language == null
                ? "langue : " + nom + " n'a pas de langage dans le registre"
                : "langue résolue : " + language.displayName()
                        + " — coloration et mots réservés vivent");
    }

    /** Ouvre un échantillon : son onglet naît (ou se rappelle), le
     *  document se charge, le mini serveur se branche au .java. */
    private void open(Samples.Sample sample) {
        int connu = ouverts.indexOf(sample);
        if (connu >= 0) {
            ongletActif = connu;
        } else {
            ouverts.add(sample);
            ongletActif = ouverts.size() - 1;
        }
        editor.open(sample.nomFichier(), sample.text());
        journal.line("ouvert : " + sample.nomFichier());
        mountLsp(sample, "mini serveur LSP branché");
        refreshTabs();
    }

    /** Branche le mini serveur LSP d'un échantillon .java — le
     *  geste partagé de l'ouverture et de l'activation d'onglet. */
    private void mountLsp(Samples.Sample sample, String message) {
        if (!sample.nomFichier().endsWith(".java")) {
            return;
        }
        LspSessionMount.place(this, editor, sample.nomFichier(),
                journalLsp(message));
    }

    /** Le geste de pose LSP qui journalise sa réussite ET son échec
     *  (l'ancien chemin silencieux tuait le serveur sans trace). */
    private Journal journalLsp(String message) {
        return new Journal() {

            @Override
            public void run() {
                journal.line(message);
            }

            @Override
            public void echec(String cause) {
                journal.line("pose LSP échouée : " + cause);
            }
        };
    }

    /** Copie le journal entier au presse-papiers système. */
    private void copierJournal() {
        Object service = getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText(
                    "journal celeris", journal.text()));
            journal.line("journal copié au presse-papiers");
        }
    }

    @Override
    protected void onDestroy() {
        if (editor != null) {
            editor.close();
            editor = null;
        }
        super.onDestroy();
    }

    // ── Le tiroir des échantillons (NavigationView de la gauche) ──

    /** Branche le tiroir : le menu se pose depuis les échantillons,
     *  le hamburger de la barre d'outils l'ouvre. */
    private void mountDrawer() {
        android.view.Menu menu = navigationDrawer.getMenu();
        menu.removeGroup(SAMPLES_GROUP);
        for (int i = 0; i < Samples.tous().size(); i++) {
            Samples.Sample sample =
                    Samples.tous().get(i);
            menu.add(SAMPLES_GROUP, MenuIds.sample(i),
                    i, sample.nomFichier());
        }
        navigationDrawer.setNavigationItemSelectedListener(item -> {
            int i = MenuIds.position(item.getItemId());
            drawerRoot.closeDrawer(androidx.core.view.GravityCompat.START);
            if (i >= 0 && i < Samples.tous().size()) {
                open(Samples.tous().get(i));
            }
            return true;
        });
        barreOutils.setNavigationOnClickListener(
                v -> drawerRoot.openDrawer(
                        androidx.core.view.GravityCompat.START));
        // L'écouteur des onglets : UN seul, posé ICI au montage —
        // jamais empilé par les rafraîchissements (le retour terrain
        // du 2026-11-19 : chaque pression d'onglet redéclenchait
        // l'activation par autant d'écouteurs empilés).
        barreOnglets.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab onglet) {
                        activerOnglet(onglet.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab onglet) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab onglet) {
                    }
                });
    }

    /** Le groupe du menu du tiroir — les identifiants stables des
     *  échantillons (position + base, jamais View.generateViewId). */
    private static final int SAMPLES_GROUP = 1;
    private static final int SAMPLE_BASE_ID = 1000;

    /** Les identifiants du menu du tiroir : un par échantillon,
     *  l'aller-retour position ↔ identifiant vit ici. */
    private static final class MenuIds {

        private MenuIds() {
        }

        static int sample(int position) {
            return SAMPLE_BASE_ID + position;
        }

        static int position(int identifier) {
            return identifier - SAMPLE_BASE_ID;
        }
    }

    // ── La barre d'onglets (TabLayout du haut) ────────────────────

    /** Re-pose les onglets de la barre : la sélection suit l'actif,
     *  la croix ferme sans activer. L'ÉCOUTEUR vit au montage
     *  (monterTiroir) — il ne se pose PLUS ici : l'ancienne pose à
     *  chaque rafraîchissement EMPILOIT les écouteurs, chaque
     *  pression d'onglet redéclenchait l'activation autant de fois
     *  que d'écouteurs survivants (le bug « ça bug quand j'appuie
     *  sur l'onglet inactif » du retour terrain du 2026-11-19). */
    private void refreshTabs() {
        barreOnglets.removeAllTabs();
        for (int i = 0; i < ouverts.size(); i++) {
            TabLayout.Tab onglet = barreOnglets.newTab();
            mountTabView(onglet, i);
            barreOnglets.addTab(onglet, i == ongletActif);
        }
        Samples.Sample actif = ouverts.get(ongletActif);
        barreOutils.setTitle(actif.nomFichier());
    }

    /** Pose la vue d'un onglet : le nom et sa croix de fermeture. */
    private void mountTabView(TabLayout.Tab onglet, int indice) {
        View vue = LayoutInflater.from(this)
                .inflate(R.layout.vue_onglet, barreOnglets, false);
        TextView nom = vue.findViewById(R.id.nom_onglet);
        nom.setText(ouverts.get(indice).nomFichier());
        nom.setTextColor(indice == ongletActif
                ? getColor(R.color.accent_cyan_clair)
                : getColor(R.color.texte_secondaire));
        TextView croix = vue.findViewById(R.id.croix_onglet);
        croix.setTextColor(getColor(R.color.texte_secondaire));
        croix.setOnClickListener(v -> closeTab(indice));
        onglet.setCustomView(vue);
    }

    /** Ferme un onglet : le suivant prend la place par activation —
     *  le dernier vivant ne meurt pas. */
    private void closeTab(int indice) {
        if (ouverts.size() <= 1) {
            journal.line("le dernier onglet ouvert ne se ferme pas");
            return;
        }
        ouverts.remove(indice);
        journal.line("fermé — actif : "
                + ouverts.get(Math.min(indice, ouverts.size() - 1)).nomFichier());
        // L'activation porte TOUT le rebranchement (document, serveur,
        // onglets) : -1 force l'activation même si l'indice ne bouge pas.
        ongletActif = -1;
        activerOnglet(Math.min(indice, ouverts.size() - 1));
    }

    /** Active un onglet : le document se rouvre, le .java rebranche
     *  son serveur, la barre d'outils suit le titre. */
    private void activerOnglet(int indice) {
        if (indice == ongletActif) {
            return;
        }
        ongletActif = indice;
        Samples.Sample sample = ouverts.get(indice);
        editor.open(sample.nomFichier(), sample.text());
        mountLsp(sample, "mini serveur LSP rebranché");
        refreshTabs();
    }

    // ── Le journal (le panneau bas de la scène) ───────────────────

    /** Branche le journal : les deux boutons de son en-tête. */
    private void mountJournal() {
        ImageButton copier = findViewById(R.id.bouton_copier_journal);
        copier.setOnClickListener(v -> copierJournal());
        ImageButton replier = findViewById(R.id.bouton_replier_journal);
        replier.setOnClickListener(v -> toggleJournal());
    }

    /** Bascule le journal : replié, il ne garde que son en-tête. */
    private void toggleJournal() {
        journalReplie = !journalReplie;
        findViewById(R.id.defileur_journal).setVisibility(
                journalReplie ? View.GONE : View.VISIBLE);
        View panneau = findViewById(R.id.panneau_journal);
        ViewGroup.LayoutParams settings = panneau.getLayoutParams();
        settings.height = journalReplie
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : (int) (130f * getResources().getDisplayMetrics().density);
        panneau.setLayoutParams(settings);
        ImageButton bouton = findViewById(R.id.bouton_replier_journal);
        bouton.setContentDescription(getString(journalReplie
                ? R.string.action_deplier
                : R.string.action_replier));
    }

    /** Le panneau du journal : horodaté, repliable, copiable — ses
     *  vues vivent dans le layout, la classe ne fait que servir. */
    private static final class JournalPanneau {

        private final TextView lines;
        private final ScrollView defileur;
        private final View panneau;
        private final StringBuilder text = new StringBuilder();

        JournalPanneau(TextView lines, ScrollView defileur, View panneau) {
            this.lines = lines;
            this.defileur = defileur;
            this.panneau = panneau;
            lines.setTypeface(Typeface.MONOSPACE);
        }

        void line(String message) {
            String horodatage = new SimpleDateFormat("HH:mm:ss.SSS",
                    Locale.ROOT).format(new Date());
            text.append('[').append(horodatage).append("] ")
                    .append(message).append('\n');
            lines.post(() -> {
                lines.setText(text.toString());
                if (defileur != null) {
                    defileur.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        String text() {
            return text.toString();
        }
    }
}
