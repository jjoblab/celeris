package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Aides de comparaison d'arbres pour les tests du socle : l'équivalence se
 * vérifie ancre par ancre et nœud par nœud (sorte, catégorie, bornes
 * relatives, nom de plan, drapeau d'erreur) — les instances, elles, peuvent
 * différer (c'est le propre de l'incrémental).
 */
final class Arbres {

    private Arbres() {
    }

    /** Vérifie que deux arbres décrivent la même structure sur le document donné. */
    static void verifierEquivalence(SyntaxTree obtenu, SyntaxTree temoin, Document doc) {
        assertEquals(temoin.revision(), obtenu.revision(), "révision");
        assertEquals(temoin.chunks().size(), obtenu.chunks().size(), "nombre de chunks");
        for (int i = 0; i < temoin.chunks().size(); i++) {
            Chunk attendu = temoin.chunks().get(i);
            Chunk reel = obtenu.chunks().get(i);
            assertEquals(attendu.startLine(), reel.startLine(), "chunk " + i + " ligne de début");
            assertEquals(attendu.startCol(), reel.startCol(), "chunk " + i + " colonne de début");
            assertEquals(attendu.endLine(), reel.endLine(), "chunk " + i + " ligne de fin");
            assertEquals(attendu.endCol(), reel.endCol(), "chunk " + i + " colonne de fin");
            assertEquals(temoin.chunkStartOffset(doc, attendu),
                    obtenu.chunkStartOffset(doc, reel), "chunk " + i + " offset absolu");
            assertEquals(temoin.chunkEndOffset(doc, attendu),
                    obtenu.chunkEndOffset(doc, reel), "chunk " + i + " offset absolu de fin");
            verifierNoeuds(reel.root(), attendu.root(), i);
        }
    }

    private static void verifierNoeuds(SynNode reel, SynNode attendu, int chunk) {
        assertEquals(attendu.kind(), reel.kind(), "sorte du nœud du chunk " + chunk);
        assertEquals(attendu.category(), reel.category(), "catégorie du chunk " + chunk);
        assertEquals(attendu.start(), reel.start(), "début du nœud du chunk " + chunk);
        assertEquals(attendu.end(), reel.end(), "fin du nœud du chunk " + chunk);
        assertEquals(attendu.outlineName(), reel.outlineName(), "nom du chunk " + chunk);
        assertEquals(attendu.isError(), reel.isError(), "erreur du chunk " + chunk);
        List<SynNode> enfantsReels = reel.children();
        List<SynNode> enfantsAttendus = attendu.children();
        assertEquals(enfantsAttendus.size(), enfantsReels.size(),
                "enfants du nœud du chunk " + chunk);
        for (int i = 0; i < enfantsAttendus.size(); i++) {
            verifierNoeuds(enfantsReels.get(i), enfantsAttendus.get(i), chunk);
        }
    }

    /** Vérifie que les paniers et états de deux lexiques sont égaux ligne à ligne. */
    static void verifierEgalite(LexDocument obtenu, LexDocument temoin) {
        assertEquals(temoin.lineCount(), obtenu.lineCount(), "nombre de lignes");
        for (int ligne = 0; ligne < temoin.lineCount(); ligne++) {
            assertTrue(
                    java.util.Arrays.equals(temoin.line(ligne).tokens(),
                            obtenu.line(ligne).tokens()),
                    "paniers de la ligne " + ligne);
            assertEquals(temoin.line(ligne).exitState(), obtenu.line(ligne).exitState(),
                    "états de la ligne " + ligne);
            assertEquals(temoin.bracketDepthAtLineStart(ligne),
                    obtenu.bracketDepthAtLineStart(ligne),
                    "profondeurs de la ligne " + ligne);
        }
    }
}
