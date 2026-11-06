package jo.celeris.core.lsp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * L'ordonnancement des requêtes LSP (ADR-005) : annulation, priorités,
 * fusion, resynchronisation — la couche qu'il aurait fallu écrire
 * au-dessus de toute liaison tierce, la raison d'être du client
 * propre.
 *
 * <ul>
 *   <li><b>Fusion</b> : deux demandes identiques en vol ne font
 *   QU'UNE requête sur le fil — les deux appelants partagent le
 *   futur gardé ;</li>
 *   <li><b>Annulation</b> : une demande plus fraîche de la même
 *   famille (clé uri + famille) remplace celle en vol — notification
 *   $/cancelRequest au serveur, le futur remplacé échoue
 *   (REMPLACÉE) ;</li>
 *   <li><b>Priorités</b> : une demande interactive devance toute
 *   demande de fond en vol du même document (DEVANCÉE) — jamais
 *   l'inverse ;</li>
 *   <li><b>Resynchronisation</b> : une réponse qui arrive après un
 *   changement de version du document est périmée (PÉRIMÉE) —
 *   l'appelant l'apprend, il re-émet s'il veut.</li>
 * </ul>
 *
 * <p>Les règles ne sont pas des options : les frappes rapides
 * déclenchent des complétions en cascade, seule la dernière compte —
 * le dépôt de départ laissait tout partir et attendait tout.</p>
 */
public final class OrdonnanceurLsp {

    /** La priorité d'une demande : interactive (l'utilisateur attend) ou de fond. */
    public enum Priorite {

        /** L'utilisateur attend la réponse : survol, complétion, signature. */
        INTERACTIF,

        /** Travail de fond : formatage, jetons, symboles d'espace de travail. */
        FOND
    }

    /** La clé de remplacement : un document, une famille de demande. */
    public record Cle(String uri, String famille) {
    }

    /** Une requête en vol : son identifiant, son futur brut, sa priorité. */
    private static final class EnVol {
        private final long id;
        private final CompletableFuture<ValeurJson> brut;
        private final Priorite priorite;

        EnVol(long id, CompletableFuture<ValeurJson> brut, Priorite priorite) {
            this.id = id;
            this.brut = brut;
            this.priorite = priorite;
        }
    }

    /** La signature de fusion : même méthode, mêmes paramètres. */
    private record Signature(String methode, String parametres) {
    }

    private final ClientLsp client;
    private final Map<Signature, CompletableFuture<ValeurJson>> fusions =
            new ConcurrentHashMap<>();
    private final Map<Cle, EnVol> enVol = new ConcurrentHashMap<>();

    private OrdonnanceurLsp(ClientLsp client) {
        this.client = client;
    }

    /**
     * Crée l'ordonnanceur sur un client initialisé.
     *
     * @param client le client LSP
     * @return l'ordonnanceur prêt à servir
     */
    public static OrdonnanceurLsp sur(ClientLsp client) {
        return new OrdonnanceurLsp(client);
    }

    /**
     * La complétion typée (B-04) : requête au serveur par la clé du
     * document, priorité interactive — la réponse est rendue au
     * modèle, la coupure du serveur honorée (PIEGE-17 : jamais
     * réordonnée ni complétée localement).
     *
     * @param uri      l'uri du document
     * @param position la position du curseur
     * @return le futur de la liste de complétion
     */
    public CompletableFuture<ListeCompletion> completion(String uri, PositionLsp position) {
        return demander("textDocument/completion", parametresPosition(uri, position),
                Priorite.INTERACTIF, new Cle(uri, "completion"))
                .thenApply(ListeCompletion::de);
    }

    /**
     * Le survol (B-09) : la documentation se RÉSOUT À LA DEMANDE — la
     * requête part à l'appel, jamais avant — priorité interactive.
     *
     * @param uri      l'uri du document
     * @param position la position survolée
     * @return le futur du survol
     */
    public CompletableFuture<SurvolLsp> survol(String uri, PositionLsp position) {
        return demander("textDocument/hover", parametresPosition(uri, position),
                Priorite.INTERACTIF, new Cle(uri, "survol"))
                .thenApply(SurvolLsp::de);
    }

