package jo.celeris.android.view.internal;


import java.util.IdentityHashMap;
import java.util.Map;

import jo.celeris.core.syntax.MoteurAnalyse;
import jo.celeris.core.text.TextSession;

/**
 * Le parc des moteurs d'analyse par session (le retour terrain du
 * 2026-11-19 : « quand j'appuie sur l'un des onglets la coloration
 * se refait encore et encore ») : la tokenisation appartient AU
 * DOCUMENT — le moteur de la session quittee se PARQUE avec sa
 * publication, une session revenue le retrouve et ne re-tokenise
 * RIEN. Le modèle du dépôt de départ : un fichier ouvert garde sa
 * coloration à travers les changements d'onglet.
 *
 * <p>Le moteur parqué reste VIVANT : il publie les éditions de son
 * document (le fil d'analyse suit la session, pas l'écran). Le parc
 * garde un moteur par document, vidé par {@link #toutFermer()} à la
 * fermeture de la vue — jamais par le nombre de changements
 * d'onglet.</p>
 */
public final class ParcMoteurs {

    private final Map<TextSession, MoteurAnalyse> parques =
            new IdentityHashMap<>();

    /**
     * Parque le moteur de la session quittee — le moteur courant
     * devient celui de la session revenue, ou null si le document
     * n'en avait jamais eu.
     *
     * @param quittee   la session qui quitte l'écran (son moteur se
     *                  parque sous elle)
     * @param courant   le moteur de la session quittee, null si aucun
     * @param revenue   la session qui prend l'écran
     * @param premiere  la première ligne visible (la fenêtre du
     *                  moteur qui revient)
     * @param derniere  la dernière ligne visible
     * @return le moteur parqué de la session revenue, null si aucun
     */
    public MoteurAnalyse basculer(@org.jspecify.annotations.Nullable TextSession quittee,
            @org.jspecify.annotations.Nullable MoteurAnalyse courant,
            TextSession revenue, int premiere, int derniere) {
        if (courant != null && quittee != null) {
            parques.put(quittee, courant);
        }
        MoteurAnalyse revenu = parques.remove(revenue);
        if (revenu != null) {
            // Le moteur parqué revient : sa fenêtre suit l'écran —
            // la publication survit au détachement, la vue n'attend
            // AUCUNE re-tokenisation pour recolorer.
            revenu.fenetre(premiere, derniere);
        }
        return revenu;
    }

    /** Ferme TOUT les moteurs parqués — la vue qui meurt ne laisse
     *  aucun fil d'analyse vivant derrière elle (C2). */
    public void toutFermer() {
        for (MoteurAnalyse moteur : parques.values()) {
            moteur.close();
        }
        parques.clear();
    }
}
