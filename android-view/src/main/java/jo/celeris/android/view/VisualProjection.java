package jo.celeris.android.view;

/**
 * Projection lignes de document ↔ rangées visuelles, à travers le modèle
 * de plis (PIEGE-04).
 *
 * <p>Les indices de première/dernière rangée visible sont des RANGÉES,
 * pas des lignes du document : toute fenêtre, toute conversion, passe
 * par cette projection — jamais par l'identité naïve « rangée = ligne »,
 * fausse dès qu'un pli cache des lignes.</p>
 *
 * <p>La projection lit le modèle de plis en direct : plier ou déplier
 * prend effet à la conversion suivante, sans reconstruction. Les
 * conversions n'allouent rien (PIEGE-15) : elles vivent dans la boucle
 * de dessin.</p>
 */
public final class VisualProjection {

    private final FoldModel plis;
    private int lineCount;

    /**
     * Construit la projection d'un document à plis donnés.
     *
     * @param lineCount le nombre de lignes du document
     * @param folds     le modèle de plis, lu en direct
     */
    public VisualProjection(int lineCount, FoldModel folds) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("negative line count: " + lineCount);
        }
        this.lineCount = lineCount;
        this.plis = folds;
    }

    /** Le nombre de rangées visibles : lignes moins lignes cachées. */
    public int rowCount() {
        int cachees = 0;
        for (FoldModel.Pli pli : plis.plis()) {
            cachees += lignesCachees(pli);
        }
        return lineCount - cachees;
    }

    /**
     * La rangée visuelle qui porte la ligne donnée — une ligne cachée se
     * projette sur la rangée de son en-tête.
     *
     * @param line le numéro de ligne, borné au document
     * @return le numéro de rangée visuelle
     */
    public int rowOfLine(int line) {
        int cible = borneLigne(line);
        for (FoldModel.Pli pli : plis.plis()) {
            if (cible > pli.debut() && cible <= pli.fin()) {
                cible = pli.debut();
                break;
            }
        }
        return cible - cacheesAvant(cible);
    }

    /**
     * La première ligne du document portée par la rangée donnée.
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return la première ligne affichée par la rangée
     */
    public int firstLineOfRow(int row) {
        int borne = borneRangee(row);
        int cachees = 0;
        for (FoldModel.Pli pli : plis.plis()) {
            if (pli.debut() >= lineCount) {
                break;
            }
            int rangeeEnTete = pli.debut() - cachees;
            if (borne < rangeeEnTete) {
                return borne + cachees;
            }
            if (borne == rangeeEnTete) {
                return pli.debut();
            }
            cachees += lignesCachees(pli);
        }
        return borne + cachees;
    }

    /**
     * La dernière ligne du document couverte par la rangée donnée — y
     * compris les lignes cachées sous un pli.
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return la dernière ligne couverte par la rangée
     */
    public int lastLineOfRow(int row) {
        int borne = borneRangee(row);
        int cachees = 0;
        for (FoldModel.Pli pli : plis.plis()) {
            if (pli.debut() >= lineCount) {
                break;
            }
            int rangeeEnTete = pli.debut() - cachees;
            if (borne < rangeeEnTete) {
                return borne + cachees;
            }
            if (borne == rangeeEnTete) {
                return Math.min(pli.fin(), lineCount - 1);
            }
            cachees += lignesCachees(pli);
        }
        return borne + cachees;
    }

    private int lignesCachees(FoldModel.Pli pli) {
        int finBornee = Math.min(pli.fin(), lineCount - 1);
        return Math.max(0, finBornee - pli.debut());
    }

    private int cacheesAvant(int line) {
        int total = 0;
        for (FoldModel.Pli pli : plis.plis()) {
            int finBornee = Math.min(pli.fin(), lineCount - 1);
            if (line > finBornee) {
                total += finBornee - pli.debut();
            } else if (line > pli.debut()) {
                total += line - pli.debut() - 1;
            }
        }
        return total;
    }

    private int borneLigne(int line) {
        return Math.max(0, Math.min(line, lineCount - 1));
    }

    private int borneRangee(int row) {
        return Math.max(0, Math.min(row, rowCount() - 1));
    }
}
