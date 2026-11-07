package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.Language;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.text.Document;

/**
 * Les langages de la voie de grammaires (C-15/C-16) : le reste des P1
 * (TOML, YAML, properties, Markdown) et la longue traîne P2 (Python,
 * Shell) se colorent et se replient par grammaire, sans analyseur
 * dédié — le critère de la matrice : coloration + plis par langue, le
 * tout consommé du même arbre.
 */
class LangagesGrammairesTest {

    /** TOML : sections, clés, scalaires et plis des chaînes triples. */
    @Test
    void tomlColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.toml"));
        Analysis analyse = analyse(LangagesGrammaires.toml(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires #");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les sections [table]");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les clés");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.CONSTANT), "les booléens");
        assertTrue(aUneLigne(analyse, TokenKind.OPEN_BRACKET),
                "la chaîne triple ouvre un crochet apparié");
        assertFalse(TreeFolds.regions(doc, analyse).isEmpty(),
                "le gabarit multi-lignes se replie");
    }

    /** YAML : clés, scalaires, ancres et plis des séquences en flux. */
    @Test
    void yamlColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.yaml"));
        Analysis analyse = analyse(LangagesGrammaires.yaml(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires #");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les clés");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.CONSTANT), "les booléens");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les indicateurs de bloc");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "la séquence en flux multi-lignes se replie");
    }

    /**
     * properties : clés, valeurs et commentaires — le format n'a aucune
     * structure pliable, le plan de plis est vide, à bon droit.
     */
    @Test
    void propertiesColoreSansPlis() {
        Document doc = Document.of(corpus("exemple.properties"));
        Analysis analyse = analyse(LangagesGrammaires.proprietes(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires #");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les clés");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les valeurs");
        assertTrue(TreeFolds.regions(doc, analyse).isEmpty(),
                "rien à plier dans un format ligne à ligne");
    }

    /** Markdown : titres, clôtures de code, citations et plis. */
    @Test
    void markdownColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.md"));
        Analysis analyse = analyse(LangagesGrammaires.markdown(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les titres");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "le code clôturé et en ligne");
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les citations");
        assertTrue(aUneLigne(analyse, TokenKind.PUNCTUATION), "les marqueurs d'emphase");
        assertTrue(aUneLigne(analyse, TokenKind.CONSTANT), "les emphases");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "la clôture de code se replie");
    }

    /** Python : la longue traîne P2, chaînes triples repliées comprises. */
    @Test
    void pythonColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.py"));
        Analysis analyse = analyse(LangagesGrammaires.python(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les mots réservés");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les classes");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les fonctions et variables");
        assertTrue(aUneLigne(analyse, TokenKind.ANNOTATION), "les décorateurs");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "la chaîne triple et les appels multi-lignes se replient");
    }

    /** Shell : la longue traîne P2, fonctions repliées comprises. */
    @Test
    void shellColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.sh"));
        Analysis analyse = analyse(LangagesGrammaires.shell(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires #");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les mots réservés");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les variables et fonctions");
        assertTrue(aUneLigne(analyse, TokenKind.OPERATOR), "les opérateurs");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "le corps de fonction multi-lignes se replie");
    }

    /** SQL : mots réservés insensibles à la casse, bloc replié compris. */
    @Test
    void sqlColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.sql"));
        Analysis analyse = analyse(LangagesGrammaires.sql(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires --");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les mots réservés");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les types des colonnes");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les identifiants");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes et littéraux");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.OPERATOR), "les opérateurs");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "le commentaire en bloc se replie");
    }

    /** C : préprocesseur, types natifs et corps repliés. */
    @Test
    void cColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.c"));
        Analysis analyse = analyse(LangagesGrammaires.c(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires en bloc");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les mots réservés et le préprocesseur");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les types natifs");
        assertTrue(aUneLigne(analyse, TokenKind.CONSTANT), "NULL et les constantes");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.OPERATOR), "les opérateurs");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "le corps des fonctions se replie");
    }

    /** C++ : classes, espaces de noms et gabarits, plis compris. */
    @Test
    void cppColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.cpp"));
        Analysis analyse = analyse(LangagesGrammaires.cpp(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires //");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "class, namespace, template");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "std et les types natifs");
        assertTrue(aUneLigne(analyse, TokenKind.CONSTANT), "nullptr et constantes");
        assertTrue(aUneLigne(analyse, TokenKind.STRING), "les chaînes");
        assertTrue(aUneLigne(analyse, TokenKind.NUMBER), "les nombres");
        assertTrue(aUneLigne(analyse, TokenKind.OPERATOR), ":: et les opérateurs");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "le corps des classes se replie");
    }

    /** R8/ProGuard : les règles et leurs blocs de membres repliés. */
    @Test
    void proguardColoreEtReplie() {
        Document doc = Document.of(corpus("exemple.pro"));
        Analysis analyse = analyse(LangagesGrammaires.proguard(), doc);
        assertTrue(aUneLigne(analyse, TokenKind.COMMENT), "les commentaires #");
        assertTrue(aUneLigne(analyse, TokenKind.KEYWORD), "les règles -keep & co");
        assertTrue(aUneLigne(analyse, TokenKind.TYPE), "les noms pleinement qualifiés");
        assertTrue(aUneLigne(analyse, TokenKind.IDENTIFIER), "les jokers * et **");
        assertTrue(aUneLigne(analyse, TokenKind.OPERATOR), "les points de suspension");
        assertTrue(aUneLigne(analyse, TokenKind.PUNCTUATION), "les séparateurs");
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertFalse(regions.isEmpty(), "les blocs de membres se replient");
    }

    /** Les langages de la voie se font identifier par nom de fichier. */
    @Test
    void registreIdentifieLesLangages() {
        List<Language> langages = LangagesGrammaires.langages();
        assertEquals(10, langages.size(), "dix langages embarqués");
        LanguageRegistry registre = LanguageRegistry.of(
                langages.toArray(new Language[0]));
        assertEquals("toml", identifie(registre, "conf/apparail.toml"));
        assertEquals("yaml", identifie(registre, "manifeste.yml"));
        assertEquals("properties", identifie(registre, "ressources.properties"));
        assertEquals("markdown", identifie(registre, "LISEZMOI.md"));
        assertEquals("python", identifie(registre, "outillage/build.py"));
        assertEquals("shell", identifie(registre, "scripts/assembly.sh"));
        assertEquals("sql", identifie(registre, "migrations/001_registre.sql"));
        assertEquals("c", identifie(registre, "natif/tampon.c"));
        assertEquals("cpp", identifie(registre, "natif/moteur.cpp"));
        assertEquals("c", identifie(registre, "natif/tampon.h"));
        assertEquals("cpp", identifie(registre, "natif/moteur.hpp"));
        assertEquals("proguard", identifie(registre, "app/proguard-rules.pro"));
    }

    private static String identifie(LanguageRegistry registre, String nom) {
        Language langage = registre.byFilename(nom);
        assertTrue(langage != null, "non identifié : " + nom);
        return langage.id();
    }

    private static Analysis analyse(GrammaireSyntax langage, Document doc) {
        return SyntaxService.of(langage).analyseFull(doc);
    }

    private static boolean aUneLigne(Analysis analyse, TokenKind sorte) {
        for (int ligne = 0; ligne < analyse.lex().lineCount(); ligne++) {
            for (Token jeton : analyse.lex().line(ligne).tokens()) {
                if (jeton.kind() == sorte) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String corpus(String nom) {
        String chemin = "corpus/" + nom;
        try (InputStream flux = LangagesGrammairesTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("corpus absent : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("corpus illisible : " + chemin, e);
        }
    }
}
