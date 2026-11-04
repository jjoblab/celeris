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
        int nombre = doc.lineCount();
        LexedLine[] lignes = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        LexState etat = lexeur.initialState();
        int profondeur = 0;
        for (int ligne = 0; ligne < nombre; ligne++) {
            LexedLine lue = lexeur.lex(doc.lineText(ligne), etat);
            lignes[ligne] = lue;
            profondeurs[ligne] = profondeur;
            profondeur += deltaCrochets(lue.tokens());
            etat = lue.exitState();
        }
        return new LexResult(new LexDocument(lignes, profondeurs, lexeur.initialState()),
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
        site.verifier(previous.lineCount(), doc.lineCount());
        int nombre = doc.lineCount();
        List<LexedLine> relues = new ArrayList<>();
        List<Integer> profondeursRelues = new ArrayList<>();
        LexState etat = etatEntree(previous, site.firstLine());
        int profondeur = previous.bracketDepthAtLineStart(site.firstLine());
        int ligne = site.firstLine();
        int derniere = site.lastLine();
        while (ligne < nombre) {
            LexedLine lue = lexeur.lex(doc.lineText(ligne), etat);
            relues.add(lue);
            profondeursRelues.add(profondeur);
            profondeur += deltaCrochets(lue.tokens());
            etat = lue.exitState();
            if (ligne >= derniere && ligne + 1 < nombre
                    && converge(lue, previous, ligne + 1, site.lineDelta())) {
                break;
            }
            ligne++;
        }
        return assembler(doc, lexeur, previous, site, relues, profondeursRelues,
                Math.min(ligne, nombre - 1), profondeur);
    }

    private LexResult assembler(Document doc, LanguageLexer lexeur, LexDocument previous,
                                EditSite site, List<LexedLine> relues,
                                List<Integer> profondeursRelues, int derniereRelue,
                                int profondeurApres) {
        int nombre = doc.lineCount();
        LexedLine[] lignes = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        for (int i = 0; i < relues.size(); i++) {
            lignes[site.firstLine() + i] = relues.get(i);
            profondeurs[site.firstLine() + i] = profondeursRelues.get(i);
        }
        for (int i = 0; i < site.firstLine(); i++) {
            lignes[i] = previous.line(i);
            profondeurs[i] = previous.bracketDepthAtLineStart(i);
        }
        int decalage = site.lineDelta();
        int differentiel = 0;
        if (derniereRelue + 1 < nombre) {
            int ancienneAlignee = derniereRelue + 1 - decalage;
            differentiel = profondeurApres
                    - previous.bracketDepthAtLineStart(ancienneAlignee);
        }
        for (int i = derniereRelue + 1; i < nombre; i++) {
            lignes[i] = previous.line(i - decalage);
            profondeurs[i] = previous.bracketDepthAtLineStart(i - decalage) + differentiel;
        }
        return new LexResult(
                new LexDocument(lignes, profondeurs, lexeur.initialState()),
                site.firstLine(), Math.max(derniereRelue, site.firstLine()));
    }

    private static LexState etatEntree(LexDocument previous, int firstLine) {
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

    private static int deltaCrochets(Token[] jetons) {
        int delta = 0;
        for (Token jeton : jetons) {
            if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                delta++;
            } else if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                delta--;
            }
        }
        return delta;
    }
}
