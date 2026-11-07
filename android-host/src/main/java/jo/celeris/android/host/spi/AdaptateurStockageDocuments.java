package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * L'adaptateur de production du système de fichiers virtuel (E-01) :
 * les {@link CheminDoc} validés par construction deviennent des
 * identifiants de documents du framework d'accès au stockage — la
 * résolution descend l'arbre segment par segment, du dossier racine
 * accordé jusqu'au fichier. L'écriture crée les parents implicites au
 * passage (« mkdir -p »), l'ordre de listage est l'ordre
 * lexicographique des noms — le même contrat que l'implémentation de
 * référence {@link MemoireSystemeFichiers}.
 *
 * <p>L'adaptateur est mince par construction : il ne connaît ni
 * {@code ContentResolver} ni {@code DocumentsContract} — tout accès
 * au stockage passe par la porte {@link ArbreDocuments}. Un dossier
 * sur le chemin d'un fichier répond « absent » à l'ouverture comme à
 * l'écriture. Deux entrées homonymes dans un même dossier sont une
 * décision du fournisseur : la première répond, l'ordre du
 * fournisseur fait foi.</p>
 */
public final class AdaptateurStockageDocuments implements SystemeFichiersVirtuel {

    private final ArbreDocuments arbre;
    private final String idRacine;

    /**
     * Construit l'adaptateur sur l'arbre donné.
     *
     * @param arbre    la porte vers l'arbre de documents du stockage
     * @param idRacine l'identifiant du dossier racine accordé à
     *                 l'application — le « / » du système virtuel
     */
    public AdaptateurStockageDocuments(ArbreDocuments arbre, String idRacine) {
        this.arbre = Objects.requireNonNull(arbre, "arbre");
        this.idRacine = Objects.requireNonNull(idRacine, "idRacine");
    }

    @Override
    public FichierVirtuel ouvrir(CheminDoc chemin) {
        EntreeDocument entree = resoudre(chemin);
        if (entree == null || entree.dossier()) {
            throw new FichierAbsentException(chemin);
        }
        return new FichierVirtuel(chemin, arbre.lire(entree.idDocument()));
    }

    @Override
    public void ecrire(CheminDoc chemin, String contenu) {
        EntreeDocument existant = resoudre(chemin);
        if (existant != null) {
            if (existant.dossier()) {
                throw new FichierAbsentException(chemin);
            }
            arbre.remplacer(existant.idDocument(), contenu);
            return;
        }
        String idParent = idRacine;
        List<String> segments = chemin.segments();
        for (int i = 0; i < segments.size() - 1; i++) {
            String segment = segments.get(i);
            EntreeDocument enfant = enfantNomme(idParent, segment);
            idParent = enfant != null
                    ? enfant.idDocument()
                    : arbre.creer(idParent, segment, true);
        }
        String nom = chemin.nom();
        EntreeDocument fichier = enfantNomme(idParent, nom);
        String idFichier = fichier != null
                ? fichier.idDocument()
                : arbre.creer(idParent, nom, false);
        arbre.remplacer(idFichier, contenu);
    }

    @Override
    public List<CheminDoc> lister(CheminDoc dossier) {
        EntreeDocument entree = resoudre(dossier);
        if (entree == null || !entree.dossier()) {
            return List.of();
        }
        List<CheminDoc> fichiers = new ArrayList<>();
        for (EntreeDocument enfant : arbre.enfants(entree.idDocument())) {
            if (!enfant.dossier()) {
                fichiers.add(dossier.resoudre(enfant.nom()));
            }
        }
        fichiers.sort(Comparator.comparing(CheminDoc::ecriture));
        return fichiers;
    }

    @Override
    public boolean estFichier(CheminDoc chemin) {
        EntreeDocument entree = resoudre(chemin);
        return entree != null && !entree.dossier();
    }

    private EntreeDocument resoudre(CheminDoc chemin) {
        String idCourant = idRacine;
        EntreeDocument trouve = null;
        for (String segment : chemin.segments()) {
            trouve = enfantNomme(idCourant, segment);
            if (trouve == null) {
                return null;
            }
            idCourant = trouve.idDocument();
        }
        return trouve;
    }

    private EntreeDocument enfantNomme(String idParent, String nom) {
        for (EntreeDocument enfant : arbre.enfants(idParent)) {
            if (enfant.nom().equals(nom)) {
                return enfant;
            }
        }
        return null;
    }
}
