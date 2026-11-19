package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;
import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.ServerCapsLsp;







/**
 * La correspondance exercée forme par forme : chaque branche du
 * protocole — chaîne nue, contenu balisé, liste de contenus, décalages
 * de paramètres, gravité absente — reçoit son verdict (PIEGE-16, le
 * redressement M8.6).
 */
public final class CorrespondanceLspTest {

    private static final String URI = "file:///banc/exemple.xml";

    @Test
    void laPositionSeLitEtSecrit() {
        assertEquals(new LspPosition(3, 7),
                LspMapping.readPosition(new Position(3, 7)));
        assertEquals(new LspPosition(0, 0), LspMapping.readPosition(null));
        assertEquals(new Position(3, 7),
                LspMapping.writePosition(new LspPosition(3, 7)));
    }

    @Test
    void laPlageAbsenteVautLOrigine() {
        assertEquals(new LspPosition(0, 0), LspMapping.readRange(null).start());
        Range borneNulle = new Range();
        assertEquals(new LspPosition(0, 0),
                LspMapping.readRange(borneNulle).end());
    }

    @Test
    void leDiagnosticSuitSesTroisFormesDeMessage() {
        Diagnostic gauche = new Diagnostic();
        gauche.setRange(new Range(new Position(1, 2), new Position(3, 4)));
        gauche.setSeverity(DiagnosticSeverity.Warning);
        gauche.setMessage(Either.forLeft("message nu"));
        gauche.setCode(Either.forLeft("E001"));
        gauche.setSource("factice");
        DiagnosticLsp luGauche = LspMapping.readDiagnostic(gauche);
        assertEquals(new LspPosition(1, 2), luGauche.range().start());
        assertEquals(DiagnosticLsp.AVERTISSEMENT, luGauche.severity());
        assertEquals("message nu", luGauche.message());
        assertEquals("E001", luGauche.code());
        assertEquals("factice", luGauche.source());

        Diagnostic balise = new Diagnostic();
        balise.setMessage(Either.forRight(new MarkupContent("markdown", "** Grave **")));
        balise.setCode(Either.forRight(42));
        DiagnosticLsp luBalise = LspMapping.readDiagnostic(balise);
        assertEquals("** Grave **", luBalise.message());
        assertEquals("42", luBalise.code());

        Diagnostic muet = new Diagnostic();
        DiagnosticLsp luMuet = LspMapping.readDiagnostic(muet);
        assertEquals(0, luMuet.severity());
        assertEquals("", luMuet.message());
        assertNull(luMuet.code());
        PublishDiagnosticsParams sansListe = new PublishDiagnosticsParams();
        sansListe.setUri(URI);
        assertEquals(List.of(), LspMapping.publishedDiagnostics(sansListe));
    }

    @Test
    void lElementRefuseLeLibelleAbsent() {
        CompletionItem muet = new CompletionItem();
        assertThrows(IllegalArgumentException.class,
                () -> DocumentsMapping.readItem(muet));
        CompletionItem complet = new CompletionItem();
        complet.setLabel("ex:exemplaire");
        complet.setKind(CompletionItemKind.Field);
        complet.setDetail("detail");
        complet.setDocumentation(Either.forLeft("documentation nue"));
        complet.setDeprecated(Boolean.TRUE);
        jo.celeris.core.lsp.types.CompletionItemLsp lu = DocumentsMapping.readItem(complet);
        assertTrue(lu.obsolete());
        assertEquals("documentation nue", lu.documentation());
        CompletionItem sansGenre = new CompletionItem();
        sansGenre.setLabel("nu");
        assertEquals(0, DocumentsMapping.readItem(sansGenre).kind());
        assertNull(DocumentsMapping.readItem(sansGenre).documentation());
        CompletionItem documentationBalisee = new CompletionItem();
        documentationBalisee.setLabel("nu");
        documentationBalisee.setDocumentation(
                Either.forRight(new MarkupContent("markdown", "balisee")));
        assertEquals("balisee",
                DocumentsMapping.readItem(documentationBalisee).documentation());
    }

    @Test
    void laListePorteSaCoupure() {
        CompletionItem element = new CompletionItem();
        element.setLabel("ex");
        assertEquals(List.of(),
                DocumentsMapping.readList(null).elements());
        assertFalse(DocumentsMapping.readList(null).isIncomplete());
        CompletionListLsp tableauNu = DocumentsMapping.readList(
                Either.forLeft(List.of(element)));
        assertEquals(1, tableauNu.elements().size());
        assertFalse(tableauNu.isIncomplete());
        CompletionListLsp coupee = DocumentsMapping.readList(
                Either.forRight(new CompletionList(true, List.of(element))));
        assertTrue(coupee.isIncomplete());
        assertEquals(1, coupee.elements().size());
        assertEquals(List.of(), DocumentsMapping.readList(
                Either.forRight(null)).elements());
    }

