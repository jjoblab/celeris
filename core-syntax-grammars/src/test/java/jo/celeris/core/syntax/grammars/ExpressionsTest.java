package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La fabrique d'expressions et son garde (ADR-014) : formes
 * catastrophiques visibles refusées — la forme exponentielle classique,
 * la forme délimitée épargnée — et l'échappement littéral des
 * substitutions de fermeture.
 */
class ExpressionsTest {

    /** Les formes exponentielles classiques se refusent. */
    @Test
    void formesExponentiellesRefusees() {
        assertTrue(CatastrophicGuard.catastrophique("(a+)+"));
        assertTrue(CatastrophicGuard.catastrophique("(a*)*"));
        assertTrue(CatastrophicGuard.catastrophique("(?:\\w+\\s*)*"));
        assertTrue(CatastrophicGuard.catastrophique("(a{2,})*"));
        assertTrue(CatastrophicGuard.catastrophique("((a)+)+"));
        assertTrue(CatastrophicGuard.catastrophique("(?:a[ab]+)+"),
                "le délimiteur ordinaire que la suite peut avaler ne protège pas");
        assertTrue(CatastrophicGuard.catastrophique("(?:\\.-+\\.)+"),
                "conservateur : le délimiteur revient aussi en fin d'itération");
    }

    /** L'imbriquée cache le mal au père : le quantificateur non borné
     * vit dans tous les groupes ouverts, la forme ((a+))+ ne passe
     * plus par le sommet seul. */
    @Test
    void formesImbriqueesRefusees() {
        assertTrue(CatastrophicGuard.catastrophique("((a+))+"),
                "l'imbriquée porte le mal au père, pas au sommet seul");
        assertTrue(CatastrophicGuard.catastrophique("((a+))*"));
        assertTrue(CatastrophicGuard.catastrophique("(((a+)))+"),
                "la profondeur supplémentaire ne dilue rien");
        assertTrue(CatastrophicGuard.catastrophique("((?:\\w+\\s*))+"),
                "l'imbriquée délimitée d'intérieur non borné se refuse aussi");
    }

    /** Les formes bornées ou délimitées passent. */
    @Test
    void formesBornesEtDelimiteesPasse() {
        assertFalse(CatastrophicGuard.catastrophique("(a+){2}"));
        assertFalse(CatastrophicGuard.catastrophique("(abc|def)*"));
        assertFalse(CatastrophicGuard.catastrophique("a+b+"));
        assertFalse(CatastrophicGuard.catastrophique("[+]+"));
        assertFalse(CatastrophicGuard.catastrophique("(?:\\.\\w+)+"));
        assertFalse(CatastrophicGuard.catastrophique(
                "\\b[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+\\b"));
        assertFalse(CatastrophicGuard.catastrophique(
                "[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*(?=[ \\t]*=)"));
        assertFalse(CatastrophicGuard.catastrophique("(`{3,})([^`\\n]*)$"));
    }

    /** L'échappement littéral protège les métacaractères du texte capturé. */
    @Test
    void echappementLitteral() {
        assertEquals("a\\.b", Expressions.echapperLitteral("a.b"));
        assertEquals("\\(\\)", Expressions.echapperLitteral("()"));
        assertEquals("\\\\\\$", Expressions.echapperLitteral("\\$"));
        assertEquals("plain", Expressions.echapperLitteral("plain"));
    }


    /** Les classes larges couvrent ou épargnent le délimiteur, selon leur lettre. */
    @Test
    void alphabetDesClassesLarges() {
        assertFalse(CatastrophicGuard.catastrophique("(?:\\.\\d+)+"),
                "\\d ne couvre pas le point : délimité");
        assertTrue(CatastrophicGuard.catastrophique("(?:\\.\\W+)+"),
                "\\W couvre le point : catastrophique");
        assertFalse(CatastrophicGuard.catastrophique("(?:\\:\\s+)+"),
                "\\s ne couvre pas le deux-points");
        assertTrue(CatastrophicGuard.catastrophique("(?:\\:\\S+)+"),
                "\\S couvre le deux-points");
        assertFalse(CatastrophicGuard.catastrophique("(?:\\.[0-9]+)+"),
                "l'intervalle des chiffres ne couvre pas le point");
        assertTrue(CatastrophicGuard.catastrophique("(?:\\.[.-0]+)+"),
                "l'intervalle .-0 couvre le point");
        assertFalse(CatastrophicGuard.catastrophique("(?:\\.\\ba+)+"),
                "la largeur nulle ne mange rien — le reste ne couvre pas le point");
        assertTrue(CatastrophicGuard.catastrophique("(?:(?=a)\\.\\w+)+"),
                "conservateur : le look-around en tête interdit la délimitation");
    }

    /** Les groupes nommés et leurs closeurs se parcourent sans se confondre. */
    @Test
    void groupesNomesEtDelimitation() {
        assertFalse(CatastrophicGuard.catastrophique("(?<nom>\\.\\w+)+"),
                "le groupe nommé pointe le point, la suite ne l'avale pas");
        assertFalse(CatastrophicGuard.catastrophique("(?'nom'\\.\\w+)+"),
                "le closeur apostrophe aussi");
        assertTrue(CatastrophicGuard.catastrophique("((?<=a)\\w+)+"),
                "le look-behind en tête d'un groupe quantifié interdit la délimitation");
        assertTrue(CatastrophicGuard.catastrophique("(?<nom>a+)+"),
                "sans délimiteur, l'imbriqué se refuse");
    }

    /** Le modèle de validation remplace les références, pas les échappements. */
    @Test
    void modeleDeValidation() {
        assertEquals("^[ \\t]*\\w+[ \\t]*$", Expressions.modele("^[ \\t]*\\1[ \\t]*$"),
                "les références deviennent \\w+");
        assertEquals("\\\\1", Expressions.modele("\\\\1"),
                "le double échappement reste littéral");
    }
}
