package jo.celeris.core.ai;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.JetonAnnulation;
import jo.celeris.core.text.Document;

/**
 * L'état du texte fantôme côté éditeur (F-01, ADR-012) : la suggestion
 * visible, la requête en vol, et leur discipline.
 *
 * <p>Une proposition périmée ne s'affiche jamais : la livraison porte la
 * révision de son instantané, celle du document au départ du vol — les
 * deux doivent encore coïncider à l'arrivée, sinon la proposition se
 * jette, jamais elle ne se ré-ancre ni ne se fusionne à la hâte
 * (ADR-007). La frappe suivante refuse le fantôme et annule le vol
 * (F-09 : annulation immédiate, l'attente ne bloque jamais la frappe).
 * L'acceptation se prend par mot, par ligne ou en bloc : la session rend
 * le fragment à insérer à l'ancre, l'hôte l'applique par sa voie de
 * transaction ordinaire — le reste du fantôme survit à l'acceptation
 * partielle, c'est un geste de l'utilisateur sur une suggestion visible,
 * pas une livraison de modèle.</p>
 *
 * <p>Modèle d'exécution (ADR-007) : écrivain unique. Toutes les méthodes
 * se prennent sur le fil d'écriture de l'hôte ; le fil de publication
 * injecté ramène les livraisons du modèle à ce fil — l'état ne connaît
 * qu'un seul fil, le jeton fait le reste. Aucune méthode n'attend une
 * réponse : {@link #demander} rend immédiatement, la réponse arrive (ou
 * pas) par le fil de publication.</p>
 */
public final class SessionTexteFantome {

    private final ModeleTexteFantome modele;
    private final Executor publication;

    private @Nullable SuggestionFantome suggestion;
    private @Nullable JetonAnnulation enVol;

    /**
     * Une session de texte fantôme sur le modèle donné.
     *
     * @param modele       le modèle branché par l'hôte — local ou distant
     * @param publication  le fil de publication des livraisons : le fil
     *                     d'écriture de l'hôte (ADR-007)
     */
    public SessionTexteFantome(ModeleTexteFantome modele, Executor publication) {
        this.modele = modele;
        this.publication = publication;
    }

    /**
     * Demande une complétion : la précédente requête en vol est supplantée
     * — son jeton meurt —, une nouvelle part avec un jeton frais. La
     * suggestion visible, elle, reste telle quelle jusqu'à la réponse.
     *
     * @param document l'instantané immuable contre lequel complèter
     * @param offset   la position du curseur
     */
    public void demander(Document document, int offset) {
        annulerEnVol();
        JetonAnnulation jeton = new JetonAnnulation();
        enVol = jeton;
        int revisionAttendue = document.revision();
        modele.completer(new RequeteModele(document, offset, jeton),
                new ModeleTexteFantome.Recepteur() {
                    @Override
                    public void suggestion(SuggestionFantome arrivee) {
                        publication.execute(
                                () -> recevoir(jeton, revisionAttendue, document, arrivee));
                    }

                    @Override
                    public void aucune() {
                        publication.execute(() -> recevoirAucune(jeton));
                    }
                });
    }

    /**
     * Une livraison arrive : jeton mort, révision périmée ou ancre hors
     * document — la proposition se jette ; sinon elle devient le fantôme
     * visible.
     */
    private void recevoir(JetonAnnulation jeton, int revisionAttendue,
            Document document, SuggestionFantome arrivee) {
        if (jeton.estAnnule()) {
            return;
        }
        if (arrivee.revision() != revisionAttendue) {
            return;
        }
        if (arrivee.offset() > document.length()) {
            return;
        }
        suggestion = arrivee;
    }

    /** L'aveu du modèle ferme le fantôme visible — l'avis courant meurt. */
    private void recevoirAucune(JetonAnnulation jeton) {
        if (jeton.estAnnule()) {
            return;
        }
        suggestion = null;
    }

    /** Enterre la requête en vol, s'il y en a une. */
    private void annulerEnVol() {
        JetonAnnulation vol = enVol;
        if (vol != null) {
            vol.annuler();
            enVol = null;
        }
    }

    /**
     * La frappe suivante refuse le fantôme et annule le vol (F-01, F-09) :
     * le texte qui change rend la suggestion obsolète, la requête qui
     * vole ne sert plus à rien.
     */
    public void frappe() {
        annulerEnVol();
        suggestion = null;
    }

    /**
     * Le refus explicite — clavier ou geste (F-01) : même effet que la
     * frappe, l'intention diffère — l'utilisateur rejette, il n'écrit pas.
     */
    public void refuser() {
        annulerEnVol();
        suggestion = null;
    }

    /**
     * La suggestion visible — le reste, après acceptation partielle.
     *
     * @return le fantôme visible, ou {@code null} s'il n'y en a pas
     */
    public @Nullable SuggestionFantome suggestionActive() {
        return suggestion;
    }

    /**
     * Accepte le premier mot du fantôme : les espaces meneuses, le mot,
     * puis les espaces qui le suivent jusqu'au mot suivant. Le reste
     * survit, ancré pareil.
     *
     * @return le fragment à insérer à l'ancre, vide s'il n'y a pas de
     *         fantôme
     */
    public String accepterMot() {
        SuggestionFantome courante = suggestion;
        if (courante == null) {
            return "";
        }
        return accepterJusqua(finMot(courante.texte()));
    }

    /**
     * Accepte la première ligne du fantôme : jusqu'au premier saut de
     * ligne compris — le reste du fantôme, s'il existe, survit.
     *
     * @return le fragment à insérer à l'ancre, vide s'il n'y a pas de
     *         fantôme
     */
    public String accepterLigne() {
        SuggestionFantome courante = suggestion;
        if (courante == null) {
            return "";
        }
        int saut = courante.texte().indexOf('\n');
        return accepterJusqua(saut < 0 ? courante.texte().length() : saut + 1);
    }

    /**
     * Accepte tout le fantôme : il disparaît, ancre comprise.
     *
     * @return le fragment à insérer à l'ancre, vide s'il n'y a pas de
     *         fantôme
     */
    public String accepterTout() {
        SuggestionFantome courante = suggestion;
        if (courante == null) {
            return "";
        }
        return accepterJusqua(courante.texte().length());
    }

    /**
     * La fin du premier mot : espaces meneuses comprises, espaces de
     * traîne comprise — l'acceptation par mot pose le mot et l'espace
     * qui le sépare du suivant, jamais elle ne coupe deux mots.
     */
    private static int finMot(String texte) {
        int n = texte.length();
        int i = 0;
        while (i < n && Character.isWhitespace(texte.charAt(i))) {
            i++;
        }
        while (i < n && !Character.isWhitespace(texte.charAt(i))) {
            i++;
        }
        int fin = i;
        while (fin < n && Character.isWhitespace(texte.charAt(fin))) {
            fin++;
        }
        return fin;
    }

    /**
     * Consomme le fantôme jusqu'à la fin donnée : le fragment part à
     * l'hôte, le reste survit ancré pareil — le geste de l'utilisateur
     * clôt le vol.
     */
    private String accepterJusqua(int fin) {
        SuggestionFantome courante = suggestion;
        if (courante == null) {
            return "";
        }
        annulerEnVol();
        String fragment = courante.texte().substring(0, fin);
        String reste = courante.texte().substring(fin);
        suggestion = reste.isEmpty()
                ? null
                : new SuggestionFantome(reste, courante.offset(), courante.revision());
        return fragment;
    }
}
