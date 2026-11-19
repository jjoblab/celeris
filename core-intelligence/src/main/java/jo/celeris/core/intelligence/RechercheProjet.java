package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * La recherche dans le projet (A-21) : la requête se pose sur le projet
 * embarqué — les fichiers ouverts, hors ligne, aucun réseau ni accès
 * disque. Elle s'adosse à l'index (B-11) : la requête qui nomme un
 * identifiant entier suit la voie de l'index — les occurrences déjà
 * comptées servent, jamais les chaînes ni les commentaires (les jetons
 * du lexique ont décidé à l'indexation) ; la requête partielle suit la
 * voie du balayage — chaque fichier se lit de bout en bout, le texte
 * brut décide. L'ordre est déterministe : les uris du dictionnaire,
 * puis les offsets croissants.
 */
public final class RechercheProjet {

    private RechercheProjet() {
    }

    /** Une trouvaille : l'uri du fichier porteur, l'offset du motif. */
    public record Trouvaille(String uri, int offset) {
    }

    /**
     * Cherche le motif dans le projet embarqué, en s'adossant à
     * l'index quand il en dispose.
     *
     * @param projet les fichiers du projet, uri vers document — le
     *               projet embarqué, hors ligne
     * @param index  l'index des symboles du projet, ou null : la
     *               voie du balayage seule
     * @param pattern  le motif cherché, identifiant entier ou texte
     *               partiel
     * @return les trouvailles, uris du dictionnaire puis offsets
     *         croissants
     * @throws IllegalArgumentException si le motif est vide
     */
    public static List<Trouvaille> search(Map<String, Document> projet,
            @Nullable IndexSymboles index, String pattern) {
        Objects.requireNonNull(projet, "projet");
        Objects.requireNonNull(pattern, "motif");
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("motif must not be empty");
        }
        if (index != null && estIdentifiant(pattern)) {
            List<Trouvaille> trouvailles = new ArrayList<>();
            for (IndexSymboles.Usage usage : index.usages(pattern)) {
                trouvailles.add(new Trouvaille(usage.uri(), usage.offset()));
            }
            return List.copyOf(trouvailles);
        }
        List<Trouvaille> trouvailles = new ArrayList<>();
        for (Map.Entry<String, Document> fichier : new TreeMap<>(projet).entrySet()) {
            String text = fichier.getValue().text();
            int offset = text.indexOf(pattern);
            while (offset >= 0) {
                trouvailles.add(new Trouvaille(fichier.getKey(), offset));
                offset = text.indexOf(pattern, offset + pattern.length());
            }
        }
        return List.copyOf(trouvailles);
    }

    /**
     * Vrai quand le motif est un identifiant entier : une lettre,
     * un trait bas ou un dollar en tête, jamais de blanc ni de
     * ponctuation — la voie de l'index se l'autorise, le balayage
     * prend le reste.
     */
    private static boolean estIdentifiant(String pattern) {
        if (!Character.isJavaIdentifierStart(pattern.charAt(0))) {
            return false;
        }
        for (int i = 1; i < pattern.length(); i++) {
            if (!Character.isJavaIdentifierPart(pattern.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
