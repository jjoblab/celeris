package jo.celeris.core.lsp;

import java.util.List;

/**
 * Les capacités du serveur, lues de la réponse d'initialisation — la
 * surface bornée du protocole que le client consomme : fournisseurs de
 * complétion, de survol, de signature, de définition, de références, de
 * surlignages, de symboles, de plis, de sélection, de renommage, de
 * correctifs, de formatage et d'indices en ligne, genre de
 * synchronisation, légende des jetons sémantiques.
 *
 * <p>L'objet se construit par la fabrique typée (ADR-015) : la
 * correspondance de la frontière protocolaire y dépose ce que la
 * bibliothèque a négocié à la poignée de main.</p>
 */
public final class ServerCapsLsp {

    private final int syncKind;
    private final boolean supportsCompletion;
    private final boolean supportsHover;
    private final boolean supportsSignature;
    private final boolean supportsDefinition;
    private final boolean supportsReferences;
    private final boolean supportsHighlights;
    private final boolean supportsSymbols;
    private final boolean supportsFolds;
    private final boolean supportsRangeSelection;
    private final boolean supportsRename;
    private final boolean supportsCodeActions;
    private final boolean supportsFormatting;
    private final boolean supportsInlayHints;
    private final List<String> tokenTypes;
    private final List<String> tokenModifiers;

    private ServerCapsLsp(int syncKind, boolean supportsCompletion,
            boolean supportsHover, boolean supportsSignature, boolean supportsDefinition,
            boolean supportsReferences, boolean supportsHighlights, boolean supportsSymbols,
            boolean supportsFolds, boolean supportsRangeSelection, boolean supportsRename,
            boolean supportsCodeActions, boolean supportsFormatting, boolean supportsInlayHints,
            List<String> tokenTypes, List<String> tokenModifiers) {
        this.syncKind = syncKind;
        this.supportsCompletion = supportsCompletion;
        this.supportsHover = supportsHover;
        this.supportsSignature = supportsSignature;
        this.supportsDefinition = supportsDefinition;
        this.supportsReferences = supportsReferences;
        this.supportsHighlights = supportsHighlights;
        this.supportsSymbols = supportsSymbols;
        this.supportsFolds = supportsFolds;
        this.supportsRangeSelection = supportsRangeSelection;
        this.supportsRename = supportsRename;
        this.supportsCodeActions = supportsCodeActions;
        this.supportsFormatting = supportsFormatting;
        this.supportsInlayHints = supportsInlayHints;
        this.tokenTypes = List.copyOf(tokenTypes);
        this.tokenModifiers = List.copyOf(tokenModifiers);
    }

    /** Les capacités vides — avant l'initialisation. */
    public static ServerCapsLsp empty() {
        return of(-1, false, false, false, false, false, false, false,
                false, false, false, false, false, false, List.of(), List.of());
    }

    /**
     * Construit les capacités de leurs champs résolus — la voie typée
     * de la frontière protocolaire (ADR-015) : la correspondance y
     * dépose ce que la bibliothèque a négocié.
     *
     * @param syncKind        le genre de synchronisation (1
     *                            complète, 2 incrémentale), -1 si absent
     * @param supportsCompletion      le serveur propose la complétion
     * @param supportsHover          le serveur propose le survol
     * @param supportsSignature       le serveur propose l'aide aux paramètres
     * @param supportsDefinition      le serveur propose la définition
     * @param supportsReferences      le serveur propose les références
     * @param supportsHighlights     le serveur propose les surlignages
     *                            d'occurrences
     * @param supportsSymbols        le serveur propose les symboles de
     *                            document
     * @param supportsFolds            le serveur propose les plis
     * @param supportsRangeSelection le serveur propose les plages de
     *                            sélection
     * @param supportsRename       le serveur propose le renommage
     * @param supportsCodeActions      le serveur propose les correctifs rapides
     * @param supportsFormatting       le serveur propose le formatage du document
     * @param supportsInlayHints         le serveur propose les indices en ligne
     * @param tokenTypes         la légende des types de jetons, vide sans légende
     * @param tokenModifiers la légende des modificateurs, vide sans légende
     * @return les capacités construites
     */
    public static ServerCapsLsp of(int syncKind, boolean supportsCompletion,
            boolean supportsHover, boolean supportsSignature, boolean supportsDefinition,
            boolean supportsReferences, boolean supportsHighlights, boolean supportsSymbols,
            boolean supportsFolds, boolean supportsRangeSelection, boolean supportsRename,
            boolean supportsCodeActions, boolean supportsFormatting, boolean supportsInlayHints,
            List<String> tokenTypes, List<String> tokenModifiers) {
        return new ServerCapsLsp(syncKind, supportsCompletion, supportsHover,
                supportsSignature, supportsDefinition, supportsReferences, supportsHighlights,
                supportsSymbols, supportsFolds, supportsRangeSelection, supportsRename,
                supportsCodeActions, supportsFormatting, supportsInlayHints, tokenTypes,
                tokenModifiers);
    }

    /** Le genre de synchronisation (1 complète, 2 incrémentale), -1 si absent. */
    public int syncKind() {
        return syncKind;
    }

    /** Vrai quand le serveur propose la complétion. */
    public boolean supportsCompletion() {
        return supportsCompletion;
    }

    /** Vrai quand le serveur propose le survol. */
    public boolean supportsHover() {
        return supportsHover;
    }

    /** Vrai quand le serveur propose l'aide aux paramètres. */
    public boolean supportsSignature() {
        return supportsSignature;
    }

    /** Vrai quand le serveur propose la définition. */
    public boolean supportsDefinition() {
        return supportsDefinition;
    }

    /** Vrai quand le serveur propose les références. */
    public boolean supportsReferences() {
        return supportsReferences;
    }

    /** Vrai quand le serveur propose les surlignages d'occurrences. */
    public boolean supportsHighlights() {
        return supportsHighlights;
    }

    /** Vrai quand le serveur propose les symboles de document. */
    public boolean supportsSymbols() {
        return supportsSymbols;
    }

    /** Vrai quand le serveur propose les plis. */
    public boolean supportsFolds() {
        return supportsFolds;
    }

    /** Vrai quand le serveur propose les plages de sélection. */
    public boolean supportsRangeSelection() {
        return supportsRangeSelection;
    }

    /** Vrai quand le serveur propose le renommage. */
    public boolean supportsRename() {
        return supportsRename;
    }

    /** Vrai quand le serveur propose les correctifs rapides. */
    public boolean supportsCodeActions() {
        return supportsCodeActions;
    }

    /** Vrai quand le serveur propose le formatage du document. */
    public boolean supportsFormatting() {
        return supportsFormatting;
    }

    /** Vrai quand le serveur propose les indices en ligne. */
    public boolean supportsInlayHints() {
        return supportsInlayHints;
    }

    /** La légende des types de jetons sémantiques, vide si le serveur n'en propose pas. */
    public List<String> tokenTypes() {
        return tokenTypes;
    }

    /** La légende des modificateurs de jetons sémantiques, vide sans légende. */
    public List<String> tokenModifiers() {
        return tokenModifiers;
    }
}
