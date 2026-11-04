package jo.celeris.core.syntax;

/**
 * Parseur d'un langage (ADR-004) : deux responsabilités seulement — dire
 * si un jeton démarre un chunk de haut niveau (à la profondeur des
 * chunks), et analyser les jetons d'un chunk en un sous-arbre tolérant.
 * Le découpage, la matérialisation, le réemploi et le décalage des ancres
 * vivent dans le moteur commun, pas ici.
 *
 * <p>Contrat de tolérance : {@link #parseChunk} ne lève JAMAIS sur un
 * source malformé — la malformation devient un nœud ERROR et le sous-arbre
 * couvre tout le chunk. Les bornes des nœuds sont relatives au début du
 * chunk (premier jeton).</p>
 */
public interface LanguageParser {

    /**
     * La profondeur à laquelle vivent les chunks de haut niveau : 0 pour
     * un langage à déclarations libres (scripts), 1 pour un langage dont
     * les membres vivent dans une enveloppe (fichier à racine unique).
     */
    int chunkLevel();

    /**
     * Décide si le jeton d'indice donné démarre un chunk. Appelée pour
     * les jetons à profondeur absolue inférieure ou égale au niveau des
     * chunks ; le prédicat peut relire le contexte précédent de la
     * tranche. Les chunks ne s'imbriquent jamais : un prédicat honnête ne
     * démarre un chunk qu'à la profondeur de découpe.
     *
     * @param slice la tranche en cours de parcours
     * @param index l'indice du jeton candidat
     * @param depth la profondeur de crochets absolue au jeton candidat
     * @return vrai si un chunk démarre à ce jeton
     */
    boolean startsChunk(TokenSlice slice, int index, int depth);

    /**
     * Analyse les jetons d'un chunk en un sous-arbre tolérant.
     *
     * @param slice la tranche du chunk (offsets relatifs au chunk)
     * @return la racine du sous-arbre, jamais null, jamais d'exception
     */
    SynNode parseChunk(TokenSlice slice);
}
