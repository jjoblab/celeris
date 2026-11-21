package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;

/**
 * La fermeture de largeur NULLE (audit coloration, piste H8) : le
 * format TextMate l'utilise légitimement — {@code end} en anticipation
 * {@code (?=...)}, ancre {@code $} — et les grammaires réelles du
 * dépôt en regorgent (csharp : {@code using} clos par {@code (?=;)},
 * clause {@code where} close par {@code $}). Le lexeur REJETTE
 * aujourd'hui toute correspondance de longueur nulle
 * ({@code m.getEnd() == m.getBegin()}) : un bloc dont la fermeture
 * est de largeur nulle NE SE FERME JAMAIS, l'état ouvert se propage
 * jusqu'à la fin du document et colore faux toute la suite — la cause
 * plausible des « incohérences sur certains fichiers ».
 *
 * <p>Les moteurs TextMate de référence acceptent la largeur nulle
 * avec une garde anti-boucle (avancer d'un caractère quand la
 * correspondance ne progresse pas). Ces bancs exigent ce contrat :
 * la fermeture de largeur nulle ferme le bloc, la ligne suivante
 * n'hérite PAS de la portée du bloc resté ouvert.</p>
 */
class LargeurNulleFinTest {

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammar grammaireDePaquet(String id) {
        var flux = LargeurNulleFinTest.class.getResourceAsStream(
                "/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return Grammars.read(new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * csharp {@code class Z { int a = 1; }} : la déclaration ouvre un
     * bloc dont la fermeture {@code (?<=\})|(?=;)} est de largeur
     * nulle — le bloc DOIT se fermer sur la ligne, l'état de sortie
     * est vide (la sonde de l'audit le montre ouvert aujourd'hui).
     */
    @Test
    void finEnAnticipationFermeLeBlocCsharp() {
        Grammar grammaire = grammaireDePaquet("csharp");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        LexedLine ligne = lexeur.lex("class Z { int a = 1; }",
                GrammarState.EMPTY);
        assertFalse(((GrammarState) ligne.exitState()).dansBloc(),
                "la fermeture de largeur nulle ((?<=})|(?=;)) doit fermer "
                        + "le bloc de déclaration — l'état ouvert fuit "
                        + "aujourd'hui vers toute la suite du fichier");
    }

    /**
     * La propagation du défaut : le bloc jamais clos AVALE la fin de
     * ligne — le corps de la classe devient UN seul jeton (la sonde
     * de l'audit : KEYWORD[0..5] STRING[5..26], deux jetons pour toute
     * la ligne) au lieu des jetons attendus du corps.
     */
    @Test
    void leBlocJamaisClosAvaleLaFinDeLigneCsharp() {
        Grammar grammaire = grammaireDePaquet("csharp");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        LexedLine ligne = lexeur.lex("class Z { int a = 1; }",
                GrammarState.EMPTY);
        assertTrue(ligne.tokens().length > 2,
                "le corps de la classe se colore jeton par jeton — "
                        + "obtenu " + ligne.tokens().length
                        + " jetons pour toute la ligne : le bloc ouvert "
                        + "l'avale entière");
    }

    /**
     * Une grammaire minimale construite au banc : bloc ouvert par
     * {@code «} et clos par l'anticipation {@code (?=»)} — largeur
     * nulle exacte. Le bloc se ferme à l'anticipation.
     */
    @Test
    void fermetureAnticipationGrammaireMinimale() {
        BlockRule bloc = new BlockRule(
                Expressions.compiler("«"), "(?=»)",
                Expressions.compiler("(?=»)"), false,
                "bloc.audit", "contenu.audit", null, null,
                java.util.List.of());
        Grammar grammaire = new Grammar("audit", "source.audit",
                java.util.List.of(bloc), java.util.List.of(),
                new GrammarReport(0, 0, 0, 0, 0, 0, 0, 0),
                java.util.List.of());
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        LexedLine ligne = lexeur.lex("« contenu »", GrammarState.EMPTY);
        assertFalse(((GrammarState) ligne.exitState()).dansBloc(),
                "l'anticipation (?=») de largeur nulle doit fermer le bloc");
    }

    /**
     * L'ancre de fin de ligne {@code $} (clause where de csharp) est
     * de largeur nulle elle aussi : le bloc clos à la fin de ligne.
     */
    @Test
    void ancreFinDeLigneFermeLeBloc() {
        BlockRule bloc = new BlockRule(
                Expressions.compiler("ou:"), "$",
                Expressions.compiler("$"), false,
                "bloc.fin", null, null, null,
                java.util.List.of());
        Grammar grammaire = new Grammar("audit", "source.audit",
                java.util.List.of(bloc), java.util.List.of(),
                new GrammarReport(0, 0, 0, 0, 0, 0, 0, 0),
                java.util.List.of());
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        LexedLine ligne = lexeur.lex("ou: condition", GrammarState.EMPTY);
        assertFalse(((GrammarState) ligne.exitState()).dansBloc(),
                "l'ancre $ de largeur nulle doit fermer le bloc en fin de ligne");
    }
}
