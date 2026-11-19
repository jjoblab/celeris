package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TemplateEditLsp;
import jo.celeris.core.lsp.types.CompletionItemLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jo.celeris.core.lsp.ServerCapsLsp;











/**
 * Les traces de contrat du jalon (sortie M6, portées à la frontière par
 * l'ADR-015) : les réponses enregistrées de vrais serveurs —
 * l'initialisation, la complétion tronquée, le survol balisé, la
 * signature aux paramètres par bornes, la définition multiple, les
 * indices à parties, l'édition d'espace avec édit de gabarit — se
 * rejouent par la MÊME machinerie que le fil : le désérialiseur de la
 * bibliothèque lit la forme enregistrée, la correspondance la traduit
 * en domaine. Toute divergence future — une montée de version qui
 * change la lecture, une forme que le domaine ne tient plus — casse la
 * construction. L'édit de gabarit, forme maison de M6, est porté à la
 * forme de la 1.0.0 de la bibliothèque (édits de gabarit du protocole)
 * — la trace garde ses valeurs, la forme change de siècle. Contenu
 * original, aucune pièce tierce (neutralité, ADR-011).
 */
public final class CorpusTracesTest {

    /**
     * Le Gson de la bibliothèque : les adaptateurs du protocole compris
     * (les {@code Either} y suivent la forme JSON), sans résolveur de
     * requêtes — le lecteur des traces se donne le type, comme la
     * correspondance en production.
     */
    private static final Gson GSON = new MessageJsonHandler(Map.of()).getGson();

    /** La forme d'une liste d'indices — le type du fil. */
    private static final Type INLAY_HINTS =
            new TypeToken<List<InlayHint>>() { }.getType();

    /** L'initialisation : la surface bornée se lit des capacités annoncées. */
    @Test
    void laTraceInitialisation() {
        InitializeResult reponse =
                GSON.fromJson(trace("initialisation"), InitializeResult.class);
        ServerCapsLsp capabilities =
                LspMapping.readCapabilities(reponse.getCapabilities());
        assertEquals(1, capabilities.syncKind());
        assertTrue(capabilities.supportsCompletion());
        assertTrue(capabilities.supportsHover());
        assertTrue(capabilities.supportsSignature());
        assertTrue(capabilities.supportsDefinition());
        assertTrue(capabilities.supportsReferences());
        assertTrue(capabilities.supportsFormatting());
        assertTrue(capabilities.supportsInlayHints(), "inlayHintProvider lu");
        assertEquals(List.of("keyword", "variable", "parameter", "function", "type"),
                capabilities.tokenTypes());
        assertEquals(List.of("declaration", "static", "deprecated"),
                capabilities.tokenModifiers());
    }

    /** La complétion tronquée : la coupure honorée, l'ordre du serveur tenu (PIEGE-17). */
    @Test
    void laTraceCompletionTronquee() {
        CompletionList reponse =
                GSON.fromJson(trace("completion-tronquee"), CompletionList.class);
        CompletionListLsp liste = DocumentsMapping.readList(
                Either.forRight(reponse));
        assertTrue(liste.isIncomplete());
        assertEquals(3, liste.elements().size());
        assertEquals("ajouter", liste.elements().get(0).label());
        // la liste tronquée ne se trie jamais — la même revient
        assertEquals(liste, liste.sort());
        // le filtrage garde l'ordre relatif, n'ajoute rien
        assertEquals(List.of("ajouter", "ajuster"),
                liste.filter("aj").elements().stream()
                        .map(CompletionItemLsp::label).toList());
    }

    /** Le survol : le contenu balisé et la plage, résolus à la demande. */
    @Test
    void laTraceSurvol() {
        Hover reponse = GSON.fromJson(trace("survol"), Hover.class);
        HoverLsp hover = DocumentsMapping.readHover(reponse);
        assertEquals("markdown", hover.kind());
        assertTrue(hover.text().startsWith("fun ajouter"));
        assertTrue(hover.text().contains("Ajoute deux entiers."));
        RangeLsp range = hover.range();
        assertNotNull(range);
        assertEquals(new LspPosition(2, 10), range.start());
        assertEquals(new LspPosition(2, 17), range.end());
    }

