package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Moteur de lexicographie incrémentale (ADR-004). Une édition ne relit que
 * les lignes dont le contenu peut changer, puis continue tant que l'état
 * de sortie diverge de l'ancien — la cascade d'un commentaire de bloc
 * s'arrête exactement à sa fermeture, jamais au-delà (PIEGE-19) ; un
 * commentaire ouvert sans fermeture cascade jusqu'à la dernière ligne et
 * termine. Les paniers au-delà de la reconvergence sont partagés à
 * l'identité avec la passe précédente.
 *
 * <p>La profondeur de crochets au début de chaque ligne accompagne les
 * paniers : recalculée sur les lignes relues, décalée du différentiel net
 * sur la suite — un différentiel nul laisse la suite intacte.</p>
 */
public final class IncrementalLexer {

    /** Passe complète : toutes les lignes, à l'ouverture ou à la revalidation. */
    public LexResult lexFull(Document doc, LanguageLexer lexeur) {
        return lexFull(doc, lexeur, new TokenCancellation());
    }

    /**
     * Passe complète avec point de contrôle d'annulation (ADR-007) : tous
     * les 256 lignes, le jeton s'interroge — annuler coûte moins cher que
     * terminer. Sans annulation, la passe est identique à la voie simple.
     *
     * @param doc    le document
     * @param lexeur le lexeur du langage
     * @param token  le jeton d'annulation interrogé entre les lots de lignes
     * @return le lexique complet du document
     * @throws AnalysisCancelled si le travail est annulé en cours de passe
     */
    public LexResult lexFull(Document doc, LanguageLexer lexeur, TokenCancellation token) {
        int nombre = doc.lineCount();
        LexedLine[] lines = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        LexState etat = lexeur.initialState();
        int profondeur = 0;
        for (int line = 0; line < nombre; line++) {
            if ((line & 0xFF) == 0) {
                token.check();
            }
            LexedLine lue = lexeur.lex(doc.lineText(line), etat);
            lines[line] = lue;
            profondeurs[line] = profondeur;
            profondeur += deltaCrochets(lue.tokens());
            LexState sortie = lue.exitState();
            // Une ligne LUE porte toujours son état (le contrat du
            // lexeur) : la traduction ne sert que NullAway.
            etat = sortie == null ? etat : sortie;
        }
        return new LexResult(new LexDocument(lines, profondeurs, lexeur.initialState()),
                0, Math.max(0, nombre - 1));
    }

    /**
     * Passe bornée à la fenêtre d'ouverture (G-01, la suite — le
     * retard résiduel du prompt du 2026-11-23) : les lignes de la
     * fenêtre (marge comprise) se lisent SEULES, les autres vivent
     * VIDES — aucun jeton, profondeur reportée de la dernière ligne
     * lue, état de sortie de la dernière ligne lue en frontière. La
     * complétion par lots relit le reste depuis cette frontière
     * ({@link #completerLexique}) : la première couleur d'un grand
     * document ne paie plus l'intégrale, seulement le visible.
     *
     * <p>La ligne vide du lexique partiel est un REMPLI, pas un
     * verdict : sa profondeur (celle de la frontière) sert aux
     * guides de la zone vide, son état de sortie ne se lit JAMAIS au
     * delà de la frontière — la complétion relit depuis la dernière
     * ligne réellement lue, jamais depuis une ligne vide.</p>
     *
     * @param doc    le document
     * @param lexeur le lexeur du langage
     * @param first  la première ligne visible
     * @param last   la dernière ligne visible
     * @param marge  le nombre de lignes au-delà du visible (le
     *               défilement ne re-paie pas la lecture)
     * @param token  le jeton d'annulation, interrogé entre les lots
     * @return le lexique partiel : fenêtre et marge lues, reste vide
     * @throws AnalysisCancelled si la passe est annulée en cours
     */
    public LexResult lexFenetre(Document doc, LanguageLexer lexeur,
            int first, int last, int marge, TokenCancellation token) {
        int nombre = doc.lineCount();
        LexedLine[] lines = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        LexState etat = lexeur.initialState();
        int profondeur = 0;
        int premier = Math.max(0, first - marge);
        int dernier = Math.min(nombre - 1, last + marge);
        for (int line = 0; line <= dernier; line++) {
            if ((line & 0xFF) == 0) {
                token.check();
            }
            if (line < premier) {
                // Le préfixe vide : ses lignes ne portent RIEN, la
                // frontière d'état reste celle de la première ligne
                // lue — la complétion relira le préfixe depuis zéro.
                lines[line] = EmptyLexicon.EMPTY_LINE;
                profondeurs[line] = 0;
                continue;
            }
            LexedLine lue = lexeur.lex(doc.lineText(line), etat);
            lines[line] = lue;
            profondeurs[line] = profondeur;
            profondeur += deltaCrochets(lue.tokens());
            LexState sortie = lue.exitState();
            // Une ligne LUE porte toujours son état (le contrat du
            // lexeur) : la traduction ne sert que NullAway.
            etat = sortie == null ? etat : sortie;
        }
        for (int line = dernier + 1; line < nombre; line++) {
            // Le suffixe vide : la profondeur reportée de la frontière
            // sert aux guides, les jetons viendront de la complétion.
            lines[line] = EmptyLexicon.EMPTY_LINE;
            profondeurs[line] = profondeur;
        }
        return new LexResult(new LexDocument(lines, profondeurs,
                lexeur.initialState()), premier, Math.max(premier, dernier));
    }

