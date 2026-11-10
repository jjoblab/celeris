package jo.celeris.lsp.protocole;

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

import jo.celeris.core.lsp.CapacitesServeur;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionGabaritLsp;
import jo.celeris.core.lsp.ElementCompletion;
import jo.celeris.core.lsp.IndiceLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;

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
        CapacitesServeur capacites =
                CorrespondanceLsp.lireCapacites(reponse.getCapabilities());
        assertEquals(1, capacites.genreSynchro());
        assertTrue(capacites.sertCompletion());
        assertTrue(capacites.sertSurvol());
        assertTrue(capacites.sertSignature());
        assertTrue(capacites.sertDefinition());
        assertTrue(capacites.sertReferences());
        assertTrue(capacites.sertFormatage());
        assertTrue(capacites.sertIndices(), "inlayHintProvider lu");
        assertEquals(List.of("keyword", "variable", "parameter", "function", "type"),
                capacites.typesJetons());
        assertEquals(List.of("declaration", "static", "deprecated"),
                capacites.modificateursJetons());
    }

    /** La complétion tronquée : la coupure honorée, l'ordre du serveur tenu (PIEGE-17). */
    @Test
    void laTraceCompletionTronquee() {
        CompletionList reponse =
                GSON.fromJson(trace("completion-tronquee"), CompletionList.class);
        ListeCompletion liste = CorrespondanceDocuments.lireListe(
                Either.forRight(reponse));
        assertTrue(liste.estIncomplete());
        assertEquals(3, liste.elements().size());
        assertEquals("ajouter", liste.elements().get(0).libelle());
        // la liste tronquée ne se trie jamais — la même revient
        assertEquals(liste, liste.trier());
        // le filtrage garde l'ordre relatif, n'ajoute rien
        assertEquals(List.of("ajouter", "ajuster"),
                liste.filtrer("aj").elements().stream()
                        .map(ElementCompletion::libelle).toList());
    }

    /** Le survol : le contenu balisé et la plage, résolus à la demande. */
    @Test
    void laTraceSurvol() {
        Hover reponse = GSON.fromJson(trace("survol"), Hover.class);
        SurvolLsp survol = CorrespondanceDocuments.lireSurvol(reponse);
        assertEquals("markdown", survol.genre());
        assertTrue(survol.texte().startsWith("fun ajouter"));
        assertTrue(survol.texte().contains("Ajoute deux entiers."));
        PlageLsp plage = survol.plage();
        assertNotNull(plage);
        assertEquals(new PositionLsp(2, 10), plage.debut());
        assertEquals(new PositionLsp(2, 17), plage.fin());
    }

    /** La signature : paramètres par bornes, l'actif au-delà de l'arité retombe au dernier. */
    @Test
    void laTraceSignature() {
        SignatureHelp reponse =
                GSON.fromJson(trace("signature"), SignatureHelp.class);
        SignatureLsp aide = CorrespondanceDocuments.lireSignature(reponse);
        assertEquals(1, aide.entrees().size());
        SignatureLsp.Entree entree = aide.entrees().get(0);
        assertEquals("ajouter(a: Int, b: Int, c: Int): Int", entree.libelle());
        assertEquals(List.of("a", "b", "c"), entree.parametres().stream()
                .map(SignatureLsp.Parametre::libelle).toList());
        assertEquals("le premier terme", entree.parametres().get(0).documentation());
        assertEquals(2, aide.parametreCourant(),
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
        List<LieuLsp> lieux = CorrespondanceDocuments.lireLieux(
                Either.forLeft(enregistres));
        assertEquals(2, lieux.size());
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt",
                lieux.get(0).uri());
        PlageLsp seconde = lieux.get(1).plage();
        assertNotNull(seconde);
        assertEquals(new PositionLsp(40, 2), seconde.debut());
    }

    /** Les indices : parties cliquables, la chaîne nue, les remplissages. */
    @Test
    void laTraceIndices() {
        List<IndiceLsp> indices = CorrespondanceCouverture.lireIndices(
                GSON.fromJson(trace("indices"),
                        INLAY_HINTS));
        assertEquals(2, indices.size());
        IndiceLsp premier = indices.get(0);
        assertEquals(3, premier.parties().size());
        assertEquals(1, premier.genre());
        assertTrue(premier.remplissageGauche());
        assertFalse(premier.remplissageDroite());
        LieuLsp cible = premier.parties().get(1).cible();
        assertNotNull(cible, "la partie « : » porte sa cible de navigation");
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt", cible.uri());
        IndiceLsp second = indices.get(1);
        assertEquals(1, second.parties().size());
        assertEquals("a =", second.parties().get(0).valeur());
        assertTrue(second.remplissageDroite());
    }

    /**
     * L'édition d'espace : le document édité ET l'édit de gabarit —
     * la forme de la 1.0.0, le bloqué historique débloqué (ADR-015).
     */
    @Test
    void laTraceEditionEspace() {
        EditionEspaceLsp espace = CorrespondanceEspace.lireEspace(
                GSON.fromJson(trace("edition-espace"),
                        WorkspaceEdit.class));
        assertEquals(1, espace.documents().size());
        assertEquals(7, espace.documents().get(0).version());
        assertEquals("additionner", espace.documents().get(0).editions()
                .get(0).nouveauTexte());
        assertEquals(1, espace.gabarits().size(), "l'edit de gabarit vit dans la trace");
        EditionGabaritLsp gabarit = espace.gabarits().get(0);
        assertEquals("file:///src/main/java/com/exemple/app/Calcul.kt",
                gabarit.uri());
        assertEquals(new PositionLsp(20, 4), gabarit.position());
        assertTrue(gabarit.corps().contains("${nom:soustraire}"));
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
