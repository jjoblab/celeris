package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le socle JSON du client LSP (ADR-005) : valeur immuable, analyseur
 * strict (RFC 8259) et sérialiseur compact. Le client ne dépend d'aucune
 * bibliothèque tierce — JSON-RPC exige le nôtre, borné à la surface du
 * protocole.
 */
class JsonTest {

    /** Les littéraux : vrai, faux, nul. */
    @Test
    void lesLitteraux() {
        assertInstanceOf(JsonBooleen.class, JsonLecteur.lire("true"));
        assertTrue(((JsonBooleen) JsonLecteur.lire("true")).valeur());
        assertTrue(!((JsonBooleen) JsonLecteur.lire("false")).valeur());
        assertInstanceOf(JsonNul.class, JsonLecteur.lire("null"));
    }

    /** Les chaînes : échappements usuels, Unicode direct et échappé. */
    @Test
    void lesChainesEtEchappements() {
        assertEquals("a\"b\\c/d\be\ff\ng\rh\ti",
                ((JsonChaine) JsonLecteur.lire("\"a\\\"b\\\\c\\/d\\be\\ff\\ng\\rh\\ti\"")).texte());
        assertEquals("é ☃", ((JsonChaine) JsonLecteur.lire("\"é ☃\"")).texte());
        assertEquals("é", ((JsonChaine) JsonLecteur.lire("\"\\u00e9\"")).texte());
        assertEquals("снег", ((JsonChaine) JsonLecteur.lire(
                "\"\\u0441\\u043d\\u0435\\u0433\"")).texte());
    }

    /** Les nombres : entiers exacts, décimaux, exposants — et l'entier reste entier. */
    @Test
    void lesNombres() {
        assertEquals(42, ((JsonNombre) JsonLecteur.lire("42")).entier());
        assertEquals(-7, ((JsonNombre) JsonLecteur.lire("-7")).entier());
        assertEquals(0, ((JsonNombre) JsonLecteur.lire("0")).entier());
        assertEquals(3.14, ((JsonNombre) JsonLecteur.lire("3.14")).decimal(), 1e-12);
        assertEquals(-0.5, ((JsonNombre) JsonLecteur.lire("-0.5")).decimal(), 1e-12);
        assertEquals(1.5e10, ((JsonNombre) JsonLecteur.lire("1.5e10")).decimal(), 1e-3);
        assertEquals(2.0e-3, ((JsonNombre) JsonLecteur.lire("2E-3")).decimal(), 1e-12);
        assertTrue(((JsonNombre) JsonLecteur.lire("2.0")).estEntier());
        assertTrue(!((JsonNombre) JsonLecteur.lire("2.5")).estEntier());
        assertEquals(2, ((JsonNombre) JsonLecteur.lire("2")).entier());
    }

