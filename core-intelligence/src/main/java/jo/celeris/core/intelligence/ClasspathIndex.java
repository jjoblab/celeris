package jo.celeris.core.intelligence;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L'index de classpath (chantier C8) : le nom simple des types connus
 * vers leurs noms pleinement qualifiés — l'index que l'hôte tient de
 * son classpath, de son serveur de langage ou de son build. Valeur
 * pure : les noms qualifiés se valident à la construction (segments
 * non vides, séparés par le point), les doublons se retirent, l'ordre
 * de déclaration se conserve — l'ordre des candidats est déterministe.
 */
public final class ClasspathIndex {

    private final Map<String, List<String>> nomsVersFqn;

    private ClasspathIndex(Map<String, List<String>> nomsVersFqn) {
        this.nomsVersFqn = nomsVersFqn;
    }

    /**
     * Construit l'index d'une table de déclaration.
     *
     * @param noms la table nom simple vers ses noms qualifiés, dans
     *             l'ordre de préférence de l'hôte
     * @return l'index validé, les doublons retirés
     * @throws IllegalArgumentException si un nom est vide ou un nom
     *         qualifié mal formé (segment vide, séparé par le point)
     */
    public static ClasspathIndex of(Map<String, List<String>> noms) {
        Map<String, List<String>> validee = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entree : noms.entrySet()) {
            String simple = entree.getKey();
            if (simple == null || simple.isBlank()) {
                throw new IllegalArgumentException("blank simple name");
            }
            Set<String> distincts = new LinkedHashSet<>();
            for (String fqn : entree.getValue()) {
                if (fqn == null || !estQualifie(fqn)) {
                    throw new IllegalArgumentException(
                            "malformed qualified name: " + fqn);
                }
                if (simpleDe(fqn).equals(simple)) {
                    distincts.add(fqn);
                }
            }
            validee.put(simple, List.copyOf(distincts));
        }
        return new ClasspathIndex(Map.copyOf(validee));
    }

    /** Vrai quand le nom est un nom qualifié bien formé — segments non vides. */
    private static boolean estQualifie(String fqn) {
        if (fqn.isBlank() || fqn.startsWith(".") || fqn.endsWith(".")) {
            return false;
        }
        for (int i = 0; i < fqn.length(); i++) {
            if (fqn.charAt(i) == '.' && fqn.charAt(i + 1) == '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Les noms qualifiés d'un nom simple, dans l'ordre de déclaration
     * de l'hôte — l'ordre des candidats.
     *
     * @param nomSimple le nom simple cherché
     * @return ses noms qualifiés, vides pour un nom inconnu
     */
    public List<String> fqnDe(String nomSimple) {
        return nomsVersFqn.getOrDefault(nomSimple, List.of());
    }

    /** Le nombre de noms simples connus de l'index. */
    public int taille() {
        return nomsVersFqn.size();
    }

    /** Le segment final d'un nom qualifié — son nom simple. */
    private static String simpleDe(String fqn) {
        int point = fqn.lastIndexOf('.');
        return point < 0 ? fqn : fqn.substring(point + 1);
    }
}