    /**
     * Complète le lexique partiel d'une fenêtre : relit par lots les
     * lignes encore vides — le préfixe depuis zéro, le suffixe depuis
     * la frontière d'état de la dernière ligne lue. La complétion de
     * l'arbre ({@code completer}) l'appelle AVANT de bâtir les
     * chunks : aucun chunk ne se bâtit sur des jetons fantômes.
     *
     * @param doc      le document du lexique
     * @param lexeur  le lexeur du langage
     * @param partial le lexique partiel de la fenêtre
     * @param token   le jeton d'annulation, interrogé entre les lots
     * @param maxLignes le nombre maximal de lignes relues par lot
     * @return le lexique complété d'autant de lignes, le même si
     *         rien à relire
     * @throws AnalysisCancelled si la complétion est annulée en cours
     */
    public LexDocument completerLexique(Document doc, LanguageLexer lexeur,
            LexDocument partial, TokenCancellation token, int maxLignes) {
        LexedLine[] lines = partial.lines();
        int nombre = lines.length;
        if (maxLignes <= 0 || nombre == 0) {
            return partial;
        }
        LexedLine[] relues = lines.clone();
        int[] profondeurs = partial.profondeurs().clone();
        int reluesCompte = 0;
        // Une SEULE passe ordonnée : depuis la ligne 0, chaque ligne
        // vide se relit dans l'état courant, chaque ligne lue fait
        // avancer l'état — l'ordre du document est respecté, la
        // cascade des états reste correcte de bout en bout.
        LexState etat = lexeur.initialState();
        int profondeur = 0;
        for (int line = 0; line < nombre && reluesCompte < maxLignes; line++) {
            if ((line & 0xFF) == 0) {
                token.check();
            }
            if (lines[line] == EmptyLexicon.EMPTY_LINE) {
                LexedLine lue = lexeur.lex(doc.lineText(line), etat);
                relues[line] = lue;
                profondeurs[line] = profondeur;
                profondeur += deltaCrochets(lue.tokens());
                LexState sortie = lue.exitState();
                etat = sortie == null ? etat : sortie;
                reluesCompte++;
            } else {
                LexState sortie = lines[line].exitState();
                etat = sortie == null ? etat : sortie;
                profondeur = profondeurs[line];
            }
        }
        if (reluesCompte == 0) {
            return partial;
        }
        return new LexDocument(relues, profondeurs, lexeur.initialState());
    }

    /**
     * Passe incrémentale après une édition.
     *
     * @param doc      le document après l'édition
     * @param lexeur   le lexeur du langage
     * @param previous le lexique du document avant l'édition
     * @param site     le site de l'édition en lignes
     * @return le nouveau lexique et la plage relue
     */
    public LexResult lexIncremental(Document doc, LanguageLexer lexeur,
                                    LexDocument previous, EditSite site) {
        site.check(previous.lineCount(), doc.lineCount());
        int nombre = doc.lineCount();
        List<LexedLine> relues = new ArrayList<>();
        List<Integer> profondeursRelues = new ArrayList<>();
        LexState etat = entryState(previous, site.firstLine());
        int profondeur = previous.bracketDepthAtLineStart(site.firstLine());
        int line = site.firstLine();
        int derniere = site.lastLine();
        while (line < nombre) {
            LexedLine lue = lexeur.lex(doc.lineText(line), etat);
            relues.add(lue);
            profondeursRelues.add(profondeur);
            profondeur += deltaCrochets(lue.tokens());
            LexState sortie = lue.exitState();
            etat = sortie == null ? etat : sortie;
            if (line >= derniere && line + 1 < nombre
                    && converge(lue, previous, line + 1, site.lineDelta())) {
                break;
            }
            line++;
        }
        return assemble(doc, lexeur, previous, site, relues, profondeursRelues,
                Math.min(line, nombre - 1), profondeur);
    }

