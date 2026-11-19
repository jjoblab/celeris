package jo.celeris.android.host;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.view.EditorView;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;

/**
 * D2 (l'analyse croisée) : le zoom se SOUVIENT par fichier — le
 * changement d'onglet ne le remet plus à zéro ; chaque document
 * rouvre à SA taille, la taille par défaut sinon.
 */
@RunWith(RobolectricTestRunner.class)
public class MemoireZoomTest {

    /** Le kit de banc : java, registre immédiat. */
    private static final LanguageKit KIT = LanguageKit
            .builder(Runnable::run)
            .addSupport(LanguageSupport.builder(
                            LanguageDescriptor.of("java", "Java",
                                    java.util.List.of("java")))
                    .syntax(JavaSyntax.INSTANCE)
                    .build())
            .build();

    private static CelerisEditor monter() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        PopupLayer couche = new PopupLayer(RuntimeEnvironment.getApplication());
        return CelerisEditor.attach(vue, couche, KIT);
    }

    /** Le zoom d'un fichier lui revient au changement d'onglet. */
    @Test
    public void leZoomRevientAuFichier() {
        CelerisEditor editeur = monter();
        editeur.open("Main.java", "class A {}\n");
        editeur.settings().setFontSizeSp(20f);
        assertEquals(20f, editeur.settings().fontSizeSp(), 0.01f);

        // l'autre onglet : SA taille à lui (la défaut ici)
        editeur.open("Autre.md", "# titre\n");
        assertEquals("l'autre fichier rouvre à la taille par défaut",
                14f, editeur.settings().fontSizeSp(), 0.01f);

        // le retour au .java : SA taille revient
        editeur.open("Main.java", "class A {}\n");
        assertEquals("le zoom du fichier lui revient",
                20f, editeur.settings().fontSizeSp(), 0.01f);
        editeur.close();
    }

    /** Un zoom posé sur un fichier SANS ouverture ne touche à rien :
     *  la mémoire vit par nom, elle ne fuit pas sur les autres. */
    @Test
    public void leZoomNeFuitPasSurLesAutres() {
        CelerisEditor editeur = monter();
        editeur.open("A.java", "class A {}\n");
        editeur.settings().setFontSizeSp(18f);
        editeur.open("B.java", "class B {}\n");
        editeur.open("A.java", "class A {}\n");
        assertEquals(18f, editeur.settings().fontSizeSp(), 0.01f);
        editeur.open("B.java", "class B {}\n");
        assertEquals("B garde la taille par défaut",
                14f, editeur.settings().fontSizeSp(), 0.01f);
        editeur.close();
    }
}
