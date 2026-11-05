package jo.celeris.android.widgets;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.FindOptions;
import jo.celeris.core.text.FindReplace;
import jo.celeris.core.text.Match;
import jo.celeris.core.text.SelRange;

/**
 * La barre de recherche (M5, A-20 facette interface) : l'état de la
 * barre au-dessus du moteur du cœur — la requête et ses options, la
 * liste des occurrences recalculée à chaque changement, l'occurrence
 * courante et sa navigation circulaire. Le remplacement appartient à la
 * session (replaceAll par la voie de transaction, livré M2).
 */
public final class SearchBarModel {

    private final LongSupplier horloge;

    private Document document;

    private String requete = "";

    private FindOptions options = new FindOptions();

    private List<Match> occurrences = List.of();

    private int index = -1;

    /**
     * Une barre sur le document donné, à l'horloge injectée (ADR-007).
     *
     * @param document le document parcouru
     * @param horloge  l'horloge des budgets regex
     */
    public SearchBarModel(Document document, LongSupplier horloge) {
        this.document = Objects.requireNonNull(document, "document");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /**
     * Le document parcouru change — les occurrences se recalculent.
     *
     * @param document le nouveau document
     */
    public void setDocument(Document document) {
        this.document = Objects.requireNonNull(document, "document");
        recalculer();
    }

    /**
     * La requête courante.
     *
     * @return la requête, vide à l'ouverture
     */
    public String query() {
        return requete;
    }

    /**
     * Change la requête : les occurrences se recalculent, la première
     * devient courante.
     *
     * @param requete la nouvelle requête
     */
    public void setQuery(String requete) {
        this.requete = Objects.requireNonNull(requete, "requete");
        recalculer();
    }

    /**
     * Bascule la sensibilité à la casse.
     *
     * @param caseSensitive vrai pour distinguer la casse
     */
    public void setCaseSensitive(boolean caseSensitive) {
        options = options.withCaseSensitive(caseSensitive);
        recalculer();
    }

    /**
     * Bascule le mot entier.
     *
     * @param wholeWord vrai pour les mots entiers
     */
    public void setWholeWord(boolean wholeWord) {
        options = options.withWholeWord(wholeWord);
        recalculer();
    }

    /**
     * Bascule le mode regex.
     *
     * @param regex vrai pour les expressions régulières
     */
    public void setRegex(boolean regex) {
        options = options.withRegex(regex);
        recalculer();
    }

    /**
     * Les occurrences courantes.
     *
     * @return la liste, vide sans requête ou sans correspondance
     */
    public List<Match> matches() {
        return occurrences;
    }

    /**
     * L'indice de l'occurrence courante.
     *
     * @return l'indice, -1 sans occurrence
     */
    public int currentIndex() {
        return index;
    }

    /**
     * L'occurrence courante, en plage de sélection.
     *
     * @return la plage, ou null sans occurrence
     */
    public @Nullable SelRange current() {
        if (index < 0 || index >= occurrences.size()) {
            return null;
        }
        Match courante = occurrences.get(index);
        return new SelRange(courante.start(), courante.end());
    }

    /**
     * Passe à l'occurrence suivante, en boucle.
     *
     * @return vrai si une occurrence est courante
     */
    public boolean next() {
        if (occurrences.isEmpty()) {
            return false;
        }
        index = (index + 1) % occurrences.size();
        return true;
    }

    /**
     * Passe à l'occurrence précédente, en boucle.
     *
     * @return vrai si une occurrence est courante
     */
    public boolean previous() {
        if (occurrences.isEmpty()) {
            return false;
        }
        index = (index - 1 + occurrences.size()) % occurrences.size();
        return true;
    }

    /**
     * Choisit pour courante l'occurrence la plus proche d'une position —
     * l'ouverture depuis le curseur.
     *
     * @param offset la position de référence
     */
    public void setCurrentNear(int offset) {
        index = -1;
        for (int i = 0; i < occurrences.size(); i++) {
            if (occurrences.get(i).start() >= offset) {
                index = i;
                return;
            }
        }
        if (!occurrences.isEmpty()) {
            index = 0;
        }
    }

    private void recalculer() {
        occurrences = FindReplace.of(horloge)
                .find(document.text(), requete, options).matches();
        if (occurrences.isEmpty()) {
            index = -1;
        } else {
            index = Math.min(Math.max(index, 0), occurrences.size() - 1);
        }
    }
}