    /** La signature : paramètres par bornes, l'actif au-delà de l'arité retombe au dernier. */
    @Test
    void laTraceSignature() {
        SignatureHelp reponse =
                GSON.fromJson(trace("signature"), SignatureHelp.class);
        SignatureLsp help = DocumentsMapping.readSignature(reponse);
        assertEquals(1, help.entrees().size());
        SignatureLsp.Entry input = help.entrees().get(0);
        assertEquals("ajouter(a: Int, b: Int, c: Int): Int", input.label());
        assertEquals(List.of("a", "b", "c"), input.parameters().stream()
                .map(SignatureLsp.Parameter::label).toList());
        assertEquals("le premier terme", input.parameters().get(0).documentation());
        assertEquals(2, help.parametreCourant(),
                "l'actif 5 au-dela de l'arite 3 retombe au dernier");
    }

    /**
     * La définition : les lieux multiples dans l'ordre du serveur — la
     * trace se lit en liste directe, l'{@code Either} se pose après le
     * vol : ses deux côtés sont des listes, le lecteur sans résolveur
     * de requêtes ne peut pas choisir.
     */
    @Test
    void laTraceDefinition() {
        List<Location> enregistres = GSON.fromJson(trace("definition"),
                new TypeToken<List<Location>>() { }.getType());
        List<LocationLsp> lieux = DocumentsMapping.readLocations(
                Either.forLeft(enregistres));
        assertEquals(2, lieux.size());
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt",
                lieux.get(0).uri());
        RangeLsp seconde = lieux.get(1).range();
        assertNotNull(seconde);
        assertEquals(new LspPosition(40, 2), seconde.start());
    }

    /** Les indices : parties cliquables, la chaîne nue, les remplissages. */
    @Test
    void laTraceIndices() {
        List<InlayHintLsp> inlayHints = CoverageMapping.readInlayHints(
                GSON.fromJson(trace("indices"),
                        INLAY_HINTS));
        assertEquals(2, inlayHints.size());
        InlayHintLsp premier = inlayHints.get(0);
        assertEquals(3, premier.parties().size());
        assertEquals(1, premier.kind());
        assertTrue(premier.remplissageGauche());
        assertFalse(premier.remplissageDroite());
        LocationLsp target = premier.parties().get(1).target();
        assertNotNull(target, "la partie « : » porte sa cible de navigation");
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt", target.uri());
        InlayHintLsp second = inlayHints.get(1);
        assertEquals(1, second.parties().size());
        assertEquals("a =", second.parties().get(0).value());
        assertTrue(second.remplissageDroite());
    }

    /**
     * L'édition d'espace : le document édité ET l'édit de gabarit —
     * la forme de la 1.0.0, le bloqué historique débloqué (ADR-015).
     */
    @Test
    void laTraceEditionEspace() {
        WorkspaceEditLsp espace = WorkspaceMapping.readWorkspace(
                GSON.fromJson(trace("edition-espace"),
                        WorkspaceEdit.class));
        assertEquals(1, espace.documents().size());
        assertEquals(7, espace.documents().get(0).version());
        assertEquals("additionner", espace.documents().get(0).edits()
                .get(0).newText());
        assertEquals(1, espace.templates().size(), "l'edit de gabarit vit dans la trace");
        TemplateEditLsp gabarit = espace.templates().get(0);
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt",
                gabarit.uri());
        assertEquals(new LspPosition(20, 4), gabarit.position());
        assertTrue(gabarit.body().contains("${nom:soustraire}"));
    }

    private static String trace(String nom) {
        String chemin = "traces/" + nom + ".json";
        try (InputStream flux = CorpusTracesTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("missing trace resource: " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unreadable trace resource: " + chemin, e);
        }
    }
}
