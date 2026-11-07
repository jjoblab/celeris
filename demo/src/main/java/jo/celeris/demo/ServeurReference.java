package jo.celeris.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.ErreurRpc;
import jo.celeris.core.lsp.JsonBooleen;
import jo.celeris.core.lsp.JsonChaine;
import jo.celeris.core.lsp.JsonEcrivain;
import jo.celeris.core.lsp.JsonLecteur;
import jo.celeris.core.lsp.JsonNombre;
import jo.celeris.core.lsp.JsonNul;
import jo.celeris.core.lsp.JsonObjet;
import jo.celeris.core.lsp.JsonTableau;
import jo.celeris.core.lsp.MessageRpc;
import jo.celeris.core.lsp.NotificationRpc;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.ReponseRpc;
import jo.celeris.core.lsp.RequeteRpc;
import jo.celeris.core.lsp.Transport;
import jo.celeris.core.lsp.TransportMemoire;
import jo.celeris.core.lsp.ValeurJson;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le serveur de langage de référence, EN PROCESSUS (M7.5, §8) : la
 * démonstration y branche son client LSP par le transport mémoire —
 * livraison synchrone, aucun fil, aucun réseau. Il suit la
 * synchronisation complète (didOpen, didChange, didClose) et répond à
 * l'aide aux paramètres (B-07) et au survol (B-09) : l'appel ouvert
 * sous la position sert sa signature connue, le symbole survolé sert
 * sa documentation — les tables du terrain, servies par le protocole
 * cette fois, pas par l'appel direct.
 */
public final class ServeurReference implements AutoCloseable {

    private final Transport transport;
    private final Map<String, String> documents = new HashMap<>();

    private ServeurReference(Transport transport) {
        this.transport = transport;
        transport.brancher(this::servir);
    }

    /**
     * Branche le serveur de référence sur le bout serveur d'une paire
     * en mémoire.
     *
     * @param paire la paire créée pour la session de langage
     * @return le serveur branché
     */
    public static ServeurReference brancher(TransportMemoire.Paire paire) {
        return new ServeurReference(paire.boutB());
    }

    private void servir(String trame) {
        MessageRpc message = MessageRpc.de(JsonLecteur.lire(trame));
        if (message instanceof NotificationRpc notification) {
            suivre(notification);
        } else if (message instanceof RequeteRpc requete) {
            repondre(requete);
        }
    }

    /** La synchronisation complète : chaque notification remplace l'état du document. */
    private void suivre(NotificationRpc notification) {
        JsonObjet params = objet(notification.params());
        String uri = uriDe(params);
        if (uri == null) {
            return;
        }
        switch (notification.methode()) {
            case "textDocument/didOpen" -> documents.put(uri,
                    texteDeDidOpen(params));
            case "textDocument/didChange" -> documents.put(uri,
                    texteDeDidChange(params));
            case "textDocument/didClose" -> documents.remove(uri);
            default -> {
                // les autres notifications ne concernent pas le terrain
            }
        }
    }

    private void repondre(RequeteRpc requete) {
        switch (requete.methode()) {
            case "initialize" -> envoyer(requete.id(), JsonObjet.vide()
                    .ajouter("capabilities", JsonObjet.vide()
                            .ajouter("hoverProvider", JsonBooleen.VRAI)
                            .ajouter("signatureHelpProvider", JsonObjet.vide()
                                    .ajouter("triggerCharacters",
                                            JsonTableau.vide()
                                                    .ajouter(JsonChaine.of("("))))));
            case "shutdown" -> envoyer(requete.id(), JsonNul.nul());
            case "textDocument/signatureHelp" -> envoyer(requete.id(),
                    aideParametres(objet(requete.params())));
            case "textDocument/hover" -> envoyer(requete.id(),
                    survol(objet(requete.params())));
            default -> envoyerErreur(requete.id(), requete.methode());
        }
    }

