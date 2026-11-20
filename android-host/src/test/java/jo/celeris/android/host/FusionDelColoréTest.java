package jo.celeris.android.host;

import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.MultiSelection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La régression du terrain, la plus proche de la démo réelle : la
 * façade complète, la syntaxe Java (la couleur vit), les DEL au
 * clavier physique frame par frame, la publication drainée à la fin.
 */
@RunWith(RobolectricTestRunner.class)
public class FusionDelColoréTest {

    private static final LanguageKit KIT = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static KeyEvent del() {
        long t = System.currentTimeMillis();
        return new KeyEvent(t, t, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, 0);
    }

    /** Un canvas qui enregistre chaque tranche de texte tracée. */
    private static final class CanvasEnregistreur extends android.graphics.Canvas {
        private final StringBuilder texte = new StringBuilder();

        @Override
        public void drawText(String texte, float x, float y,
                android.graphics.Paint peinture) {
            this.texte.append(texte);
        }

        @Override
        public void drawText(CharSequence texte, int debut, int fin,
                float x, float y, android.graphics.Paint peinture) {
            this.texte.append(texte, debut, fin);
        }

        @Override
        public void drawText(String texte, int debut, int fin,
                float x, float y, android.graphics.Paint peinture) {
            this.texte.append(texte, debut, fin);
        }

        @Override
        public void drawText(char[] texte, int index, int compte,
                float x, float y, android.graphics.Paint peinture) {
            this.texte.append(texte, index, index + compte);
        }
    }

    @Test
    public void leVraiFichierDeLaDemoResteTracee() {
        // LE fichier Java réel de la démo (Echantillons.EXEMPLE_JAVA),
        // avec ses lignes vides — le scénario du terrain
        String source = "package exemple;\n"
                + "\n"
                + "/** Salut — l'exemple vit sur la façade. */\n"
                + "public final class Salut {\n"
                + "    private String nom = \"celeris\";\n"
                + "\n"
                + "    public static void main(String[] args) {\n"
                + "        String nom = \"celeris\";\n"
                + "        System.out.println(\"Salut, \" + nom +\" !\");\n"
                + "        System.out.println(nom.length());\n"
                + "    }\n"
                + "}\n";
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.measure(1080, 1920);
        vue.layout(0, 0, 1080, 1920);
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Salut.java", source);
        CanvasEnregistreur c0 = new CanvasEnregistreur();
        vue.draw(c0);
        assertTrue("départ complet", c0.texte.toString().contains("main"));

        // le caret au début de « String nom = » (ligne 7) — les DEL
        // remontent : indentation, puis ligne VIDE, puis la fusion
        int debut = editeur.session().document().lineStart(7);
        editeur.session().setSelection(MultiSelection.caretsAt(debut));
        for (int i = 0; i < 8; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
            CanvasEnregistreur ci = new CanvasEnregistreur();
            vue.draw(ci);
        }
        String doc = editeur.session().document().text();
        CanvasEnregistreur cf = new CanvasEnregistreur();
        vue.draw(cf);
        String trace = cf.texte.toString();
        // les mots de la ligne fusionnée restent visibles :
        assertTrue("la ligne fusionnée reste tracée après la remontée "
                + "(doc = '" + doc.replace("\n", "\\n") + "' ; trace = '"
                + trace + "')",
                trace.contains("nom")
                        && trace.contains("celeris"));
        editeur.close();
    }

    @Test
    public void laFusionAvecViewportPetitEtScroll() {
        // leViewport ÉTROIT d'un vrai appareil (hauteur 480 = ~29
        // lignes visibles) : le caret ligne 60 exige le DÉFILEMENT,
        // la remontée par DEL fait SAUTER le scroll — le terrain
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            source.append("int champ").append(i).append(" = ")
                    .append(i).append(";\n");
        }
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.measure(1080, 480);
        vue.layout(0, 0, 1080, 480);
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", source.toString());
        // amener le caret à la ligne 60 (sous le viewport) : le DEL#1
        // déclenche revealCaret → la vue DÉFILE
        int debut = editeur.session().document().lineStart(60);
        editeur.session().setSelection(MultiSelection.caretsAt(debut));
        // la 1re frappe dévoile le caret (revealCaret suit le DEL)
        for (int i = 0; i < 9; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
            CanvasEnregistreur ci = new CanvasEnregistreur();
            vue.draw(ci);
        }
        // la ligne fusionnée (59) doit rester dans le viewport tracé
        CanvasEnregistreur cf = new CanvasEnregistreur();
        vue.draw(cf);
        String trace = cf.texte.toString();
        assertTrue("la ligne fusionnée reste tracée après la remontée "
                + "avec défilement (trace = '"
                + trace.substring(0, Math.min(250, trace.length()))
                + "' ; doc ligne 59 = '"
                + editeur.session().document().lineText(59) + "')",
                trace.contains("champ60"));
        editeur.close();
    }

    @Test
    public void laLigneFusionneeColoreeResteTracee() {
        StringBuilder source = new StringBuilder();
        source.append("class Essai {\n");
        for (int i = 0; i < 60; i++) {
            source.append("    int champ").append(i).append(" = ")
                    .append(i).append(";\n");
        }
        source.append("}\n");
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.measure(1080, 1920);
        vue.layout(0, 0, 1080, 1920);
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, KIT);
        editeur.open("Main.java", source.toString());

        // une première image colorée
        CanvasEnregistreur c0 = new CanvasEnregistreur();
        vue.draw(c0);
        assertTrue("l'image de départ est colorée et complète",
                c0.texte.toString().contains("champ59"));

        // le caret au début de la ligne 60 (champ59), les DEL remontent
        int debut = editeur.session().document().lineStart(60);
        editeur.session().setSelection(MultiSelection.caretsAt(debut));
        for (int i = 0; i < 12; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
            CanvasEnregistreur ci = new CanvasEnregistreur();
            vue.draw(ci); // la frame de CHAQUE frappe
        }
        String doc = editeur.session().document().text();
        // la ligne fusionnée (59) : '…champ58 = 58;int champ59…' ou proche
        CanvasEnregistreur cf = new CanvasEnregistreur();
        vue.draw(cf);
        String trace = cf.texte.toString();
        // diagnostic : QUELLES lignes manquent au tracé ?
        StringBuilder manquantes = new StringBuilder();
        for (int i = 50; i <= 59; i++) {
            String morceau = "champ" + i + " =";
            if (!trace.contains(morceau)) {
                manquantes.append("champ").append(i).append(' ');
            }
        }
        assertTrue("la ligne fusionnée reste tracée (manquantes : "
                + manquantes + " ; doc ligne 59 = '"
                + editeur.session().document().lineText(59) + "')",
                trace.contains("champ59 ="));
        editeur.close();
    }
}
