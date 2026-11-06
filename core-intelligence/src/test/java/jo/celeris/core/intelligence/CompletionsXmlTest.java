package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la complétion XML guidée par le schéma (C-02) : les
 * attributs suggérés depuis le schéma embarqué selon la balise, les
 * unités (dp, sp, les tailles) selon l'attribut, les références
 * {@code @string/} selon le type — le contexte décide, jamais le
 * fichier entier.
 */
final class CompletionsXmlTest {

    /** Les attributs de la balise connue répondent au préfixe tapé. */
    @Test
    void attributsDeLaBaliseConnueRepondentAuPrefixe() {
        String texte = "<Button\n    and";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertFalse(candidats.isEmpty(), "au moins un attribut android: suggéré");
        for (String candidat : candidats) {
            assertTrue(candidat.startsWith("and"), "chaque candidat porte le préfixe : "
                    + candidat);
        }
        assertTrue(candidats.contains("android:layout_width"),
                "les attributs de mise en page sont suggérés : " + candidats);
        assertTrue(candidats.contains("android:text"),
                "les attributs du Button sont suggérés : " + candidats);
    }

    /** Le nom de balise se complète après le chevron ouvrant. */
    @Test
    void nomDeBaliseSeCompleteApresLeChevron() {
        String texte = "<Text";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.contains("TextView"), "TextView suggéré : " + candidats);
        for (String candidat : candidats) {
            assertTrue(candidat.startsWith("Text"), "chaque candidat porte le préfixe : "
                    + candidat);
        }
    }

    /** Les valeurs de taille suggèrent les constantes et les unités. */
    @Test
    void valeursDeTailleSuggerentConstantesEtUnites() {
        String texte = "<Button\n    android:layout_width=\"ma";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.contains("match_parent"), "match_parent suggéré : " + candidats);
        assertTrue(candidats.contains("wrap_content"), "wrap_content suggéré : " + candidats);
    }

    /** Les unités dp et sp se suggèrent dans une taille à nombre. */
    @Test
    void unitesDpSpSeSuggerentDansUneTaille() {
        String texte = "<TextView\n    android:textSize=\"16";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.contains("16sp"), "le suffixe sp suggéré : " + candidats);
    }

    /** Les références @type/ se suggèrent dans une valeur d'attribut. */
    @Test
    void referencesSeSuggerentDansUneValeur() {
        String texte = "<TextView\n    android:text=\"@";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.contains("@string/"), "@string/ suggéré : " + candidats);
        assertTrue(candidats.contains("@drawable/"), "@drawable/ suggéré : " + candidats);
        for (String candidat : candidats) {
            assertTrue(candidat.startsWith("@"), "chaque référence porte l'arobase : "
                    + candidat);
        }
    }

    /** La balise inconnue retombe sur les attributs communs. */
    @Test
    void baliseInconnueRetombeSurLesAttributsCommuns() {
        String texte = "<Inconnue\n    android:la";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.contains("android:layout_width"),
                "les attributs communs répondent : " + candidats);
    }

    /** Hors d'une balise — dans le texte — rien ne se suggère. */
    @Test
    void horsBaliseRienNeSeSuggerere() {
        String texte = "<Button>a";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertTrue(candidats.isEmpty(), "le texte ne porte pas de complétion : " + candidats);
    }

    /** Les candidats restent bornés et distincts. */
    @Test
    void candidatsDistinctsEtBornes() {
        String texte = "<Button\n    ";
        List<String> candidats = CompletionsXml.completer(texte, texte.length());
        assertEquals(candidats.size(), candidats.stream().distinct().count(),
                "aucun doublon");
        assertTrue(candidats.size() <= CompletionsXml.MAX_CANDIDATS,
                "la liste reste courte");
    }
}
