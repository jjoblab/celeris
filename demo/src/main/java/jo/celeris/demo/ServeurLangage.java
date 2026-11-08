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
 * La session de langage de référence de la démonstration (M7.5, §8) : le
 * client LSP branché au serveur de référence EN PROCESSUS — transport
 * mémoire, livraison synchrone, aucun réseau. Les popups y demandent
 * l'aide aux paramètres et le survol par les futurs du protocole ;
 * la synchronisation complète suit la session à chaque édition —
 * l'ordonnanceur annule et remplace, les frappes rapides ne
 * s'empilent jamais (PIEGE-18).
 */
public final class ServeurLangage implements LanguageSession {

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

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        client.ouvrir(uri, langue, texte);
    }

    @Override
    public void synchroniser(String uri, String texte) {
        client.changer(uri, ++version, texte);
    }

    @Override
    public CompletableFuture<@Nullable SignatureAideModel> signature(
            String uri, Document doc, int caret) {
        return ordonnanceur.signature(uri, PositionsLsp.position(doc, caret))
                .thenApply(ServeurLangage::enModele);
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(
            String uri, Document doc, String symbole, int caret) {
        return ordonnanceur.survol(uri, PositionsLsp.position(doc, caret))
                .thenApply(survol -> enModele(survol, symbole));
    }

    @Override
    public boolean sertCompletion() {
        // la complétion reste l'affaire des mots du code — le serveur
        // de référence ne sert que la signature et le survol
        return false;
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.ListeCompletion> completion(
            String uri, Document doc, int caret) {
        throw new UnsupportedOperationException(
                "le serveur de référence ne sert pas la complétion");
    }

    @Override
    public void poserAuditeurDiagnostics(
            java.util.function.BiConsumer<String,
                    java.util.List<jo.celeris.core.lsp.DiagnosticLsp>> auditeur) {
        // le serveur de référence ne publie jamais de diagnostics
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
