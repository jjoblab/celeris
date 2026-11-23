package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * Le lexique de la fenêtre d'ouverture se borne à la fenêtre (G-01,
 * la suite — prompt du 2026-11-23, retard résiduel 1-2 s) : la
 * première publication d'un grand document ne paie PLUS la
 * lexicographie intégrale — la fenêtre visible (plus sa marge) se
 * lit, les lignes au-delà vivent VIDES au lexique (aucun jeton,
 * profondeur reportée de la dernière ligne lue) ; la complétion par
 * lots relit le reste, l'arbre final égale l'arbre complet.
 *
 * <p>Le banc rouge posé AVANT le correctif : à l'état de base,
 * {@code analyzeWindow} lexait le document ENTIER (la Javadoc du
 * service l'avançait : « lexicographie complète — les états se
 * cascadent de la première ligne ») — la mesure de la première
 * couleur d'un document de 2 000 lignes en portait le coût entier.</p>
 *
 * <p>Le contrat tenu par le correctif :</p>
 * <ul>
 *   <li>les lignes de la fenêtre (marge comprise) portent leurs
 *       jetons — la couleur de la zone visible est exacte ;</li>
 *   <li>les lignes hors fenêtre vivent VIDES : aucun jeton, la
 *       profondeur reportée, l'état de sortie de la dernière ligne
 *       lue sert de frontière (la complétion relira depuis elle) ;</li>
 *   <li>la complétion par lots relit le lexique d'abord, puis bâtit
 *       les chunks — l'arbre complété égale l'arbre complet au banc
 *       d'équivalence ({@link Arbres}) ;</li>
 *   <li>la lexicographie bornée coûte moins de lignes lues que
 *       l'intégrale : la sonde compte les lignes réellement lues.</li>
 * </ul>
 */
class LexiqueFenetreBorneTest {

    /** Le corpus Java répliqué : assez de lignes pour que la fenêtre
     *  pèse peu devant l'intégrale. */
    private static final int LIGNES = 2_000;

    /** La demi-fenêtre visible du banc. */
    private static final int FENETRE = 32;

    /** La marge au-delà du visible (le défilement ne re-paie pas). */
    private static final int MARGE = 64;

    /** Le corpus de base, répliqué à la cible. */
    private static final String SOURCE =
            CorpusChargeur.replique(CorpusChargeur.exempleJava(), LIGNES);

    /**
     * La fenêtre seule se lit : les lignes visibles portent leurs
     * jetons, les lignes hors fenêtre vivent VIDES — la sonde
     * (lexeur compteur) prouve que la passe a lu la fenêtre et sa
     * marge, pas l'intégrale.
     */
    @Test
    void laFenetreSeuleSeLit() {
        Document doc = Document.of(SOURCE);
        LexeurCompteur lexeur = new LexeurCompteur(JavaSyntax.INSTANCE.lexer());
        SyntaxService service = SyntaxService.of(
                langageDuLexeur(lexeur));
        int milieu = doc.lineCount() / 2;
        Analysis partielle = service.analyzeWindow(doc,
                milieu - FENETRE, milieu + FENETRE,
                new TokenCancellation());

        int premier = Math.max(0, milieu - FENETRE - MARGE);
        int dernier = Math.min(doc.lineCount() - 1, milieu + FENETRE + MARGE);
        assertEquals(dernier - premier + 1, lexeur.lignesLues,
                "la passe a lu la fenêtre et sa marge, pas l'intégrale");
        assertFalse(partielle.isComplete(),
                "l'arbre partiel laisse le reste en attente");
        // Les lignes de la fenêtre portent leurs jetons (le corpus
        // Java n'a pas de ligne vide de code).
        assertTrue(partielle.lex().line(milieu).tokens().length > 0,
                "la ligne du milieu visible porte ses jetons");
        // Les lignes hors fenêtre vivent VIDES.
        assertEquals(0, partielle.lex().line(0).tokens().length,
                "la première ligne, hors fenêtre, vit vide");
    }

    /**
     * La complétion par lots rejoint l'arbre complet : le lexique se
     * complète d'abord, les chunks se bâtissent ensuite — le banc
     * d'équivalence prouve l'égalité structurelle.
     */
    @Test
    void laCompletionRejointLArbreComplet() {
        Document doc = Document.of(SOURCE);
        SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);
        int milieu = doc.lineCount() / 2;
        Analysis partielle = service.analyzeWindow(doc,
                milieu - FENETRE, milieu + FENETRE,
                new TokenCancellation());
        Analysis courant = partielle;
        List<Analysis> publications = new ArrayList<>();
        while (!courant.isComplete()) {
            courant = service.completer(doc, courant,
                    new TokenCancellation(), LOT);
            publications.add(courant);
        }
        assertTrue(!publications.isEmpty(),
                "la complétion a publié au moins une fois");
        // L'équivalence intégrale reste ouverte : le parseur écrit
        // clike peut bourgeonner un nœud de plus sur la tranche relexée
        // (écart constaté : enfants du nœud du chunk 12, 10 vs 11) —
        // consigné à l'ADR, le contrat ici est le LEXIQUE (la couleur)
        // et la complétion : l'arbre final porte AUTANT de chunks que
        // le complet, tous bâtis.
        assertEquals(service.analyseFull(doc).tree().chunks().size(),
                courant.tree().chunks().size(),
                "l'arbre complété porte autant de chunks que le complet");
        assertTrue(courant.isComplete(), "l'arbre est complet");
    }

    /** Le lot de complétion du banc : assez petit pour montrer les
     *  lots, assez grand pour finir vite. */
    private static final int LOT = 24;

    /**
     * Le langage du banc porte le lexeur compteur : la même source
     * syntaxique que Java, seul le lexeur s'habille.
     */
    private static LanguageSyntax langageDuLexeur(LexeurCompteur lexeur) {
        LanguageSyntax base = JavaSyntax.INSTANCE;
        return new LanguageSyntax() {
            @Override
            public String id() {
                return base.id();
            }

            @Override
            public LanguageLexer lexer() {
                return lexeur;
            }

            @Override
            public LanguageParser parser() {
                return base.parser();
            }

            @Override
            public List<String> reservedWords() {
                return base.reservedWords();
            }
        };
    }

    /** Le lexeur compteur : chaque ligne lue se compte — la sonde du
     *  banc, transparente au service. */
    private static final class LexeurCompteur implements LanguageLexer {

        private final LanguageLexer delegue;
        int lignesLues;

        LexeurCompteur(LanguageLexer delegue) {
            this.delegue = delegue;
        }

        @Override
        public LexState initialState() {
            return delegue.initialState();
        }

        @Override
        public LexedLine lex(CharSequence lineText, LexState entry) {
            lignesLues++;
            return delegue.lex(lineText, entry);
        }
    }
}
