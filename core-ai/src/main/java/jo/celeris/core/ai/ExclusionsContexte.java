package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Le fichier d'exclusions de contexte (F-07, ADR-012) : les chemins
 * jamais envoyés à un modèle — secrets, clés, données du
 * propriétaire. Le contrôle s'en prend à la lecture du fichier, le
 * garde ({@link GardeContexte}) le consulte avant chaque départ.
 *
 * <p>Le format, une règle par ligne : les lignes vides et les
 * commentaires ({@code #}) s'ignorent ; un chemin exact exclut ce
 * chemin ; un dossier (barre oblique finale, ou ancêtre du chemin
 * visé) exclut tout son contenu ; un joker ({@code *.jks},
 * {@code cle-*}) exclut tout ce qui y colle, barres obliques
 * comprises.</p>
 */
public final class ExclusionsContexte {

    private final List<String> regles;

    private ExclusionsContexte(List<String> regles) {
        this.regles = List.copyOf(regles);
    }

    /**
     * Lit le fichier d'exclusions : une règle par ligne, commentaires
     * et lignes vides ignorées.
     *
     * @param contenuFichier le contenu du fichier d'exclusions
     * @return les exclusions prêtes à consulter
     */
    public static ExclusionsContexte de(String contenuFichier) {
        List<String> regles = new ArrayList<>();
        for (String ligne : contenuFichier.split("\n", -1)) {
            String nue = ligne.strip();
            if (!nue.isEmpty() && !nue.startsWith("#")) {
                regles.add(nue);
            }
        }
        return new ExclusionsContexte(regles);
    }

    /**
     * Les exclusions vides : rien n'est jamais exclu.
     *
     * @return des exclusions sans règle
     */
    public static ExclusionsContexte aucune() {
        return new ExclusionsContexte(List.of());
    }

    /**
     * Le chemin demandé est-il exclu ? Un chemin exact se refuse, un
     * dossier refuse tout son contenu, un joker refuse tout ce qui y
     * colle.
     *
     * @param chemin le chemin du document visé, normalisé
     * @return vrai si ce chemin ne part jamais vers un modèle
     */
    public boolean estExclu(String chemin) {
        for (String regle : regles) {
            if (regleExclut(regle, chemin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * La règle exclut-elle le chemin ? Dossier (barre finale ou
     * ancêtre), chemin exact, ou joker.
     */
    private static boolean regleExclut(String regle, String chemin) {
        if (regle.endsWith("/")) {
            String dossier = regle.substring(0, regle.length() - 1);
            return chemin.startsWith(dossier + "/");
        }
        if (regle.indexOf('*') >= 0) {
            return jokerColle(regle, chemin);
        }
        return chemin.equals(regle) || chemin.startsWith(regle + "/");
    }

    /**
     * Le joker colle-t-il ? Les segments autour des astérisques se
     * recollent dans l'ordre — {@code cle-*.pem} prend
     * {@code cle-prod.pem} comme {@code cle-test.pem}, l'astérisque
     * traverse les barres obliques. Une règle sans barre oblique vaut
     * pour le chemin entier et pour chacun de ses suffixes de
     * composantes — {@code *.jks} prend {@code certs/debug.jks}.
     */
    private static boolean jokerColle(String regle, String chemin) {
        if (jokerColleAuChemin(regle, chemin)) {
            return true;
        }
        if (regle.indexOf('/') >= 0) {
            return false;
        }
        int barre = chemin.lastIndexOf('/');
        while (barre >= 0) {
            if (jokerColleAuChemin(regle, chemin.substring(barre + 1))) {
                return true;
            }
            barre = chemin.lastIndexOf('/', barre - 1);
        }
        return false;
    }

    /**
     * Le joker colle-t-il au chemin entier ? Les segments se recollent
     * dans l'ordre, l'astérisque prenant n'importe quelle suite.
     */
    private static boolean jokerColleAuChemin(String regle, String chemin) {
        String[] segments = regle.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (String segment : segments) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(segment));
        }
        return Pattern.matches(regex.toString(), chemin);
    }
}
