package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditSpan;

/**
 * Les signets (M5, B-15) : des lignes marquées, du saut au suivant et
 * au précédent en boucle — la persistance par session passe par la
 * projection des offsets (ADR-003) : le modèle porte les débuts de
 * lignes marquées et s'abonne à la session
 * ({@code TextSession.addProjector}), chaque édition les décale, le
 * signet dont la ligne disparaît tombe au point de fusion puis se
 * dédoublonne.
 */
public final class BookmarkModel implements jo.celeris.core.text.PositionProjector {

    private final TreeSet<Integer> offsets = new TreeSet<>();

    /** Un modèle vide : les signets se posent ligne par ligne. */
    public BookmarkModel() {
    }

    /**
     * Bascule le signet d'une ligne.
     *
     * @param doc  le document courant de la session
     * @param line la ligne à marquer ou démarquer
     */
    public void toggle(Document doc, int line) {
        Integer debut = doc.lineStart(line);
        if (!offsets.remove(debut)) {
            offsets.add(debut);
        }
    }

    /**
     * Vrai si la ligne porte un signet.
     *
     * @param doc  le document courant de la session
     * @param line la ligne interrogée
     * @return son statut
     */
    public boolean isBookmarked(Document doc, int line) {
        return offsets.contains(doc.lineStart(line));
    }

    /**
     * Les lignes marquées, dans l'ordre du document.
     *
     * @param doc le document courant de la session
     * @return les lignes triées
     */
    public List<Integer> lines(Document doc) {
        List<Integer> lignes = new ArrayList<>(offsets.size());
        for (Integer offset : offsets) {
            lignes.add(doc.lineForOffset(Math.min(offset, Math.max(0, doc.length()))));
        }
        return lignes;
    }

    /**
     * La ligne marquée suivante, en boucle.
     *
     * @param doc  le document courant de la session
     * @param line la ligne de départ (exclue)
     * @return la ligne marquée suivante, -1 sans signet
     */
    public int nextAfter(Document doc, int line) {
        int apres = doc.lineStart(line + 1);
        Integer suivant = offsets.ceiling(apres);
        if (suivant == null) {
            suivant = offsets.isEmpty() ? null : offsets.first();
        }
        return suivant == null ? -1 : doc.lineForOffset(suivant);
    }

    /**
     * La ligne marquée précédente, en boucle.
     *
     * @param doc  le document courant de la session
     * @param line la ligne de départ (exclue)
     * @return la ligne marquée précédente, -1 sans signet
     */
    public int previousBefore(Document doc, int line) {
        int debutLigne = doc.lineStart(line);
        Integer precedent = offsets.lower(debutLigne);
        if (precedent == null) {
            precedent = offsets.isEmpty() ? null : offsets.last();
        }
        return precedent == null ? -1 : doc.lineForOffset(precedent);
    }

    /**
     * Projette les signets à travers une édition appliquée : avant,
     * inchangés ; dedans, ramenés au point de fusion ; après, décalés
     * du différentiel. La fermeture de session détache le projecteur —
     * les signets ne survivent pas au changement de fichier.
     *
     * @param span la plage de l'édition appliquée
     */
    @Override
    public void project(EditSpan span) {
        TreeSet<Integer> projetes = new TreeSet<>();
        for (Integer offset : offsets) {
            if (offset < span.start()) {
                projetes.add(offset);
            } else if (offset < span.end()) {
                projetes.add(span.start());
            } else {
                projetes.add(offset + span.delta());
            }
        }
        offsets.clear();
        offsets.addAll(projetes);
    }
}
