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
        return lexFull(doc, lexeur, new JetonAnnulation());
    }

    /**
     * Passe complète avec point de contrôle d'annulation (ADR-007) : tous
     * les 256 lignes, le jeton s'interroge — annuler coûte moins cher que
     * terminer. Sans annulation, la passe est identique à la voie simple.
     *
     * @param doc    le document
     * @param lexeur le lexeur du langage
     * @param jeton  le jeton d'annulation interrogé entre les lots de lignes
     * @return le lexique complet du document
     * @throws AnalyseAnnulee si le travail est annulé en cours de passe
     */
    public LexResult lexFull(Document doc, LanguageLexer lexeur, JetonAnnulation jeton) {
        int nombre = doc.lineCount();
        LexedLine[] lignes = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        LexState etat = lexeur.initialState();
        int profondeur = 0;
        for (int ligne = 0; ligne < nombre; ligne++) {
            if ((ligne & 0xFF) == 0) {
                jeton.verifier();
            }
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
        LexedLine[] anciennes = previous.lignes();
        int[] anciennesProfondeurs = previous.profondeurs();
        LexedLine[] lignes = new LexedLine[nombre];
        int[] profondeurs = new int[nombre];
        for (int i = 0; i < relues.size(); i++) {
            lignes[site.firstLine() + i] = relues.get(i);
            profondeurs[site.firstLine() + i] = profondeursRelues.get(i);
        }
        assemblerPrefixe(previous, anciennes, anciennesProfondeurs, lignes, profondeurs,
                site.firstLine());
        int decalage = site.lineDelta();
        int differentiel = 0;
        if (derniereRelue + 1 < nombre) {
            int ancienneAlignee = Math.max(0,
                    Math.min(derniereRelue + 1 - decalage, anciennesProfondeurs.length - 1));
            differentiel = profondeurApres - anciennesProfondeurs[ancienneAlignee];
        }
        assemblerSuffixe(previous, anciennes, anciennesProfondeurs, lignes, profondeurs,
                derniereRelue + 1, decalage, differentiel);
        return new LexResult(
                new LexDocument(lignes, profondeurs, lexeur.initialState()),
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
    private static void assemblerPrefixe(LexDocument previous, LexedLine[] anciennes,
                                         int[] anciennesProfondeurs, LexedLine[] lignes,
                                         int[] profondeurs, int prefixe) {
        int copie = Math.min(prefixe, anciennes.length);
        if (copie > 0) {
            System.arraycopy(anciennes, 0, lignes, 0, copie);
            System.arraycopy(anciennesProfondeurs, 0, profondeurs, 0, copie);
        }
        for (int i = copie; i < prefixe; i++) {
            lignes[i] = previous.line(i);
            profondeurs[i] = previous.bracketDepthAtLineStart(i);
        }
    }

    /**
     * Le suffixe au-delà de la dernière ligne relue : paniers partagés à
     * l'identité, profondeurs décalées du seul différentiel — un
     * différentiel nul les copie de masse, sinon l'ajustement reste
     * élément par élément sur les seuls entiers.
     */
    private static void assemblerSuffixe(LexDocument previous, LexedLine[] anciennes,
                                         int[] anciennesProfondeurs, LexedLine[] lignes,
                                         int[] profondeurs, int debut, int decalage,
                                         int differentiel) {
        int nombre = lignes.length;
        int ancienDebut = debut - decalage;
        if (debut >= nombre) {
            return;
        }
        if (ancienDebut >= 0 && ancienDebut <= anciennes.length
                && nombre - debut <= anciennes.length - ancienDebut) {
            System.arraycopy(anciennes, ancienDebut, lignes, debut, nombre - debut);
            if (differentiel == 0) {
                System.arraycopy(anciennesProfondeurs, ancienDebut, profondeurs, debut,
                        nombre - debut);
            } else {
                for (int i = debut; i < nombre; i++) {
                    profondeurs[i] = anciennesProfondeurs[i - decalage] + differentiel;
                }
            }
            return;
        }
        for (int i = debut; i < nombre; i++) {
            lignes[i] = previous.line(i - decalage);
            profondeurs[i] = previous.bracketDepthAtLineStart(i - decalage) + differentiel;
        }
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
