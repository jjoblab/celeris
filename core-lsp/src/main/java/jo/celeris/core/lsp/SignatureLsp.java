package jo.celeris.core.lsp;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * L'aide aux paramètres du serveur (B-07) : les signatures candidates,
 * la signature active et le paramètre courant — le surlignage du
 * paramètre en cours de frappe. Le paramètre courant est BORNÉ à
 * l'arité de la signature active : les serveurs généreux qui envoient
 * un indice au-delà ne cassent pas le surlignage, il retombe sur le
 * dernier. Les champs absents restent nuls ou négatifs, jamais devinés.
 * La lecture du fil — chaîne nue ou décalages bornés — vit derrière la
 * frontière protocolaire (ADR-015, CorrespondanceDocuments).
 */
public final class SignatureLsp {

    /** Un paramètre de signature : son libellé, sa documentation. */
    public record Parametre(String libelle, @Nullable String documentation) {
    }

    /** Une signature candidate : son libellé, ses paramètres dans l'ordre. */
    public record Entree(String libelle, List<Parametre> parametres,
                         @Nullable String documentation) {

        /** Les paramètres copiés, profonds pour l'auditeur. */
        public Entree {
            parametres = List.copyOf(parametres);
        }
    }

    private static final int ABSENT = -1;

    private final List<Entree> entrees;
    private final int signatureActive;
    private final int parametreActif;

    private SignatureLsp(List<Entree> entrees, int signatureActive, int parametreActif) {
        this.entrees = List.copyOf(entrees);
        this.signatureActive = signatureActive;
        this.parametreActif = parametreActif;
    }

    /**
     * Construit l'aide aux paramètres de ses entrées résolues — la voie
     * typée de la frontière protocolaire (ADR-015) : la correspondance
     * y dépose les signatures déjà lues de la bibliothèque.
     *
     * @param entrees          les signatures candidates dans l'ordre du serveur
     * @param signatureActive  l'index de la signature active, -1 si absent
     * @param parametreActif   l'index du paramètre actif, -1 si absent
     * @return l'aide construite
     */
    public static SignatureLsp de(List<Entree> entrees, int signatureActive,
            int parametreActif) {
        return new SignatureLsp(entrees, signatureActive, parametreActif);
    }

    /** Les signatures candidates dans l'ordre du serveur. */
    public List<Entree> entrees() {
        return entrees;
    }

    /** L'index de la signature active, -1 si le serveur n'en dit rien. */
    public int signatureActiveIndex() {
        return signatureActive;
    }

    /**
     * La signature active — l'entrée à l'index donné, nulle si l'index
     * est absent ou hors des bornes : le choix du repli appartient à
     * l'hôte, jamais deviné ici.
     *
     * @return la signature active, ou null
     */
    public @Nullable Entree signatureActive() {
        if (signatureActive < 0 || signatureActive >= entrees.size()) {
            return null;
        }
        return entrees.get(signatureActive);
    }

    /**
     * Le paramètre courant, BORNÉ à l'arité de la signature active :
     * l'indice du serveur au-delà retombe sur le dernier paramètre —
     * le surlignage tient. Sans signature active, sans paramètre ou
     * sans indice : -1.
     *
     * @return l'index du paramètre courant, -1 s'il n'y en a pas
     */
    public int parametreCourant() {
        Entree active = signatureActive();
        if (parametreActif < 0 || active == null || active.parametres().isEmpty()) {
            return ABSENT;
        }
        return Math.min(parametreActif, active.parametres().size() - 1);
    }
}
