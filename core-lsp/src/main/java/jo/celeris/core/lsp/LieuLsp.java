package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un lieu de navigation (B-10, B-11) : l'uri et la plage d'une cible —
 * la définition donnée par le serveur, une référence raffinée. Le lieu
 * est ce que l'historique de navigation M5-t4 consomme : la cible
 * visitée s'y empile, le retour au point de départ s'y lit. La règle
 * des rôles (ADR-005) tient : l'index hors ligne répond vite, le
 * serveur répond juste, jamais de chaînage.
 *
 * @param uri   l'uri du fichier cible, vide si le serveur ne la donne pas
 * @param plage la plage de la cible dans son fichier, nulle si le
 *              serveur ne la donne pas — jamais devinée
 */
public record LieuLsp(String uri, @Nullable PlageLsp plage) {

    /**
     * Analyse le lieu depuis sa forme JSON.
     *
     * @param json l'objet lieu de la réponse du serveur
     * @return le lieu lu — uri vide et plage nulle si la forme est
     *         incomplète
     */
    public static LieuLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new LieuLsp("", null);
        }
        String uri = objet.membreTexte("uri");
        PlageLsp plage = null;
        if (objet.membre("range") instanceof JsonObjet portee) {
            plage = PlageLsp.de(portee);
        }
        return new LieuLsp(uri == null ? "" : uri, plage);
    }

    /**
     * Analyse la liste des lieux de la réponse — le tableau dans
     * l'ordre du serveur, l'objet seul toléré, le vide sans casse.
     *
     * @param json la valeur « result » de la requête
     * @return les lieux lus, vide si la réponse est vide ou nulle
     */
    public static List<LieuLsp> liste(ValeurJson json) {
        List<LieuLsp> lieux = new ArrayList<>();
        if (json instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                lieux.add(de(tableau.element(i)));
            }
        } else if (json instanceof JsonObjet) {
            lieux.add(de(json));
        }
        return List.copyOf(lieux);
    }
}
