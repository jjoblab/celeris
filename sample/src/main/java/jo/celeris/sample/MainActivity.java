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

    private CelerisEditor editeur;
    private JournalPanneau journal;
    /** Les fichiers ouverts, dans l'ordre des onglets. */
    private final List<Echantillons.Echantillon> ouverts = new ArrayList<>();
    private int ongletActif = -1;
    private DrawerLayout tiroirRacine;
    private NavigationView tiroirNavigation;
    private MaterialToolbar barreOutils;
    private TabLayout barreOnglets;
    private boolean journalReplie;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        setContentView(R.layout.activite_principale);

        tiroirRacine = findViewById(R.id.tiroir_racine);
        tiroirNavigation = findViewById(R.id.tiroir_navigation);
        barreOutils = findViewById(R.id.barre_outils);
        barreOnglets = findViewById(R.id.barre_onglets);
        EditorView vue = findViewById(R.id.vue_editeur);
        PopupLayer couche = findViewById(R.id.couche_popups);

        journal = new JournalPanneau(
                findViewById(R.id.lignes_journal),
                findViewById(R.id.defileur_journal),
                findViewById(R.id.panneau_journal));
        LanguageKit kit = monterKit();
        editeur = CelerisEditor.attach(vue, couche, kit);
        editeur.settings().setTheme(EditorThemes.SOMBRE);
        editeur.settings().setFontSizeSp(14f);

        monterTiroir();
        monterJournal();
        journal.ligne("montage prêt — l'assemblage court en fil démon");
        // L'ouverture de départ : le premier échantillon du drawer.
        ouvrir(Echantillons.tous().get(0));
        sonderLangue(kit, 0);
    }

    /** Monte le chargeur de paquets — l'assemblage ENTIER part sur
     *  le fil démon, le fil de l'interface ne l'attend jamais. */
    private LanguageKit monterKit() {
        LanguageKit kit = langages;
        if (kit == null) {
            kit = LanguageKit.builder(LanguageKit.sharedDaemon())
                    .grammarFactory(PackGrammars.INSTANCE)
                    .builtinPacks()
                    .onError(erreur -> journal.ligne(
                            "PAQUET FAUTIF : " + erreur.getMessage()))
                    .build();
            langages = kit;
            kit.preload();
        }
        return kit;
    }

    /** Journalise l'arrivée de la langue du fichier ouvert. */
    private void sonderLangue(LanguageKit kit, int tentative) {
        String nom = ouverts.get(ongletActif).nomFichier();
        LanguageSupport langue = kit.languageForSiPret(nom);
        if (langue != null || tentative > 15) {
            journal.ligne(langue == null
                    ? "langue : le registre n'a pas résolu " + nom
                    : "langue résolue : " + langue.displayName()
                            + " — coloration et mots réservés vivent");
            return;
        }
        getWindow().getDecorView().postDelayed(
                () -> sonderLangue(kit, tentative + 1), 200L);
    }

    /** Ouvre un échantillon : son onglet naît (ou se rappelle), le
     *  document se charge, le mini serveur se branche au .java. */
    private void ouvrir(Echantillons.Echantillon echantillon) {
        int connu = ouverts.indexOf(echantillon);
        if (connu >= 0) {
            ongletActif = connu;
        } else {
            ouverts.add(echantillon);
            ongletActif = ouverts.size() - 1;
        }
        editeur.open(echantillon.nomFichier(), echantillon.texte());
        journal.ligne("ouvert : " + echantillon.nomFichier());
        if (echantillon.nomFichier().endsWith(".java")) {
            SessionLspPoseuse.poser(this, editeur, echantillon.nomFichier(),
                    () -> journal.ligne("mini serveur LSP branché"));
        }
        rafraichirOnglets();
    }

    /** Copie le journal entier au presse-papiers système. */
    private void copierJournal() {
        Object service = getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText(
                    "journal celeris", journal.texte()));
            journal.ligne("journal copié au presse-papiers");
        }
    }

    @Override
    protected void onDestroy() {
        if (editeur != null) {
            editeur.close();
            editeur = null;
        }
        super.onDestroy();
    }

    // ── Le tiroir des échantillons (NavigationView de la gauche) ──

    /** Branche le tiroir : le menu se pose depuis les échantillons,
     *  le hamburger de la barre d'outils l'ouvre. */
    private void monterTiroir() {
        android.view.Menu menu = tiroirNavigation.getMenu();
        menu.removeGroup(GROUPE_ECHANTILLONS);
        for (int i = 0; i < Echantillons.tous().size(); i++) {
            Echantillons.Echantillon echantillon =
                    Echantillons.tous().get(i);
            menu.add(GROUPE_ECHANTILLONS, MenuIds.echantillon(i),
                    i, echantillon.nomFichier());
        }
        tiroirNavigation.setNavigationItemSelectedListener(item -> {
            int i = MenuIds.position(item.getItemId());
            tiroirRacine.closeDrawer(androidx.core.view.GravityCompat.START);
            if (i >= 0 && i < Echantillons.tous().size()) {
                ouvrir(Echantillons.tous().get(i));
            }
            return true;
        });
        barreOutils.setNavigationOnClickListener(
                v -> tiroirRacine.openDrawer(
                        androidx.core.view.GravityCompat.START));
    }

    /** Le groupe du menu du tiroir — les identifiants stables des
     *  échantillons (position + base, jamais View.generateViewId). */
    private static final int GROUPE_ECHANTILLONS = 1;
    private static final int BASE_ID_ECHANTILLON = 1000;

    /** Les identifiants du menu du tiroir : un par échantillon,
     *  l'aller-retour position ↔ identifiant vit ici. */
    private static final class MenuIds {

        private MenuIds() {
        }

        static int echantillon(int position) {
            return BASE_ID_ECHANTILLON + position;
        }

        static int position(int identifiant) {
            return identifiant - BASE_ID_ECHANTILLON;
        }
    }

    // ── La barre d'onglets (TabLayout du haut) ────────────────────

    /** Re-pose les onglets de la barre : la sélection suit l'actif,
     *  la croix ferme sans activer. */
    private void rafraichirOnglets() {
        barreOnglets.removeAllTabs();
        for (int i = 0; i < ouverts.size(); i++) {
            TabLayout.Tab onglet = barreOnglets.newTab();
            poserVueOnglet(onglet, i);
            barreOnglets.addTab(onglet, i == ongletActif);
        }
        Echantillons.Echantillon actif = ouverts.get(ongletActif);
        barreOutils.setTitle(actif.nomFichier());
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

    /** Pose la vue d'un onglet : le nom et sa croix de fermeture. */
    private void poserVueOnglet(TabLayout.Tab onglet, int indice) {
        View vue = LayoutInflater.from(this)
                .inflate(R.layout.vue_onglet, barreOnglets, false);
        TextView nom = vue.findViewById(R.id.nom_onglet);
        nom.setText(ouverts.get(indice).nomFichier());
        nom.setTextColor(indice == ongletActif
                ? getColor(R.color.accent_cyan_clair)
                : getColor(R.color.texte_secondaire));
        TextView croix = vue.findViewById(R.id.croix_onglet);
        croix.setTextColor(getColor(R.color.texte_secondaire));
        croix.setOnClickListener(v -> fermerOnglet(indice));
        onglet.setCustomView(vue);
    }

    /** Ferme un onglet : le suivant prend la place, l'éditeur rouvre
     *  l'onglet qui devient actif — le dernier vivant ne meurt pas. */
    private void fermerOnglet(int indice) {
        if (ouverts.size() <= 1) {
            journal.ligne("le dernier onglet ouvert ne se ferme pas");
            return;
        }
        ouverts.remove(indice);
        ongletActif = Math.min(indice, ouverts.size() - 1);
        Echantillons.Echantillon actif = ouverts.get(ongletActif);
        editeur.open(actif.nomFichier(), actif.texte());
        journal.ligne("fermé — actif : " + actif.nomFichier());
        if (actif.nomFichier().endsWith(".java")) {
            SessionLspPoseuse.poser(this, editeur, actif.nomFichier(),
                    () -> journal.ligne("mini serveur LSP rebranché"));
        }
        rafraichirOnglets();
    }

    /** Active un onglet : le document se rouvre, le .java rebranche
     *  son serveur, la barre d'outils suit le titre. */
    private void activerOnglet(int indice) {
        if (indice == ongletActif) {
            return;
        }
        ongletActif = indice;
        Echantillons.Echantillon echantillon = ouverts.get(indice);
        editeur.open(echantillon.nomFichier(), echantillon.texte());
        if (echantillon.nomFichier().endsWith(".java")) {
            SessionLspPoseuse.poser(this, editeur, echantillon.nomFichier(),
                    () -> journal.ligne("mini serveur LSP rebranché"));
        }
        rafraichirOnglets();
    }

    // ── Le journal (le panneau bas de la scène) ───────────────────

    /** Branche le journal : les deux boutons de son en-tête. */
    private void monterJournal() {
        ImageButton copier = findViewById(R.id.bouton_copier_journal);
        copier.setOnClickListener(v -> copierJournal());
        ImageButton replier = findViewById(R.id.bouton_replier_journal);
        replier.setOnClickListener(v -> basculerJournal());
    }

    /** Bascule le journal : replié, il ne garde que son en-tête. */
    private void basculerJournal() {
        journalReplie = !journalReplie;
        findViewById(R.id.defileur_journal).setVisibility(
                journalReplie ? View.GONE : View.VISIBLE);
        View panneau = findViewById(R.id.panneau_journal);
        ViewGroup.LayoutParams reglages = panneau.getLayoutParams();
        reglages.height = journalReplie
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : (int) (130f * getResources().getDisplayMetrics().density);
        panneau.setLayoutParams(reglages);
        ImageButton bouton = findViewById(R.id.bouton_replier_journal);
        bouton.setContentDescription(getString(journalReplie
                ? R.string.action_deplier
                : R.string.action_replier));
    }

    /** Le panneau du journal : horodaté, repliable, copiable — ses
     *  vues vivent dans le layout, la classe ne fait que servir. */
    private static final class JournalPanneau {

        private final TextView lignes;
        private final ScrollView defileur;
        private final View panneau;
        private final StringBuilder texte = new StringBuilder();

        JournalPanneau(TextView lignes, ScrollView defileur, View panneau) {
            this.lignes = lignes;
            this.defileur = defileur;
            this.panneau = panneau;
            lignes.setTypeface(Typeface.MONOSPACE);
        }

        void ligne(String message) {
            String horodatage = new SimpleDateFormat("HH:mm:ss.SSS",
                    Locale.ROOT).format(new Date());
            texte.append('[').append(horodatage).append("] ")
                    .append(message).append('\n');
            lignes.post(() -> {
                lignes.setText(texte.toString());
                if (defileur != null) {
                    defileur.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        String texte() {
            return texte.toString();
        }
    }
}
