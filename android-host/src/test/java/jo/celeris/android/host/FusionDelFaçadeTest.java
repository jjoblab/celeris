package jo.celeris.android.host;

import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.MultiSelection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La régression du propriétaire (retour de terrain, session 21) vue
 * par la FAÇADE : dans l'application de démonstration, des DEL
 * enchaînés au clavier font remonter le caret à la ligne précédente —
 * les mots de cette ligne disparaissent de l'image jusqu'à Enter.
 * Le montage complet (kit de langue, publication, gouttière) y vit,
 * comme dans la démo : le banc y reproduit la frappe exacte.
 */
@RunWith(RobolectricTestRunner.class)
public class FusionDelFaçadeTest {

    /** La langue de banc : java posé directement, registre immédiat. */
    private static final LanguageKit KIT = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static final String SOURCE = """
            class Premiere {
                void methode() {
                    corps();
                }
                void autre() {
                    suite();
                }
            }
            """;

    private static KeyEvent del() {
        long temps = System.currentTimeMillis();
        return new KeyEvent(temps, temps, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, 0);
    }

    /** La variante du terrain avec le DÉFILEMENT : le caret vit
     *  BAS dans le document (sous le viewport), les DEL le font
     *  remonter — la vue suit le curseur vers le haut, la ligne
     *  fusionnée doit rester tracée après le saut de viewport. */
    @Test
    public void lesMotsRestentDessinesApresRemonteeAvecDefilement() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.measure(1080, 1920);
        vue.layout(0, 0, 1080, 1920);
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editor = CelerisEditor.attach(vue, couche, KIT);
        // un document LONG : la ligne 60 vit bien sous le viewport
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            source.append("ligne numero ").append(i).append("\n");
        }
        editor.open("Main.java", source.toString());
        int debutLigne60 = editor.session().document().lineStart(60);
        editor.session().setSelection(MultiSelection.caretsAt(debutLigne60));
        for (int i = 0; i < 9; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
        }
        CanvasEnregistreur canvas = new CanvasEnregistreur();
        vue.draw(canvas);
        String trace = canvas.text.toString();
        // la ligne fusionnée (59) porte le début de l'ancienne 60 —
        // ses mots doivent rester tracés après la remontée du caret
        String ligneFusionnee = editor.session().document().lineText(59);
        assertTrue("la ligne fusionnée '" + ligneFusionnee
                + "' reste tracée après la remontée (scrollY="
                + vue.getScrollY() + ")",
                trace.contains(ligneFusionnee.substring(0,
                        Math.min(12, ligneFusionnee.length()))));
        editor.close();
    }

    /** Après la fusion par DEL, les mots de la ligne fusionnée
     *  restent dans l'image. */
    @Test
    public void lesMotsRestentDessinesApresFusion() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.measure(1080, 1920);
        vue.layout(0, 0, 1080, 1920);
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        CelerisEditor editor = CelerisEditor.attach(vue, couche, KIT);
        editor.open("Main.java", SOURCE);

        // Le scénario exact du terrain : le caret au début de
        // « corps(); » (ligne 2) ; les 8 DEL mangent l'indentation,
        // le 9e mange le saut — le caret REMONTE à la ligne
        // précédente, les deux lignes fusionnent.
        int debutCorps = editor.session().document().lineStart(2);
        editor.session().setSelection(MultiSelection.caretsAt(debutCorps));
        for (int i = 0; i < 9; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
        }
        String text = editor.session().document().text();
        // le document APRES la frappe réelle : la fusion a eu lieu,
        // le texte de la ligne précédente vit toujours (c'est le
        // comportement du terrain, on l'accepte tel quel)
        assertTrue("la fusion a eu lieu (texte : "
                + text.replace("\n", "\\n") + ")",
                text.contains("corps()") && text.chars()
                        .filter(c -> c == '\n').count() == 7);

        // l'image : la ligne fusionnée doit rester dessinée
        CanvasEnregistreur canvas = new CanvasEnregistreur();
        vue.draw(canvas);
        String trace = canvas.text.toString();
        assertTrue("« corps() » reste dans l'image après la fusion (tracé : "
                + trace + ")",
                trace.contains("corps()"));
        editor.close();
    }

    /** Un canvas qui enregistre chaque tranche de texte tracée. */
    private static final class CanvasEnregistreur extends android.graphics.Canvas {
        private final StringBuilder text = new StringBuilder();

        @Override
        public void drawText(String text, float x, float y,
                android.graphics.Paint paint) {
            this.text.append(text);
        }

        @Override
        public void drawText(CharSequence text, int start, int end,
                float x, float y, android.graphics.Paint paint) {
            this.text.append(text, start, end);
        }

        @Override
        public void drawText(String text, int start, int end,
                float x, float y, android.graphics.Paint paint) {
            this.text.append(text, start, end);
        }

        @Override
        public void drawText(char[] text, int index, int compte,
                float x, float y, android.graphics.Paint paint) {
            this.text.append(text, index, index + compte);
        }
    }
}
