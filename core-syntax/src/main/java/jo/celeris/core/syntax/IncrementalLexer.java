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
            etat = lue.exitState();
        }
        return new LexResult(new LexDocument(lines, profondeurs, lexeur.initialState()),
                0, Math.max(0, nombre - 1));
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
            etat = lue.exitState();
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
        return previous.line(firstLine - 1).exitState();
    }

    private static boolean converge(LexedLine lue, LexDocument previous,
                                    int ligneSuivante, int decalage) {
        int ancienne = ligneSuivante - 1 - decalage;
        if (ancienne < 0 || ancienne >= previous.lineCount()) {
            return false;
        }
        return lue.exitState().equals(previous.line(ancienne).exitState());
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
