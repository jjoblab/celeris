package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;

/**
 * L'index de symboles hors ligne (ADR-005) : alimenté par le même arbre
 * que la coloration, remplacé fichier par fichier à chaque ré-analyse —
 * la navigation répond sans serveur (« aller à un symbole » B-13, usages
 * de base B-11), à budget mémoire tenu, persistant par forme sérialisée
 * compacte.
 *
 * <p>La règle des rôles (ADR-005) : l'index répond <b>vite et hors
 * ligne</b> — symboles du fichier et du projet, occurrences entières à
 * casse exacte ; le serveur LSP répond <b>juste</b> — le raffinement
 * sémantique lui appartient. Jamais de chaînage : la vue ne sait pas
 * qui a répondu.</p>
 */
public final class IndexSymboles {

    /** Une occurrence d'usage : l'uri et l'offset où le nom vit. */
    public record Usage(String uri, int offset) {
    }

    /** Le budget d'occurrences par défaut — deux cent mille entrées. */
    public static final int BUDGET_DEFAUT = 200_000;

    private static final String EN_TETE = "# celeris index symboles 1";

    /** L'état indexé d'un fichier : ses symboles, ses occurrences, sa saturation. */
    private static final class Fichier {
        private final List<Symbole> symboles = new ArrayList<>();
        private final Map<String, List<Integer>> occurrences = new LinkedHashMap<>();
        private boolean sature;
    }

    private final int budgetOccurrences;
    private final Map<String, Fichier> fichiers = new LinkedHashMap<>();

    private IndexSymboles(int budgetOccurrences) {
        this.budgetOccurrences = budgetOccurrences;
    }

    /** Un index vide au budget par défaut. */
    public static IndexSymboles vide() {
        return vide(BUDGET_DEFAUT);
    }

    /**
     * Un index vide au budget donné — les occurrences de tous les
     * fichiers y sont bornées, la saturation est signée par fichier.
     *
     * @param budgetOccurrences le nombre total d'occurrences admises
     * @return l'index vide
     */
    public static IndexSymboles vide(int budgetOccurrences) {
        return new IndexSymboles(budgetOccurrences);
    }

    /**
     * Indexe (ou ré-indexe) un fichier : le plan du même arbre fournit
     * les déclarations, le lexique fournit les occurrences — le fichier
     * remplace son état, jamais il ne cumule.
     *
     * @param uri     l'uri du fichier
     * @param doc     le document porteur
     * @param analyse l'analyse courante du document
     */
    public void remplacer(String uri, Document doc, Analysis analyse) {
        // l'ancien état part d'abord : son budget est rendu avant le nouveau décompte
        fichiers.remove(uri);
        Fichier fichier = new Fichier();
        for (OutlineEntry entree : TreeOutline.entries(doc, analyse)) {
            fichier.symboles.add(new Symbole(entree.name(), entree.category(), uri,
                    entree.startOffset(), entree.endOffset(), entree.startLine()));
        }
        int restant = budgetOccurrences - nombreOccurrences();
        for (ScanIdentifiants.NomOffset releve : ScanIdentifiants.scanner(doc, analyse.lex())) {
            if (restant <= 0) {
                fichier.sature = true;
                break;
            }
            fichier.occurrences.computeIfAbsent(releve.nom(), nom -> new ArrayList<>())
                    .add(releve.offset());
            restant--;
        }
        fichiers.put(uri, fichier);
    }

    /** Retire un fichier de l'index — le fichier fermé quitte, les autres restent. */
    public void retirer(String uri) {
        fichiers.remove(uri);
    }

    /** Toutes les déclarations indexées, dans l'ordre (uri, offset). */
    public List<Symbole> symboles() {
        List<Symbole> toutes = new ArrayList<>();
        for (Map.Entry<String, Fichier> entree : fichiers.entrySet()) {
            toutes.addAll(entree.getValue().symboles);
        }
        return List.copyOf(toutes);
    }

    /**
     * La recherche floue (B-13) : les symboles qui répondent à la
     * requête, par score décroissant puis par nom — l'aller à symbole
     * hors ligne.
     *
     * @param requete les lettres tapées, vide montre tout
     * @return les symboles répondants, ordonnés
     */
    public List<Symbole> rechercher(String requete) {
        List<Symbole> repondants = new ArrayList<>();
        for (Symbole symbole : symboles()) {
            if (RechercheFloue.correspond(symbole.nom(), requete)) {
                repondants.add(symbole);
            }
        }
        repondants.sort(Comparator.comparingInt(
                (Symbole symbole) -> RechercheFloue.score(symbole.nom(), requete)).reversed()
                .thenComparing(Symbole::nom));
        return List.copyOf(repondants);
    }

