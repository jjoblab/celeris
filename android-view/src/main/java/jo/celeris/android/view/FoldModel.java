package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de plis : plages de lignes repliées sous une ligne d'en-tête,
 * triées par début, sans chevauchement croisé.
 *
 * <p>Le pli {@code (début, fin)} garde la ligne d'en-tête visible et
 * cache les lignes {@code début+1} à {@code fin}. Les plis
 * <b>imbriqués</b> vivent ensemble (le redressement du 2026-11-13 :
 * « si je plie le {} imbriqué, le {} parent ne veut pas plier ») : un
 * pli qui EN CONTIENT un autre — le bloc parent autour d'un bloc déjà
 * replié — s'y pose sans le détruire, le pli interne survit sous le
 * parent et reparaît replié au dépliage du parent (la sémantique du
 * dépôt de départ : les régions repliées s'unissent dans la
 * projection, chacune garde son état). Seul le chevauchement
 * <b>croisé</b> — ni contenant ni contenu — reste rejeté : deux plis
 * qui se traversent n'ont pas de sens de lignes.</p>
 *
 * <p>La projection lit les plis FONDUS : les plis imbriqués y
 * deviennent l'union de leurs plages (une ligne cachée par plusieurs
 * plis ne se compte qu'une fois). La fonte vit ici, mémoïsée par une
 * révision incrémentée à chaque mutation — jamais dans la boucle de
 * dessin (PIEGE-15) : plier ou déplier paie la fonte une fois, la
 * boucle relit le tableau.</p>
 */
public final class FoldModel {

    /** Un pli : la ligne d'en-tête et la dernière ligne cachée. */
    record Pli(int debut, int fin) {
    }

    private final List<Pli> plis = new ArrayList<>();
    /** La révision de l'état des plis — la fonte s'y invalide. */
    private int revision;

    /** La fonte mémoïsée : paires [début, fin] unies, triées. */
    private int[] fondus;
    private int fondusRevision = -1;

    /**
     * Replie les lignes après l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête, qui reste visible
     * @param endLine   la dernière ligne cachée
     * @throws IllegalArgumentException si la plage est vide ou inversée
     * @throws IllegalStateException    si elle CROISE un pli existant —
     *                                  le pli qui le contient ou que
     *                                  celui-ci contient s'y pose bien
     */
    public void fold(int startLine, int endLine) {
        if (endLine <= startLine) {
            throw new IllegalArgumentException(
                    "fold span must be increasing: " + startLine + ".." + endLine);
        }
        for (Pli pli : plis) {
            if (croise(startLine, endLine, pli)) {
                throw new IllegalStateException(
                        "fold " + startLine + ".." + endLine + " crosses " + pli);
            }
        }
        int rang = 0;
        while (rang < plis.size() && plis.get(rang).debut() < startLine) {
            rang++;
        }
        plis.add(rang, new Pli(startLine, endLine));
        revision++;
    }

    /**
     * Déplie le pli porté par l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête du pli à déplier
     * @throws IllegalStateException si aucun pli ne porte cet en-tête
     */
    public void unfold(int startLine) {
        for (int i = 0; i < plis.size(); i++) {
            if (plis.get(i).debut() == startLine) {
                plis.remove(i);
                revision++;
                return;
            }
        }
        throw new IllegalStateException("no fold at header line " + startLine);
    }

    /**
     * Vrai si la ligne d'en-tête donnée porte un pli.
     *
     * @param startLine la ligne d'en-tête à interroger
     * @return vrai si un pli y est replié
     */
    public boolean isFolded(int startLine) {
        for (Pli pli : plis) {
            if (pli.debut() == startLine) {
                return true;
            }
        }
        return false;
    }

    /**
     * La dernière ligne cachée par le pli de l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête à interroger
     * @return sa dernière ligne cachée, —1 si aucun pli ne la porte
     */
    public int finDe(int startLine) {
        for (Pli pli : plis) {
            if (pli.debut() == startLine) {
                return pli.fin();
            }
        }
        return -1;
    }

    /** Les plis triés par début, pour la projection visuelle. */
    List<Pli> plis() {
        return plis;
    }

    /**
     * La révision de l'état des plis — incrémentée à chaque repli et
     * dépliage. La projection s'y invalide, le cache de fonte s'y
     * mémoïse.
     *
     * @return le compteur de mutations des plis
     */
    public int revision() {
        return revision;
    }

    /**
     * Les plis FONDUS : paires {@code [début, fin]} unies par union,
     * triées par début — les plis imbriqués n'y paraissent qu'une fois,
     * aucune ligne cachée n'y se compte double. Mémoïsée par révision :
     * plier ou déplier paie la fonte, la boucle de dessin relit.
     *
     * @return les plages fondues, paires [début0, fin0, début1, fin1…]
     */
    int[] plisFondus() {
        if (fondusRevision != revision) {
            fondus = fondre();
            fondusRevision = revision;
        }
        return fondus;
    }

    /**
     * L'union des plis : un pli qui DÉBORDE la fin du fusible courant
     * l'étend (le pli imbriqué compte dans son parent), le pli qui
     * commence APRÈS la fermeture ouvre sa propre plage — l'en-tête
     * d'un pli qui touche la fermante du précédent reste VISIBLE.
     */
    private int[] fondre() {
        int[] brut = new int[plis.size() * 2];
        int nombre = 0;
        for (Pli pli : plis) {
            int debut = Math.max(0, pli.debut());
            int fin = pli.fin();
            if (nombre > 0 && debut < brut[2 * (nombre - 1) + 1]) {
                int finFusible = 2 * (nombre - 1) + 1;
                if (fin > brut[finFusible]) {
                    brut[finFusible] = fin;
                }
            } else {
                brut[2 * nombre] = debut;
                brut[2 * nombre + 1] = fin;
                nombre++;
            }
        }
        if (nombre == plis.size()) {
            return brut;
        }
        int[] exact = new int[nombre * 2];
        System.arraycopy(brut, 0, exact, 0, nombre * 2);
        return exact;
    }

    /**
     * Vrai quand la plage CROISE le pli donné : elle le chevauche sans
     * le contenir ni s'y contenir — deux plis qui se traversent.
     */
    private static boolean croise(int debut, int fin, Pli pli) {
        boolean chevauche = debut <= pli.fin() && pli.debut() <= fin;
        boolean contient = pli.debut() <= debut && fin <= pli.fin();
        boolean contenu = debut <= pli.debut() && pli.fin() <= fin;
        return chevauche && !contient && !contenu;
    }
}
