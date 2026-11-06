package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * L'aide aux paramètres du serveur (B-07) : les signatures candidates,
 * la signature active et le paramètre courant — le surlignage du
 * paramètre en cours de frappe. Le paramètre courant est BORNÉ à
 * l'arité de la signature active : les serveurs généreux qui envoient
 * un indice au-delà ne cassent pas le surlignage, il retombe sur le
 * dernier. Les champs absents restent nuls ou négatifs, jamais devinés.
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
     * Analyse l'aide aux paramètres depuis sa réponse.
     *
     * @param json la valeur « result » de la requête d'aide aux
     *             paramètres
     * @return l'aide lue — aucune signature si la réponse est vide ou
     *         nulle
     */
    public static SignatureLsp de(ValeurJson json) {
        List<Entree> entrees = new ArrayList<>();
        if (json instanceof JsonObjet objet
                && objet.membre("signatures") instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                entrees.add(entreeDe(tableau.element(i)));
            }
        }
        return new SignatureLsp(entrees, entierOuAbsent(json, "activeSignature"),
                entierOuAbsent(json, "activeParameter"));
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

    private static Entree entreeDe(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new Entree("", List.of(), null);
        }
        String libelle = objet.membreTexte("label");
        List<Parametre> parametres = new ArrayList<>();
        if (objet.membre("parameters") instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                parametres.add(parametreDe(tableau.element(i),
                        libelle == null ? "" : libelle));
            }
        }
        return new Entree(libelle == null ? "" : libelle, parametres,
                objet.membreTexte("documentation"));
    }

    private static Parametre parametreDe(ValeurJson json, String libelleSignature) {
        if (!(json instanceof JsonObjet objet)) {
            return new Parametre("", null);
        }
        return new Parametre(libelleDeParametre(objet.membre("label"), libelleSignature),
                objet.membreTexte("documentation"));
    }

    /** Le libellé du paramètre : la chaîne du serveur ou la tranche du libellé de signature. */
    private static String libelleDeParametre(@Nullable ValeurJson label, String libelleSignature) {
        if (label instanceof JsonChaine chaine) {
            return chaine.texte();
        }
        if (label instanceof JsonTableau bornes && bornes.taille() == 2
                && bornes.element(0) instanceof JsonNombre debut
                && bornes.element(1) instanceof JsonNombre fin
                && debut.estEntier() && fin.estEntier()) {
            int d = (int) debut.entier();
            int f = (int) fin.entier();
            if (d >= 0 && f >= d && f <= libelleSignature.length()) {
                return libelleSignature.substring(d, f);
            }
        }
        return "";
    }

    private static int entierOuAbsent(ValeurJson json, String cle) {
        if (json instanceof JsonObjet objet) {
            Long valeur = objet.membreEntier(cle);
            if (valeur != null) {
                return valeur.intValue();
            }
        }
        return ABSENT;
    }
}
