package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.ClientLsp;
import jo.celeris.core.lsp.JsonObjet;
import jo.celeris.core.lsp.OrdonnanceurLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;
import jo.celeris.core.lsp.TransportMemoire;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La session de langage de la démonstration (M7.5, §8) : le client
 * LSP branché au serveur de référence EN PROCESSUS — transport
 * mémoire, livraison synchrone, aucun réseau. Les popups y demandent
 * l'aide aux paramètres et le survol par les futurs du protocole ;
 * la synchronisation complète suit la session à chaque édition —
 * l'ordonnanceur annule et remplace, les frappes rapides ne
 * s'empilent jamais (PIEGE-18).
 */
public final class ServeurLangage implements AutoCloseable {

    private final TransportMemoire.Paire paire;
    private final ServeurReference serveur;
    private final ClientLsp client;
    private final OrdonnanceurLsp ordonnanceur;
    private int version = 1;

    private ServeurLangage(TransportMemoire.Paire paire,
            ServeurReference serveur, ClientLsp client,
            OrdonnanceurLsp ordonnanceur) {
        this.paire = paire;
        this.serveur = serveur;
        this.client = client;
        this.ordonnanceur = ordonnanceur;
    }

    /**
     * Démarre la session de langage : la paire en mémoire, le serveur
     * de référence sur un bout, le client poignée-de-main sur l'autre.
     *
     * @return la session démarrée, prête à ouvrir un document
     */
    public static ServeurLangage demarrer() {
        TransportMemoire.Paire paire = TransportMemoire.creerPaire();
        ServeurReference serveur = ServeurReference.brancher(paire);
        ClientLsp client = ClientLsp.sur(paire.boutA(), "celeris-demo");
        client.initialiser(JsonObjet.vide());
        return new ServeurLangage(paire, serveur, client,
                OrdonnanceurLsp.sur(client));
    }

    /**
     * Ouvre le document suivi — version 1, synchronisation complète.
     *
     * @param uri    l'uri du document
     * @param langue l'identifiant du langage
     * @param texte  le contenu initial
     */
    public void ouvrir(String uri, String langue, String texte) {
        client.ouvrir(uri, langue, texte);
    }

    /**
     * La synchronisation complète : le texte entier, la version
     * suivante — l'état du serveur suit la session d'édition.
     *
     * @param uri   l'uri du document
     * @param texte le contenu complet courant
     */
    public void synchroniser(String uri, String texte) {
        client.changer(uri, ++version, texte);
    }

    /**
     * L'aide aux paramètres au curseur — le futur du modèle de la
     * vue, déjà converti (B-07) ; null dans le futur sans signature.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur du modèle, null si le serveur n'en dit rien
     */
    public CompletableFuture<@Nullable SignatureAideModel> signature(
            String uri, Document doc, int caret) {
        return ordonnanceur.signature(uri, PositionsLsp.position(doc, caret))
                .thenApply(ServeurLangage::enModele);
    }

    /**
     * Le survol du symbole au curseur — le futur du modèle de
     * documentation, déjà converti (B-09) ; null sans documentation.
     *
     * @param uri      l'uri du document
     * @param doc      l'instantané courant
     * @param symbole  le symbole survolé — le titre du modèle
     * @param caret    la position du curseur
     * @return le futur du modèle, null si le serveur n'en dit rien
     */
    public CompletableFuture<@Nullable CompletionDocModel> survol(
            String uri, Document doc, String symbole, int caret) {
        return ordonnanceur.survol(uri, PositionsLsp.position(doc, caret))
                .thenApply(survol -> enModele(survol, symbole));
    }

    /**
     * La conversion : la signature du protocole au modèle de la vue —
     * l'entrée active, ses paramètres, le paramètre courant borné.
     */
    static @Nullable SignatureAideModel enModele(SignatureLsp lsp) {
        SignatureLsp.Entree active = lsp.signatureActive();
        if (active == null) {
            return null;
        }
        List<String> libelles = new ArrayList<>();
        for (SignatureLsp.Parametre parametre : active.parametres()) {
            libelles.add(parametre.libelle());
        }
        return SignatureAideModel.of(active.libelle(), libelles,
                lsp.parametreCourant());
    }

    /** La conversion : le survol du protocole au modèle de documentation. */
    static @Nullable CompletionDocModel enModele(SurvolLsp survol,
            String symbole) {
        if (survol.texte().isEmpty()) {
            return null;
        }
        return CompletionDocModel.of(symbole, survol.texte());
    }

    @Override
    public void close() {
        client.close();
        serveur.close();
        paire.boutA().close();
        paire.boutB().close();
    }
}
