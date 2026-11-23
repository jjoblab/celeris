package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Le chargeur au format TextMate réel (ADR-004 révisé, ADR-014) :
 * formes que les extensions de VS Code publient et formes dégradées —
 * tout se compte au rapport, jamais en silence.
 */
class GrammairesTextMateTest {

    /** Les captures en objets portent leur nom de portée. */
    @Test
    void capturesEnObjets() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "match": "(\\\\w+)=(\\\\d+)", "name": "meta.affectation.essai",
                      "captures": {
                        "1": { "name": "variable.other.essai" },
                        "2": { "name": "constant.numeric.essai" }
                      } }
                  ]
                }
                """);
        assertEquals(1.0, grammaire.report().acceptation());
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        var line = lexeur.lex("x=42", GrammarState.EMPTY);
        assertEquals(3, line.tokens().length, "la variable, le signe, le nombre");
        assertEquals(0, line.tokens()[0].start(), "la première capture commence à zéro");
        assertEquals(jo.celeris.core.syntax.TokenKind.NUMBER, line.tokens()[2].kind(),
                "la seconde capture est un nombre");
        assertEquals(4, line.tokens()[2].end(), "et couvre 42");
    }

    /** La règle sans nom laisse son texte à la portée du contexte. */
    @Test
    void regleSansNomPorteLeContexte() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "\\\"", "end": "\\\"",
                      "name": "string.essai", "contentName": "string.essai",
                      "patterns": [ { "match": "\\\\\\\\w+" } ] }
                  ]
                }
                """);
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        var line = lexeur.lex("\"abc\"", GrammarState.EMPTY);
        assertTrue(line.tokens().length >= 2, "l'ouverture, le contenu, la fermeture");
        boolean contenuEnChaine = false;
        for (var token : line.tokens()) {
            if (token.kind() == jo.celeris.core.syntax.TokenKind.STRING) {
                contenuEnChaine = true;
            }
        }
        assertTrue(contenuEnChaine, "le texte sans nom garde la portée de la chaîne");
    }

    /** Le contenu d'un bloc sans contentName porte la portée de sa construction. */
    @Test
    void contenuSansContentNamePorteLeNom() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "<", "end": ">", "name": "string.interieur.essai" }
                  ]
                }
                """);
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        var line = lexeur.lex("<abc>", GrammarState.EMPTY);
        assertEquals(jo.celeris.core.syntax.TokenKind.STRING, line.tokens()[1].kind(),
                "l'écart porte le nom de la construction quand le contenu n'en a pas");
    }

    /**
     * La fermeture à références se substitue à l'ouverture : le
     * ici-document shell se referme sur SON mot, pas sur un autre.
     */
    @Test
    void heredocFermeSurSonMot() {
        Grammar grammaire = grammaireDePaquet("shell");
        assertEquals(1.0, grammaire.report().acceptation());
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        GrammarState etat = GrammarState.EMPTY;
        var ouverture = lexeur.lex("cat <<EOF", etat);
        etat = (GrammarState) java.util.Objects.requireNonNull(ouverture.exitState());
        assertTrue(etat.dansBloc(), "l'ici-document est ouvert");
        var milieu = lexeur.lex("du texte", etat);
        assertEquals(etat, milieu.exitState(),
                "la ligne de texte garde le bloc ouvert — FIN ne referme pas");
        var fermeture = lexeur.lex("EOF", etat);
        assertEquals(GrammarState.EMPTY, fermeture.exitState(),
                "la fermeture substituée referme sur le mot capturé");
    }

    /**
     * Le bloc de code Markdown se referme sur SA clôture : trois
     * backticks ouvrent, trois referment, l'information de langage se
     * colore à part.
     */
    @Test
    void clotureMarkdownRefermeeSurSaCloture() {
        Grammar grammaire = grammaireDePaquet("markdown");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        GrammarState etat = GrammarState.EMPTY;
        var ouverture = lexeur.lex("```kotlin", etat);
        etat = (GrammarState) java.util.Objects.requireNonNull(ouverture.exitState());
        assertTrue(etat.dansBloc(), "la clôture ouvre un bloc");
        var code = lexeur.lex("val x = 1", etat);
        assertEquals(etat, code.exitState(), "le code garde le bloc ouvert");
        var fermeture = lexeur.lex("```", etat);
        assertEquals(GrammarState.EMPTY, fermeture.exitState(), "les trois backticks referment");
    }

    /** $self en action : la substitution de commande reprend la grammaire shell. */
    @Test
    void selfDansLaSubstitutionDeCommande() {
        Grammar grammaire = grammaireDePaquet("shell");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        var line = lexeur.lex("echo $(if true)", GrammarState.EMPTY);
        boolean motCle = false;
        for (var token : line.tokens()) {
            if (token.kind() == jo.celeris.core.syntax.TokenKind.KEYWORD) {
                motCle = true;
            }
        }
        assertTrue(motCle, "$self fait revivre les mots réservés dans la substitution");
    }

    /** L'inclusion externe absente se saute et se compte ; résolue, elle s'inclut. */
    @Test
    void inclusionExterneCompteeOuResolue() {
        String host = """
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "include": "source.invite" },
                    { "match": "x", "name": "keyword.hote" }
                  ]
                }
                """;
        Grammar sansRegistre = Grammars.read(host);
        assertEquals(1, sansRegistre.report().inclusionsExterieures(),
                "l'inclusion absente se compte");
        assertEquals(1, sansRegistre.patterns().size(), "et se saute");

        Grammar invite = Grammars.read("""
                {
                  "name": "Invite", "scopeName": "source.invite",
                  "patterns": [ { "match": "\\\\\\\\d+", "name": "constant.numeric.invite" } ]
                }
                """);
        Grammar avecRegistre = Grammars.read(host, java.util.Map.of("source.invite", invite));
        assertEquals(0, avecRegistre.report().inclusionsExterieures());
        assertEquals(2, avecRegistre.patterns().size(), "l'inclusion résolue devient une référence");
    }

    /** La règle begin/while sans fermeture se dégrade en motif simple, comptée. */
    @Test
    void regleWhileDegradee() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "@\\\\w+", "while": "^", "name": "meta.annotation.essai" }
                  ]
                }
                """);
        assertEquals(1, grammaire.report().reglesWhile(), "la dégradation se compte");
        assertEquals(1, grammaire.report().acceptes(), "l'ouverture seule se compile");
        assertFalse(grammaire.patterns().isEmpty());
    }

    /** Les sous-motifs de capture se dégradent au nom, comptés. */
    @Test
    void capturesSousMotifsDegradees() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "match": "(\\\\w+)", "name": "meta.essai",
                      "captures": { "1": { "name": "variable.essai",
                                   "patterns": [ { "match": "a" } ] } } }
                  ]
                }
                """);
        assertEquals(1, grammaire.report().subPatternCaptures(), "la dégradation se compte");
    }

    /** Les injections se lisent (H-05) : sélecteur, priorité, règles. */
    @Test
    void injectionsLuesAvecLeurPriorite() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [ { "match": "a", "name": "keyword.essai" } ],
                  "injections": {
                    "L:source.essai": { "patterns": [] },
                    "R:source.essai": { "patterns": [] },
                    "source.essai meta.b": { "patterns": [] }
                  }
                }
                """);
        assertEquals(3, grammaire.injections().size(),
                "chaque clé d'injection se lit, préfixe ou pas");
        assertTrue(grammaire.injections().get(0).prioriteGauche(),
                "L: porte la priorité gauche");
        assertTrue(!grammaire.injections().get(1).prioriteGauche(),
                "R: porte la priorité droite");
        assertTrue(!grammaire.injections().get(2).prioriteGauche(),
                "sans préfixe, l'hôte reste maître chez lui");
    }

    /** Un sélecteur d'injection mal formé refuse la grammaire. */
    @Test
    void selecteurDInjectionMalFormeRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Grammars.read("""
                        {
                          "name": "Essai", "scopeName": "source.essai",
                          "patterns": [ { "match": "a" } ],
                          "injections": { "L:,": { "patterns": [] } }
                        }
                        """));
        assertTrue(String.valueOf(e.getMessage()).contains("scope selector"),
                "le refus nomme le sélecteur : " + e.getMessage());
    }

    /**
     * La forme catastrophique écarte SA règle — comptée au rapport,
     * jamais le paquet entier (H-05 : les grammaires du commerce
     * chargent telles que publiées, la règle absente ne colore rien).
     */
    @Test
    void formeCatastrophiqueEcarteSaRegle() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "X", "scopeName": "source.x",
                  "patterns": [
                    { "match": "(a+)+", "name": "meta.a" },
                    { "match": "sain", "name": "keyword.x" }
                  ]
                }
                """);
        assertEquals(1, grammaire.report().refuses(),
                "la forme catastrophique se compte, jamais en silence");
        assertEquals(1, grammaire.patterns().size(),
                "la règle saine charge, le paquet reste entier");
    }

    /**
     * La fermeture à références dont le MODÈLE est invalide écarte sa
     * règle au chargement — comptée, jamais en silence.
     */
    @Test
    void fermetureModeleInvalideEcartee() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "(\\\\()", "end": "\\\\1(", "name": "meta.paren.essai" }
                  ]
                }
                """);
        assertTrue(grammaire.patterns().isEmpty(), "la règle à fermeture invalide est écartée");
        assertEquals(1, grammaire.report().refuses(), "le refus se compte");
        assertEquals(2, grammaire.report().patterns(), "ouverture et fermeture rencontrées");
    }

    /**
     * La substitution qui produit un motif invalide ne casse jamais la
     * lexicographie : le repli ne matche jamais, le bloc reste ouvert
     * jusqu'à la fin du document — dégradé, jamais gelé.
     */
    @Test
    void fermetureSubstitueeInvalideTientLeCoup() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "x([a-z]*)", "end": "\\\\1+", "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1.0, grammaire.report().acceptation(),
                "le modèle \\w++ compile — la substitution vide, elle, ne le peut pas");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        GrammarState etat = (GrammarState) java.util.Objects.requireNonNull(
                lexeur.lex("x z", GrammarState.EMPTY).exitState());
        assertTrue(etat.dansBloc(),
                "l'ouverture matche (groupe vide), la fermeture substituée est invalide");
        var suite = lexeur.lex("zz", etat);
        assertEquals(etat, suite.exitState(),
                "le repli ne matche jamais, sans exception : le bloc reste ouvert");
    }


    /** applyEndPatternLast se tolère sans effet — compté. */
    @Test
    void drapeauApplyEndPatternLastTolere() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "a", "end": "b", "applyEndPatternLast": true,
                      "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1, grammaire.report().reglesDrapeaux(), "le drapeau se compte");
    }

    /** La règle vide se tolère comme la référence le fait. */
    @Test
    void regleVideToleree() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [ { }, { "match": "a", "name": "keyword.essai" } ]
                }
                """);
        assertEquals(1, grammaire.patterns().size(), "la règle vide passe son chemin");
    }

    /** Le référentiel hors objet se refuse avec le champ en cause. */
    @Test
    void referentielHorsObjetRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Grammars.read("""
                        {
                          "name": "Essai", "scopeName": "source.essai",
                          "patterns": [], "repository": 3
                        }
                        """));
        assertTrue(String.valueOf(e.getMessage()).contains("repository"));
    }

    /** Deux inclusions de la même portée externe partagent leur référence liée. */
    @Test
    void inclusionExterneLieeDeuxFois() {
        Grammar invite = Grammars.read("""
                {
                  "name": "Invite", "scopeName": "source.invite",
                  "patterns": [ { "match": "\\\\\\\\d+", "name": "constant.invite" } ]
                }
                """);
        Grammar host = Grammars.read("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "include": "source.invite" },
                    { "begin": "\\\\\\\\{", "end": "\\\\\\\\}",
                      "patterns": [ { "include": "source.invite" } ] }
                  ]
                }
                """, java.util.Map.of("source.invite", invite));
        assertEquals(2, host.patterns().size());
        assertEquals(0, host.report().inclusionsExterieures(),
                "les deux inclusions résolvent la même liaison");
    }

    /** La fermeture qui référence au-delà des groupes de l'ouverture reste littérale. */
    @Test
    void referenceAuDelaDesGroupesResteLitterale() {
        Grammar grammaire = Grammars.read("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "(a)", "end": "\\\\\\\\2x", "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1.0, grammaire.report().acceptation(),
                "le modèle compile — la référence hors groupes, substituée littérale, est refusée au vol");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        GrammarState etat = (GrammarState) java.util.Objects.requireNonNull(
                lexeur.lex("a", GrammarState.EMPTY).exitState());
        assertTrue(etat.dansBloc(), "le bloc s'ouvre");
        var suite = lexeur.lex("zz", etat);
        assertEquals(etat, suite.exitState(), "la fermeture substituée invalide ne matche jamais");
    }

    /**
     * Le nom racine manquant se tolère : le format TextMate le rend
     * facultatif et la grammaire PHP du commerce (microsoft/vscode)
     * s'en passe — la portée racine sert de repli affichable (H-05,
     * la longue traîne du chantier C5).
     */
    @Test
    void nomAbsentTombeSurLaPorteeRacine() {
        Grammar grammaire = Grammars.read("""
                { "scopeName": "source.essai", "patterns": [] }
                """);
        assertEquals("source.essai", grammaire.nom(),
                "la portée racine sert de nom quand le nom manque");
    }

    /** La grammaire dont la portée racine manque se refuse. */
    @Test
    void porteeRacineAbsenteRefusee() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Grammars.read("""
                        { "name": "Essai", "patterns": [] }
                        """));
        assertTrue(String.valueOf(e.getMessage()).contains("scopeName"));
    }

    /** La grammaire sans motifs se refuse : un tableau est attendu. */
    @Test
    void motifsHorsTableauRefuses() {
        assertThrows(IllegalArgumentException.class, () -> Grammars.read("""
                { "name": "Essai", "scopeName": "source.essai", "patterns": 3 }
                """));
    }

    /** L'emoji astral (quatre octets UTF-8) revient en positions UTF-16 exactes. */
    @Test
    void emojiAstralEnPositionsExactes() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        var line = lexeur.lex("x = \"🚀\"", GrammarState.EMPTY);
        boolean fuseeDansChaine = false;
        for (var token : line.tokens()) {
            if (token.kind() == jo.celeris.core.syntax.TokenKind.PUNCTUATION
                    && token.start() == 4 && token.end() == 5) {
                fuseeDansChaine = true;
            }
        }
        assertTrue(fuseeDansChaine, "l'ouverture de chaîne suit le blanc, positions exactes");
    }

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammar grammaireDePaquet(String id) {
        var flux = GrammairesTextMateTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
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
}
