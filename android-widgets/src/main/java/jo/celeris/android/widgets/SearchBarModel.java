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
import jo.celeris.core.text.TextSession;

/**
 * La barre de recherche (M5, A-20 facette interface) : l'état de la
 * barre au-dessus du moteur du cœur — la requête et ses options, la
 * liste des occurrences recalculée à chaque changement, l'occurrence
 * courante et sa navigation circulaire. Le remplacement appartient à la
 * session (replaceAll par la voie de transaction, livré M2).
 *
 * <p>C10 (l'analyse croisée) : la barre VIT avec le document. Posée
 * sur une {@link TextSession}, elle réécoute les éditions — les
 * occurrences suivent la frappe au lieu de périmer à la première
 * modification ; l'occurrence courante se remplace par la voie de
 * transaction de la session (annulable, PIEGE-07) ; et les signaux
 * du moteur ({@code truncated} au plafond de collecte,
 * {@code tooCostly} au budget regex) remontent à l'hôte au lieu de
 * se perdre dans le calcul. Posée sur un {@link Document} nu (la
 * signature historique), la barre fonctionne comme avant : le
 * recalcul part aux changements de requête et d'options, mais rien
 * ne suit les mutations directes du document — un document nu ne
 * parle pas.</p>
 */
public final class SearchBarModel {

    private final LongSupplier horloge;

    /** La session écoutée, null en mode document nu. */
    private @Nullable TextSession session;

    private Document document;

    private String requete = "";

    private FindOptions options = new FindOptions();

    private List<Match> occurrences = List.of();

    private int index = -1;

    private boolean tronque;

    private boolean tropCouteux;

    /** L'écouteur des éditions de la session (C10) — détaché au
     *  changement de document et à la fermeture. */
    private final EditListenerBridge ecouteur = new EditListenerBridge();

    /** Pont d'écoute : la classe nommée se retire elle-même (la
     *  session ne retient que l'écouteur, jamais la barre entière
     *  par une lambda captive). */
    private final class EditListenerBridge
            implements jo.celeris.core.text.EditListener {
        @Override
        public void onTextEdit(int start, int end, String removed, String inserted) {
            recalculer();
        }
    }

    /**
     * Une barre sur le document donné, à l'horloge injectée (ADR-007).
     * Le document nu ne notifie pas ses mutations : préférer la pose
     * sur session pour le recalcul vivant.
     *
     * @param document le document parcouru
     * @param horloge  l'horloge des budgets regex
     */
    public SearchBarModel(Document document, LongSupplier horloge) {
        this.document = Objects.requireNonNull(document, "document");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.session = null;
    }

    /**
     * Une barre vivante sur la session (C10) : les éditions y
     * rafraîchissent les occurrences, le remplacement courant passe
     * par la voie de transaction (annulable).
     *
     * @param session la session parcourue et écoutée
     * @param horloge l'horloge des budgets regex
     */
    public SearchBarModel(TextSession session, LongSupplier horloge) {
        this.session = Objects.requireNonNull(session, "session");
        this.document = session.document();
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        session.addEditListener(ecouteur);
    }

    /**
     * La barre se ferme : l'écouteur se détache de la session — la
     * barre morte ne retient plus le document.
     */
    public void close() {
        if (session != null) {
            session.removeEditListener(ecouteur);
            session = null;
        }
    }

    /**
     * Le document parcouru change — les occurrences se recalculent.
     * En mode session, l'ancienne session est détachée d'abord.
     *
     * @param document le nouveau document
     */
    public void setDocument(Document document) {
        Objects.requireNonNull(document, "document");
        if (session != null) {
            session.removeEditListener(ecouteur);
            session = null;
        }
        this.document = document;
        recalculer();
    }

    /**
     * La session parcourue change (C10) : l'ancienne se détache, la
     * neuve s'écoute — le changement d'onglet repart d'occurrences
     * fraîches.
     *
     * @param session la nouvelle session parcourue et écoutée
     */
    public void setSession(TextSession session) {
        Objects.requireNonNull(session, "session");
        if (this.session != null) {
            this.session.removeEditListener(ecouteur);
        }
        this.session = session;
        this.document = session.document();
        session.addEditListener(ecouteur);
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
     * La dernière passe s'est-elle arrêtée au plafond de collecte
     * ({@link FindReplace#MAX_MATCHES}) — C10 : le signal remonte à
     * l'hôte au lieu de se perdre dans le calcul.
     *
     * @return vrai si la collecte était tronquée
     */
    public boolean isTruncated() {
        return tronque;
    }

    /**
     * La dernière passe a-t-elle dépassé le budget regex
     * ({@link FindReplace#REGEX_BUDGET_MS}) — C10 : le signal
     * remonte à l'hôte.
     *
     * @return vrai si la passe a avorté sur le budget temps
     */
    public boolean isTooCostly() {
        return tropCouteux;
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

    /**
     * Remplace l'occurrence courante (C10) : la passe traverse la
     * voie de transaction de la session (annulable, PIEGE-07), puis
     * l'occurrence suivante devient courante — la navigation repart
     * de là où le remplacement s'est arrêté. En mode document nu,
     * la main revient : sans session, pas de voie de transaction.
     *
     * @param remplacement le texte qui remplace l'occurrence courante
     * @return vrai si un remplacement a eu lieu
     */
    public boolean replaceCurrent(String remplacement) {
        Objects.requireNonNull(remplacement, "remplacement");
        if (session == null || index < 0 || index >= occurrences.size()) {
            return false;
        }
        Match courante = occurrences.get(index);
        boolean passe = session.replaceRange(courante.start(), courante.end(),
                remplacement);
        if (passe) {
            // le recalcul (parti de l'écouteur) a déjà rafraîchi les
            // occurrences ; la courante devient celle qui commence à
            // la fin du remplacement — le prochain next() avance.
            setCurrentNear(courante.start() + remplacement.length());
        }
        return passe;
    }

    private void recalculer() {
        document = session != null ? session.document() : document;
        FindReplace.SearchResult passe = FindReplace.of(horloge)
                .find(document.text(), requete, options);
        occurrences = passe.matches();
        tronque = passe.truncated();
        tropCouteux = passe.tooCostly();
        if (occurrences.isEmpty()) {
            index = -1;
        } else {
            index = Math.min(Math.max(index, 0), occurrences.size() - 1);
        }
    }
}
