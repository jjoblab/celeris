package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Un objet JSON : les membres dans l'ordre d'insertion (le protocole
 * compte dessus pour ses différences de version), immuable par copie —
 * {@link #ajouter} rend un nouvel objet, l'original ne bouge jamais.
 */
public final class JsonObjet implements ValeurJson {

    private final Map<String, ValeurJson> membres;

    private JsonObjet(Map<String, ValeurJson> membres) {
        this.membres = membres;
    }

    /** L'objet vide. */
    public static JsonObjet vide() {
        return new JsonObjet(new LinkedHashMap<>());
    }

    /**
     * Rend un nouvel objet : celui-ci plus le membre donné — le doublon
     * de clé écrase l'ancienne valeur, comme à l'analyse.
     *
     * @param cle     la clé du membre
     * @param valeur  la valeur du membre
     * @return la copie étendue, this n'est pas modifié
     */
    public JsonObjet ajouter(String cle, ValeurJson valeur) {
        Map<String, ValeurJson> copie = new LinkedHashMap<>(membres);
        copie.put(cle, valeur);
        return new JsonObjet(copie);
    }

    /** Le nombre de membres distincts. */
    public int taille() {
        return membres.size();
    }

    /** Les clés dans l'ordre d'insertion. */
    public List<String> cles() {
        return new ArrayList<>(membres.keySet());
    }

    /**
     * La valeur d'un membre.
     *
     * @param cle la clé cherchée
     * @return la valeur, ou null si la clé est absente (un membre
     *         explicitement null rend {@link JsonNul}, pas null)
     */
    public @Nullable ValeurJson membre(String cle) {
        return membres.get(cle);
    }

    /**
     * La valeur d'un membre ou le défaut.
     *
     * @param cle    la clé cherchée
     * @param defaut la valeur rendue quand la clé est absente
     * @return la valeur du membre, ou le défaut
     */
    public ValeurJson membreOu(String cle, ValeurJson defaut) {
        ValeurJson valeur = membres.get(cle);
        return valeur == null ? defaut : valeur;
    }

    /**
     * Le texte d'un membre chaîne.
     *
     * @param cle la clé cherchée
     * @return le texte, ou null si la clé est absente ou n'est pas une
     *         chaîne
     */
    public @Nullable String membreTexte(String cle) {
        ValeurJson valeur = membres.get(cle);
        return valeur instanceof JsonChaine chaine ? chaine.texte() : null;
    }

    /**
     * L'entier d'un membre nombre.
     *
     * @param cle la clé cherchée
     * @return l'entier, ou null si la clé est absente ou n'est pas un
     *         nombre
     */
    public @Nullable Long membreEntier(String cle) {
        ValeurJson valeur = membres.get(cle);
        return valeur instanceof JsonNombre nombre ? Long.valueOf(nombre.entier()) : null;
    }

    /**
     * Le booléen d'un membre.
     *
     * @param cle la clé cherchée
     * @return le booléen, ou null si la clé est absente ou n'est pas un
     *         booléen
     */
    public @Nullable Boolean membreBooleen(String cle) {
        ValeurJson valeur = membres.get(cle);
        return valeur instanceof JsonBooleen booleen
                ? Boolean.valueOf(booleen.valeur()) : null;
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof JsonObjet objet && membres.equals(objet.membres);
    }

    @Override
    public int hashCode() {
        return membres.hashCode();
    }

    @Override
    public String toString() {
        return JsonEcrivain.ecrire(this);
    }
}