    @Test
    void leSurvolJointLesContenusBalises() {
        assertEquals(HoverLsp.EMPTY, DocumentsMapping.readHover(null));
        Hover liste = new Hover();
        liste.setContents(Either.forLeft(List.of(
                Either.forLeft("premier"),
                Either.forRight(new MarkedString("xml", "second")))));
        assertEquals("premier\nsecond", DocumentsMapping.readHover(liste).text());
        assertEquals("", DocumentsMapping.readHover(liste).kind());
        Hover balise = new Hover(
                new MarkupContent("markdown", "survolee"),
                new Range(new Position(1, 0), new Position(1, 2)));
        HoverLsp lu = DocumentsMapping.readHover(balise);
        assertEquals("survolee", lu.text());
        assertEquals("markdown", lu.kind());
        jo.celeris.core.lsp.types.RangeLsp plageLue = lu.range();
        assertNotNull(plageLue);
        assertEquals(new LspPosition(1, 0), plageLue.start());
        Hover muet = new Hover();
        assertEquals("", DocumentsMapping.readHover(muet).text());
        assertEquals("", DocumentsMapping.readHover(muet).kind());
        assertNull(DocumentsMapping.readHover(muet).range());
    }

    @Test
    void laSignatureTrancheLesDecalagesBornes() {
        assertEquals(-1, DocumentsMapping.readSignature(null).signatureActiveIndex());
        SignatureHelp help = new SignatureHelp();
        SignatureInformation input = new SignatureInformation();
        org.eclipse.lsp4j.ParameterInformation horsBornes =
                new org.eclipse.lsp4j.ParameterInformation();
        horsBornes.setLabel(Either.forRight(Tuple.two(8, 40)));
        input.setParameters(List.of(horsBornes));
        help.setSignatures(List.of(input));
        SignatureLsp lu = DocumentsMapping.readSignature(help);
        assertEquals("", lu.entrees().get(0).label());
        assertEquals("", lu.entrees().get(0).parameters().get(0).label());
        assertEquals(-1, lu.parametreCourant());
    }

    @Test
    void lesLieuxLisentLesDeuxReponses() {
        assertEquals(List.of(), DocumentsMapping.readLocations(null));
        Location lieu = new Location(URI,
                new Range(new Position(0, 1), new Position(0, 2)));
        assertEquals(1, DocumentsMapping.readLocations(
                Either.forLeft(List.of(lieu))).size());
        LocationLink lien = new LocationLink();
        lien.setTargetUri(URI);
        lien.setTargetRange(new Range(new Position(2, 0), new Position(2, 3)));
        List<LocationLsp> lus = DocumentsMapping.readLocations(
                Either.forRight(List.of(lien)));
        assertEquals(1, lus.size());
        jo.celeris.core.lsp.types.RangeLsp plageCible = lus.get(0).range();
        assertNotNull(plageCible);
        assertEquals(new LspPosition(2, 0), plageCible.start());
    }

    @Test
    void lesCapacitesLisentLeursDeuxSyncs() {
        ServerCapsLsp empty = LspMapping.readCapabilities(null);
        assertEquals(-1, empty.syncKind());
        assertFalse(empty.supportsCompletion());

        ServerCapabilities complete = new ServerCapabilities();
        complete.setTextDocumentSync(Either.forLeft(TextDocumentSyncKind.Full));
        complete.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions());
        assertEquals(1,
                LspMapping.readCapabilities(complete).syncKind());
        assertTrue(LspMapping.readCapabilities(complete).supportsCompletion());

        ServerCapabilities incrementale = new ServerCapabilities();
        incrementale.setTextDocumentSync(Either.forRight(new TextDocumentSyncOptions()));
        assertEquals(-1,
                LspMapping.readCapabilities(incrementale).syncKind());

        ServerCapabilities sansLegende = new ServerCapabilities();
        sansLegende.setSemanticTokensProvider(
                new SemanticTokensWithRegistrationOptions());
        assertEquals(List.of(),
                LspMapping.readCapabilities(sansLegende).tokenTypes());

        TextDocumentSyncOptions options = new TextDocumentSyncOptions();
        options.setChange(TextDocumentSyncKind.Incremental);
        ServerCapabilities avecLegende = new ServerCapabilities();
        avecLegende.setTextDocumentSync(Either.forRight(options));
        avecLegende.setSemanticTokensProvider(
                new SemanticTokensWithRegistrationOptions());
        avecLegende.getSemanticTokensProvider().setLegend(
                new org.eclipse.lsp4j.SemanticTokensLegend(
                        List.of("type"), List.of("declaration")));
        ServerCapsLsp lues = LspMapping.readCapabilities(avecLegende);
        assertEquals(2, lues.syncKind());
        assertEquals(List.of("type"), lues.tokenTypes());
        assertEquals(List.of("declaration"), lues.tokenModifiers());
    }
}
