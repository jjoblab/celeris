package jo.celeris.android.view;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Projection lignes de document ↔ rangées visuelles, à travers le modèle
 * de plis (PIEGE-04) et le retour à la ligne souple par offsets (A-18,
 * variante code).
 *
 * <p>Les indices de première/dernière rangée visible sont des RANGÉES,
 * pas des lignes du document : toute fenêtre, toute conversion, passe
 * par cette projection — jamais par l'identité naïve « rangée = ligne »,
 * fausse dès qu'un pli cache des lignes ou qu'une ligne coupée occupe
 * plusieurs rangées.</p>
 *
 * <p>La coupure par offsets se compte en colonnes de caractère dans la
 * ligne du document : une ligne de longueur {@code L} occupe
 * {@code max(1, ceil(L / colonnes))} rangées, chacune porte sa tranche
 * d'offsets. Le budget vaut au moins une colonne (PIEGE-08 : un mot plus
 * large que la fenêtre est coupé par caractère) ; un budget nul coupe le
 * retour à la ligne — chaque ligne garde une rangée entière.</p>
 *
 * <p>La projection lit le modèle de plis en direct : plier ou déplier
 * prend effet à la conversion suivante, sans reconstruction. Les plis
 * IMBRIQUÉS se lisent FONDUS (le modèle sert l'union de ses plages —
 * une ligne cachée par un parent et son enfant ne se compte qu'une
 * fois, le dépliage du parent rend l'en-tête de l'enfant). Les
 * conversions n'allouent rien (PIEGE-15) : elles vivent dans la boucle
 * de dessin. Les sommes préfixées de rangées par ligne se bâtissent à la
 * construction — hors du chemin de dessin, en un passage sur les
 * longueurs (G-01 : la projection d'un très gros fichier ne met en page
 * aucune rangée, elle ne compte que des rangées par ligne).</p>
 */
public final class VisualProjection {

    private final FoldModel plis;
    private final int lineCount;
    private final int colonnes;
    private int[] rangneesAvantLigne;
    private final @Nullable int[] longueurs;
    /** Les indentations de tête par ligne (B-05) : les rangées de
     *  suite démarrent à l'indentation de leur ligne — le budget
     *  effectif s'en réduit. Nul quand les longueurs ne sont pas
     *  connues (pas de retour à la ligne). */
    private @Nullable int[] indents;
    /** Le maximum des longueurs, mémo à la construction : le
     *  défilement horizontal le relit à CHAQUE trame (B-08, l'analyse
     *  croisée) — le recalcul balayait tout le tableau à chaque
     *  frappe. Zéro quand les longueurs ne sont pas connues. */
    private int plusLargeLigne;

    /**
     * Construit la projection héritée, sans longueurs connues : une
     * rangée par ligne, tranches entières (fin non bornée).
     *
     * @param lineCount le nombre de lignes du document
     * @param folds     le modèle de plis, lu en direct
     */
    public VisualProjection(int lineCount, FoldModel folds) {
        this(lineCount, folds, null, 0, null);
    }

    /**
     * Construit la projection d'un document, coupes par offsets selon le
     * budget de colonnes.
     *
     * @param document le document affiché, consulté pour ses longueurs
     *                 de lignes uniquement
     * @param folds    le modèle de plis, lu en direct
     * @param columns  le budget de colonnes par rangée ; nul ou négatif
     *                 coupe le retour à la ligne
     */
    public VisualProjection(Document document, FoldModel folds, int columns) {
        this(document.lineCount(), folds, longueursDe(document),
                Math.max(0, columns), document);
    }

    private VisualProjection(int lineCount, FoldModel folds,
            @Nullable int[] longueurs, int columns, @Nullable Document document) {
        if (lineCount < 0) {
            throw new IllegalArgumentException("negative line count: " + lineCount);
        }
        this.lineCount = lineCount;
        this.plis = folds;
        this.longueurs = longueurs;
        this.indents = longueurs != null && columns > 0 && document != null
                ? indentsDe(document, columns) : null;
        this.colonnes = columns;
        this.plusLargeLigne = maxDes(longueurs);
        this.rangneesAvantLigne = batirPrefixe(lineCount, longueurs,
                indents, columns);
    }

    /** Le nombre de lignes du document porté par la projection. */
    public int lineCount() {
        return lineCount;
    }

