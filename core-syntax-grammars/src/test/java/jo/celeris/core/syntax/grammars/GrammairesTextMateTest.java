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
        Grammaire grammaire = Grammaires.lire("""
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
        assertEquals(1.0, grammaire.rapport().acceptation());
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        var ligne = lexeur.lex("x=42", EtatGrammaire.VIDE);
        assertEquals(3, ligne.tokens().length, "la variable, le signe, le nombre");
        assertEquals(0, ligne.tokens()[0].start(), "la première capture commence à zéro");
        assertEquals(jo.celeris.core.syntax.TokenKind.NUMBER, ligne.tokens()[2].kind(),
                "la seconde capture est un nombre");
        assertEquals(4, ligne.tokens()[2].end(), "et couvre 42");
    }

    /** La règle sans nom laisse son texte à la portée du contexte. */
    @Test
    void regleSansNomPorteLeContexte() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "\\\"", "end": "\\\"",
                      "name": "string.essai", "contentName": "string.essai",
                      "patterns": [ { "match": "\\\\\\\\w+" } ] }
                  ]
                }
                """);
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        var ligne = lexeur.lex("\"abc\"", EtatGrammaire.VIDE);
        assertTrue(ligne.tokens().length >= 2, "l'ouverture, le contenu, la fermeture");
        boolean contenuEnChaine = false;
        for (var jeton : ligne.tokens()) {
            if (jeton.kind() == jo.celeris.core.syntax.TokenKind.STRING) {
                contenuEnChaine = true;
            }
        }
        assertTrue(contenuEnChaine, "le texte sans nom garde la portée de la chaîne");
    }

    /** Le contenu d'un bloc sans contentName porte la portée de sa construction. */
    @Test
    void contenuSansContentNamePorteLeNom() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "<", "end": ">", "name": "string.interieur.essai" }
                  ]
                }
                """);
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        var ligne = lexeur.lex("<abc>", EtatGrammaire.VIDE);
        assertEquals(jo.celeris.core.syntax.TokenKind.STRING, ligne.tokens()[1].kind(),
                "l'écart porte le nom de la construction quand le contenu n'en a pas");
    }

    /**
     * La fermeture à références se substitue à l'ouverture : le
     * ici-document shell se referme sur SON mot, pas sur un autre.
     */
    @Test
    void heredocFermeSurSonMot() {
        Grammaire grammaire = grammaireDePaquet("shell");
        assertEquals(1.0, grammaire.rapport().acceptation());
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        EtatGrammaire etat = EtatGrammaire.VIDE;
        var ouverture = lexeur.lex("cat <<EOF", etat);
        etat = (EtatGrammaire) ouverture.exitState();
        assertTrue(etat.dansBloc(), "l'ici-document est ouvert");
        var milieu = lexeur.lex("du texte", etat);
        assertEquals(etat, milieu.exitState(),
                "la ligne de texte garde le bloc ouvert — FIN ne referme pas");
        var fermeture = lexeur.lex("EOF", etat);
        assertEquals(EtatGrammaire.VIDE, fermeture.exitState(),
                "la fermeture substituée referme sur le mot capturé");
    }

    /**
     * Le bloc de code Markdown se referme sur SA clôture : trois
     * backticks ouvrent, trois referment, l'information de langage se
     * colore à part.
     */
    @Test
    void clotureMarkdownRefermeeSurSaCloture() {
        Grammaire grammaire = grammaireDePaquet("markdown");
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        EtatGrammaire etat = EtatGrammaire.VIDE;
        var ouverture = lexeur.lex("```kotlin", etat);
        etat = (EtatGrammaire) ouverture.exitState();
        assertTrue(etat.dansBloc(), "la clôture ouvre un bloc");
        var code = lexeur.lex("val x = 1", etat);
        assertEquals(etat, code.exitState(), "le code garde le bloc ouvert");
        var fermeture = lexeur.lex("```", etat);
        assertEquals(EtatGrammaire.VIDE, fermeture.exitState(), "les trois backticks referment");
    }

    /** $self en action : la substitution de commande reprend la grammaire shell. */
    @Test
    void selfDansLaSubstitutionDeCommande() {
        Grammaire grammaire = grammaireDePaquet("shell");
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        var ligne = lexeur.lex("echo $(if true)", EtatGrammaire.VIDE);
        boolean motCle = false;
        for (var jeton : ligne.tokens()) {
            if (jeton.kind() == jo.celeris.core.syntax.TokenKind.KEYWORD) {
                motCle = true;
            }
        }
        assertTrue(motCle, "$self fait revivre les mots réservés dans la substitution");
    }

    /** L'inclusion externe absente se saute et se compte ; résolue, elle s'inclut. */
    @Test
    void inclusionExterneCompteeOuResolue() {
        String hote = """
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "include": "source.invite" },
                    { "match": "x", "name": "keyword.hote" }
                  ]
                }
                """;
        Grammaire sansRegistre = Grammaires.lire(hote);
        assertEquals(1, sansRegistre.rapport().inclusionsExterieures(),
                "l'inclusion absente se compte");
        assertEquals(1, sansRegistre.motifs().size(), "et se saute");

        Grammaire invite = Grammaires.lire("""
                {
                  "name": "Invite", "scopeName": "source.invite",
                  "patterns": [ { "match": "\\\\\\\\d+", "name": "constant.numeric.invite" } ]
                }
                """);
        Grammaire avecRegistre = Grammaires.lire(hote, java.util.Map.of("source.invite", invite));
        assertEquals(0, avecRegistre.rapport().inclusionsExterieures());
        assertEquals(2, avecRegistre.motifs().size(), "l'inclusion résolue devient une référence");
    }

    /** La règle begin/while sans fermeture se dégrade en motif simple, comptée. */
    @Test
    void regleWhileDegradee() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "@\\\\w+", "while": "^", "name": "meta.annotation.essai" }
                  ]
                }
                """);
        assertEquals(1, grammaire.rapport().reglesWhile(), "la dégradation se compte");
        assertEquals(1, grammaire.rapport().acceptes(), "l'ouverture seule se compile");
        assertFalse(grammaire.motifs().isEmpty());
    }

    /** Les sous-motifs de capture se dégradent au nom, comptés. */
    @Test
    void capturesSousMotifsDegradees() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "match": "(\\\\w+)", "name": "meta.essai",
                      "captures": { "1": { "name": "variable.essai",
                                   "patterns": [ { "match": "a" } ] } } }
                  ]
                }
                """);
        assertEquals(1, grammaire.rapport().capturesSousMotifs(), "la dégradation se compte");
    }

    /** Les injections se lisent (H-05) : sélecteur, priorité, règles. */
    @Test
    void injectionsLuesAvecLeurPriorite() {
        Grammaire grammaire = Grammaires.lire("""
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
                () -> Grammaires.lire("""
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
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "X", "scopeName": "source.x",
                  "patterns": [
                    { "match": "(a+)+", "name": "meta.a" },
                    { "match": "sain", "name": "keyword.x" }
                  ]
                }
                """);
        assertEquals(1, grammaire.rapport().refuses(),
                "la forme catastrophique se compte, jamais en silence");
        assertEquals(1, grammaire.motifs().size(),
                "la règle saine charge, le paquet reste entier");
    }

    /**
     * La fermeture à références dont le MODÈLE est invalide écarte sa
     * règle au chargement — comptée, jamais en silence.
     */
    @Test
    void fermetureModeleInvalideEcartee() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "(\\\\()", "end": "\\\\1(", "name": "meta.paren.essai" }
                  ]
                }
                """);
        assertTrue(grammaire.motifs().isEmpty(), "la règle à fermeture invalide est écartée");
        assertEquals(1, grammaire.rapport().refuses(), "le refus se compte");
        assertEquals(2, grammaire.rapport().motifs(), "ouverture et fermeture rencontrées");
    }

    /**
     * La substitution qui produit un motif invalide ne casse jamais la
     * lexicographie : le repli ne matche jamais, le bloc reste ouvert
     * jusqu'à la fin du document — dégradé, jamais gelé.
     */
    @Test
    void fermetureSubstitueeInvalideTientLeCoup() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "x([a-z]*)", "end": "\\\\1+", "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1.0, grammaire.rapport().acceptation(),
                "le modèle \\w++ compile — la substitution vide, elle, ne le peut pas");
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        EtatGrammaire etat =
                (EtatGrammaire) lexeur.lex("x z", EtatGrammaire.VIDE).exitState();
        assertTrue(etat.dansBloc(),
                "l'ouverture matche (groupe vide), la fermeture substituée est invalide");
        var suite = lexeur.lex("zz", etat);
        assertEquals(etat, suite.exitState(),
                "le repli ne matche jamais, sans exception : le bloc reste ouvert");
    }


    /** applyEndPatternLast se tolère sans effet — compté. */
    @Test
    void drapeauApplyEndPatternLastTolere() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "a", "end": "b", "applyEndPatternLast": true,
                      "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1, grammaire.rapport().reglesDrapeaux(), "le drapeau se compte");
    }

    /** La règle vide se tolère comme la référence le fait. */
    @Test
    void regleVideToleree() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [ { }, { "match": "a", "name": "keyword.essai" } ]
                }
                """);
        assertEquals(1, grammaire.motifs().size(), "la règle vide passe son chemin");
    }

    /** Le référentiel hors objet se refuse avec le champ en cause. */
    @Test
    void referentielHorsObjetRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Grammaires.lire("""
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
        Grammaire invite = Grammaires.lire("""
                {
                  "name": "Invite", "scopeName": "source.invite",
                  "patterns": [ { "match": "\\\\\\\\d+", "name": "constant.invite" } ]
                }
                """);
        Grammaire hote = Grammaires.lire("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "include": "source.invite" },
                    { "begin": "\\\\\\\\{", "end": "\\\\\\\\}",
                      "patterns": [ { "include": "source.invite" } ] }
                  ]
                }
                """, java.util.Map.of("source.invite", invite));
        assertEquals(2, hote.motifs().size());
        assertEquals(0, hote.rapport().inclusionsExterieures(),
                "les deux inclusions résolvent la même liaison");
    }

    /** La fermeture qui référence au-delà des groupes de l'ouverture reste littérale. */
    @Test
    void referenceAuDelaDesGroupesResteLitterale() {
        Grammaire grammaire = Grammaires.lire("""
                {
                  "name": "Essai", "scopeName": "source.essai",
                  "patterns": [
                    { "begin": "(a)", "end": "\\\\\\\\2x", "name": "meta.essai" }
                  ]
                }
                """);
        assertEquals(1.0, grammaire.rapport().acceptation(),
                "le modèle compile — la référence hors groupes, substituée littérale, est refusée au vol");
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        EtatGrammaire etat = (EtatGrammaire) lexeur.lex("a", EtatGrammaire.VIDE).exitState();
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
        Grammaire grammaire = Grammaires.lire("""
                { "scopeName": "source.essai", "patterns": [] }
                """);
        assertEquals("source.essai", grammaire.nom(),
                "la portée racine sert de nom quand le nom manque");
    }

    /** La grammaire dont la portée racine manque se refuse. */
    @Test
    void porteeRacineAbsenteRefusee() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Grammaires.lire("""
                        { "name": "Essai", "patterns": [] }
                        """));
        assertTrue(String.valueOf(e.getMessage()).contains("scopeName"));
    }

    /** La grammaire sans motifs se refuse : un tableau est attendu. */
    @Test
    void motifsHorsTableauRefuses() {
        assertThrows(IllegalArgumentException.class, () -> Grammaires.lire("""
                { "name": "Essai", "scopeName": "source.essai", "patterns": 3 }
                """));
    }

    /** L'emoji astral (quatre octets UTF-8) revient en positions UTF-16 exactes. */
    @Test
    void emojiAstralEnPositionsExactes() {
        Grammaire grammaire = grammaireDePaquet("python");
        GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
        var ligne = lexeur.lex("x = \"🚀\"", EtatGrammaire.VIDE);
        boolean fuseeDansChaine = false;
        for (var jeton : ligne.tokens()) {
            if (jeton.kind() == jo.celeris.core.syntax.TokenKind.PUNCTUATION
                    && jeton.start() == 4 && jeton.end() == 5) {
                fuseeDansChaine = true;
            }
        }
        assertTrue(fuseeDansChaine, "l'ouverture de chaîne suit le blanc, positions exactes");
    }

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammaire grammaireDePaquet(String id) {
        var flux = GrammairesTextMateTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return Grammaires.lire(new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
