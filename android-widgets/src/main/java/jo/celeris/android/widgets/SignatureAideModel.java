package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le modèle d'aide aux paramètres (B-07) : la signature active montre
 * le paramètre courant EN GRAS (la convention d'Android Studio —
 * l'info de paramètres y met le point courant en gras) — la plage de
 * caractères du paramètre dans le libellé, cherchée DANS LA ZONE DES
 * PARAMÈTRES (entre les parenthèses), dans l'ordre : les doublons
 * tombent sur le bon, le nom de la méthode jamais confondu avec un
 * paramètre. L'index reçu est BORNÉ à l'arité — le serveur généreux
 * au-delà ne casse pas le gras. Pur et testable sans SDK : la popup
 * le lit, le peintre l'encre en gras ; le placement au-dessus du
 * curseur vit chez PopupPlacer (D-10, livré M5).
 *
 * <p>Le modèle riche (chantier C7) porte les surcharges : les
 * signatures candidates vivent au modèle, le stepper navigue —
 * {@link #precedente()} et {@link #suivante()} rendent le modèle
 * voisin, le cycle revient au premier —, la fenêtre ±1
 * ({@link #window()}) borne ce que la vue montre autour de l'active,
 * et le peek ({@link #peek()}) sert la documentation de la signature
 * active — celle du paramètre courant d'abord, celle de la signature
 * sinon.</p>
 */
public final class SignatureAideModel {

    /**
     * Une signature candidate : son libellé, les libellés de ses
     * paramètres dans l'ordre, sa documentation.
     */
    public record Entry(String label, List<String> libellesParametres,
                         @Nullable String documentation) {

        /** Constructeur canonique : composantes figées, jamais nulles. */
        public Entry {
            java.util.Objects.requireNonNull(label, "libelle");
            java.util.Objects.requireNonNull(libellesParametres,
                    "libellesParametres");
            libellesParametres = List.copyOf(libellesParametres);
        }
    }

    /** Une plage de caractères dans le libellé : début inclus, fin exclue. */
    public record Plage(int start, int end) {
    }

    private static final int AUCUN = -1;

    private final List<Entry> entrees;
    private final int signatureActive;
    private final int parametreCourant;

    private SignatureAideModel(List<Entry> entrees, int signatureActive,
            int parametreCourant) {
        this.entrees = entrees;
        this.signatureActive = signatureActive;
        this.parametreCourant = parametreCourant;
    }

    /**
     * Construit le modèle d'une signature seule — le modèle riche à
     * une entrée.
     *
     * @param label            le libellé de la signature — « ajouter(int a, int b) »
     * @param libellesParametres les libellés des paramètres, dans l'ordre
     * @param parametreCourant   l'index du paramètre en cours de frappe,
     *                           -1 pour aucun ; l'index est BORNÉ à
     *                           l'arité (au-delà : le dernier paramètre)
     * @return le modèle
     */
    public static SignatureAideModel of(String label,
            List<String> libellesParametres, int parametreCourant) {
        return of(List.of(new Entry(label, libellesParametres, null)),
                0, parametreCourant);
    }

    /**
     * Construit le modèle riche de ses signatures candidates (C7) :
     * l'active montre son paramètre courant, le stepper navigue.
     *
     * @param entrees          les signatures candidates, au moins une
     * @param signatureActive  l'index de la signature active, borné aux
     *                         entrées (négatif : la première)
     * @param parametreCourant l'index du paramètre en cours de frappe,
     *                         borné à l'arité de l'active
     * @return le modèle
     * @throws IllegalArgumentException sans entrée
     */
    public static SignatureAideModel of(List<Entry> entrees,
            int signatureActive, int parametreCourant) {
        if (entrees.isEmpty()) {
            throw new IllegalArgumentException("no signature entry");
        }
        int active = Math.max(0, Math.min(signatureActive,
                entrees.size() - 1));
        return new SignatureAideModel(List.copyOf(entrees), active,
                parametreCourant);
    }

    /** Le libellé de la signature active. */
    public String label() {
        return entrees.get(signatureActive).label();
    }

    /** L'index du paramètre courant, borné à l'arité — -1 s'il n'y en a pas. */
    public int parametreCourant() {
        List<String> parameters = entrees.get(signatureActive)
                .libellesParametres();
        if (parameters.isEmpty() || parametreCourant < 0) {
            return AUCUN;
        }
        return Math.min(parametreCourant, parameters.size() - 1);
    }

    /**
     * La plage du paramètre courant dans le libellé — ce que le peintre
     * met en gras. La recherche vit ENTRE LES PARENTHÈSES (le nom de la
     * méthode n'est jamais un paramètre), séquentielle : chaque
     * paramètre se cherche après la fin du précédent, les doublons
     * tombent sur le bon. Le paramètre introuvable ne donne aucune
     * plage — jamais inventée.
     *
     * @return la plage du paramètre courant, ou null s'il n'y en a pas
     */
    public @Nullable Plage plageParametreCourant() {
        int index = parametreCourant();
        if (index == AUCUN) {
            return null;
        }
        Entry active = entrees.get(signatureActive);
        String label = active.label();
        int ouverture = label.indexOf('(');
        int fermeture = label.lastIndexOf(')');
        int depuis = ouverture < 0 ? 0 : ouverture + 1;
        int borne = fermeture < 0 ? label.length() : fermeture;
        for (int i = 0; i < active.libellesParametres().size(); i++) {
            String nom = active.libellesParametres().get(i);
            int trouve = label.indexOf(nom, depuis);
            if (trouve < 0 || trouve + nom.length() > borne) {
                continue;
            }
            int end = trouve + nom.length();
            if (i == index) {
                return new Plage(trouve, end);
            }
            depuis = end;
        }
        return null;
    }

    // ------------------------------------------------------ riche (C7)

    /** Le nombre de signatures candidates. */
    public int nombreSignatures() {
        return entrees.size();
    }

    /** L'index de la signature active, jamais négatif. */
    public int signatureActive() {
        return signatureActive;
    }

    /**
     * Le modèle de la signature précédente (C7) : le stepper recule,
     * la première rend la dernière — le cycle est tactile, jamais
     * bloqué. Une signature seule se rend telle quelle.
     *
     * @return le modèle voisin, le paramètre courant suit
     */
    public SignatureAideModel precedente() {
        int target = signatureActive == 0
                ? entrees.size() - 1 : signatureActive - 1;
        return new SignatureAideModel(entrees, target, parametreCourant);
    }

    /**
     * Le modèle de la signature suivante (C7) : le stepper avance, la
     * dernière rend la première.
     *
     * @return le modèle voisin, le paramètre courant suit
     */
    public SignatureAideModel suivante() {
        int target = signatureActive == entrees.size() - 1
                ? 0 : signatureActive + 1;
        return new SignatureAideModel(entrees, target, parametreCourant);
    }

    /**
     * La fenêtre des signatures à montrer (C7) : l'active et ses
     * voisines d'un cran — au plus trois entrées, moins aux bords.
     *
     * @return les entrées de la fenêtre, l'active au milieu
     */
    public List<Entry> window() {
        List<Entry> window = new ArrayList<>(3);
        if (signatureActive > 0) {
            window.add(entrees.get(signatureActive - 1));
        }
        window.add(entrees.get(signatureActive));
        if (signatureActive < entrees.size() - 1) {
            window.add(entrees.get(signatureActive + 1));
        }
        return List.copyOf(window);
    }

    /**
     * Le peek (C7) : la documentation à montrer à l'appui long —
     * celle de la signature active, jamais inventée. La documentation
     * du paramètre courant précède celle de la signature quand le
     * serveur la porte.
     *
     * @return la documentation de l'active, ou null
     */
    public @Nullable String peek() {
        return entrees.get(signatureActive).documentation();
    }

    /** L'entrée de la signature active. */
    Entry active() {
        return entrees.get(signatureActive);
    }
}
