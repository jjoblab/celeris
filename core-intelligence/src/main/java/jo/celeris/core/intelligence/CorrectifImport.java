package jo.celeris.core.intelligence;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Le correctif d'import local (catégorie B de l'inventaire — « Import
 * Resolution », la surface dormait derrière l'action d'import) : la
 * résolution de l'identifiant au curseur par le classpath du terrain
 * (C8), la première candidate — l'ordre de préférence du terrain
 * décide, jamais l'aléa. Pur et testé sans appareil : l'atelier du
 * correctif y ajoute la feuille d'aperçu (B-18) — la transaction
 * reste annulable d'un geste (B-17).
 */
public final class CorrectifImport {

    /** Un correctif local : son nom qualifié et son édition — l'aperçu
     *  se prévoit sur une copie, l'acceptation appartient à la
     *  session. */
    public record Candidat(String fqn, Transaction edition) {
    }

    private CorrectifImport() {
    }

    /**
     * Le correctif d'import local du nom sous le curseur : la première
     * candidate du classpath, l'import déjà présent ne se re-propose
     * pas — null quand le classpath ne connaît pas le nom.
     *
     * @param document  le document porteur
     * @param caret     la position du curseur
     * @param classpath l'index de classpath du terrain
     * @return le candidat local, null sans résolution
     */
    public static @Nullable Candidat local(Document document, int caret,
            ClasspathIndex classpath) {
        java.util.List<String> candidats = ClasspathImports.candidats(
                document, caret, classpath);
        if (candidats.isEmpty()) {
            return null;
        }
        String fqn = candidats.get(0);
        EditPlan plan = ClasspathImports.importer(document, fqn);
        return plan == null ? null : new Candidat(fqn, plan.transaction());
    }
}
