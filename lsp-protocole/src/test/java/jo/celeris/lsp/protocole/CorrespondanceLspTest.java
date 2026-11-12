package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
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

import jo.celeris.core.lsp.CapacitesServeur;







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
        assertEquals(new PositionLsp(3, 7),
                CorrespondanceLsp.lirePosition(new Position(3, 7)));
        assertEquals(new PositionLsp(0, 0), CorrespondanceLsp.lirePosition(null));
        assertEquals(new Position(3, 7),
                CorrespondanceLsp.ecrirePosition(new PositionLsp(3, 7)));
    }

    @Test
    void laPlageAbsenteVautLOrigine() {
        assertEquals(new PositionLsp(0, 0), CorrespondanceLsp.lirePlage(null).debut());
        Range borneNulle = new Range();
        assertEquals(new PositionLsp(0, 0),
                CorrespondanceLsp.lirePlage(borneNulle).fin());
    }

    @Test
    void leDiagnosticSuitSesTroisFormesDeMessage() {
        Diagnostic gauche = new Diagnostic();
        gauche.setRange(new Range(new Position(1, 2), new Position(3, 4)));
        gauche.setSeverity(DiagnosticSeverity.Warning);
        gauche.setMessage(Either.forLeft("message nu"));
        gauche.setCode(Either.forLeft("E001"));
        gauche.setSource("factice");
        DiagnosticLsp luGauche = CorrespondanceLsp.lireDiagnostic(gauche);
        assertEquals(new PositionLsp(1, 2), luGauche.plage().debut());
        assertEquals(DiagnosticLsp.AVERTISSEMENT, luGauche.gravite());
        assertEquals("message nu", luGauche.message());
        assertEquals("E001", luGauche.code());
        assertEquals("factice", luGauche.source());

        Diagnostic balise = new Diagnostic();
        balise.setMessage(Either.forRight(new MarkupContent("markdown", "** Grave **")));
        balise.setCode(Either.forRight(42));
        DiagnosticLsp luBalise = CorrespondanceLsp.lireDiagnostic(balise);
        assertEquals("** Grave **", luBalise.message());
        assertEquals("42", luBalise.code());

        Diagnostic muet = new Diagnostic();
        DiagnosticLsp luMuet = CorrespondanceLsp.lireDiagnostic(muet);
        assertEquals(0, luMuet.gravite());
        assertEquals("", luMuet.message());
        assertNull(luMuet.code());
        PublishDiagnosticsParams sansListe = new PublishDiagnosticsParams();
        sansListe.setUri(URI);
        assertEquals(List.of(), CorrespondanceLsp.diagnosticsPublies(sansListe));
    }

    @Test
    void lElementRefuseLeLibelleAbsent() {
        CompletionItem muet = new CompletionItem();
        assertThrows(IllegalArgumentException.class,
                () -> CorrespondanceDocuments.lireElement(muet));
        CompletionItem complet = new CompletionItem();
        complet.setLabel("ex:exemplaire");
        complet.setKind(CompletionItemKind.Field);
        complet.setDetail("detail");
        complet.setDocumentation(Either.forLeft("documentation nue"));
        complet.setDeprecated(Boolean.TRUE);
        jo.celeris.core.lsp.types.ElementCompletion lu = CorrespondanceDocuments.lireElement(complet);
        assertTrue(lu.obsolete());
        assertEquals("documentation nue", lu.documentation());
        CompletionItem sansGenre = new CompletionItem();
        sansGenre.setLabel("nu");
        assertEquals(0, CorrespondanceDocuments.lireElement(sansGenre).genre());
        assertNull(CorrespondanceDocuments.lireElement(sansGenre).documentation());
        CompletionItem documentationBalisee = new CompletionItem();
        documentationBalisee.setLabel("nu");
        documentationBalisee.setDocumentation(
                Either.forRight(new MarkupContent("markdown", "balisee")));
        assertEquals("balisee",
                CorrespondanceDocuments.lireElement(documentationBalisee).documentation());
    }

    @Test
    void laListePorteSaCoupure() {
        CompletionItem element = new CompletionItem();
        element.setLabel("ex");
        assertEquals(List.of(),
                CorrespondanceDocuments.lireListe(null).elements());
        assertFalse(CorrespondanceDocuments.lireListe(null).estIncomplete());
        ListeCompletion tableauNu = CorrespondanceDocuments.lireListe(
                Either.forLeft(List.of(element)));
        assertEquals(1, tableauNu.elements().size());
        assertFalse(tableauNu.estIncomplete());
        ListeCompletion coupee = CorrespondanceDocuments.lireListe(
                Either.forRight(new CompletionList(true, List.of(element))));
        assertTrue(coupee.estIncomplete());
        assertEquals(1, coupee.elements().size());
        assertEquals(List.of(), CorrespondanceDocuments.lireListe(
                Either.forRight(null)).elements());
    }

    @Test
    void leSurvolJointLesContenusBalises() {
        assertEquals(SurvolLsp.VIDE, CorrespondanceDocuments.lireSurvol(null));
        Hover liste = new Hover();
        liste.setContents(Either.forLeft(List.of(
                Either.forLeft("premier"),
                Either.forRight(new MarkedString("xml", "second")))));
        assertEquals("premier\nsecond", CorrespondanceDocuments.lireSurvol(liste).texte());
        assertEquals("", CorrespondanceDocuments.lireSurvol(liste).genre());
        Hover balise = new Hover(
                new MarkupContent("markdown", "survolee"),
                new Range(new Position(1, 0), new Position(1, 2)));
        SurvolLsp lu = CorrespondanceDocuments.lireSurvol(balise);
        assertEquals("survolee", lu.texte());
        assertEquals("markdown", lu.genre());
        jo.celeris.core.lsp.types.PlageLsp plageLue = lu.plage();
        assertNotNull(plageLue);
        assertEquals(new PositionLsp(1, 0), plageLue.debut());
        Hover muet = new Hover();
        assertEquals("", CorrespondanceDocuments.lireSurvol(muet).texte());
        assertEquals("", CorrespondanceDocuments.lireSurvol(muet).genre());
        assertNull(CorrespondanceDocuments.lireSurvol(muet).plage());
    }

    @Test
    void laSignatureTrancheLesDecalagesBornes() {
        assertEquals(-1, CorrespondanceDocuments.lireSignature(null).signatureActiveIndex());
        SignatureHelp aide = new SignatureHelp();
        SignatureInformation entree = new SignatureInformation();
        org.eclipse.lsp4j.ParameterInformation horsBornes =
                new org.eclipse.lsp4j.ParameterInformation();
        horsBornes.setLabel(Either.forRight(Tuple.two(8, 40)));
        entree.setParameters(List.of(horsBornes));
        aide.setSignatures(List.of(entree));
        SignatureLsp lu = CorrespondanceDocuments.lireSignature(aide);
        assertEquals("", lu.entrees().get(0).libelle());
        assertEquals("", lu.entrees().get(0).parametres().get(0).libelle());
        assertEquals(-1, lu.parametreCourant());
    }

    @Test
    void lesLieuxLisentLesDeuxReponses() {
        assertEquals(List.of(), CorrespondanceDocuments.lireLieux(null));
        Location lieu = new Location(URI,
                new Range(new Position(0, 1), new Position(0, 2)));
        assertEquals(1, CorrespondanceDocuments.lireLieux(
                Either.forLeft(List.of(lieu))).size());
        LocationLink lien = new LocationLink();
        lien.setTargetUri(URI);
        lien.setTargetRange(new Range(new Position(2, 0), new Position(2, 3)));
        List<LieuLsp> lus = CorrespondanceDocuments.lireLieux(
                Either.forRight(List.of(lien)));
        assertEquals(1, lus.size());
        jo.celeris.core.lsp.types.PlageLsp plageCible = lus.get(0).plage();
        assertNotNull(plageCible);
        assertEquals(new PositionLsp(2, 0), plageCible.debut());
    }

    @Test
    void lesCapacitesLisentLeursDeuxSyncs() {
        CapacitesServeur vides = CorrespondanceLsp.lireCapacites(null);
        assertEquals(-1, vides.genreSynchro());
        assertFalse(vides.sertCompletion());

        ServerCapabilities complete = new ServerCapabilities();
        complete.setTextDocumentSync(Either.forLeft(TextDocumentSyncKind.Full));
        complete.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions());
        assertEquals(1,
                CorrespondanceLsp.lireCapacites(complete).genreSynchro());
        assertTrue(CorrespondanceLsp.lireCapacites(complete).sertCompletion());

        ServerCapabilities incrementale = new ServerCapabilities();
        incrementale.setTextDocumentSync(Either.forRight(new TextDocumentSyncOptions()));
        assertEquals(-1,
                CorrespondanceLsp.lireCapacites(incrementale).genreSynchro());

        ServerCapabilities sansLegende = new ServerCapabilities();
        sansLegende.setSemanticTokensProvider(
                new SemanticTokensWithRegistrationOptions());
        assertEquals(List.of(),
                CorrespondanceLsp.lireCapacites(sansLegende).typesJetons());

        TextDocumentSyncOptions options = new TextDocumentSyncOptions();
        options.setChange(TextDocumentSyncKind.Incremental);
        ServerCapabilities avecLegende = new ServerCapabilities();
        avecLegende.setTextDocumentSync(Either.forRight(options));
        avecLegende.setSemanticTokensProvider(
                new SemanticTokensWithRegistrationOptions());
        avecLegende.getSemanticTokensProvider().setLegend(
                new org.eclipse.lsp4j.SemanticTokensLegend(
                        List.of("type"), List.of("declaration")));
        CapacitesServeur lues = CorrespondanceLsp.lireCapacites(avecLegende);
        assertEquals(2, lues.genreSynchro());
        assertEquals(List.of("type"), lues.typesJetons());
        assertEquals(List.of("declaration"), lues.modificateursJetons());
    }
}
