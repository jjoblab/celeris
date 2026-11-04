package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'index de lignes de l'instantané {@link Document} (portage du
 * dépôt de départ, registre de portage) : débuts de ligne, requêtes par
 * offset et par ligne, splice incrémental à chaque édition PAR la voie de
 * mutation unique.
 */
class DocumentIndexTest {

    // ── Construction ──────────────────────────────────────────────

    @Test
    void of_documentVide_uneSeuleLigne() {
        Document doc = Document.of("");
        assertEquals(0, doc.length());
        assertEquals(1, doc.lineCount());
        assertEquals(0, doc.lineStart(0));
        assertEquals(0, doc.lineEnd(0));
        assertEquals("", doc.lineText(0));
        assertEquals("", doc.text());
    }

    @Test
    void of_ligneUnique() {
        Document doc = Document.of("hello");
        assertEquals(5, doc.length());
        assertEquals(1, doc.lineCount());
        assertEquals("hello", doc.text());
    }

    @Test
    void of_plusieursLignes() {
        Document doc = Document.of("line1\nline2\nline3");
        assertEquals(3, doc.lineCount());
        assertEquals("line1\nline2\nline3", doc.text());
    }

    @Test
    void of_sautFinal_ouvreUneLigneVide() {
        Document doc = Document.of("hello\n");
        assertEquals(2, doc.lineCount());
        assertEquals(0, doc.lineStart(0));
        assertEquals(6, doc.lineStart(1));
        assertEquals("", doc.lineText(1));
    }

    // ── lineForOffset ─────────────────────────────────────────────

    @Test
    void lineForOffset_positionsExactes() {
        Document doc = Document.of("abc\ndef\nghi");
        // a=0, b=1, c=2, \n=3, d=4, e=5, f=6, \n=7, g=8, h=9, i=10
        assertEquals(0, doc.lineForOffset(0));
        assertEquals(0, doc.lineForOffset(3));
        assertEquals(1, doc.lineForOffset(4));
        assertEquals(1, doc.lineForOffset(7));
        assertEquals(2, doc.lineForOffset(8));
        assertEquals(2, doc.lineForOffset(10));
    }

    @Test
    void lineForOffset_bornesClampees() {
        Document doc = Document.of("hello");
        assertEquals(0, doc.lineForOffset(-1));
        assertEquals(0, doc.lineForOffset(100));
    }

    // ── lineStart / lineEnd ───────────────────────────────────────

    @Test
    void lineStart_positionsExactes() {
        Document doc = Document.of("abc\ndef\nghi");
        assertEquals(0, doc.lineStart(0));
        assertEquals(4, doc.lineStart(1));
        assertEquals(8, doc.lineStart(2));
    }

    @Test
    void lineEnd_positionsExactes() {
        Document doc = Document.of("abc\ndef\nghi");
        assertEquals(3, doc.lineEnd(0));
        assertEquals(7, doc.lineEnd(1));
        assertEquals(11, doc.lineEnd(2));
    }

    @Test
    void lineStart_bornesClampees() {
        Document doc = Document.of("hello");
        assertEquals(0, doc.lineStart(-5));
        assertEquals(0, doc.lineStart(100));
    }

    @Test
    void lineEnd_bornesClampees() {
        Document doc = Document.of("ab\ncd");
        assertEquals(doc.lineEnd(-5), doc.lineEnd(0));
        assertEquals(doc.lineEnd(999), doc.lineEnd(doc.lineCount() - 1));
    }

    // ── lineText ──────────────────────────────────────────────────

    @Test
    void lineText_lignesDistinctes() {
        Document doc = Document.of("hello\nworld\nfoo");
        assertEquals("hello", doc.lineText(0));
        assertEquals("world", doc.lineText(1));
        assertEquals("foo", doc.lineText(2));
    }

    @Test
    void lineText_ligneVide() {
        Document doc = Document.of("a\n\nb");
        assertEquals("a", doc.lineText(0));
        assertEquals("", doc.lineText(1));
        assertEquals("b", doc.lineText(2));
    }

    @Test
    void lineText_bornesClampees() {
        Document doc = Document.of("ab\ncd\n");
        assertEquals(doc.lineText(-5), doc.lineText(0));
        assertEquals(doc.lineText(999), doc.lineText(doc.lineCount() - 1));
    }

    /**
     * Sémantique de référence du dépôt de départ : le contenu d'une ligne
     * est la tranche {@code [lineStart, lineEnd)} du texte complet. Le
     * retour chariot est un caractère de contenu comme les autres — seul
     * le saut de ligne délimite (la normalisation des fins de ligne est
     * une tranche ultérieure de M2).
     */
    @Test
    void lineText_semantiqueDeReference() {
        String texte = """
                première ligne

                avec accents éàü
                emoji 😀 ligne
                windows\r
                fin
                dernière sans retour""";
        Document doc = Document.of(texte);
        for (int ligne = 0; ligne < doc.lineCount(); ligne++) {
            int debut = doc.lineStart(ligne);
            int fin = doc.lineEnd(ligne);
            String attendu = debut >= fin ? "" : doc.text().substring(debut, fin);
            assertEquals(attendu, doc.lineText(ligne), "ligne " + ligne);
        }
        assertEquals("", doc.lineText(1));
        assertEquals("dernière sans retour", doc.lineText(doc.lineCount() - 1));
    }

