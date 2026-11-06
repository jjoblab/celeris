package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.core.text.TextEdit;

/**
 * Le renommage multi-fichiers (B-19) : le plan se construit sur l'index
 * hors ligne — une édition par occurrence, fichier par fichier — et
 * s'applique par la voie de transaction, annulable. Les conflits sont
 * SIGNALÉS, jamais tranchés : deux déclarations du même nom
 * (ambiguïté), un nouveau nom déjà pris (collision) — l'hôte lit, puis
 * décide.
 */
public final class Renommage {

    /** Pourquoi le renommage porte un conflit. */
    public enum RaisonConflit {

        /** Deux déclarations ou plus portent l'ancien nom. */
        AMBIGUITE,

        /** Le nouveau nom est déjà déclaré ailleurs dans le projet. */
        COLLISION
    }

    /** Un conflit signalé : le nom en cause et sa raison. */
    public record ConflitRenommage(String nom, RaisonConflit raison) {
    }

    /** Le plan de renommage : les éditions par uri, les conflits signalés. */
    public record PlanRenommage(Map<String, List<TextEdit>> editions,
                                List<ConflitRenommage> conflits) {

        /** Constructeur canonique : carte et liste figées. */
        public PlanRenommage {
            editions = Map.copyOf(editions);
            conflits = List.copyOf(conflits);
        }
    }

    private Renommage() {
    }

    /**
     * Le plan de renommage d'un nom sur tout le projet indexé.
     *
     * @param index     l'index des symboles du projet
     * @param ancienNom le nom renommé — à la casse près
     * @param nouveauNom le nom qui le remplace
     * @return le plan : une édition par occurrence, regroupée par uri ;
     *         les conflits signalés à côté
     */
    public static PlanRenommage plan(IndexSymboles index, String ancienNom,
                                     String nouveauNom) {
        List<ConflitRenommage> conflits = new ArrayList<>();
        int declarations = 0;
        for (Symbole symbole : index.symboles()) {
            if (symbole.nom().equals(ancienNom)) {
                declarations++;
            }
            if (!nouveauNom.equals(ancienNom) && symbole.nom().equals(nouveauNom)) {
                conflits.add(new ConflitRenommage(nouveauNom, RaisonConflit.COLLISION));
            }
        }
        if (declarations > 1) {
            conflits.add(new ConflitRenommage(ancienNom, RaisonConflit.AMBIGUITE));
        }
        Map<String, List<TextEdit>> editions = new LinkedHashMap<>();
        for (IndexSymboles.Usage usage : index.usages(ancienNom)) {
            editions.computeIfAbsent(usage.uri(), uri -> new ArrayList<>())
                    .add(new TextEdit(usage.offset(), ancienNom, nouveauNom));
        }
        // ordre descendant d'application : les offsets originaux restent
        // valides au fil de la transaction séquentielle
        for (List<TextEdit> liste : editions.values()) {
            liste.sort(java.util.Comparator.comparingInt(TextEdit::start).reversed());
        }
        return new PlanRenommage(editions, conflits);
    }
}
