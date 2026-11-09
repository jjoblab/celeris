package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jo.celeris.core.lsp.CapacitesServeur;
import jo.celeris.core.lsp.ElementCompletion;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;

/**
 * Le corpus de traces du jalon (sortie M6, ADR-005) lu par la frontière
 * protocolaire (ADR-015) : les réponses enregistrées de serveurs réels
 * — l'initialisation, la complétion tronquée, le survol balisé, la
 * signature aux paramètres par bornes, la définition multiple — passent
 * par le Gson de la bibliothèque puis par la correspondance vers les
 * types du domaine : chaque décodeur tient la forme enregistrée, toute
 * divergence future casse la construction. Contenu original, aucune
 * pièce tierce (neutralité, ADR-011).
 *
 * <p>L'enquête Gson de l'étape s'y lit : les quatre traces sans
 * alternative ambiguë se lisent directement par le Gson de la
 * bibliothèque ; la définition — un {@code Either} de deux listes — se
 * lit en {@code List<Location>} puis se pose en
 * {@code Either.forLeft} : sans le résolveur de requêtes qui, en
 * production, connaît le type de retour de la méthode appelée, le
 * lecteur refuse le choix (« ambiguous Either type »).</p>
 *
 * <p>Les deux traces restantes du corpus attendent la couverture
 * nouvelle (H-12) : les indices rejoindront la correspondance quand
 * elle saura lire les {@code InlayHint} ; l'édition d'espace porte
 * l'édit de gabarit {@code celeris/templateEdit} — une forme nôtre que
 * les types standard de la bibliothèque refusent (« kind » non
 * standard) — sa lecture reste du côté du domaine.</p>
 */
public final class CorpusTracesTest {

    /**
     * Le Gson de la bibliothèque : les adaptateurs du protocole compris
     * (les {@code Either} y suivent la forme JSON), sans résolveur de
     * requêtes — le lecteur des traces se donne le type, comme la
     * correspondance en production.
     */
    private static final Gson GSON = new MessageJsonHandler(Map.of()).getGson();

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