    // ── charAt ────────────────────────────────────────────────────

    @Test
    void charAt_apresRemplacement() {
        Document doc = Document.of("hello");
        Document remplace = doc.replace(5, 5, " world");
        assertEquals('h', remplace.charAt(0));
        assertEquals(' ', remplace.charAt(5));
        assertEquals('w', remplace.charAt(6));
    }

    @Test
    void charAt_horsBornes_rejete() {
        Document doc = Document.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> doc.charAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.charAt(3));
    }

    // ── replace maintient l'index ─────────────────────────────────

    @Test
    void replace_insertionSautDeLigne() {
        Document doc = Document.of("hello");
        Document remplace = doc.replace(5, 5, "\nworld");
        assertEquals("hello\nworld", remplace.text());
        assertEquals(2, remplace.lineCount());
        assertEquals("hello", remplace.lineText(0));
        assertEquals("world", remplace.lineText(1));
    }

    @Test
    void replace_suppressionSautDeLigne() {
        Document doc = Document.of("hello\nworld");
        Document remplace = doc.replace(5, 6, "");
        assertEquals("helloworld", remplace.text());
        assertEquals(1, remplace.lineCount());
    }

    @Test
    void replace_suppressionEnBordureDeLigne() {
        // Efface le saut de ligne depuis la fin de la ligne 0 : la ligne
        // suivante fusionne, le préfixe d'index est réutilisé tel quel.
        Document doc = Document.of("abc\ndef");
        Document remplace = doc.replace(3, 4, "");
        assertEquals("abcdef", remplace.text());
        assertEquals(1, remplace.lineCount());
        assertEquals("abcdef", remplace.lineText(0));
    }

    @Test
    void replace_insertionMultiLignes() {
        Document doc = Document.of("line1\nline3");
        Document remplace = doc.replace(6, 6, "line2\n");
        assertEquals("line1\nline2\nline3", remplace.text());
        assertEquals(3, remplace.lineCount());
        assertEquals("line1", remplace.lineText(0));
        assertEquals("line2", remplace.lineText(1));
        assertEquals("line3", remplace.lineText(2));
    }

    @Test
    void replace_preserveLesLignesIntactes() {
        Document doc = Document.of("aaa\nbbb\nccc\nddd");
        Document remplace = doc.replace(4, 7, "BBB");
        assertEquals("aaa\nBBB\nccc\nddd", remplace.text());
        assertEquals(4, remplace.lineCount());
        assertEquals("aaa", remplace.lineText(0));
        assertEquals("BBB", remplace.lineText(1));
        assertEquals("ccc", remplace.lineText(2));
        assertEquals("ddd", remplace.lineText(3));
    }

    @Test
    void replace_editionsSuccessivesCoherenceDeLIndex() {
        Document doc = Document.of("line1\nline2\nline3");
        doc = doc.replace(0, 0, "// ");
        assertEquals("// line1\nline2\nline3", doc.text());
        assertEquals(3, doc.lineCount());

        // "// line1\n" fait 9 caractères, donc la ligne 2 commence à 9
        doc = doc.replace(9, 9, "// ");
        assertEquals("// line1\n// line2\nline3", doc.text());
        assertEquals(3, doc.lineCount());
        assertEquals("// line2", doc.lineText(1));
    }

    @Test
    void replace_documentEntier() {
        Document doc = Document.of("old\ncontent");
        Document remplace = doc.replace(0, doc.length(), "new content");
        assertEquals("new content", remplace.text());
        assertEquals(1, remplace.lineCount());
    }

    @Test
    void replace_enBordureDeDocument() {
        Document doc = Document.of("abc");
        assertEquals("Xabc", doc.replace(0, 0, "X").text());
        assertEquals("abcX", doc.replace(3, 3, "X").text());
    }

    // ── Seuils « gros document » ──────────────────────────────────

    @Test
    void isLarge_seuilCaracteresExact() {
        assertFalse(Document.of("x".repeat(2_500_000)).isLarge(),
                "exactement au seuil de caractères : pas gros");
        assertTrue(Document.of("x".repeat(2_500_001)).isLarge(),
                "un caractère de plus que le seuil : gros");
    }

    @Test
    void isLarge_seuilLignesExact() {
        // 49 999 sauts de ligne ouvrent exactement 50 000 lignes.
        StringBuilder contenu = new StringBuilder();
        for (int ligne = 0; ligne < 49_999; ligne++) {
            contenu.append('\n');
        }
        assertFalse(Document.of(contenu.toString()).isLarge(),
                "exactement au seuil de lignes : pas gros");
        assertTrue(Document.of(contenu.append('\n').toString()).isLarge(),
                "une ligne de plus que le seuil : gros");
    }

    // ── Mémo du texte complet ─────────────────────────────────────

    /**
     * Le mémo du texte complet rend la référence stable : les lecteurs
     * répétés d'un même instantané (synchronisation distante, rendu)
     * partagent la même instance au lieu de recalculer la chaîne.
     */
    @Test
    void text_referenceStableSurUnMemeInstantane() {
        Document doc = Document.of("ligne\nrépétée");
        assertSame(doc.text(), doc.text());
    }
}
