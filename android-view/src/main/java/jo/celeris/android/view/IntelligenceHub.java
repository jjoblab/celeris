package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jo.celeris.core.intelligence.CompletionMots;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.PasteReindenter;
import jo.celeris.core.intelligence.SmartTyping;
import jo.celeris.core.intelligence.TypingRules;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Le concentrateur d'intelligence de la vue (M7.5, §8) : le point de
 * branchement unique entre les moteurs purs du dépôt — frappe
 * intelligente, indentation, complétion — et les événements RÉELS de
 * la vue. Le fournisseur est un SPI : l'hôte pose les moteurs du
 * dépôt, son serveur LSP ou les siens ; sans fournisseur la frappe
 * passe telle quelle — la vue ne décide JAMAIS à la place de l'hôte.
 *
 * <p>L'Entrée reste l'exception assumée : sans fournisseur, elle
 * insère un saut de ligne nu — une Entrée muette serait un défaut,
 * pas une neutralité. La composition IME n'est jamais détourée : le
 * clavier possède sa région, les règles ne s'appliquent qu'aux
 * textes validés ({@code commitText}).</p>
 *
 * <p>L'application passe par la voie de transaction unique (ADR-003)
 * avec sa sélection d'après explicite : annuler restitue la sélection
 * d'avant (A-13), le curseur vit là où l'usage l'attend — entre les
 * deux caractères d'une paire, après l'indentation d'une Entrée.</p>
 */
public final class IntelligenceHub {

    /**
     * Le SPI des fournisseurs d'intelligence (M7.5, §8) : chaque
     * méthode retombe sur le moteur pur du dépôt — l'hôte n'implante
     * que ce qu'il veut détourner.
     */
    public interface Provider {

        /**
         * La frappe d'un texte validé : la fermeture des paires,
         * l'entourage de la sélection, le saut du fermant déjà
         * présent (A-05, A-08).
         *
         * @param doc       le document, jamais muté
         * @param selection la sélection courante
         * @param typed     le texte frappé
         * @param rules     les règles de frappe du langage
         * @return le plan du geste, vide pour laisser passer la frappe
         */
        default Optional<EditPlan> insertTyped(Document doc,
                MultiSelection selection, String typed, TypingRules rules) {
            return SmartTyping.insert(doc, selection, typed, rules);
        }

        /**
         * L'Entrée : le saut porte l'indentation attendue, le fermant
         * qui reste sur la ligne descend seul sous le curseur (A-06).
         *
         * @param doc       le document, jamais muté
         * @param analysis  l'analyse courante, éventuellement absente
         * @param selection la sélection courante
         * @param style     le style d'indentation de l'hôte
         * @return le plan du geste, jamais nul
         */
        default EditPlan enter(Document doc, @Nullable Analysis analysis,
                MultiSelection selection, IndentStyle style) {
            return SmartTyping.enter(doc, analysis, selection, style);
        }

        /**
         * Le retour arrière : la paire vide disparaît entière, la
         * citation vide aussi, l'indentation excédentaire se retire
         * d'un cran (A-07).
         *
         * @param doc       le document, jamais muté
         * @param selection la sélection courante
         * @param rules     les règles de frappe du langage
         * @param style     le style d'indentation de l'hôte
         * @return le plan du geste, vide pour retomber sur le cœur
         */
        default Optional<EditPlan> backspace(Document doc,
                MultiSelection selection, TypingRules rules,
                IndentStyle style) {
            return SmartTyping.backspace(doc, selection, rules, style);
        }

        /**
         * Le collage multiligne réindenté au contexte d'accueil : la
         * première ligne complète la ligne d'accueil, les suivantes
         * s'alignent sur la profondeur du point de collage (A-09).
         *
         * @param doc      le document, jamais muté
         * @param analysis l'analyse courante, éventuellement absente
         * @param offset   le point d'accueil du collage
         * @param pasted   le texte collé
         * @param style    le style d'indentation de l'hôte
         * @return le texte à coller, réindenté
         */
        default String pasteAdjusted(Document doc, @Nullable Analysis analysis,
                int offset, String pasted, IndentStyle style) {
            return PasteReindenter.reindent(doc, analysis, offset, pasted,
                    style);
        }

        /**
         * Les candidats de complétion au curseur : les mots du code —
         * jamais les chaînes ni les commentaires — rejoints aux mots
         * réservés du langage (B-03).
         *
         * @param doc      le document porteur
         * @param analysis l'analyse courante, éventuellement absente
         * @param caret    la position du curseur
         * @param keywords les mots réservés du langage
         * @return les candidats distincts, le préfixe déjà tapé en tête
         */
        default List<String> completionCandidates(Document doc,
                @Nullable Analysis analysis, int caret,
                List<String> keywords) {
            if (analysis == null) {
                return List.of();
            }
            return CompletionMots.completer(doc, analysis, caret, keywords);
        }
    }

    /** Le style d'indentation par défaut du concentrateur : quatre espaces. */
    private static final IndentStyle DEFAULT_STYLE = IndentStyle.ESPACES_4;

    private final EditorView vue;
    private @Nullable Provider provider;

    /**
     * Construit le concentrateur d'une vue — la vue n'est consultée
     * qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire de la session et du langage
     */
    IntelligenceHub(EditorView vue) {
        this.vue = vue;
    }

