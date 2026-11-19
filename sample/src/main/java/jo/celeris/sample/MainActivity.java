package jo.celeris.sample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.syntax.grammars.PackGrammars;

/**
 * L'activité unique de l'exemple : le montage minimal du § 4.2
 * d'USAGE, en code — pas de ressource de gabarit, la scène se pose à
 * la main pour que TOUT ce que l'hôte écrit reste visible ici.
 *
 * <p>Le chargeur de langues vit en champ statique : UN seul fil
 * partagé sert toutes les scènes du processus (ADR-007, T5).</p>
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

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);

        // La scène du § 4.1 : la vue sous la couche porte-popups.
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

        // Une fois par processus : le chargeur des paquets intégrés.
        LanguageKit kit = langages;
        if (kit == null) {
            kit = LanguageKit.builder(LanguageKit.sharedDaemon())
                    .grammarFactory(PackGrammars.INSTANCE)
                    .builtinPacks()
                    .build();
            langages = kit;
            kit.preload();
        }

        // Le chemin court : la façade compose session, langue
        // détectée, popups, frappe intelligente et réglages.
        editeur = CelerisEditor.attach(vue, couche, kit);
        editeur.open("Salut.java", EXEMPLE_JAVA);
        editeur.settings().setTheme(EditorThemes.SOMBRE);
        editeur.settings().setFontSizeSp(14f);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
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