    /**
     * Les usages d'un nom (B-11) : occurrences ENTIÈRES à casse exacte
     * à travers les fichiers indexés — déclarations comprises, jamais
     * les chaînes ni les commentaires (les jetons du lexique ont
     * décidé à l'indexation).
     *
     * @param nom le nom cherché, à la casse près
     * @return les occurrences dans l'ordre (uri, offset)
     */
    public List<Usage> usages(String nom) {
        List<Usage> usages = new ArrayList<>();
        for (Map.Entry<String, Fichier> entree : fichiers.entrySet()) {
            List<Integer> offsets = entree.getValue().occurrences.get(nom);
            if (offsets == null) {
                continue;
            }
            for (Integer offset : offsets) {
                usages.add(new Usage(entree.getKey(), offset));
            }
        }
        return List.copyOf(usages);
    }

    /** Le nombre total d'occurrences indexées — la mémoire occupée, pour le budget. */
    public int nombreOccurrences() {
        int total = 0;
        for (Fichier fichier : fichiers.values()) {
            for (List<Integer> offsets : fichier.occurrences.values()) {
                total += offsets.size();
            }
        }
        return total;
    }

    /** Vrai quand le fichier a saturé le budget : ses occurrences sont tronquées. */
    public boolean sature(String uri) {
        Fichier fichier = fichiers.get(uri);
        return fichier != null && fichier.sature;
    }

    /**
     * La forme sérialisée compacte de l'index — une entrée par ligne,
     * prête à dormir sur l'appareil.
     *
     * @return la forme sérialisée
     */
    public String serialiser() {
        StringBuilder forme = new StringBuilder(EN_TETE).append('\n');
        for (Map.Entry<String, Fichier> entree : fichiers.entrySet()) {
            forme.append("F\t").append(entree.getKey());
            if (entree.getValue().sature) {
                forme.append("\tsature");
            }
            forme.append('\n');
            for (Symbole symbole : entree.getValue().symboles) {
                forme.append("S\t").append(symbole.categorie()).append('\t')
                        .append(symbole.debut()).append('\t').append(symbole.fin())
                        .append('\t').append(symbole.ligne()).append('\t')
                        .append(symbole.nom()).append('\n');
            }
            for (Map.Entry<String, List<Integer>> occurrence
                    : entree.getValue().occurrences.entrySet()) {
                forme.append("O\t").append(occurrence.getKey());
                for (Integer offset : occurrence.getValue()) {
                    forme.append('\t').append(offset);
                }
                forme.append('\n');
            }
        }
        return forme.toString();
    }

    /**
     * Refait l'index de sa forme sérialisée.
     *
     * @param forme la forme produite par {@link #serialiser()}
     * @return l'index identique
     * @throws IllegalArgumentException si la forme n'est pas la nôtre
     */
    public static IndexSymboles deserialiser(String forme) {
        String[] lignes = forme.split("\n", -1);
        if (lignes.length == 0 || !EN_TETE.equals(lignes[0])) {
            throw new IllegalArgumentException("unknown index format");
        }
        IndexSymboles index = new IndexSymboles(BUDGET_DEFAUT);
        Fichier fichier = null;
        String uri = null;
        for (int i = 1; i < lignes.length; i++) {
            String ligne = lignes[i];
            if (ligne.isEmpty()) {
                continue;
            }
            String[] champs = ligne.split("\t", -1);
            if ("F".equals(champs[0])) {
                uri = champs[1];
                fichier = new Fichier();
                fichier.sature = champs.length > 2 && "sature".equals(champs[2]);
                index.fichiers.put(uri, fichier);
            } else if ("S".equals(champs[0]) && fichier != null && uri != null) {
                fichier.symboles.add(new Symbole(champs[5],
                        jo.celeris.core.syntax.NodeCategory.valueOf(champs[1]), uri,
                        Integer.parseInt(champs[2]), Integer.parseInt(champs[3]),
                        Integer.parseInt(champs[4])));
            } else if ("O".equals(champs[0]) && fichier != null) {
                List<Integer> offsets = new ArrayList<>();
                for (int c = 2; c < champs.length; c++) {
                    offsets.add(Integer.valueOf(Integer.parseInt(champs[c])));
                }
                fichier.occurrences.put(champs[1], offsets);
            } else {
                throw new IllegalArgumentException("unknown index line: " + champs[0]);
            }
        }
        return index;
    }
}
