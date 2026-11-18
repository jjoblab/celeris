package jo.celeris.sample;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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
import java.util.Date;
import java.util.Locale;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.grammars.PackGrammars;

/**
 * L'activité unique de l'exemple : le montage minimal du § 4.2
 * d'USAGE, en code — pas de ressource de gabarit, la scène se pose à
 * la main pour que TOUT ce que l'hôte écrit reste visible ici.
 *
 * <p>Le chargeur de langues vit en champ statique : UN seul fil
 * partagé sert toutes les scènes du processus (ADR-007, T5).</p>
 *
 * <p>Le JOURNAL VIT DE BASE (la revue du propriétaire du 2026-11-19 :
 * « ajoute une petite section pour les logs comme ça on pourra
 * savoir ce qui marche ou pas, la possibilité de copier les logs ») :
 * chaque étape du montage s'y écrit — le temps de lancement de
 * l'assemblage des paquets, les paquets fautifs, la langue résolue —
 * et le bouton COPIER emporte tout le texte au presse-papiers.</p>
 */
public final class MainActivity extends Activity {

    private static final String EXEMPLE_JAVA =
            "package exemple;\n"
            + "\n"
            + "/** Salut — l'exemple vit sur la façade. */\n"
            + "public final class Salut {\n"
            + "    public static void main(String[] args) {\n"
            + "        String nom = \"celeris\";\n"
            + "        System.out.println(\"Salut, \" + nom + \" !\");\n"
            + "    }\n"
            + "}\n";

    /** Le chargeur partagé — un seul assemblage par processus. */
    private static volatile LanguageKit langages;