    /**
     * La définition (B-10) : les lieux donnés par le serveur — le
     * chemin en ligne de la navigation, le retour au point de départ
     * par l'historique M5-t4.
     *
     * @param uri      l'uri du document
     * @param position la position du symbole
     * @return le futur des lieux de définition
     */
    public CompletableFuture<List<LieuLsp>> definition(String uri, PositionLsp position) {
        return demander("textDocument/definition", parametresPosition(uri, position),
                Priorite.INTERACTIF, new Cle(uri, "definition"))
                .thenApply(LieuLsp::liste);
    }

    /**
     * Les références (B-11) : le raffinement serveur des usages — le
     * contexte exige les déclarations.
     *
     * @param uri      l'uri du document
     * @param position la position du symbole
     * @return le futur des lieux de référence
     */
    public CompletableFuture<List<LieuLsp>> references(String uri, PositionLsp position) {
        return demander("textDocument/references",
                parametresPosition(uri, position)
                        .ajouter("context", JsonObjet.vide()
                                .ajouter("includeDeclaration", JsonBooleen.VRAI)),
                Priorite.INTERACTIF, new Cle(uri, "references"))
                .thenApply(LieuLsp::liste);
    }

    /**
     * L'aide aux paramètres (B-07) : les signatures du serveur, la
     * signature active et le paramètre courant borné — priorité
     * interactive, l'utilisateur tape.
     *
     * @param uri      l'uri du document
     * @param position la position du curseur dans l'appel
     * @return le futur de l'aide aux paramètres
     */
    public CompletableFuture<SignatureLsp> signature(String uri, PositionLsp position) {
        return demander("textDocument/signatureHelp", parametresPosition(uri, position),
                Priorite.INTERACTIF, new Cle(uri, "signature"))
                .thenApply(SignatureLsp::de);
    }

    /**
     * Les indices en ligne (B-08) : le serveur les rend pour la plage
     * visible — priorité DE FOND, l'interactif du même document les
     * devance (ADR-007).
     *
     * @param uri   l'uri du document
     * @param plage la plage visible demandée
     * @return le futur des indices de la plage
     */
    public CompletableFuture<List<IndiceLsp>> indices(String uri, PlageLsp plage) {
        return demander("textDocument/inlayHint",
                JsonObjet.vide()
                        .ajouter("textDocument", JsonObjet.vide()
                                .ajouter("uri", JsonChaine.of(uri)))
                        .ajouter("range", plage.versJson()),
                Priorite.FOND, new Cle(uri, "indices"))
                .thenApply(IndiceLsp::liste);
    }

    /**
     * Le renommage au serveur (B-19) : l'édition d'espace de travail
     * qu'il propose — documents et gabarits compris.
     *
     * @param uri        l'uri du document
     * @param position   la position du symbole à renommer
     * @param nouveauNom le nom qui remplace l'ancien
     * @return le futur de l'édition d'espace proposée
     */
    public CompletableFuture<EditionEspaceLsp> renommage(String uri, PositionLsp position,
                                                         String nouveauNom) {
        return demander("textDocument/rename",
                parametresPosition(uri, position)
                        .ajouter("newName", JsonChaine.of(nouveauNom)),
                Priorite.INTERACTIF, new Cle(uri, "renommage"))
                .thenApply(EditionEspaceLsp::de);
    }

