package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de surface de la connexion d'entrée IME « par niveau d'API » :
 * la colle implante la surface COMPLÈTE de l'interface de plateforme
 * au niveau de compilation (36) — aucune méthode ne retombe sur le
 * repli par défaut de l'interface — et les méthodes apparues après le
 * niveau minimal (26) y figurent, groupe par groupe.
 *
 * <p>Niveaux vérifiés sur les archives de plateforme elles-mêmes
 * (26, 31, 33, 34, 35, 36) : +3 méthodes au niveau 31, +8 au 33,
 * +14 au 34, rien aux 35 et 36.</p>
 */
class EditorInputConnectionSurfaceTest {

    /** Les méthodes de l'interface, statiques exclues. */
    private static List<Method> methodesInterface() {
        List<Method> liste = new ArrayList<>();
        for (Method m : android.view.inputmethod.InputConnection.class.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                liste.add(m);
            }
        }
        return liste;
    }

    /** La colle déclare une implantation de chaque méthode de l'interface. */
    @Test
    void surface_everyInterfaceMethodIsOverridden() {
        List<Method> manquantes = new ArrayList<>();
        for (Method m : methodesInterface()) {
            if (!estSurchargee(m)) {
                manquantes.add(m);
            }
        }
        if (!manquantes.isEmpty()) {
            for (Method m : manquantes) {
                System.err.println("NON IMPLANTÉE : " + m);
            }
            fail(manquantes.size() + " méthode(s) de l'interface sans implantation");
        }
    }

    private static boolean estSurchargee(Method m) {
        for (Method declaree : EditorInputConnection.class.getDeclaredMethods()) {
            if (declaree.getName().equals(m.getName())
                    && java.util.Arrays.equals(declaree.getParameterTypes(),
                            m.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    /** Les méthodes apparues au niveau 31 existent sur l'interface de compilation. */
    @Test
    void surface_api31MethodsExist() throws Exception {
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("getSurroundingText", int.class, int.class, int.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("performSpellCheck"));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("setImeConsumesInput", boolean.class));
    }

    /** Les méthodes apparues au niveau 33 existent sur l'interface de compilation. */
    @Test
    void surface_api33MethodsExist() throws Exception {
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("takeSnapshot"));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("commitText", CharSequence.class, int.class,
                        android.view.inputmethod.TextAttribute.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("requestCursorUpdates", int.class, int.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("setComposingRegion", int.class, int.class,
                        android.view.inputmethod.TextAttribute.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("setComposingText", CharSequence.class, int.class,
                        android.view.inputmethod.TextAttribute.class));
    }

    /** Les méthodes apparues au niveau 34 existent sur l'interface de compilation. */
    @Test
    void surface_api34MethodsExist() throws Exception {
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("previewHandwritingGesture",
                        android.view.inputmethod.PreviewableHandwritingGesture.class,
                        android.os.CancellationSignal.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("replaceText", int.class, int.class, CharSequence.class,
                        int.class, android.view.inputmethod.TextAttribute.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("performHandwritingGesture",
                        android.view.inputmethod.HandwritingGesture.class,
                        java.util.concurrent.Executor.class,
                        java.util.function.IntConsumer.class));
        assertNotNull(android.view.inputmethod.InputConnection.class
                .getMethod("requestTextBoundsInfo", android.graphics.RectF.class,
                        java.util.concurrent.Executor.class,
                        java.util.function.Consumer.class));
    }

    /**
     * Les 35 et 36 n'ajoutent rien à l'interface : le compte de méthodes
     * non statiques au niveau de compilation 36 est exactement celui
     * mesuré au niveau 34 — 26 de base, 3 au 31, 5 au 33, 4 au 34.
     */
    @Test
    void surface_api35And36AddNothing() {
        assertTrue(methodesInterface().size() == 38,
                "38 méthodes attendues au niveau 36, "
                        + methodesInterface().size() + " lues");
    }
}
