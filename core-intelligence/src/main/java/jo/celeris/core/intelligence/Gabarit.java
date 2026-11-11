package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Un gabarit dynamique (M5, A-22) : le rendu — le corps où chaque
 * {@code ${variable}} a laissé sa valeur par défaut, vide sans
 * deux-points — et ses trous, dans l'ordre du rendu : l'ordre de
 * tabulation. Les occurrences d'une même variable sont liées (les
 * miroirs, {@link Gabarits#miroirs}).
 *
 * <p>Le gabarit riche (chantier C7) porte les trous à choix et les
 * macros. Le trou à choix — {@code ${fruit|pomme,poire,banane|}} —
 * rend son premier choix, la liste fermée vit au trou
 * ({@link Trou#choix()}). Les macros — la table de l'hôte, noms en
 * MAJUSCULES à la manière du dépôt de départ ({@code $FILE$},
 * {@code $DATE$}, {@code $USER$}) — se résolvent au rendu : la
 * variable macro devient sa valeur, SANS trou — elle n'attend pas la
 * frappe ; la variable inconnue de la table reste un trou libre, son
 * défaut vide ou déclaré.</p>
 *
 * @param rendu le corps rendu, défauts en place
 * @param trous les occurrences, dans l'ordre du rendu
 */
public record Gabarit(String rendu, List<Trou> trous) {

    /**
     * Constructeur canonique : composantes figées, jamais nulles.
     */
    public Gabarit {
        java.util.Objects.requireNonNull(rendu, "rendu");
        java.util.Objects.requireNonNull(trous, "trous");
        trous = List.copyOf(trous);
    }

    /**
     * Analyse le corps d'un gabarit, sans macros : chaque
     * {@code ${nom}}, {@code ${nom:defaut}} et {@code ${nom|a,b,c|}}
     * devient un trou — le défaut du trou à choix est son premier
     * choix —, les autres segments restent littéraux.
     *
     * @param corps le corps du gabarit
     * @return le gabarit rendu et analysé
     * @throws IllegalArgumentException si une balise n'est pas fermée
     */
    public static Gabarit of(String corps) {
        return of(corps, Map.of());
    }

    /**
     * Analyse le corps d'un gabarit contre la table des macros de
     * l'hôte : la variable macro se résout à sa valeur, sans trou ;
     * les autres deviennent des trous — défaut déclaré, premier choix
     * d'un trou à choix, vide sinon.
     *
     * @param corps  le corps du gabarit
     * @param macros la table des macros de l'hôte — noms en
     *               MAJUSCULES, valeurs rendues telles quelles
     * @return le gabarit rendu et analysé
     * @throws IllegalArgumentException si une balise n'est pas fermée
     */
    public static Gabarit of(String corps, Map<String, String> macros) {
        StringBuilder rendu = new StringBuilder();
        List<Trou> trous = new ArrayList<>();
        int i = 0;
        while (i < corps.length()) {
            int ouverture = corps.indexOf("${", i);
            if (ouverture < 0) {
                rendu.append(corps, i, corps.length());
                break;
            }
            int fermeture = corps.indexOf('}', ouverture + 2);
            if (fermeture < 0) {
                throw new IllegalArgumentException("unclosed placeholder at " + ouverture);
            }
            rendu.append(corps, i, ouverture);
            String nom = corps.substring(ouverture + 2, fermeture);
            String macro = macros.get(variableDe(nom, ouverture));
            if (macro != null) {
                // la macro se résout au rendu : elle n'attend pas la frappe
                rendu.append(macro);
            } else {
                ajouterTrou(rendu, trous, nom, ouverture);
            }
            i = fermeture + 1;
        }
        return new Gabarit(rendu.toString(), trous);
    }

    /**
     * Le nom de variable d'une balise — avant son premier séparateur,
     * jamais vide.
     */
    private static String variableDe(String nom, int ouverture) {
        int separateur = premierSeparateur(nom);
        String variable = separateur < 0 ? nom : nom.substring(0, separateur);
        if (variable.isBlank()) {
            throw new IllegalArgumentException("blank variable at " + ouverture);
        }
        return variable;
    }

    /**
     * Le trou d'une balise non-macro se pose au rendu : son défaut —
     * déclaré, premier choix d'un trou à choix, vide sinon —, ses
     * choix, ses bornes.
     */
    private static void ajouterTrou(StringBuilder rendu, List<Trou> trous,
            String nom, int ouverture) {
        int separateur = premierSeparateur(nom);
        String variable = variableDe(nom, ouverture);
        String defaut = "";
        List<String> choix = List.of();
        if (separateur >= 0 && nom.charAt(separateur) == ':') {
            defaut = nom.substring(separateur + 1);
        } else if (separateur >= 0 && nom.charAt(separateur) == '|') {
            int fermetureChoix = nom.lastIndexOf('|');
            if (fermetureChoix <= separateur) {
                throw new IllegalArgumentException("unclosed choice at " + ouverture);
            }
            choix = litChoix(nom.substring(separateur + 1, fermetureChoix),
                    ouverture);
            defaut = choix.get(0);
        }
        int debut = rendu.length();
        rendu.append(defaut);
        trous.add(new Trou(debut, debut + defaut.length(), variable, choix));
    }

    /**
     * La position du premier séparateur de nom — deux-points du défaut
     * ou barre du choix, —1 pour un nom seul.
     */
    private static int premierSeparateur(String nom) {
        for (int i = 0; i < nom.length(); i++) {
            char c = nom.charAt(i);
            if (c == ':' || c == '|') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Les choix d'un trou à choix — la liste fermée, les doublons
     * retirés, chaque choix non vide.
     */
    private static List<String> litChoix(String liste, int ouverture) {
        Set<String> distincts = new LinkedHashSet<>();
        for (String choix : liste.split(",", -1)) {
            if (choix.isBlank()) {
                throw new IllegalArgumentException(
                        "blank choice at " + ouverture);
            }
            distincts.add(choix);
        }
        if (distincts.isEmpty()) {
            throw new IllegalArgumentException("blank choice at " + ouverture);
        }
        return List.copyOf(distincts);
    }

    /**
     * Les variables distinctes, dans l'ordre de leur première occurrence
     * — l'ordre de tabulation des variables.
     *
     * @return les noms distincts
     */
    public List<String> variables() {
        Set<String> noms = new LinkedHashSet<>();
        for (Trou trou : trous) {
            noms.add(trou.variable());
        }
        return List.copyOf(noms);
    }

    /**
     * Les occurrences d'une variable, liées entre elles.
     *
     * @param variable le nom de la variable
     * @return ses occurrences, dans l'ordre du rendu
     */
    public List<Trou> occurrencesDe(String variable) {
        List<Trou> occurrences = new ArrayList<>();
        for (Trou trou : trous) {
            if (trou.variable().equals(variable)) {
                occurrences.add(trou);
            }
        }
        return occurrences;
    }
}
