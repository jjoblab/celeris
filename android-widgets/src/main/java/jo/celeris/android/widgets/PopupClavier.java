package jo.celeris.android.widgets;

/**
 * Le contrat clavier d'une carte de popup (le chantier d'unification,
 * phase 5 de la mission) : les cartes INTERACTIVES — la liste de
 * complétion, le menu d'intentions — y reçoivent la navigation que
 * l'éditeur leur DÉLÈGUE. PIEGE-27 demeure : la carte ne prend jamais le
 * focus ; l'éditeur garde le clavier et lui rend les verdicts des
 * touches — flèches, validation (Enter, Tab), la carte décline le reste
 * (faux rend la touche à la vie normale de l'éditeur).
 */
public interface PopupClavier {

    /**
     * La flèche déléguée déplace la surbrillance de la carte.
     *
     * @param pas le nombre de rangées du déplacement — négatif vers le
     *            haut, positif vers le bas
     * @return vrai quand la carte a bougé sa surbrillance (elle consomme
     *         la touche)
     */
    boolean surFleche(int pas);

    /**
     * La validation déléguée choisit la rangée courante — Enter comme
     * Tab (la complétion ouverte précède le fantôme IA sur la touche).
     *
     * @return vrai quand la carte a validé sa rangée (elle consomme la
     *         touche)
     */
    boolean surValidation();
}
