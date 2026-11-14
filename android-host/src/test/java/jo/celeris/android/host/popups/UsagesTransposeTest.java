package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * La transpose des usages chez l'orchestrateur (le déménagement du
 * 2026-11-14, la seconde surface portée) : le lieu du document ouvert
 * devient l'offset du saut (la colonne du protocole se lit dans SA
 * ligne), l'extrait se rogne, l'ÉTRANGÈRE vit muette — l'audit
 * Android Studio du 2026-11-14 : l'IDE de référence montre les usages
 * de tous les fichiers, le nom du fichier du serveur se lit, le lieu
 * ne se visite pas. Pur, sans appareil — les régimes de la carte
 * vivent au banc du kit ({@code ReferencesViewTest}).
 */
final class UsagesTransposeTest {

    private static final String URI = "file:///exemple.java";

    private static final String AUTRE = "file:///ailleurs/Concurrence.kt";

    private static final String TEXTE = """
            int total = 1;
            int copie = total;
            int longue = 0; // une ligne volontairement tres longue pour rogner
            """;

    /** Le lieu du document ouvert : le début de plage devient l'offset
     *  du saut — la colonne se lit dans SA ligne, la frontière y
     *  répond (l'uri du document). */
    @Test
    void leLieuDuDocumentDevientLOffsetDuSaut() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        Document document = session.document();
        LieuLsp lieu = new LieuLsp(URI,
                new PlageLsp(new PositionLsp(1, 12),
                        new PositionLsp(1, 17)));
        int offset = Usages.offsetDe(lieu, URI, document);
        assertEquals(TEXTE.indexOf("total", TEXTE.indexOf("copie")),
                offset, "le second usage vit à la copie");
        assertEquals("L2 · int copie = total;",
                Usages.libelleDe(document, offset),
                "le libellé porte la ligne et l'extrait");
    }

    /** L'étrangère vit muette (l'audit Android Studio : tous les
     *  fichiers comptent) : l'uri étrangère rend —1 — son nom court et
     *  sa ligne se lisent, le lieu ne se visite pas. */
    @Test
    void letrangereVitMue() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        Document document = session.document();
        LieuLsp etrangere = new LieuLsp(AUTRE,
                new PlageLsp(new PositionLsp(4, 3),
                        new PositionLsp(4, 8)));
        assertEquals(-1, Usages.offsetDe(etrangere, URI, document),
                "l'uri étrangère ne répond pas à la frontière");
        assertEquals("Concurrence.kt · L5",
                Usages.libelleEtrangere(etrangere),
                "le nom court du fichier du serveur et sa ligne se lisent");
        LieuLsp sansPlage = new LieuLsp(AUTRE, null);
        assertEquals(-1, Usages.offsetDe(sansPlage, URI, document),
                "le lieu sans plage ne se devine jamais");
        assertEquals("Concurrence.kt",
                Usages.libelleEtrangere(sansPlage),
                "le lieu sans plage porte son nom seul");
    }

    /** L'extrait se rogne au-delà de quarante-huit caractères — la
     *  carte reste lisible, jamais une rangée qui mange la couche. */
    @Test
    void lextraitSeRogne() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        Document document = session.document();
        int ligne = document.lineForOffset(
                TEXTE.indexOf("volontairement"));
        String libelle = Usages.libelleDe(document,
                document.lineStart(ligne));
        assertTrue(libelle.length() <= 48 + " · L3…".length(),
                "l'extrait rogné tient la rangée : " + libelle);
        assertTrue(libelle.endsWith("…"), "l'extrait rogné l'annonce");
    }

    /** La ligne hors bornes rend —1 — le lieu au-delà du document ne
     *  se devine jamais. */
    @Test
    void laLigneHorsBornesNeSeDevinePas() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        Document document = session.document();
        LieuLsp horsBornes = new LieuLsp(URI,
                new PlageLsp(new PositionLsp(99, 0),
                        new PositionLsp(99, 5)));
        assertEquals(-1, Usages.offsetDe(horsBornes, URI, document),
                "la ligne hors bornes rend —1");
    }

    /** Le compte du serveur (l'audit Android Studio) : la ressource de
     *  l'hôte porte TOUS les usages, jamais le seul document ouvert. */
    @Test
    void laRessourceCompteCommeLIde() {
        // deux lieux : un ouvert, une étrangère — l'en-tête dira « 2 »
        List<LieuLsp> lieux = List.of(
                new LieuLsp(URI, new PlageLsp(
                        new PositionLsp(0, 4), new PositionLsp(0, 9))),
                new LieuLsp(AUTRE, new PlageLsp(
                        new PositionLsp(4, 3), new PositionLsp(4, 8))));
        long ouverts = lieux.stream().filter(
                l -> Usages.offsetDe(l, URI,
                        TextSession.of(TEXTE, () -> 0L).document()) >= 0)
                .count();
        assertEquals(1, ouverts, "un seul lieu vit au document ouvert");
        assertEquals(2, lieux.size(),
                "l'en-tête compte pourtant les deux — comme l'IDE");
    }
}
