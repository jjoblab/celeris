package jo.celeris.core.intelligence;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * L'import automatique (B-05) : le candidat d'import s'insère en tête
 * du bloc des imports — après la déclaration de paquet, avant le
 * premier import — par une ÉDITION DE POSITION UNIQUE. L'import déjà
 * présent ne se re-insère jamais : la présence se vérifie à la ligne
 * d'import, jamais dans une chaîne.
 */
public final class ImportAuto {

    private ImportAuto() {
    }

    /**
     * Le plan d'insertion d'un import.
     *
     * @param doc            le document porteur
     * @param importComplet  la ligne d'import complète, saut de ligne
     *                       compris (par exemple
     *                       {@code import java.util.List;\n})
     * @return le plan — une seule édition à la tête du bloc, la
     *         sélection au début de l'insert ; ou null quand l'import
     *         est déjà présent
     */
    public static @Nullable EditPlan importer(Document doc, String importComplet) {
        if (dejaPresent(doc, importComplet)) {
            return null;
        }
        int position = positionDeTete(doc);
        Transaction transaction = Transaction.of(new TextEdit(position, "", importComplet));
        return new EditPlan(transaction, MultiSelection.caretsAt(position));
    }

    /** Vrai quand la ligne d'import existe déjà, ligne entière — jamais dans une chaîne. */
    private static boolean dejaPresent(Document doc, String importComplet) {
        String ligne = importComplet.stripTrailing();
        for (int l = 0; l < doc.lineCount(); l++) {
            if (doc.lineText(l).strip().equals(ligne)) {
                return true;
            }
        }
        return false;
    }

    /**
     * La tête du bloc des imports : le début de la ligne du premier
     * import ; sans import, le début de la ligne qui suit la
     * déclaration de paquet ; sans paquet, la position zéro.
     */
    private static int positionDeTete(Document doc) {
        int lignePaquet = -1;
        for (int l = 0; l < doc.lineCount(); l++) {
            String ligne = doc.lineText(l).strip();
            if (ligne.startsWith("import ")) {
                return doc.lineStart(l);
            }
            if (lignePaquet < 0 && ligne.startsWith("package ")) {
                lignePaquet = l;
            }
        }
        if (lignePaquet < 0) {
            return 0;
        }
        // la ligne qui suit le paquet — le bloc des imports y naîtra
        return lignePaquet + 1 < doc.lineCount()
                ? doc.lineStart(lignePaquet + 1) : doc.length();
    }

}
