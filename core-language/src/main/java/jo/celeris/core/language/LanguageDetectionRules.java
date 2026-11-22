package jo.celeris.core.language;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jo.celeris.core.language.model.LanguageDescriptor;

/**
 * Les règles de détection D'UN nom par descripteur seul (chantier A de
 * la coloration) : la face publique de la précédence du
 * {@link LanguageRegistry} — nom de fichier exact, motif de nom,
 * extension la plus longue — lue sur le DESCRIPTEUR d'un paquet sans
 * jamais le charger. La priorité d'assemblage du {@link LanguageKit}
 * s'en sert : décider si un paquet peut porter « Main.java » ne coûte
 * que la lecture de {@code language.json}, pas la compilation de sa
 * grammaire.
 *
 * <p>La décision est VOLONTAIREMENT large : elle ne choisit pas le
 * langage (c'est le registre assemblé qui départage), elle ne dit que
 * « ce paquet peut être celui-là » — faux positif admis (le paquet
 * passe en tête de file, il ne remplace personne), faux négatif
 * interdit (le paquet resterait derrière pour rien : le nom exact
 * `filenames` et les motifs `filenamePatterns` y comptent aussi).</p>
 */
public final class LanguageDetectionRules {

    private LanguageDetectionRules() {
    }

    /**
     * Vrai si le descripteur peut porter le nom de fichier : extension
     * revendiquée (la plus longue seule déciderait au registre, toute
     * extension PEUT ici), nom de fichier exact, motif de nom. Le nom
     * se compare en minuscule du basename, comme au registre.
     *
     * @param descripteur le descripteur du langage candidat
     * @param filename    le nom du fichier ouvert
     * @return vrai si le paquet peut porter ce nom
     */
    public static boolean peutPorter(LanguageDescriptor descripteur,
            String filename) {
        String nom = basename(filename).toLowerCase(Locale.ROOT);
        if (nom.isEmpty()) {
            return false;
        }
        for (String extension : descripteur.extensions()) {
            if (nom.endsWith("." + extension)) {
                return true;
            }
        }
        for (String exact : descripteur.filenames()) {
            if (nom.equals(exact.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String motif : descripteur.filenamePatterns()) {
            if (motifMatche(motif, filename)) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si le motif de nom correspond au nom complet. */
    private static boolean motifMatche(String motif, String filename) {
        try {
            return Pattern.compile(motif, Pattern.CASE_INSENSITIVE
                    | Pattern.UNICODE_CASE).matcher(filename).matches();
        } catch (PatternSyntaxException e) {
            // Le motif fautif ne Priorise rien : le registre complet le
            // refusera à sa charge, la file d'attente ne le tri pas.
            return false;
        }
    }

    /** Le nom de fichier sans son chemin. */
    private static String basename(String filename) {
        int barre = Math.max(filename.lastIndexOf('/'),
                filename.lastIndexOf('\\'));
        return barre < 0 ? filename : filename.substring(barre + 1);
    }
}
