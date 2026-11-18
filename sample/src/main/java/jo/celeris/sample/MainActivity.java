package jo.celeris.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.grammars.PackGrammars;

/**
 * L'activité unique de l'exemple : le montage du § 4.2 d'USAGE en
 * code — la scène se pose à la main pour que TOUT ce que l'hôte
 * écrit reste visible ici.
 *
 * <p>Le retour terrain du 2026-11-19 : « ajouter des exemples
 * d'autres langages (kt, xml…), choix depuis un drawer, ouverture
 * dans des onglets » — le DRAWER gauche sert les échantillons du
 * dépôt, la barre d'ONGLETS porte les fichiers ouverts, le mini
 * serveur LSP Java se branche au fichier .java (diagnostics réels,
 * complétion, signatures). Le JOURNAL vit toujours en bas :
 * horodaté, repliable, copiable.</p>
 */
public final class MainActivity extends Activity {

    private static final float HAUTEUR_JOURNAL_DP = 130f;

    /** Le chargeur partagé — un seul assemblage par processus. */
    private static volatile LanguageKit langages;

    private CelerisEditor editeur;
    private JournalPanneau journal;
    /** Les fichiers ouverts, dans l'ordre des onglets. */
    private final List<Echantillons.Echantillon> ouverts = new ArrayList<>();
    private int ongletActif = -1;
    private LinearLayout barreOnglets;
    private LinearLayout tiroir;
    private FrameLayout scene;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);

        scene = new FrameLayout(this);
        EditorView vue = new EditorView(this);
        vue.setId(android.R.id.text1);
        PopupLayer couche = new PopupLayer(this);
        couche.setId(android.R.id.text2);
        scene.addView(vue, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scene.addView(couche, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(scene);
        getWindow().getDecorView().setBackgroundColor(0xFF101418);

        journal = new JournalPanneau(this);
        LanguageKit kit = monterKit();
        editeur = CelerisEditor.attach(vue, couche, kit);
        editeur.settings().setTheme(EditorThemes.SOMBRE);
        editeur.settings().setFontSizeSp(14f);

        monterTiroir();
        monterBarreOnglets();
        journal.monterDans(scene, this::copierJournal);
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

    // ── Le tiroir des échantillons (la gauche de la scène) ─────────

    /** Monte le tiroir : un échantillon par rangée, l'ouverture y passe. */
    private void monterTiroir() {
        tiroir = new LinearLayout(this);
        tiroir.setOrientation(LinearLayout.VERTICAL);
        tiroir.setBackgroundColor(0xEE161B21);
        tiroir.setVisibility(View.GONE);
        TextView titre = new TextView(this);
        titre.setText("Échantillons");
        titre.setTextColor(0xFF9FE8FF);
        titre.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        titre.setTextSize(13f);
        titre.setPadding(24, 28, 24, 16);
        tiroir.addView(titre);
        ScrollView defileur = new ScrollView(this);
        LinearLayout rangs = new LinearLayout(this);
        rangs.setOrientation(LinearLayout.VERTICAL);
        for (Echantillons.Echantillon echantillon : Echantillons.tous()) {
            Button rang = new Button(this);
            rang.setText(echantillon.nomFichier());
            rang.setTextSize(12f);
            rang.setAllCaps(false);
            rang.setOnClickListener(v -> {
                tiroir.setVisibility(View.GONE);
                ouvrir(echantillon);
            });
            rangs.addView(rang, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        defileur.addView(rangs);
        tiroir.addView(defileur, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        scene.addView(tiroir, new FrameLayout.LayoutParams(
                (int) (220 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
    }

    /** Bascule le tiroir — le bouton de la barre d'onglets y passe. */
    private void basculerTiroir() {
        tiroir.setVisibility(tiroir.getVisibility() == View.VISIBLE
                ? View.GONE : View.VISIBLE);
    }

    // ── La barre d'onglets (le haut de la scène) ───────────────────

    /** Monte la barre : le bouton tiroir, puis un onglet par fichier. */
    private void monterBarreOnglets() {
        LinearLayout barre = new LinearLayout(this);
        barre.setOrientation(LinearLayout.VERTICAL);
        barreOnglets = new LinearLayout(this);
        barreOnglets.setOrientation(LinearLayout.HORIZONTAL);
        barreOnglets.setBackgroundColor(0xEE161B21);
        barre.addView(barreOnglets, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        scene.addView(barre, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
    }

    /** Re-pose les onglets de la barre — l'actif s'illumine. */
    private void rafraichirOnglets() {
        barreOnglets.removeAllViews();
        Button tiroirBouton = new Button(this);
        tiroirBouton.setText("☰");
        tiroirBouton.setTextSize(14f);
        tiroirBouton.setOnClickListener(v -> basculerTiroir());
        barreOnglets.addView(tiroirBouton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < ouverts.size(); i++) {
            poserOnglet(i);
        }
    }

    /** Pose l'onglet d'un fichier ouvert — l'actif s'illumine. */
    private void poserOnglet(int indice) {
        Button onglet = new Button(this);
        onglet.setText(ouverts.get(indice).nomFichier());
        onglet.setTextSize(11f);
        onglet.setAllCaps(false);
        onglet.setTextColor(indice == ongletActif
                ? 0xFF9FE8FF : 0xFF7A8894);
        if (indice == ongletActif) {
            onglet.setBackgroundColor(0x332AA198);
        } else {
            onglet.setBackground(new ColorDrawable(0x00000000));
        }
        onglet.setOnClickListener(v -> activerOnglet(indice));
        barreOnglets.addView(onglet, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Active un onglet : le document se rouvre, le .java rebranche son serveur. */
    private void activerOnglet(int indice) {
        ongletActif = indice;
        Echantillons.Echantillon echantillon = ouverts.get(indice);
        editeur.open(echantillon.nomFichier(), echantillon.texte());
        if (echantillon.nomFichier().endsWith(".java")) {
            SessionLspPoseuse.poser(this, editeur, echantillon.nomFichier(),
                    () -> journal.ligne("mini serveur LSP rebranché"));
        }
        rafraichirOnglets();
    }

    /** Le panneau du journal : horodaté, repliable, copiable. */
    private static final class JournalPanneau {

        private final TextView lignes;
        private final StringBuilder texte = new StringBuilder();
        private ScrollView defileur;
        private View panneau;
        private boolean replie;

        JournalPanneau(Context contexte) {
            lignes = new TextView(contexte);
            lignes.setTypeface(Typeface.MONOSPACE);
            lignes.setTextSize(10f);
            lignes.setTextColor(0xFFCFE3E8);
            lignes.setPadding(24, 8, 24, 16);
        }

        void monterDans(FrameLayout scene, Runnable copier) {
            LinearLayout colonne = new LinearLayout(scene.getContext());
            colonne.setOrientation(LinearLayout.VERTICAL);
            colonne.setBackgroundColor(0xEE101418);
            panneau = colonne;
            LinearLayout entete = new LinearLayout(scene.getContext());
            entete.setOrientation(LinearLayout.HORIZONTAL);
            entete.setGravity(Gravity.CENTER_VERTICAL);
            TextView titre = new TextView(scene.getContext());
            titre.setText("Journal");
            titre.setTextColor(0xFF9FE8FF);
            titre.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            titre.setTextSize(12f);
            entete.addView(titre, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ajouterBouton(entete, "Copier", copier);
            Runnable[] bascule = new Runnable[1];
            bascule[0] = () -> basculer(bascule[0]);
            ajouterBouton(entete, "Cacher", () -> basculer(bascule[0]));
            colonne.addView(entete, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            defileur = new ScrollView(scene.getContext());
            defileur.addView(lignes, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            colonne.addView(defileur, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            scene.addView(colonne, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (HAUTEUR_JOURNAL_DP
                            * scene.getResources().getDisplayMetrics().density),
                    Gravity.BOTTOM));
        }

        /** Bascule le journal : replié, il ne garde que son en-tête. */
        private void basculer(Runnable rappel) {
            replie = !replie;
            defileur.setVisibility(replie ? View.GONE : View.VISIBLE);
            ViewGroup.LayoutParams reglages = panneau.getLayoutParams();
            reglages.height = replie
                    ? ViewGroup.LayoutParams.WRAP_CONTENT
                    : (int) (HAUTEUR_JOURNAL_DP * panneau.getResources()
                            .getDisplayMetrics().density);
            panneau.setLayoutParams(reglages);
            rappel.run();
        }

        private static void ajouterBouton(LinearLayout entete, String libelle,
                Runnable geste) {
            Button bouton = new Button(entete.getContext());
            bouton.setText(libelle);
            bouton.setTextSize(12f);
            bouton.setOnClickListener(v -> geste.run());
            entete.addView(bouton, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
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