    /**
     * Épisse la projection à une édition (B-05, l'analyse croisée) :
     * la projection neuve naît de l'ancienne — les entrées d'avant la
     * première ligne touchée se recopient telles quelles, celles de
     * la ligne touchée à la fin se recalculent sur le document édité.
     * Le dépôt de départ épisse pareil ses sommes préfixées sur
     * place ; ici l'immuabilité garde la voie sûre (l'ancienne
     * projection peut vivre dans une frame en cours), le passage ne
     * balaye que les lignes restantes, jamais le document entier.
     *
     * <p>Sans retour à la ligne (budget nul), l'épissure retombe sur
     * la construction fraîche : une rangée par ligne, rien à
     * recopier.</p>
     *
     * @param document      le document APRÈS l'édition
     * @param premiereLigne la première ligne touchée par l'édition
     * @param deltaLignes   le nombre de lignes ajoutées (négatif si
     *                      retirées)
     * @return la projection épicée du document édité
     */
    public VisualProjection splice(Document document, int premiereLigne,
            int deltaLignes) {
        if (longueurs == null || colonnes <= 0) {
            return new VisualProjection(document, plis, 0);
        }
        int nombreNeuf = document.lineCount();
        int[] longueursNeuves = new int[nombreNeuf];
        int[] indentsNeufs = new int[nombreNeuf];
        int borne = Math.min(premiereLigne, Math.min(lineCount, nombreNeuf));
        System.arraycopy(longueurs, 0, longueursNeuves, 0, borne);
        if (indents != null) {
            System.arraycopy(indents, 0, indentsNeufs, 0, borne);
        }
        // Les longueurs et indentations d'après la borne se relisent
        // sur le document édité — un seul passage sur les lignes
        // restantes, jamais le document entier.
        int plafond = Math.max(1, colonnes / 2);
        for (int ligne = borne; ligne < nombreNeuf; ligne++) {
            int debut = document.lineStart(ligne);
            int fin = document.lineEnd(ligne);
            longueursNeuves[ligne] = fin - debut;
            int blancs = 0;
            while (blancs < fin - debut) {
                char c = document.charAt(debut + blancs);
                if (c != ' ' && c != '\t') {
                    break;
                }
                blancs++;
            }
            indentsNeufs[ligne] = Math.min(blancs, plafond);
        }
        int[] prefixeNeuf = new int[nombreNeuf + 1];
        System.arraycopy(rangneesAvantLigne, 0, prefixeNeuf, 0, borne + 1);
        for (int ligne = borne; ligne < nombreNeuf; ligne++) {
            prefixeNeuf[ligne + 1] = prefixeNeuf[ligne]
                    + rangneesDe(longueursNeuves, indentsNeufs, ligne, colonnes);
        }
        VisualProjection epissee = new VisualProjection(nombreNeuf, plis,
                longueursNeuves, colonnes, null);
        epissee.indents = indentsNeufs;
        epissee.rangneesAvantLigne = prefixeNeuf;
        epissee.plusLargeLigne = recalculerPlusLarge(longueurs, longueursNeuves,
                borne, plusLargeLigne);
        return epissee;
    }

    /** Le plus large après épissure : le mémo d'avant reste valable
     *  quand aucune longueur d'après la borne ne le dépasse — sinon
     *  un passage sur les neuves. */
    private static int recalculerPlusLarge(int[] anciennes, int[] neuves,
            int borne, int memoAvant) {
        for (int ligne = borne; ligne < neuves.length; ligne++) {
            if (neuves[ligne] > memoAvant) {
                memoAvant = neuves[ligne];
            }
        }
        if (memoAvant > 0) {
            return memoAvant;
        }
        int max = 0;
        for (int longueur : neuves) {
            if (longueur > max) {
                max = longueur;
            }
        }
        return max;
    }

    /** Le nombre de rangées visibles : rangées des lignes moins celles des cachées. */
    public int rowCount() {
        int cachees = 0;
        int[] f = plis.plisFondus();
        for (int i = 0; i < f.length; i += 2) {
            cachees += rangneesCachees(f[i], f[i + 1]);
        }
        return rangneesAvantLigne[lineCount] - cachees;
    }

    /** Le budget de colonnes signé par la clé de cache (0 = sans coupure). */
    public int columns() {
        return colonnes;
    }

