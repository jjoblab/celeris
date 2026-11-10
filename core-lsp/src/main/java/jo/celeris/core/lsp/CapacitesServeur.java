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
public final class CapacitesServeur {

    private final int genreSynchro;
    private final boolean sertCompletion;
    private final boolean sertSurvol;
    private final boolean sertSignature;
    private final boolean sertDefinition;
    private final boolean sertReferences;
    private final boolean sertSurlignages;
    private final boolean sertSymboles;
    private final boolean sertPlis;
    private final boolean sertSelectionPlages;
    private final boolean sertRennomage;
    private final boolean sertCorrectifs;
    private final boolean sertFormatage;
    private final boolean sertIndices;
    private final List<String> typesJetons;
    private final List<String> modificateursJetons;

    private CapacitesServeur(int genreSynchro, boolean sertCompletion,
            boolean sertSurvol, boolean sertSignature, boolean sertDefinition,
            boolean sertReferences, boolean sertSurlignages, boolean sertSymboles,
            boolean sertPlis, boolean sertSelectionPlages, boolean sertRennomage,
            boolean sertCorrectifs, boolean sertFormatage, boolean sertIndices,
            List<String> typesJetons, List<String> modificateursJetons) {
        this.genreSynchro = genreSynchro;
        this.sertCompletion = sertCompletion;
        this.sertSurvol = sertSurvol;
        this.sertSignature = sertSignature;
        this.sertDefinition = sertDefinition;
        this.sertReferences = sertReferences;
        this.sertSurlignages = sertSurlignages;
        this.sertSymboles = sertSymboles;
        this.sertPlis = sertPlis;
        this.sertSelectionPlages = sertSelectionPlages;
        this.sertRennomage = sertRennomage;
        this.sertCorrectifs = sertCorrectifs;
        this.sertFormatage = sertFormatage;
        this.sertIndices = sertIndices;
        this.typesJetons = List.copyOf(typesJetons);
        this.modificateursJetons = List.copyOf(modificateursJetons);
    }

    /** Les capacités vides — avant l'initialisation. */
    public static CapacitesServeur vides() {
        return de(-1, false, false, false, false, false, false, false,
                false, false, false, false, false, false, List.of(), List.of());
    }

    /**
     * Construit les capacités de leurs champs résolus — la voie typée
     * de la frontière protocolaire (ADR-015) : la correspondance y
     * dépose ce que la bibliothèque a négocié.
     *
     * @param genreSynchro        le genre de synchronisation (1
     *                            complète, 2 incrémentale), -1 si absent
     * @param sertCompletion      le serveur propose la complétion
     * @param sertSurvol          le serveur propose le survol
     * @param sertSignature       le serveur propose l'aide aux paramètres
     * @param sertDefinition      le serveur propose la définition
     * @param sertReferences      le serveur propose les références
     * @param sertSurlignages     le serveur propose les surlignages
     *                            d'occurrences
     * @param sertSymboles        le serveur propose les symboles de
     *                            document
     * @param sertPlis            le serveur propose les plis
     * @param sertSelectionPlages le serveur propose les plages de
     *                            sélection
     * @param sertRennomage       le serveur propose le renommage
     * @param sertCorrectifs      le serveur propose les correctifs rapides
     * @param sertFormatage       le serveur propose le formatage du document
     * @param sertIndices         le serveur propose les indices en ligne
     * @param typesJetons         la légende des types de jetons, vide sans légende
     * @param modificateursJetons la légende des modificateurs, vide sans légende
     * @return les capacités construites
     */
    public static CapacitesServeur de(int genreSynchro, boolean sertCompletion,
            boolean sertSurvol, boolean sertSignature, boolean sertDefinition,
            boolean sertReferences, boolean sertSurlignages, boolean sertSymboles,
            boolean sertPlis, boolean sertSelectionPlages, boolean sertRennomage,
            boolean sertCorrectifs, boolean sertFormatage, boolean sertIndices,
            List<String> typesJetons, List<String> modificateursJetons) {
        return new CapacitesServeur(genreSynchro, sertCompletion, sertSurvol,
                sertSignature, sertDefinition, sertReferences, sertSurlignages,
                sertSymboles, sertPlis, sertSelectionPlages, sertRennomage,
                sertCorrectifs, sertFormatage, sertIndices, typesJetons,
                modificateursJetons);
    }

    /** Le genre de synchronisation (1 complète, 2 incrémentale), -1 si absent. */
    public int genreSynchro() {
        return genreSynchro;
    }

    /** Vrai quand le serveur propose la complétion. */
    public boolean sertCompletion() {
        return sertCompletion;
    }

    /** Vrai quand le serveur propose le survol. */
    public boolean sertSurvol() {
        return sertSurvol;
    }

    /** Vrai quand le serveur propose l'aide aux paramètres. */
    public boolean sertSignature() {
        return sertSignature;
    }

    /** Vrai quand le serveur propose la définition. */
    public boolean sertDefinition() {
        return sertDefinition;
    }

    /** Vrai quand le serveur propose les références. */
    public boolean sertReferences() {
        return sertReferences;
    }

    /** Vrai quand le serveur propose les surlignages d'occurrences. */
    public boolean sertSurlignages() {
        return sertSurlignages;
    }

    /** Vrai quand le serveur propose les symboles de document. */
    public boolean sertSymboles() {
        return sertSymboles;
    }

    /** Vrai quand le serveur propose les plis. */
    public boolean sertPlis() {
        return sertPlis;
    }

    /** Vrai quand le serveur propose les plages de sélection. */
    public boolean sertSelectionPlages() {
        return sertSelectionPlages;
    }

    /** Vrai quand le serveur propose le renommage. */
    public boolean sertRennomage() {
        return sertRennomage;
    }

    /** Vrai quand le serveur propose les correctifs rapides. */
    public boolean sertCorrectifs() {
        return sertCorrectifs;
    }

    /** Vrai quand le serveur propose le formatage du document. */
    public boolean sertFormatage() {
        return sertFormatage;
    }

    /** Vrai quand le serveur propose les indices en ligne. */
    public boolean sertIndices() {
        return sertIndices;
    }

    /** La légende des types de jetons sémantiques, vide si le serveur n'en propose pas. */
    public List<String> typesJetons() {
        return typesJetons;
    }

    /** La légende des modificateurs de jetons sémantiques, vide sans légende. */
    public List<String> modificateursJetons() {
        return modificateursJetons;
    }
}