    /** Les nombres interdits : zéro en tête, plus unaire, NaN, décimales doubles. */
    @Test
    void lesNombresRejetes() {
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("01"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("+1"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("NaN"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("1."));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire(".5"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("-"));
    }

    /** Les objets : ordre d'insertion conservé, accès par clé, doublon le dernier gagne. */
    @Test
    void lesObjets() {
        JsonObjet objet = (JsonObjet) JsonLecteur.lire(
                "{\"b\": 1, \"a\": [true, null], \"c\": {\"d\": \"e\"}}");
        assertEquals(3, objet.taille());
        assertEquals(List.of("b", "a", "c"), List.copyOf(objet.cles()));
        assertEquals(1, ((JsonNombre) objet.membre("b")).entier());
        assertNull(objet.membre("absente"));
        JsonTableau tableau = (JsonTableau) objet.membre("a");
        assertEquals(2, tableau.taille());
        assertInstanceOf(JsonBooleen.class, tableau.element(0));
        assertInstanceOf(JsonNul.class, tableau.element(1));
        JsonObjet imbrique = (JsonObjet) objet.membre("c");
        assertEquals("e", ((JsonChaine) imbrique.membre("d")).texte());
        JsonObjet doublon = (JsonObjet) JsonLecteur.lire("{\"k\": 1, \"k\": 2}");
        assertEquals(1, doublon.taille());
        assertEquals(2, ((JsonNombre) doublon.membre("k")).entier());
    }

    /** Les tableaux vides et imbriqués, les chaînes vides, l'objet vide. */
    @Test
    void lesStructuresVides() {
        assertEquals(0, ((JsonTableau) JsonLecteur.lire("[]")).taille());
        assertEquals(0, ((JsonObjet) JsonLecteur.lire("{}")).taille());
        assertEquals("", ((JsonChaine) JsonLecteur.lire("\"\"")).texte());
        assertEquals(1, ((JsonTableau) JsonLecteur.lire("[[[]]]")).taille());
    }

    /** Les blancs tolérés autour et entre les valeurs. */
    @Test
    void lesBlancsToleres() {
        assertEquals(2, ((JsonTableau) JsonLecteur.lire("  \t\r\n[ 1 , 2 ] ")).taille());
    }

    /** Les entrées invalides : reste après la valeur, structures ouvertes, échappement inconnu. */
    @Test
    void lesEntreesInvalides() {
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire(""));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("   "));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("1 2"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("{\"a\":1,}"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("[1,]"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("[1 2]"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("\"\\x\""));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("\"abc"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("{\"a\" 1}"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("hello"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("'a'"));
        assertThrows(IllegalArgumentException.class, () -> JsonLecteur.lire("\"\\u00\""));
    }

    /** Le sérialiseur : forme compacte, échappement des contrôles, aller-retour. */
    @Test
    void leSerialiseurCompact() {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("jsonrpc", JsonChaine.of("2.0"))
                .ajouter("id", JsonNombre.of(3))
                .ajouter("nul", JsonNul.nul());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":3,\"nul\":null}",
                JsonEcrivain.ecrire(objet));
        assertEquals("[1,2.5,true,false,null,\"x\"]",
                JsonEcrivain.ecrire(JsonTableau.vide()
                        .ajouter(JsonNombre.of(1))
                        .ajouter(JsonNombre.of(2.5))
                        .ajouter(JsonBooleen.of(true))
                        .ajouter(JsonBooleen.of(false))
                        .ajouter(JsonNul.nul())
                        .ajouter(JsonChaine.of("x"))));
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\"",
                JsonEcrivain.ecrire(JsonChaine.of("a\"b\\c\n\u0001")));
        String source = "{\"a\":[1,2.5,\"s\",true,null],\"b\":{\"c\":-3}}";
        assertEquals(source, JsonEcrivain.ecrire(JsonLecteur.lire(source)));
    }

    /** L'immutabilité par copie : ajouter ne mute jamais la valeur d'origine. */
    @Test
    void limmutabiliteParCopie() {
        JsonObjet base = JsonObjet.vide().ajouter("a", JsonNombre.of(1));
        JsonObjet etendu = base.ajouter("b", JsonNombre.of(2));
        assertEquals(1, base.taille());
        assertEquals(2, etendu.taille());
        assertEquals(1, ((JsonNombre) etendu.membre("a")).entier());
        JsonTableau tableauBase = JsonTableau.vide().ajouter(JsonNombre.of(1));
        JsonTableau tableauEtendu = tableauBase.ajouter(JsonNul.nul());
        assertEquals(1, tableauBase.taille());
        assertEquals(2, tableauEtendu.taille());
    }

    /** L'accès commode : membreOu rend le défaut quand la clé manque. */
    @Test
    void laccesAvecDefaut() {
        JsonObjet objet = (JsonObjet) JsonLecteur.lire("{\"a\": 1}");
        assertEquals(1, ((JsonNombre) objet.membreOu("a", JsonNombre.of(9))).entier());
        assertEquals(9, ((JsonNombre) objet.membreOu("z", JsonNombre.of(9))).entier());
        JsonTableau tableau = (JsonTableau) JsonLecteur.lire("[\"seul\"]");
        assertEquals("seul", ((JsonChaine) tableau.element(0)).texte());
    }
}
