package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * Le banc du CRITÈRE DE L'AUDIT figé en CI (feuille de route A5.3) :
 * « zéro divergence incrémental/complet sur 1 150 éditions
 * aléatoires » — la preuve que la reprise incrémentale du lexeur
 * reconverge EXACTEMENT vers la passe complète, quel que soit le
 * chemin d'éditions. La graine est FIXE : le banc est reproductible,
 * une divergence éventuelle se rejoue à l'identique pour l'autopsie.
 *
 * <p>Le générateur produit des gestes réalistes : insertions de
 * mots-clés et d'identifiants, suppressions, remplacements, ouverture
 * et fermeture de chaînes et de commentaires (les états qui fuient),
 * coupures et soudures de lignes. Chaque geste paie sa comparaison :
 * le lexique incrémental DOIT égaler le complet — jetons, états de
 * sortie, profondeurs de crochets — sinon le banc nomme la première
 * ligne divergente et échoue.</p>
 */
final class DivergenceNulleBancTest {

    /** La graine fixe — la reproductibilité prime sur la couverture
     *  statistique : le banc CI rejoue TOUJOURS la même suite. */
    private static final long GRAINE = 20261119L;

    /** Le compte de l'audit : 1 150 éditions aléatoires. */
    private static final int EDITIONS = 1_150;

    private static final LanguageLexer LEXEUR = JavaSyntax.INSTANCE.lexer();

    /** Les fragments insérables : mots-clés, identifiants, littéraux,
     *  opérateurs, commentaires, sauts — de quoi ouvrir et fermer des
     *  états (chaînes, blocs) au hasard. */
    private static final String[] FRAGMENTS = {
            "int ", "return ", "class ", "void ", "final ",
            "alpha", "omega", "x1", "valeur",
            "42", "3.14", "0x1F", "'c'", "\"chaîne\"",
            " + ", " - ", " * ", " == ", " && ", " = ",
            "(", ")", "{", "}", "[", "]", ";", ",",
            "// commentaire\n", "/* bloc\nouvert ", " ferme */",
            "\n", "\n\n", "\"non fermée", "\"\",\n",
            "    ", "\t", " ... ",
    };

    /**
     * 1 150 éditions aléatoires : l'incrémental égale le complet à
     * chaque étape — jetons, états, profondeurs.
     */
    @Test
    void milleCentCinquanteEditionsSansDivergence() {
        Random alea = new Random(GRAINE);
        String text = """
                package exemple;

                class Premiere {
                    int un() { return 1; }
                    String deux(String s) { return s + "x"; }
                }
                """;
        Document document = Document.of(text);
        IncrementalLexer moteur = new IncrementalLexer();
        LexDocument reference = moteur.lexFull(document, LEXEUR).lex();

        for (int i = 0; i < EDITIONS; i++) {
            Geste geste = tirer(alea, document);
            Document apres = Document.of(geste.appliquerA(document.text()));
            EditSite site = EditSite.of(document, apres, geste.start,
                    geste.end, geste.inser);
            LexDocument increm = moteur.lexIncremental(apres, LEXEUR,
                    reference, site).lex();
            reference = moteur.lexFull(apres, LEXEUR).lex();
            document = apres;

            java.util.Optional<String> gap = premierEcart(reference, increm);
            assertTrue(gap.isEmpty(),
                    "édition " + i + " (" + geste + ") : " + gap.orElse(""));
        }
        // Le document final a bien vécu : mille gestes y ont passé, le
        // texte reste vivant (non vide, lignes présentes).
        assertTrue(document.length() > 0 && document.lineCount() >= 1,
                "le document a survécu à mille éditions");
    }

    /** Un geste d'édition : plage remplacée et texte inséré. */
    private record Geste(int start, int end, String inser) {

        String appliquerA(String text) {
            int d = Math.min(start, text.length());
            int f = Math.min(end, text.length());
            return text.substring(0, d) + inser + text.substring(f);
        }

        @Override
        public String toString() {
            return "[" + start + ".." + end + ") ← « " + inser + " »";
        }
    }

    /** Tire un geste réaliste : insertion, suppression, remplacement. */
    private static Geste tirer(Random alea, Document document) {
        int n = document.length();
        String fragment = FRAGMENTS[alea.nextInt(FRAGMENTS.length)];
        int sorte = alea.nextInt(10);
        if (sorte < 5) {
            // insertion simple
            return new Geste(position(alea, n), position(alea, n), fragment);
        }
        if (sorte < 7) {
            // suppression d'une petite plage
            int d = position(alea, n);
            int f = Math.min(n, d + 1 + alea.nextInt(12));
            return new Geste(d, f, "");
        }
        if (sorte < 9) {
            // remplacement
            int d = position(alea, n);
            int f = Math.min(n, d + alea.nextInt(8));
            return new Geste(d, f, fragment);
        }
        // coupure ou soudure de ligne au hasard
        int line = alea.nextInt(document.lineCount());
        int lineStart = document.lineStart(line);
        return new Geste(lineStart, lineStart, "\n");
    }

    /** Une position valide, biaisée vers le début (le texte vivant). */
    private static int position(Random alea, int length) {
        if (length == 0) {
            return 0;
        }
        return alea.nextInt(length);
    }

    /**
     * La première divergence entre référence et incrémental : jetons
     * (sorte, bornes), états de sortie, profondeurs — vide si aucun.
     */
    private static java.util.Optional<String> premierEcart(LexDocument reference,
            LexDocument increm) {
        if (reference.lineCount() != increm.lineCount()) {
            return java.util.Optional.of("nombre de lignes : référence "
                    + reference.lineCount() + " vs incrémental "
                    + increm.lineCount());
        }
        for (int l = 0; l < reference.lineCount(); l++) {
            LexedLine ligneRef = reference.line(l);
            LexedLine ligneInc = increm.line(l);
            if (!java.util.Objects.equals(ligneRef.exitState(),
                    ligneInc.exitState())) {
                return java.util.Optional.of("état de sortie L" + l + " : "
                        + ligneRef.exitState() + " vs " + ligneInc.exitState());
            }
            Token[] jetonsRef = ligneRef.tokens();
            Token[] jetonsInc = ligneInc.tokens();
            if (jetonsRef.length != jetonsInc.length) {
                return java.util.Optional.of("L" + l + " : " + jetonsRef.length
                        + " jetons vs " + jetonsInc.length + " ("
                        + decrire(jetonsRef) + " vs " + decrire(jetonsInc) + ")");
            }
            for (int t = 0; t < jetonsRef.length; t++) {
                Token a = jetonsRef[t];
                Token b = jetonsInc[t];
                if (a.kind() != b.kind() || a.start() != b.start()
                        || a.end() != b.end()) {
                    return java.util.Optional.of("L" + l + " jeton " + t + " : "
                            + a.kind() + "[" + a.start() + ".." + a.end()
                            + "] vs " + b.kind() + "[" + b.start() + ".."
                            + b.end() + "]");
                }
            }
            if (reference.bracketDepthAtLineStart(l)
                    != increm.bracketDepthAtLineStart(l)) {
                return java.util.Optional.of("profondeur L" + l + " : "
                        + reference.bracketDepthAtLineStart(l) + " vs "
                        + increm.bracketDepthAtLineStart(l));
            }
        }
        return java.util.Optional.empty();
    }

    private static String decrire(Token[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.kind()).append('[').append(t.start()).append(':')
                    .append(t.end()).append(']');
        }
        return sb.toString();
    }
}