    /**
     * La longueur de la plus large ligne, pour le bornage du défilement
     * horizontal ; zéro quand les longueurs ne sont pas connues.
     *
     * @return la longueur de la plus large ligne, en caractères
     */
    public int longestLineLength() {
        return plusLargeLigne;
    }

    /**
     * La rangée visuelle qui porte la ligne donnée — une ligne cachée se
     * projette sur la première rangée de son en-tête ; sous coupures, la
     * ligne coupée garde la rangée de sa première tranche.
     *
     * @param line le numéro de ligne, borné au document
     * @return le numéro de première rangée visuelle de la ligne
     */
    public int rowOfLine(int line) {
        int cible = borneLigne(line);
        int[] f = plis.plisFondus();
        for (int i = 0; i < f.length; i += 2) {
            if (cible > f[i] && cible <= f[i + 1]) {
                cible = f[i];
                break;
            }
        }
        return rangneesAvantLigne[cible] - rangneesCacheesAvant(cible);
    }

    /**
     * La première ligne du document portée par la rangée donnée.
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return la ligne affichée par la rangée
     */
    public int firstLineOfRow(int row) {
        int borne = borneRangee(row);
        int cachees = 0;
        int debutRegion = 0;
        int[] f = plis.plisFondus();
        for (int i = 0; i < f.length; i += 2) {
            int debut = f[i];
            int fin = f[i + 1];
            if (debut >= lineCount) {
                break;
            }
            int finBornee = Math.min(fin, lineCount - 1);
            int rangeeEnTete = rangneesAvantLigne[debut] - cachees;
            if (borne < rangeeEnTete) {
                return chercheLigne(borne + cachees, debutRegion, debut);
            }
            if (borne < rangeeEnTete + rangneesDeLigne(debut)) {
                return debut;
            }
            cachees += rangneesCachees(debut, fin);
            debutRegion = finBornee + 1;
        }
        return chercheLigne(borne + cachees, debutRegion, lineCount);
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
        int[] f = plis.plisFondus();
        for (int i = 0; i < f.length; i += 2) {
            int debut = f[i];
            int fin = f[i + 1];
            if (debut >= lineCount) {
                break;
            }
            int rangeeEnTete = rangneesAvantLigne[debut] - cachees;
            if (borne < rangeeEnTete + rangneesDeLigne(debut)) {
                if (borne >= rangeeEnTete) {
                    return Math.min(fin, lineCount - 1);
                }
                return chercheLigne(borne + cachees, 0, debut);
            }
            cachees += rangneesCachees(debut, fin);
        }
        return chercheLigne(borne + cachees, 0, lineCount);
    }

    /**
     * Le rang de la tranche portée par la rangée donnée dans sa ligne :
     * zéro pour la première tranche, un pour la continuation.
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return l'indice de tranche dans la ligne, ≥ 0
     */
    public int spanIndexAtRow(int row) {
        int ligne = firstLineOfRow(row);
        return row - rowOfLine(ligne);
    }

    /**
     * La colonne de début de la tranche portée par la rangée donnée.
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return le début de la tranche dans la ligne
     */
    public int spanStartAtRow(int row) {
        // Le budget effectif (B-05) : la première rangée porte le
        // budget brut, les suivantes le budget réduit de l'indent —
        // la suite démarre au retrait, chaque rangée de suite porte
        // colonnes - indent.
        int ligne = firstLineOfRow(row);
        int indent = indents != null ? indents[ligne] : 0;
        int tranche = spanIndexAtRow(row);
        if (tranche == 0 || colonnes <= 0) {
            return tranche * colonnes;
        }
        return colonnes + (tranche - 1) * Math.max(1, colonnes - indent);
    }

    /** L'indentation de tête de la ligne portée par la rangée
     * donnée — le retrait visuel où démarrent ses rangées de suite
     * (B-05). Zéro sans retour à la ligne. */
    public int indentAtRow(int row) {
        if (indents == null) {
            return 0;
        }
        return indents[firstLineOfRow(row)];
    }

    /**
     * La colonne de fin exclusive de la tranche portée par la rangée
     * donnée, bornée à la longueur de la ligne — la plus grande valeur
     * entière quand les longueurs ne sont pas connues (l'appelant borne
     * alors à sa ligne).
     *
     * @param row le numéro de rangée, borné aux rangées visibles
     * @return la fin exclusive de la tranche dans la ligne
     */
    public int spanEndAtRow(int row) {
        if (longueurs == null) {
            return Integer.MAX_VALUE;
        }
        int ligne = firstLineOfRow(row);
        int debut = spanStartAtRow(row);
        int fin = colonnes <= 0 ? longueurs[ligne] : debut + colonnes;
        return Math.min(fin, longueurs[ligne]);
    }

