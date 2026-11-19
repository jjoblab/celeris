package jo.celeris.core.intelligence;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

import java.util.List;
import java.util.Optional;


/**
 * La frappe intelligente XML/HTML (C5, l'analyse croisée) : le « > »
 * qui clôt une balise ouvrante insère sa fermante appariée — le caret
 * se pose entre les deux. Le dépôt de départ ferme pareil ses balises
 * sur le chevron ; les balises auto-fermantes ({@code <br>},
 * {@code <img/>}) ne se doublent jamais, et le chevron d'une balise
 * FERMANTE ne déclenche rien.
 *
 * <p>Le renommage lié (C6) vit dans {@link XmlLinkedTags} : la
 * frappe et le retour arrière dans le nom ouvrant se reflètent dans
 * la fermante — ce moteur-ci n'y touche pas.</p>
 */
public final class XmlSmartTyping {

    private XmlSmartTyping() {
    }

    /** Les balises vides qui ne se ferment jamais. */
    private static final List<String> VIDES = List.of(
            "br", "hr", "img", "input", "meta", "link", "area", "base",
            "col", "embed", "source", "track", "wbr");

    /**
     * La fermeture de balise sur « > » : le chevron qui clôt une
     * balise ouvrante insère la fermante appariée après lui.
     *
     * @param doc    le document, jamais muté
     * @param caret  la position du chevron frappé (après insertion)
     * @param rules  les règles de frappe du langage (sans effet ici,
     *               la signature aligne la voie du fournisseur)
     * @return le plan du geste, vide quand la balise ne se ferme pas
     */
    public static Optional<EditPlan> closeTag(Document doc, int caret,
            TypingRules rules) {
        // Le chevron n'est PAS encore dans le document : la frappe le
        // porte, le plan l'insère avec sa fermante. Remonter au « < ».
        int lt = openTagStart(doc, caret - 1);
        if (lt < 0) {
            return Optional.empty();
        }
        int nomDebut = lt + 1;
        if (nomDebut < doc.length() && doc.charAt(nomDebut) == '/') {
            // La balise FERMANTE (</…>) ne déclenche rien.
            return Optional.empty();
        }
        int nomFin = nomDebut;
        while (nomFin < doc.length() && estNomXml(doc.charAt(nomFin))) {
            nomFin++;
        }
        if (nomFin == nomDebut) {
            return Optional.empty();
        }
        String nom = doc.subText(nomDebut, nomFin);
        // La balise auto-fermante (« …/ » avant le chevron) ou vide —
        // jamais de doublon de fermante.
        boolean autoFermante = caret >= nomFin + 1
                && doc.charAt(caret - 1) == '/';
        if (autoFermante
                || VIDES.contains(nom.toLowerCase(java.util.Locale.ROOT))) {
            return Optional.empty();
        }
        String insere = "></" + nom + ">";
        return Optional.of(new EditPlan(
                Transaction.of(List.of(new TextEdit(caret, "", insere))),
                MultiSelection.caretsAt(caret + 1)));
    }

    /** Le « < » ouvrant le plus proche avant la position, hors
     *  texte : un « > » sur le chemin écarte la liaison (la balise
     *  est déjà close). */
    private static int openTagStart(Document doc, int avant) {
        for (int i = Math.min(avant, doc.length() - 1); i >= 0; i--) {
            char c = doc.charAt(i);
            if (c == '>') {
                return -1;
            }
            if (c == '<') {
                return i;
            }
        }
        return -1;
    }

    /** Vrai pour un caractère de nom XML : lettre, chiffre, tiret,
     *  soulignement, point ou deux-points. */
    private static boolean estNomXml(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_'
                || c == '.' || c == ':';
    }
}
