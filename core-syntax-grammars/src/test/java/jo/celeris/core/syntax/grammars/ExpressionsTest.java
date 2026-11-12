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
        assertTrue(GardeCatastrophique.catastrophique("(a+)+"));
        assertTrue(GardeCatastrophique.catastrophique("(a*)*"));
        assertTrue(GardeCatastrophique.catastrophique("(?:\\w+\\s*)*"));
        assertTrue(GardeCatastrophique.catastrophique("(a{2,})*"));
        assertTrue(GardeCatastrophique.catastrophique("((a)+)+"));
        assertTrue(GardeCatastrophique.catastrophique("(?:a[ab]+)+"),
                "le délimiteur ordinaire que la suite peut avaler ne protège pas");
        assertTrue(GardeCatastrophique.catastrophique("(?:\\.-+\\.)+"),
                "conservateur : le délimiteur revient aussi en fin d'itération");
    }

    /** L'imbriquée cache le mal au père : le quantificateur non borné
     * vit dans tous les groupes ouverts, la forme ((a+))+ ne passe
     * plus par le sommet seul. */
    @Test
    void formesImbriqueesRefusees() {
        assertTrue(GardeCatastrophique.catastrophique("((a+))+"),
                "l'imbriquée porte le mal au père, pas au sommet seul");
        assertTrue(GardeCatastrophique.catastrophique("((a+))*"));
        assertTrue(GardeCatastrophique.catastrophique("(((a+)))+"),
                "la profondeur supplémentaire ne dilue rien");
        assertTrue(GardeCatastrophique.catastrophique("((?:\\w+\\s*))+"),
                "l'imbriquée délimitée d'intérieur non borné se refuse aussi");
    }

    /** Les formes bornées ou délimitées passent. */
    @Test
    void formesBornesEtDelimiteesPasse() {
        assertFalse(GardeCatastrophique.catastrophique("(a+){2}"));
        assertFalse(GardeCatastrophique.catastrophique("(abc|def)*"));
        assertFalse(GardeCatastrophique.catastrophique("a+b+"));
        assertFalse(GardeCatastrophique.catastrophique("[+]+"));
        assertFalse(GardeCatastrophique.catastrophique("(?:\\.\\w+)+"));
        assertFalse(GardeCatastrophique.catastrophique(
                "\\b[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)+\\b"));
        assertFalse(GardeCatastrophique.catastrophique(
                "[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*(?=[ \\t]*=)"));
        assertFalse(GardeCatastrophique.catastrophique("(`{3,})([^`\\n]*)$"));
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
        assertFalse(GardeCatastrophique.catastrophique("(?:\\.\\d+)+"),
                "\\d ne couvre pas le point : délimité");
        assertTrue(GardeCatastrophique.catastrophique("(?:\\.\\W+)+"),
                "\\W couvre le point : catastrophique");
        assertFalse(GardeCatastrophique.catastrophique("(?:\\:\\s+)+"),
                "\\s ne couvre pas le deux-points");
        assertTrue(GardeCatastrophique.catastrophique("(?:\\:\\S+)+"),
                "\\S couvre le deux-points");
        assertFalse(GardeCatastrophique.catastrophique("(?:\\.[0-9]+)+"),
                "l'intervalle des chiffres ne couvre pas le point");
        assertTrue(GardeCatastrophique.catastrophique("(?:\\.[.-0]+)+"),
                "l'intervalle .-0 couvre le point");
        assertFalse(GardeCatastrophique.catastrophique("(?:\\.\\ba+)+"),
                "la largeur nulle ne mange rien — le reste ne couvre pas le point");
        assertTrue(GardeCatastrophique.catastrophique("(?:(?=a)\\.\\w+)+"),
                "conservateur : le look-around en tête interdit la délimitation");
    }

    /** Les groupes nommés et leurs closeurs se parcourent sans se confondre. */
    @Test
    void groupesNomesEtDelimitation() {
        assertFalse(GardeCatastrophique.catastrophique("(?<nom>\\.\\w+)+"),
                "le groupe nommé pointe le point, la suite ne l'avale pas");
        assertFalse(GardeCatastrophique.catastrophique("(?'nom'\\.\\w+)+"),
                "le closeur apostrophe aussi");
        assertTrue(GardeCatastrophique.catastrophique("((?<=a)\\w+)+"),
                "le look-behind en tête d'un groupe quantifié interdit la délimitation");
        assertTrue(GardeCatastrophique.catastrophique("(?<nom>a+)+"),
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
