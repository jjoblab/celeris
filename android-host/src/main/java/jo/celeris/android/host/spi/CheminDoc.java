package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un chemin de document dans le système de fichiers virtuel (E-01) :
 * racine « / », segments purs, écriture normalisée. La traversée est
 * refusée à la construction — « .. » ne sort jamais de la racine, les
 * chemins relatifs ne se forgent pas : toute opération sur le système
 * de fichiers reçoit un chemin déjà validé.
 *
 * <p>Valeur par excellence : deux chemins égaux par l'écriture sont le
 * même chemin, quelle que soit la construction qui les a produits.</p>
 */
public final class CheminDoc {

    private static final CheminDoc RACINE = new CheminDoc(List.of());

    private final List<String> segments;

    private CheminDoc(List<String> segments) {
        this.segments = segments;
    }

    /**
     * La racine du système de fichiers virtuel.
     *
     * @return le chemin racine, « / »
     */
    public static CheminDoc racine() {
        return RACINE;
    }

    /**
     * Lit l'écriture donnée : elle doit commencer par « / », les
     * barres redondantes et les segments « . » disparaissent, « .. » se
     * résout dans les bornes et sortant il est refusé.
     *
     * @param ecriture l'écriture du chemin, « /a/b/c »
     * @return le chemin normalisé
     * @throws IllegalArgumentException si l'écriture est vide, ne
     *                                  commence pas par « / » ou sort
     *                                  de la racine par « .. »
     */
    public static CheminDoc de(String ecriture) {
        Objects.requireNonNull(ecriture, "ecriture");
        if (ecriture.isBlank() || !ecriture.startsWith("/")) {
            throw new IllegalArgumentException(
                    "path must be absolute: " + ecriture);
        }
        return resoudreSegments(List.of(ecriture.split("/")));
    }

    /**
     * Résout le relatif donné depuis ce chemin : même discipline que
     * {@link #de(String)} — « .. » borné résolu, « .. » sortant refusé.
     *
     * @param relatif le relatif, « b/c » ou « b/../c »
     * @return le chemin résolu et normalisé
     * @throws IllegalArgumentException si le relatif sort des bornes
     */
    public CheminDoc resoudre(String relatif) {
        Objects.requireNonNull(relatif, "relatif");
        List<String> base = new ArrayList<>(segments);
        base.addAll(List.of(relatif.split("/")));
        return resoudreSegments(base);
    }

    private static CheminDoc resoudreSegments(List<String> bruts) {
        List<String> purs = new ArrayList<>();
        for (String brut : bruts) {
            if (brut.isEmpty() || ".".equals(brut)) {
                continue;
            }
            if ("..".equals(brut)) {
                if (purs.isEmpty()) {
                    throw new IllegalArgumentException(
                            "path escapes the root: " + String.join("/", bruts));
                }
                purs.remove(purs.size() - 1);
                continue;
            }
            purs.add(brut);
        }
        return purs.isEmpty() ? RACINE : new CheminDoc(List.copyOf(purs));
    }

    /**
     * Le parent : un cran vers le haut, la racine est son propre parent.
     *
     * @return le chemin parent
     */
    public CheminDoc parent() {
        if (segments.isEmpty()) {
            return RACINE;
        }
        return new CheminDoc(segments.subList(0, segments.size() - 1));
    }

    /**
     * Les segments purs du chemin, sans la racine.
     *
     * @return les segments, non modifiables
     */
    public List<String> segments() {
        return segments;
    }

    /**
     * Le dernier segment, vide à la racine.
     *
     * @return le nom du fichier ou dossier
     */
    public String nom() {
        return segments.isEmpty() ? "" : segments.get(segments.size() - 1);
    }

    /** Vrai si ce chemin est la racine. */
    public boolean estRacine() {
        return segments.isEmpty();
    }

    /**
     * L'écriture normalisée : « /a/b/c », « / » à la racine.
     *
     * @return l'écriture, toujours « / »-préfixée
     */
    public String ecriture() {
        if (segments.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", segments);
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof CheminDoc chemin && chemin.segments.equals(segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    @Override
    public String toString() {
        return ecriture();
    }
}
