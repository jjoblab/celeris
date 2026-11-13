package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Les décisions de contenu GÉNÉRIQUES des popups de l'éditeur (le
 * déménagement du 2026-11-14, reprises de {@code PopupCoachTest} de
 * la démonstration) : la complétion des mots filtre par le préfixe
 * tapé sans se proposer le mot sous le curseur, le régime de frappe
 * propose au premier caractère d'identifiant, le fil d'Ariane
 * descend le plan, les candidats du protocole gagnent leurs badges.
 * Pur, sans appareil.
 */
final class ContenuPopupsTest {

    private static final String DOCUMENT = """
            int ajouter(int a, int b) {
                return a + b;
            }
            int mesurer(String unite, float valeur, boolean arrondir) {
                return ajouter(1, 2);
            }
            int total = ajouter(1, 2);
            """;

    private static final List<String> MOTS = List.of("int", "return");

    /** La complétion filtre par le préfixe, capée au plafond — les
     *  mots réservés de l'hôte s'y joignent, jamais sans préfixe. */
    @Test
    void completionFiltersByPrefix() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // « aj » avant le curseur : les deux fonctions et rien d'autre.
        int curseur = DOCUMENT.indexOf("ajouter(") + 2;
        List<String> candidats = ContenuPopups.completionMots(
                session.document(), curseur, MOTS);
        assertTrue(candidats.contains("ajouter"),
                "ajouter est candidat de son propre préfixe");
        assertFalse(candidats.contains("int"),
                "hors préfixe, le mot réservé ne se propose pas");
        assertTrue(candidats.size() <= ContenuPopups.MAX_CANDIDATS,
                "le plafond tient");
        // Sans préfixe tapé : rien à proposer.
        int finDeLigne = DOCUMENT.indexOf('\n');
        assertTrue(ContenuPopups.completionMots(session.document(),
                finDeLigne, MOTS).isEmpty(),
                "sans préfixe, la complétion ne propose rien");
    }

    /** La complétion se propose à la FRAPPE : dès le premier
     *  caractère d'identifiant avant le curseur ; le blanc, la
     *  ponctuation et le début du document ne proposent rien. */
    @Test
    void completionOpensOnTypingAnIdentifier() {
        TextSession session = TextSession.of("int nombreLu = 3;",
                System::nanoTime);
        Document document = session.document();
        assertFalse(ContenuPopups.doitOuvrirALaFrappe(document, 0),
                "au début du document, rien ne se propose");
        assertFalse(ContenuPopups.doitOuvrirALaFrappe(document, 4),
                "après un blanc, rien ne se propose");
        assertFalse(ContenuPopups.doitOuvrirALaFrappe(document, 14),
                "après un signe, rien ne se propose");
        assertTrue(ContenuPopups.doitOuvrirALaFrappe(document, 1),
                "le premier caractère tapé propose");
        assertTrue(ContenuPopups.doitOuvrirALaFrappe(document, 10),
                "au cœur d'un mot, la liste filtre");
    }

    /** Le symbole à l'offset : l'identifiant complet, null entre
     *  deux mots ; le préfixe se lit au caret. */
    @Test
    void symboleEtPrefixe() {
        TextSession session = TextSession.of("int nombreLu = 3;",
                System::nanoTime);
        Document document = session.document();
        assertEquals("nombreLu", ContenuPopups.symboleA(document, 12),
                "l'identifiant complet se lit à la fin du mot");
        assertNull(ContenuPopups.symboleA(document, 13),
                "entre deux mots, aucun symbole");
        assertEquals("no", ContenuPopups.prefixeCourant(document, 6),
                "le préfixe tapé se coupe au caret");
        assertEquals(4, ContenuPopups.debutMot(document, 12),
                "le début du mot suit la fin du préfixe");
    }

    /** Le fil d'Ariane englobe le curseur, externe vers interne. */
    @Test
    void pathGoesOuterToInner() {
        OutlineEntry externe = new OutlineEntry(NodeCategory.FUNCTION,
                "mesurer", 0, 100, 0, 4);
        OutlineEntry interne = new OutlineEntry(NodeCategory.FIELD,
                "total", 50, 60, 2, 2);
        List<String> chemin = ContenuPopups.pathFor(
                List.of(externe, interne), 55);
        assertEquals(List.of("mesurer", "total"), chemin,
                "la plus externe d'abord, la plus interne en dernier");
        assertTrue(ContenuPopups.pathFor(List.of(externe, interne), 150)
                .isEmpty(), "hors de tout symbole, le fil se vide");
    }

    /** Les candidats du protocole gagnent leurs badges : le genre du
     *  protocole devient celui du kit, le détail devient l'identité
     *  qualifiée, la première ligne de documentation sinon. */
    @Test
    void candidatsDuProtocole() {
        ListeCompletion reponse = ListeCompletion.de(false, List.of(
                new ElementCompletion("nombreLu", 3, "int nombreLu",
                        null, null, null, null, false),
                new ElementCompletion("mesurer", 2, null,
                        "Mesure une valeur.\n@param unite l'unité",
                        null, null, null, false)));
        List<jo.celeris.android.widgets.CompletionListView.Candidat> types =
                ContenuPopups.candidats(reponse);
        assertEquals(2, types.size(), "le protocole garde son ordre");
        assertEquals("nombreLu", types.get(0).libelle());
        assertEquals("int nombreLu", types.get(0).detail(),
                "le détail sert d'identité qualifiée");
        assertEquals(jo.celeris.android.widgets.BadgeGenre.Genre.METHODE,
                types.get(0).genre(), "le genre 3 du protocole est méthode");
        assertEquals("Mesure une valeur.", types.get(1).detail(),
                "la première ligne de documentation sinon");
        assertTrue(ContenuPopups.candidats(null).isEmpty(),
                "sans réponse, la liste vide");
        assertNotNull(ContenuPopups.docDe(reponse, 1),
                "l'élément documenté sert sa documentation");
        assertNull(ContenuPopups.docDe(reponse, 0),
                "l'élément sans documentation n'en invente pas");
    }
}
