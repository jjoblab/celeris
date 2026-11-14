package jo.celeris.core.intelligence;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * La suppression d'inspection (catégorie B de l'inventaire — « Suppress
 * Inspection », le geste dormait, le moteur naît) : l'avertissement
 * porté par une règle se masque EN LA LIGNE — le commentaire
 * « noinspection » se pose en fin de ligne, le lint l'honore à sa
 * relance (la sortie ne rapporte plus la règle masquée, les autres
 * demeurent). Une transaction annulable d'un geste, la voie unique
 * (ADR-003) — jamais le document muté avant l'application.
 */
public final class SuppressionInspection {

    /** La marque du masquage — la convention du terrain, lue par le
     *  faux build de lint à sa relance. */
    public static final String MARQUE = "// noinspection ";

    private SuppressionInspection() {
    }

    /**
     * Le plan de masquage de la règle en la ligne donnée : le
     * commentaire « noinspection » se pose en fin de ligne — après le
     * texte utile, la ligne ne se décale pas (l'insertion vit en
     * ligne, les lignes suivantes ne bougent pas).
     *
     * @param doc       le document, jamais muté
     * @param ligne     la ligne porteuse, à partir de un
     * @param regle     l'identifiant de règle à masquer, jamais vide
     * @return le plan du geste, null si la ligne porte déjà la marque
     */
    public static @Nullable EditPlan masquer(Document doc, int ligne, String regle) {
        if (ligne < 1 || ligne > doc.lineCount()) {
            return null;
        }
        String texte = doc.lineText(ligne - 1);
        if (texte.contains(MARQUE + regle)) {
            return null;
        }
        int position = doc.lineStart(ligne - 1) + texte.length();
        String marque = " " + MARQUE + regle;
        return new EditPlan(
                Transaction.of(new TextEdit(position, "", marque)),
                MultiSelection.caretsAt(position + marque.length()));
    }
}
