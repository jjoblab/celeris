package jo.celeris.android.view.internal.input;

import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.HandwritingGesture;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.PreviewableHandwritingGesture;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.TextAttribute;
import android.view.inputmethod.TextSnapshot;
import jo.celeris.android.view.CaretReveal;
import jo.celeris.android.view.CompositionState;
import jo.celeris.android.view.SurroundingTexts;
import jo.celeris.android.view.internal.access.EditorAccess;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La connexion d'entrée IME de la vue : la colle qui implante la
 * surface complète de l'interface de plateforme au niveau de
 * compilation (36) — aucune méthode ne retombe sur le repli par
 * défaut. Le niveau minimal est 26 : les méthodes apparues aux
 * niveaux 31 (entourage étendu, consommation déclarée, vérification
 * orthographique), 33 (instantané, attributs de texte, mises à jour
 * de curseur à deux arguments) et 34 (remplacement direct, gestes de
 * stylet, bornes de texte) sont simplement jamais appelées en dessous
 * de leur niveau ; les niveaux 35 et 36 n'ajoutent rien (surface
 * vérifiée sur les archives de plateforme elles-mêmes).
 *
 * <p>La colle applique, elle ne décide pas : la composition vit dans
 * {@link CompositionState}, l'entourage dans {@link SurroundingTexts},
 * le dévoilement dans {@link CaretReveal} ; toute édition passe par
 * la voie de transaction unique de la session (ADR-003), le curseur
 * primaire seul est concerné — la composition est une interaction de
 * plage primaire, la première frappe IME y ramène la sélection.</p>
 *
 * <p>Capacités assumées comme absentes, documentées : pas de texte
 * extrait pleine page ni d'instantané entier (les très gros fichiers
 * se servent par fenêtres, G-01), pas de moniteur immédiat de
 * curseur, pas de correction orthographique embarquée, pas de contenu
 * riche, pas de gestes de stylet (réponse NON PRIS EN CHARGE à
 * l'appelant), pas de commandes privées.</p>
 */
public final class EditorInputConnection  implements InputConnection {

    /**
     * La fabrique de connexion : la vue s'y délègue (C9 — le corps
     * de {@code onCreateInputConnection} vit dans sa colle) — en
     * lecture seule, la vue se déclare non éditable (TYPE_NULL, pas de
     * connexion).
     *
     * @param vue         la vue propriétaire de la session
     * @param composition l'état de composition de la vue
     * @param outAttrs    les informations à remplir
     * @return la connexion, ou null en lecture seule
     */
    public static @Nullable InputConnection create(EditorAccess acces,
            CompositionState composition, EditorInfo outAttrs) {
        if (acces.session().isReadOnly()) {
            outAttrs.inputType = InputType.TYPE_NULL;
            return null;
        }
        remplirInfos(acces.session(), outAttrs);
        return new EditorInputConnection(acces, composition);
    }

    /**
     * Remplit les informations IME de la session : texte de code sans
     * suggestions, jamais plein écran, sélection initiale annoncée —
     * la vue délègue, la colle possède sa surface.
     *
     * @param session  la session affichée
     * @param outAttrs les informations à remplir
     */
    static void remplirInfos(TextSession session, EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_NONE;
        List<SelRange> ranges = session.selection().ranges();
        if (!ranges.isEmpty()) {
            outAttrs.initialSelStart = ranges.get(0).start();
            outAttrs.initialSelEnd = ranges.get(0).end();
        }
    }

    private final EditorAccess acces;
    private final CompositionState composition;

    /**
     * Construit la connexion d'une vue.
     *
     * @param vue         la vue propriétaire de la session et du tracé
     * @param composition l'état de composition de la vue
     */
    public EditorInputConnection(EditorAccess acces, CompositionState composition) {
        this.acces = acces;
        this.composition = composition;
    }

    // ---------------------------------------------------------- lecture

    @Override
    public @Nullable CharSequence getTextBeforeCursor(int length, int flags) {
        if (length <= 0) {
            return "";
        }
        Document document = acces.session().document();
        int end = primaire().start();
        int start = Math.max(0, end - length);
        return document.subText(start, end);
    }

    @Override
    public @Nullable CharSequence getTextAfterCursor(int length, int flags) {
        if (length <= 0) {
            return "";
        }
        Document document = acces.session().document();
        int start = primaire().end();
        int end = Math.min(document.length(), start + length);
        return document.subText(start, end);
    }

    /** Le plafond du texte de sélection servi à l'IME : le protocole
     *  IME n'a pas à recevoir un document entier (A-20, l'analyse
     *  croisée — le dépôt de départ borne pareil). */
    static final int MAX_SELECTED_TEXT_CHARS = 4_096;

    @Override
    public @Nullable CharSequence getSelectedText(int flags) {
        SelRange primaire = primaire();
        if (primaire.isEmpty()) {
            return null;
        }
        CharSequence text = acces.session().document().subText(
                primaire.start(), primaire.end());
        return text.length() > MAX_SELECTED_TEXT_CHARS
                ? text.subSequence(0, MAX_SELECTED_TEXT_CHARS)
                : text;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        Document document = acces.session().document();
        int cursor = primaire().end();
        int line = document.lineForOffset(cursor);
        String texteLigne = document.subText(document.lineStart(line), cursor);
        return TextUtils.getCapsMode(texteLigne, 0, reqModes);
    }

    /**
     * L'entourage étendu (niveau 31) : la fenêtre se calcule en pur
     * ({@link SurroundingTexts}) — les très gros fichiers se servent
     * par fenêtres, jamais par texte entier (G-01). La construction
     * vit sous garde de version : l'interface n'expose la méthode
     * qu'au niveau 31, la garde ne sert que le vérificateur.
     */
    @Override
    public @Nullable SurroundingText getSurroundingText(int beforeLength,
            int afterLength, int flags) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }
        SurroundingTexts.Window window = SurroundingTexts.window(
                acces.session().document(), primaire(), beforeLength, afterLength);
        return new SurroundingText(window.text(), window.selectionStart(),
                window.selectionEnd(), window.docOffset());
    }

    /** Pas de texte extrait pleine page : l'IME travaille par fenêtres. */
    @Override
    public @Nullable ExtractedText getExtractedText(
            @Nullable ExtractedTextRequest request, int flags) {
        return null;
    }

    /**
     * Pas d'instantané entier (niveau 33) : matérialiser le texte
     * d'un très gros fichier coûte trop — l'entourage étendu le
     * remplace.
     */
    @Override
    public @Nullable TextSnapshot takeSnapshot() {
        return null;
    }

    @Override
    public @Nullable Handler getHandler() {
        return acces.getHandler();
    }

    // ---------------------------------------------------------- composition

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        return appliquer(composition.planComposing(acces.session().document(),
                primaire(), text.toString(), newCursorPosition));
    }

    /** Variante à attribut (niveau 33) : l'attribut n'est pas porté. */
    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition,
            @Nullable TextAttribute textAttribute) {
        return setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        composition.adopt(composition.planRegion(acces.session().document(),
                start, end));
        acces.invalidate();
        return true;
    }

    /** Variante à attribut (niveau 33) : l'attribut n'est pas porté. */
    @Override
    public boolean setComposingRegion(int start, int end,
            @Nullable TextAttribute textAttribute) {
        return setComposingRegion(start, end);
    }

    @Override
    public boolean finishComposingText() {
        if (composition.isComposing()) {
            composition.finish();
            acces.invalidate();
        }
        return true;
    }

    // ---------------------------------------------------------- commit

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        // M7.5 §8 — la frappe intelligente ne touche qu'aux textes
        // VALIDÉS : la composition reste l'affaire de l'IME, sa
        // région lui appartient. Le fournisseur absent laisse la
        // voie simple (sauf l'Entrée, jamais muette).
        String contenu = text.toString();
        if (!composition.isComposing() && acces.onCommit(contenu)) {
            acces.invalidate();
            return true;
        }
        return appliquer(composition.planCommit(acces.session().document(),
                primaire(), contenu, newCursorPosition));
    }

    /** Variante à attribut (niveau 33) : l'attribut n'est pas porté. */
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition,
            @Nullable TextAttribute textAttribute) {
        return commitText(text, newCursorPosition);
    }

    @Override
    public boolean commitCompletion(@Nullable CompletionInfo text) {
        if (text == null) {
            return false;
        }
        CharSequence contenu = text.getText();
        return contenu != null && commitText(contenu, 1);
    }

    @Override
    public boolean commitCorrection(@Nullable CorrectionInfo correctionInfo) {
        if (correctionInfo == null) {
            return false;
        }
        CharSequence nouveau = correctionInfo.getNewText();
        return nouveau != null && commitText(nouveau, 1);
    }

    /** Le remplacement direct (niveau 34) : édition immédiate, hors composition. */
    @Override
    public boolean replaceText(int start, int end, CharSequence text,
            int newCursorPosition, @Nullable TextAttribute textAttribute) {
        Document document = acces.session().document();
        int s = Math.max(0, Math.min(start, document.length()));
        int e = Math.max(0, Math.min(end, document.length()));
        if (s > e) {
            int permutage = s;
            s = e;
            e = permutage;
        }
        if (!acces.session().replaceRange(s, e, text.toString())) {
            return false;
        }
        composition.finish();
        int cursor = cursorAfter(s, e, text, newCursorPosition);
        setCursor(cursor);
        return true;
    }

    // ---------------------------------------------------------- suppression

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return supprimerEntourage(beforeLength, afterLength, false);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength,
            int afterLength) {
        return supprimerEntourage(beforeLength, afterLength, true);
    }

    // ---------------------------------------------------------- clavier

    /**
     * Les touches venues de l'IME suivent la voie clavier de la vue
     * (D-17) ; le retour arrière pendant une composition retire le
     * dernier cluster composé, entier — toute autre touche clôt la
     * composition d'abord.
     */
    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_DEL
                && composition.isComposing()) {
            CompositionState.Plan plan =
                    composition.planClusterBackspace(acces.session().document());
            if (plan != null) {
                return appliquer(plan);
            }
            composition.finish();
            return true;
        }
        composition.finish();
        acces.dispatchKeyEvent(event);
        return true;
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        finishComposingText();
        return true;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        return false;
    }

    @Override
    public boolean performPrivateCommand(@Nullable String action,
            @Nullable Bundle data) {
        return false;
    }

    // ---------------------------------------------------------- état

    @Override
    public boolean setSelection(int start, int end) {
        Document document = acces.session().document();
        int s = Math.max(0, Math.min(start, document.length()));
        int e = Math.max(0, Math.min(end, document.length()));
        acces.session().setSelection(MultiSelection.of(new SelRange(s, e)));
        acces.invalidate();
        return true;
    }

    @Override
    public boolean beginBatchEdit() {
        return true;
    }

    @Override
    public boolean endBatchEdit() {
        return true;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        return true;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        return false;
    }

    /** Pas de moniteur immédiat de curseur dans la vue minimale. */
    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return false;
    }

    /** Variante à filtres (niveau 33) : pas de moniteur non plus. */
    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode,
            int cursorUpdateFilter) {
        return false;
    }

    /** La consommation déclarée (niveau 31) est acceptée et mémorisée nulle part. */
    @Override
    public boolean setImeConsumesInput(boolean imeConsumesInput) {
        return true;
    }

    /** Pas de vérification orthographique embarquée (niveau 31). */
    @Override
    public boolean performSpellCheck() {
        return false;
    }

    /** Pas de contenu riche : l'éditeur refuse poliment. */
    @Override
    public boolean commitContent(@Nullable InputContentInfo inputContentInfo,
            int flags, @Nullable Bundle opts) {
        return false;
    }

    @Override
    public void closeConnection() {
        composition.finish();
    }

    /**
     * Gestes de stylet (niveau 34) : NON PRIS EN CHARGE, répondu à
     * l'appelant — l'IME retombe sur sa voie clavier.
     */
    @Override
    public void performHandwritingGesture(@Nullable HandwritingGesture gesture,
            @Nullable Executor executor, @Nullable IntConsumer callback) {
        repondreStylet(executor, callback);
    }

    @Override
    public boolean previewHandwritingGesture(
            @Nullable PreviewableHandwritingGesture gesture,
            @Nullable CancellationSignal cancellationSignal) {
        return false;
    }

    /**
     * Bornes de texte (niveau 34) : NON PRISES EN CHARGE — la
     * consommatrice n'est pas appelée, l'IME retombe sur ses
     * fenêtres.
     */
    @Override
    public void requestTextBoundsInfo(@Nullable RectF bounds,
            @Nullable Executor executor,
            @Nullable Consumer<android.view.inputmethod.TextBoundsInfoResult> callback) {
        // réponse délibérément absente : non pris en charge
    }

    // ---------------------------------------------------------- privé

    /** La plage primaire : la première de la sélection normalisée. */
    private SelRange primaire() {
        List<SelRange> ranges = acces.session().selection().ranges();
        if (ranges.isEmpty()) {
            return SelRange.caretAt(0);
        }
        return ranges.get(0);
    }

    /** Applique un plan de composition : édition, état, curseur, dévoilement. */
    private boolean appliquer(CompositionState.Plan plan) {
        if (plan.hasReplacement() && !acces.session().replaceRange(
                plan.replaceStart(), plan.replaceEnd(), plan.insertion())) {
            return false;
        }
        composition.adopt(plan);
        if (plan.caret() >= 0) {
            setCursor(plan.caret());
        } else {
            acces.curseurClavier().revealCaret();
        }
        return true;
    }

    private boolean supprimerEntourage(int before, int after,
            boolean codePoints) {
        SurroundingTexts.Deletion plan = SurroundingTexts.deleteAround(
                acces.session().document(), primaire(), before, after, codePoints);
        if (plan.start() >= plan.end()) {
            return true;
        }
        if (acces.session().replaceRange(plan.start(), plan.end(), "")) {
            acces.curseurClavier().revealCaret();
            return true;
        }
        return false;
    }

    private int cursorAfter(int start, int finRemplacee, CharSequence text,
            int newCursorPosition) {
        int nouvelleFin = start + text.length();
        Document document = acces.session().document();
        int cursor = newCursorPosition > 0
                ? nouvelleFin + newCursorPosition - 1
                : start + newCursorPosition;
        return Math.max(0, Math.min(cursor, document.length()));
    }

    private void setCursor(int offset) {
        acces.session().setSelection(MultiSelection.of(
                SelRange.caretAt(Math.max(0, Math.min(offset,
                        acces.session().document().length())))));
        acces.curseurClavier().revealCaret();
    }

    /**
     * Répond NON PRIS EN CHARGE à un geste de stylet : la constante
     * de résultat est inlinée par le compilateur (valeur copiée dans
     * le fichier de classe) — aucun accès à la plateforme au-dessous
     * du niveau 34, la méthode n'y est jamais appelée.
     */
    @android.annotation.SuppressLint("InlinedApi")
    private static void repondreStylet(@Nullable Executor executor,
            @Nullable IntConsumer callback) {
        if (executor == null || callback == null) {
            return;
        }
        executor.execute(() -> callback.accept(
                InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED));
    }
}