    private LexResult assemble(Document doc, LanguageLexer lexeur, LexDocument previous,
                                EditSite site, List<LexedLine> relues,
                                List<Integer> profondeursRelues, int derniereRelue,
                                int profondeurApres) {
        int nombre = doc.lineCount();
        LexedLine[] anciennes = previous.lines();
        int[] anciennesProfondeurs = previous.profondeurs();
        LexedLine[] lines = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        for (int i = 0; i < relues.size(); i++) {
            lines[site.firstLine() + i] = relues.get(i);
            profondeurs[site.firstLine() + i] = profondeursRelues.get(i);
        }
        assemblePrefix(previous, anciennes, anciennesProfondeurs, lines, profondeurs,
                site.firstLine());
        int decalage = site.lineDelta();
        int differentiel = 0;
        if (derniereRelue + 1 < nombre) {
            int ancienneAlignee = Math.max(0,
                    Math.min(derniereRelue + 1 - decalage, anciennesProfondeurs.length - 1));
            differentiel = profondeurApres - anciennesProfondeurs[ancienneAlignee];
        }
        assembleSuffix(previous, anciennes, anciennesProfondeurs, lines, profondeurs,
                derniereRelue + 1, decalage, differentiel);
        return new LexResult(
                new LexDocument(lines, profondeurs, lexeur.initialState()),
                site.firstLine(), Math.max(derniereRelue, site.firstLine()));
    }

    /**
     * Le préfixe intact — les lignes avant l'édition sont identiques des
     * deux côtés. La copie de masse ({@code System.arraycopy}) remplace
     * la relecture élément par élément : sur 50 000 lignes, c'est elle
     * qui tenait le plancher du budget de ré-analyse (PIEGE-13) sur
     * machine lente. Le garde-fou retombe sur la relecture bornée si le
     * site dépasse le lexique précédent — cas hors contrat, jamais vu.
     */
    private static void assemblePrefix(LexDocument previous, LexedLine[] anciennes,
                                         int[] anciennesProfondeurs, LexedLine[] lines,
                                         int[] profondeurs, int prefixe) {
        int copie = Math.min(prefixe, anciennes.length);
        if (copie > 0) {
            System.arraycopy(anciennes, 0, lines, 0, copie);
            System.arraycopy(anciennesProfondeurs, 0, profondeurs, 0, copie);
        }
        for (int i = copie; i < prefixe; i++) {
            lines[i] = previous.line(i);
            profondeurs[i] = previous.bracketDepthAtLineStart(i);
        }
    }

    /**
     * Le suffixe au-delà de la dernière ligne relue : paniers partagés à
     * l'identité, profondeurs décalées du seul différentiel — un
     * différentiel nul les copie de masse, sinon l'ajustement reste
     * élément par élément sur les seuls entiers.
     */
    private static void assembleSuffix(LexDocument previous, LexedLine[] anciennes,
                                         int[] anciennesProfondeurs, LexedLine[] lines,
                                         int[] profondeurs, int start, int decalage,
                                         int differentiel) {
        int nombre = lines.length;
        int ancienDebut = start - decalage;
        if (start >= nombre) {
            return;
        }
        if (ancienDebut >= 0 && ancienDebut <= anciennes.length
                && nombre - start <= anciennes.length - ancienDebut) {
            System.arraycopy(anciennes, ancienDebut, lines, start, nombre - start);
            if (differentiel == 0) {
                System.arraycopy(anciennesProfondeurs, ancienDebut, profondeurs, start,
                        nombre - start);
            } else {
                for (int i = start; i < nombre; i++) {
                    profondeurs[i] = anciennesProfondeurs[i - decalage] + differentiel;
                }
            }
            return;
        }
        for (int i = start; i < nombre; i++) {
            lines[i] = previous.line(i - decalage);
            profondeurs[i] = previous.bracketDepthAtLineStart(i - decalage) + differentiel;
        }
    }

    private static LexState entryState(LexDocument previous, int firstLine) {
        if (firstLine <= 0) {
            return previous.stateBefore(0);
        }
        LexedLine precedente = previous.line(firstLine - 1);
        LexState sortie = precedente.exitState();
        // La ligne VIDE du lexique partiel porte l'état null : le
        // document INITIAL sert d'entrée — l'incrément relit la ligne
        // depuis zéro quand la complétion ne l'a pas encore relue.
        return sortie == null ? previous.stateBefore(0) : sortie;
    }

    private static boolean converge(LexedLine lue, LexDocument previous,
                                    int ligneSuivante, int decalage) {
        int ancienne = ligneSuivante - 1 - decalage;
        if (ancienne < 0 || ancienne >= previous.lineCount()) {
            return false;
        }
        LexState nouvelle = lue.exitState();
        LexState ancienneEtat = previous.line(ancienne).exitState();
        return nouvelle != null && nouvelle.equals(ancienneEtat);
    }

    private static int deltaCrochets(Token[] tokens) {
        int delta = 0;
        for (Token token : tokens) {
            if (token.kind() == TokenKind.OPEN_BRACKET) {
                delta++;
            } else if (token.kind() == TokenKind.CLOSE_BRACKET) {
                delta--;
            }
        }
        return delta;
    }
}