    /**
     * L'aide aux paramètres : l'appel ouvert sous la position sert sa
     * signature — la table du terrain, la forme du protocole (B-07).
     */
    private ValeurJson aideParametres(JsonObjet params) {
        Document document = documentSuivi(params);
        PositionLsp position = positionDe(params);
        if (document == null || position == null) {
            return JsonNul.nul();
        }
        SignatureAideModel modele = PopupCoach.signatureFor(document,
                PositionsLsp.offset(document, position));
        if (modele == null) {
            return JsonNul.nul();
        }
        JsonTableau parametres = JsonTableau.vide();
        for (String libelle : parametresDuLibelle(modele.libelle())) {
            // ajouter rend un NOUVEAU tableau — le retour se garde
            parametres = parametres.ajouter(JsonObjet.vide()
                    .ajouter("label", JsonChaine.of(libelle)));
        }
        JsonObjet signature = JsonObjet.vide()
                .ajouter("label", JsonChaine.of(modele.libelle()))
                .ajouter("parameters", parametres);
        return JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide().ajouter(signature))
                .ajouter("activeSignature", JsonNombre.of(0))
                .ajouter("activeParameter",
                        JsonNombre.of(modele.parametreCourant()));
    }

    /**
     * Le survol : la documentation du symbole à la position — le
     * contenu balisé markdown, la forme du protocole (B-09).
     */
    private ValeurJson survol(JsonObjet params) {
        Document document = documentSuivi(params);
        PositionLsp position = positionDe(params);
        if (document == null || position == null) {
            return JsonNul.nul();
        }
        String symbole = PopupCoach.symboleA(document,
                PositionsLsp.offset(document, position));
        String documentation = symbole == null ? null
                : PopupCoach.documentationBrute(symbole);
        if (documentation == null) {
            return JsonNul.nul();
        }
        return JsonObjet.vide()
                .ajouter("contents", JsonObjet.vide()
                        .ajouter("kind", JsonChaine.of("markdown"))
                        .ajouter("value", JsonChaine.of(documentation)));
    }

    /** Les paramètres d'un libellé de signature — les noms, dans l'ordre des virgules. */
    private static List<String> parametresDuLibelle(String libelle) {
        List<String> noms = new ArrayList<>();
        int ouverture = libelle.indexOf('(');
        int fermeture = libelle.lastIndexOf(')');
        if (ouverture < 0 || fermeture <= ouverture) {
            return noms;
        }
        for (String morceau : libelle.substring(ouverture + 1, fermeture)
                .split(",")) {
            String nom = morceau.strip();
            int espace = nom.lastIndexOf(' ');
            noms.add(espace < 0 ? nom : nom.substring(espace + 1));
        }
        return noms;
    }

    /** Le document suivi d'une requête — null quand l'uri manque ou n'est pas suivie. */
    private @Nullable Document documentSuivi(JsonObjet params) {
        String uri = uriDe(params);
        String texte = uri == null ? null : documents.get(uri);
        return texte == null ? null
                : TextSession.of(texte, () -> 0L).document();
    }

    private static @Nullable PositionLsp positionDe(JsonObjet params) {
        if (params.membre("position") instanceof JsonObjet position) {
            return PositionLsp.de(position);
        }
        return null;
    }

    private static @Nullable String uriDe(JsonObjet params) {
        if (params.membre("textDocument") instanceof JsonObjet texteDoc) {
            return texteDoc.membreTexte("uri");
        }
        return null;
    }

    private static String texteDeDidOpen(JsonObjet params) {
        if (params.membre("textDocument") instanceof JsonObjet texteDoc) {
            String texte = texteDoc.membreTexte("text");
            if (texte != null) {
                return texte;
            }
        }
        return "";
    }

    private static String texteDeDidChange(JsonObjet params) {
        if (params.membre("contentChanges") instanceof JsonTableau changes
                && changes.taille() > 0
                && changes.element(0) instanceof JsonObjet changement) {
            String texte = changement.membreTexte("text");
            if (texte != null) {
                return texte;
            }
        }
        return "";
    }

    private static JsonObjet objet(@Nullable ValeurJson valeur) {
        return valeur instanceof JsonObjet objet ? objet : JsonObjet.vide();
    }

    private void envoyer(long id, ValeurJson resultat) {
        transport.envoyer(JsonEcrivain.ecrire(
                new ReponseRpc(id, resultat, null).versJson()));
    }

    private void envoyerErreur(long id, String methode) {
        transport.envoyer(JsonEcrivain.ecrire(new ReponseRpc(id, null,
                new ErreurRpc(ErreurRpc.METHOD_NOT_FOUND,
                        "method not found: " + methode, null)).versJson()));
    }

    @Override
    public void close() {
        transport.close();
    }
}
