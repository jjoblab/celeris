package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;


/**
 * Les capacités du serveur, lues de la réponse d'initialisation — la
 * surface bornée du protocole que le client consomme : fournisseurs de
 * complétion, de survol, de signature, de définition, de références, de
 * renommage, de correctifs, de formatage et d'indices en ligne, genre
 * de synchronisation, légende des jetons sémantiques. La valeur brute
 * reste accessible.
 */
public final class CapacitesServeur {

    private final JsonObjet capacites;

    private CapacitesServeur(JsonObjet capacites) {
        this.capacites = capacites;
    }

    /** Les capacités vides — avant l'initialisation. */
    static CapacitesServeur vides() {
        return new CapacitesServeur(JsonObjet.vide());
    }

    /**
     * Analyse les capacités depuis la réponse d'initialisation.
     *
     * @param resultat la valeur « result » de la requête initialize
     * @return les capacités lues, vides si la forme est incomplète
     */
    public static CapacitesServeur de(ValeurJson resultat) {
        if (resultat instanceof JsonObjet objet
                && objet.membre("capabilities") instanceof JsonObjet capacites) {
            return new CapacitesServeur(capacites);
        }
        return new CapacitesServeur(JsonObjet.vide());
    }

    /** Les capacités brutes du serveur. */
    public JsonObjet brutes() {
        return capacites;
    }

    /** Le genre de synchronisation (1 complète, 2 incrémentale), -1 si absent. */
    public int genreSynchro() {
        Long genre = capacites.membreEntier("textDocumentSync");
        return genre == null ? -1 : genre.intValue();
    }

    /** Vrai quand le serveur propose la complétion. */
    public boolean sertCompletion() {
        return capacites.membre("completionProvider") != null;
    }

    /** Vrai quand le serveur propose le survol. */
    public boolean sertSurvol() {
        return Boolean.TRUE.equals(capacites.membreBooleen("hoverProvider"));
    }

    /** Vrai quand le serveur propose l'aide aux paramètres. */
    public boolean sertSignature() {
        return capacites.membre("signatureHelpProvider") != null;
    }

    /** Vrai quand le serveur propose la définition. */
    public boolean sertDefinition() {
        return Boolean.TRUE.equals(capacites.membreBooleen("definitionProvider"));
    }

    /** Vrai quand le serveur propose les références. */
    public boolean sertReferences() {
        return Boolean.TRUE.equals(capacites.membreBooleen("referencesProvider"));
    }

    /** Vrai quand le serveur propose le renommage. */
    public boolean sertRennomage() {
        return capacites.membre("renameProvider") != null;
    }

    /** Vrai quand le serveur propose les correctifs rapides. */
    public boolean sertCorrectifs() {
        return capacites.membre("codeActionProvider") != null;
    }

    /** Vrai quand le serveur propose le formatage du document. */
    public boolean sertFormatage() {
        return Boolean.TRUE.equals(capacites.membreBooleen("documentFormattingProvider"));
    }

    /** Vrai quand le serveur propose les indices en ligne. */
    public boolean sertIndices() {
        return capacites.membre("inlayHintProvider") != null;
    }

    /** La légende des types de jetons sémantiques, vide si le serveur n'en propose pas. */
    public List<String> typesJetons() {
        return legende("tokenTypes");
    }

    /** La légende des modificateurs de jetons sémantiques, vide sans légende. */
    public List<String> modificateursJetons() {
        return legende("tokenModifiers");
    }

    private List<String> legende(String cle) {
        List<String> valeurs = new ArrayList<>();
        if (capacites.membre("semanticTokensProvider") instanceof JsonObjet fournisseur
                && fournisseur.membre("legend") instanceof JsonObjet legende
                && legende.membre(cle) instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                String texte = tableau.element(i) instanceof JsonChaine chaine
                        ? chaine.texte() : null;
                if (texte != null) {
                    valeurs.add(texte);
                }
            }
        }
        return List.copyOf(valeurs);
    }
}