    /** Les paramètres communs : le document porteur et la position visée. */
    private static JsonObjet parametresPosition(String uri, PositionLsp position) {
        return JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri)))
                .ajouter("position", position.versJson());
    }

    /**
     * Les correctifs rapides du serveur (B-17) : les titres à montrer,
     * les éditions à appliquer — par la voie de transaction, annulables.
     *
     * @param uri   l'uri du document
     * @param plage la plage visée par le correctif
     * @return le futur des correctifs proposés
     */
    public CompletableFuture<List<CorrectifLsp>> correctifs(String uri, PlageLsp plage) {
        return demander("textDocument/codeAction",
                JsonObjet.vide()
                        .ajouter("textDocument", JsonObjet.vide()
                                .ajouter("uri", JsonChaine.of(uri)))
                        .ajouter("range", plage.versJson())
                        .ajouter("context", JsonObjet.vide()
                                .ajouter("diagnostics", JsonTableau.vide())),
                Priorite.INTERACTIF, new Cle(uri, "correctifs"))
                .thenApply(CorrectifLsp::liste);
    }

    /**
     * Demande au serveur sous les quatre règles.
     *
     * @param methode  la méthode invoquée
     * @param params   les paramètres, ou null
     * @param priorite la priorité de la demande
     * @param cle      la clé de remplacement (uri + famille), ou null
     *                 pour une demande hors concurrence
     * @return le futur de la réponse — il peut échouer en
     *         {@link RequeteAnnuleeException}
     */
    public CompletableFuture<ValeurJson> demander(String methode,
            @Nullable JsonObjet params, Priorite priorite, @Nullable Cle cle) {
        nettoyer();
        Signature signature = new Signature(methode,
                params == null ? "" : JsonEcrivain.ecrire(params));
        CompletableFuture<ValeurJson> fusionnee = fusions.get(signature);
        if (fusionnee != null) {
            return fusionnee;
        }
        CompletableFuture<ValeurJson> brut = new CompletableFuture<>();
        if (cle != null) {
            remplacerEnVol(cle);
        }
        if (priorite == Priorite.INTERACTIF) {
            devancerLeFond(cle);
        }
        long id = client.requeter(methode, params, brut);
        return enregistrer(signature, cle, id, brut, priorite);
    }

    /** Élague les entrées terminées — les cartes ne portent que le vol, jamais l'histoire. */
    private void nettoyer() {
        fusions.values().removeIf(CompletableFuture::isDone);
        enVol.values().removeIf(enVol -> enVol.brut.isDone());
    }

    private void remplacerEnVol(Cle cle) {
        EnVol ancienne = enVol.get(cle);
        if (ancienne != null) {
            annuler(ancienne, RequeteAnnuleeException.Raison.REMPLACEE);
        }
    }

    private void devancerLeFond(@Nullable Cle cle) {
        if (cle == null) {
            return;
        }
        for (Map.Entry<Cle, EnVol> entree : enVol.entrySet()) {
            if (entree.getKey().uri().equals(cle.uri())
                    && entree.getValue().priorite == Priorite.FOND) {
                annuler(entree.getValue(), RequeteAnnuleeException.Raison.DEVANCEE);
            }
        }
    }

    private void annuler(EnVol enVol, RequeteAnnuleeException.Raison raison) {
        client.notifier("$/cancelRequest", JsonObjet.vide()
                .ajouter("id", JsonNombre.of(enVol.id)));
        enVol.brut.completeExceptionally(new RequeteAnnuleeException(raison));
    }

    private CompletableFuture<ValeurJson> enregistrer(Signature signature,
            @Nullable Cle cle, long id, CompletableFuture<ValeurJson> brut,
            Priorite priorite) {
        CompletableFuture<ValeurJson> garde = cle == null ? brut : garder(brut, cle);
        fusions.put(signature, garde);
        if (cle != null) {
            enVol.put(cle, new EnVol(id, brut, priorite));
        }
        return garde;
    }

    /** La garde de version : la réponse doit être celle du document qu'on a quitté. */
    private CompletableFuture<ValeurJson> garder(CompletableFuture<ValeurJson> brut, Cle cle) {
        int versionAuDepart = client.versionDe(cle.uri());
        return brut.thenApply(resultat -> {
            if (client.versionDe(cle.uri()) != versionAuDepart) {
                throw new RequeteAnnuleeException(RequeteAnnuleeException.Raison.PERIMEE);
            }
            return resultat;
        });
    }
}