    /**
     * Le fournisseur standard : les moteurs purs du dépôt, sans
     * détour — le choix de l'hôte qui veut la frappe intelligente
     * telle quelle.
     *
     * @return le fournisseur des moteurs du dépôt
     */
    public static Provider builtins() {
        return new Provider() {
        };
    }

    /**
     * Pose le fournisseur d'intelligence (M7.5, §8) : la frappe
     * intelligente, l'indentation à l'Entrée et la complétion se
     * branchent — null les coupe, la frappe passe telle quelle, la
     * vue ne décide jamais à la place de l'hôte.
     *
     * @param prochain le fournisseur, null pour couper
     */
    public void setProvider(@Nullable Provider prochain) {
        this.provider = prochain;
    }

    /** Le fournisseur courant — null quand l'intelligence est coupée. */
    public @Nullable Provider provider() {
        return provider;
    }

    /**
     * Les candidats de complétion au curseur, servis par le
     * fournisseur — les popups de l'hôte s'y abreuvent (B-03).
     *
     * @param keywords les mots réservés du langage
     * @return les candidats distincts, vides sans fournisseur
     */
    public List<String> completionCandidates(List<String> keywords) {
        Provider courant = provider;
        if (courant == null) {
            return List.of();
        }
        return courant.completionCandidates(document(), analysis(), caret(),
                keywords);
    }

    /**
     * Un texte validé arrive de l'IME : le saut seul porte
     * l'indentation, le multiligne se réindente, le reste suit les
     * règles de frappe.
     *
     * @param text le texte validé
     * @return vrai si le geste est consommé par l'intelligence
     */
    boolean onCommit(String text) {
        if ("\n".equals(text)) {
            return enterTyped();
        }
        if (text.indexOf('\n') >= 0) {
            return pasteMultiline(text);
        }
        return insertTyped(text);
    }

    /**
     * La frappe d'un texte : le plan du fournisseur, ou la main pour
     * la voie simple.
     *
     * @param typed le texte frappé
     * @return vrai si le geste est consommé
     */
    boolean insertTyped(String typed) {
        Provider courant = provider;
        if (courant == null) {
            return false;
        }
        return courant.insertTyped(document(), selection(), typed, rules())
                .map(this::apply).orElse(false);
    }

    /**
     * L'Entrée : le plan indenté du fournisseur, le saut nu sans
     * fournisseur — jamais muette.
     *
     * @return vrai, l'Entrée se consomme toujours
     */
    boolean enterTyped() {
        Provider courant = provider;
        if (courant != null) {
            return apply(courant.enter(document(), analysis(),
                    selection(), style()));
        }
        return apply(plainNewline());
    }

    /**
     * Le retour arrière : la règle du fournisseur, ou la main au cœur
     * pour la suppression par clusters.
     *
     * @return vrai si le geste est consommé
     */
    boolean backspaceTyped() {
        Provider courant = provider;
        if (courant == null) {
            return false;
        }
        return courant.backspace(document(), selection(), rules(), style())
                .map(this::apply).orElse(false);
    }

    /**
     * Le collage multiligne : le texte réindenté par le fournisseur,
     * inséré sur la plage primaire — le curseur vit à sa fin.
     *
     * @param pasted le texte collé
     * @return vrai, le collage se consomme toujours
     */
    boolean pasteMultiline(String pasted) {
        Provider courant = provider;
        String texte = courant == null ? pasted
                : courant.pasteAdjusted(document(), analysis(), caret(),
                        pasted, style());
        SelRange primaire = primaryRange();
        Document doc = document();
        String retire = doc.subText(primaire.start(), primaire.end());
        EditPlan plan = new EditPlan(
                Transaction.of(List.of(new TextEdit(primaire.start(), retire,
                        texte))),
                MultiSelection.of(SelRange.caretAt(primaire.start()
                        + texte.length())));
        return apply(plan);
    }

    /** Le saut de ligne nu : chaque plage remplacée, curseur après le saut. */
    private EditPlan plainNewline() {
        Document doc = document();
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (SelRange plage : selection().ranges()) {
            String retire = doc.subText(plage.start(), plage.end());
            editions.add(new TextEdit(plage.start() + delta, retire, "\n"));
            apres.add(SelRange.caretAt(plage.start() + delta + 1));
            delta += 1 - retire.length();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /** Applique un plan par la voie unique, avec dévoilement du curseur. */
    private boolean apply(EditPlan plan) {
        if (!vue.session().apply(plan.transaction(), plan.selection())) {
            return false;
        }
        vue.curseurClavier.revealCaret();
        return true;
    }

    private SelRange primaryRange() {
        List<SelRange> plages = selection().ranges();
        return plages.isEmpty() ? SelRange.caretAt(0) : plages.get(0);
    }

    private Document document() {
        return vue.session().document();
    }

    private MultiSelection selection() {
        return vue.session().selection();
    }

    private int caret() {
        return primaryRange().caret();
    }

    /**
     * Les règles du langage courant : la configuration de son support
     * (M7.6) — les règles génériques sans langage.
     */
    private TypingRules rules() {
        LanguageSupport langue = vue.syntaxe.language();
        return langue == null
                ? TypingRules.generic()
                : TypingRules.of(langue.configuration());
    }

    private IndentStyle style() {
        return DEFAULT_STYLE;
    }

    /** L'analyse publiée à la révision courante — jamais celle d'une autre. */
    private @Nullable Analysis analysis() {
        return vue.syntaxe.analyseCourante(document());
    }
}