    private CelerisEditor editeur;
    /** Le journal visible : chaque ligne horodatée s'y pose. */
    private TextView journal;
    private final StringBuilder lignesJournal = new StringBuilder();
    private ScrollView defileurJournal;
    private LinearLayout enteteJournal;
    private View panneauJournal;
    private static final float HAUTEUR_JOURNAL_DP = 150f;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);

        // La scène du § 4.1 : la vue sous la couche porte-popups,
        // le journal en dessous (repliable d'un appui).
        FrameLayout scene = new FrameLayout(this);
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

        monterJournal(scene);

        // Une fois par processus : le chargeur des paquets intégrés.
        long debut = System.currentTimeMillis();
        journaliser("montage : assemblage des paquets de langages…");
        LanguageKit kit = langages;
        if (kit == null) {
            kit = LanguageKit.builder(LanguageKit.sharedDaemon())
                    .grammarFactory(PackGrammars.INSTANCE)
                    .builtinPacks()
                    .onError(erreur -> journaliser(
                            "PAQUET FAUTIF : " + erreur.getMessage()))
                    .build();
            langages = kit;
            kit.preload();
        }
        journaliser("assemblage lancé en "
                + (System.currentTimeMillis() - debut)
                + " ms (le fil démon le termine, l'éditeur n'attend pas)");

        // Le chemin court : la façade compose session, langue
        // détectée, popups, frappe intelligente et réglages — la
        // langue arrive SANS BLOCAGE dès que l'assemblage vit.
        editeur = CelerisEditor.attach(vue, couche, kit);
        editeur.open("Salut.java", EXEMPLE_JAVA);
        journaliser("éditeur ouvert (Salut.java)");
        editeur.settings().setTheme(EditorThemes.SOMBRE);
        editeur.settings().setFontSizeSp(14f);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        // La langue résolue se journalise dès qu'elle vit : le sondage
        // léger du registre (toutes les 200 ms, quinze fois au plus).
        sonderLangue(kit, 0);
    }

    /** Journalise l'arrivée de la langue (le registre s'assemble en
     *  fil démon — l'ouverture n'a pas attendu). */
    private void sonderLangue(LanguageKit kit, int tentative) {
        LanguageSupport langue = kit.languageForSiPret("Salut.java");
        if (langue != null || tentative > 15) {
            journaliser(langue == null
                    ? "langue : le registre n'a pas résolu Salut.java"
                    : "langue résolue : " + langue.displayName()
                            + " (" + langue.id() + ") — coloration et mots réservés vivent");
            return;
        }
        if (editeur != null) {
            journaliser("langue : assemblage en cours…");
        }
        getWindow().getDecorView().postDelayed(
                () -> sonderLangue(kit, tentative + 1), 200L);
    }

    /** Monte le panneau du journal au bas de la scène : repliable,
     *  copiable, lisible sur fond sombre. */
    private void monterJournal(FrameLayout scene) {
        LinearLayout panneau = new LinearLayout(this);
        panneau.setOrientation(LinearLayout.VERTICAL);
        panneau.setBackgroundColor(0xEE101418);
        panneauJournal = panneau;

        enteteJournal = new LinearLayout(this);
        enteteJournal.setOrientation(LinearLayout.HORIZONTAL);
        enteteJournal.setGravity(Gravity.CENTER_VERTICAL);
        TextView titre = new TextView(this);
        titre.setText("Journal");
        titre.setTextColor(0xFF9FE8FF);
        titre.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        titre.setTextSize(12f);
        enteteJournal.addView(titre, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button copier = new Button(this);
        copier.setText("Copier");
        copier.setTextSize(12f);
        copier.setOnClickListener(v -> copierJournal());
        enteteJournal.addView(copier, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button replier = new Button(this);
        replier.setText("Cacher");
        replier.setTextSize(12f);
        replier.setOnClickListener(v -> basculerJournal());
        enteteJournal.addView(replier, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        panneau.addView(enteteJournal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        journal = new TextView(this);
        journal.setTypeface(Typeface.MONOSPACE);
        journal.setTextSize(10f);
        journal.setTextColor(0xFFCFE3E8);
        journal.setPadding(24, 8, 24, 16);
        defileurJournal = new ScrollView(this);
        defileurJournal.addView(journal, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        panneau.addView(defileurJournal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        scene.addView(panneau, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (HAUTEUR_JOURNAL_DP
                        * getResources().getDisplayMetrics().density),
                Gravity.BOTTOM));
    }

    /** Bascule le journal : replié, il ne garde que son en-tête. */
    private void basculerJournal() {
        boolean visible = defileurJournal.getVisibility() == View.VISIBLE;
        defileurJournal.setVisibility(visible ? View.GONE : View.VISIBLE);
        ViewGroup.LayoutParams reglages = panneauJournal.getLayoutParams();
        reglages.height = visible
                ? ViewGroup.LayoutParams.WRAP_CONTENT
                : (int) (HAUTEUR_JOURNAL_DP
                        * getResources().getDisplayMetrics().density);
        panneauJournal.setLayoutParams(reglages);
        Button bascule = (Button) enteteJournal.getChildAt(2);
        bascule.setText(visible ? "Montrer" : "Cacher");
    }

    /** Copie le journal entier au presse-papiers système. */
    private void copierJournal() {
        Object service = getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText(
                    "journal celeris", lignesJournal.toString()));
            journaliser("journal copié au presse-papiers");
        }
    }

    /** Journalise une ligne horodatée — le montage se lit. */
    private void journaliser(String message) {
        String horodatage = new SimpleDateFormat("HH:mm:ss.SSS",
                Locale.ROOT).format(new Date());
        lignesJournal.append('[').append(horodatage).append("] ")
                .append(message).append('\n');
        if (journal != null) {
            journal.post(() -> {
                journal.setText(lignesJournal.toString());
                defileurJournal.fullScroll(View.FOCUS_DOWN);
            });
        }
    }

    @Override
    protected void onDestroy() {
        // La scène meurt : tout se libère (vue, popups, fils) —
        // idempotent, le détachement de fenêtre n'est pas une
        // libération (USAGE § 19.10).
        if (editeur != null) {
            editeur.close();
            editeur = null;
        }
        super.onDestroy();
    }
}