    private int chercheLigne(int rangneeAbsolue, int lo, int hi) {
        int bas = lo;
        int haut = Math.max(lo, hi - 1);
        while (bas < haut) {
            int milieu = (bas + haut + 1) >>> 1;
            if (rangneesAvantLigne[milieu] <= rangneeAbsolue) {
                bas = milieu;
            } else {
                haut = milieu - 1;
            }
        }
        return bas;
    }

    private int rangneesDeLigne(int line) {
        return rangneesAvantLigne[line + 1] - rangneesAvantLigne[line];
    }

    private int rangneesCachees(int debut, int fin) {
        int finBornee = Math.min(fin, lineCount - 1);
        if (finBornee <= debut) {
            return 0;
        }
        return rangneesAvantLigne[finBornee + 1]
                - rangneesAvantLigne[debut + 1];
    }

    private int rangneesCacheesAvant(int line) {
        int total = 0;
        int[] f = plis.plisFondus();
        for (int i = 0; i < f.length; i += 2) {
            int finBornee = Math.min(f[i + 1], lineCount - 1);
            if (line > finBornee) {
                total += rangneesCachees(f[i], f[i + 1]);
            } else if (line > f[i]) {
                total += rangneesAvantLigne[line]
                        - rangneesAvantLigne[f[i] + 1];
            }
        }
        return total;
    }

    private static int[] batirPrefixe(int lineCount, @Nullable int[] longueurs,
            @Nullable int[] indents, int columns) {
        int[] prefixe = new int[lineCount + 1];
        for (int ligne = 0; ligne < lineCount; ligne++) {
            prefixe[ligne + 1] = prefixe[ligne]
                    + rangneesDe(longueurs, indents, ligne, columns);
        }
        return prefixe;
    }

    private static int rangneesDe(@Nullable int[] longueurs,
            @Nullable int[] indents, int ligne, int columns) {
        if (longueurs == null || columns <= 0) {
            return 1;
        }
        int longueur = longueurs[ligne];
        if (longueur <= 0) {
            return 1;
        }
        // Le budget effectif (B-05) : l'indentation de tête rétrécit
        // la largeur utile de la ligne — les rangées de suite
        // démarrent au retrait, la suite tient dans le reste.
        int indent = indents != null ? indents[ligne] : 0;
        int budget = Math.max(1, columns - indent);
        return 1 + (longueur - 1 - indent) / budget;
    }

    /** Les indentations de tête par ligne (B-05) : les blancs qui
     *  ouvrent la ligne, plafonnés à la moitié du budget — le retrait
     *  visuel ne peut pas manger la ligne entière, il reste au moins
     *  une colonne de texte. Consulté UNE fois, à la construction,
     *  hors du chemin de dessin. */
    private static int[] indentsDe(Document document, int columns) {
        int nombre = document.lineCount();
        int[] indents = new int[nombre];
        int plafond = Math.max(1, columns / 2);
        for (int ligne = 0; ligne < nombre; ligne++) {
            int debut = document.lineStart(ligne);
            int fin = document.lineEnd(ligne);
            int blancs = 0;
            while (blancs < fin - debut) {
                char c = document.charAt(debut + blancs);
                if (c != ' ' && c != '\t') {
                    break;
                }
                blancs++;
            }
            indents[ligne] = Math.min(blancs, plafond);
        }
        return indents;
    }

    private static int maxDes(@Nullable int[] longueurs) {
        if (longueurs == null) {
            return 0;
        }
        int max = 0;
        for (int longueur : longueurs) {
            if (longueur > max) {
                max = longueur;
            }
        }
        return max;
    }

    private static int[] longueursDe(Document document) {
        int nombre = document.lineCount();
        int[] longueurs = new int[nombre];
        for (int ligne = 0; ligne < nombre; ligne++) {
            longueurs[ligne] = document.lineEnd(ligne) - document.lineStart(ligne);
        }
        return longueurs;
    }

    private int borneLigne(int line) {
        return Math.max(0, Math.min(line, lineCount - 1));
    }

    private int borneRangee(int row) {
        return Math.max(0, Math.min(row, rowCount() - 1));
    }
}
