package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.Match;
import jo.celeris.core.text.WordOccurrences;

/**
 * La complétion de base (B-03) : le préfixe tape, les mots du fichier
 * répondent — sans serveur, sans réseau, au mot près. Les mots du CODE
 * seulement : les jetons du lexique décident, jamais les chaînes ni les
 * commentaires. Le mot en cours de frappe n'est pas son propre candidat ;
 * un mot identique ailleurs dans le fichier, lui, l'est.
 */
public final class CompletionMots {

    /** Le plafond de candidats — la complétion hors ligne reste courte. */
    public static final int MAX_CANDIDATS = 100;

    private CompletionMots() {
    }

    /**
     * Les candidats de complétion au curseur.
     *
     * @param doc       le document porteur
     * @param analyse   l'analyse courante du document
     * @param caret     la position du curseur
     * @param motsCles  les mots réservés du langage, se joignant aux mots
     *                  du fichier
     * @return les candidats distincts, triés sans égard à la casse, le
     *         préfixe déjà tapé en tête de chacun
     */
    public static List<String> completer(Document doc, Analysis analyse, int caret,
            List<String> motsCles) {
        Match mot = WordOccurrences.wordAt(doc.text(), caret);
        String prefixe = mot == null ? ""
                : doc.subText(mot.start(), Math.min(caret, mot.end()));
        List<String> candidats = new ArrayList<>();
        for (ScanIdentifiants.NomOffset releve : ScanIdentifiants.scanner(doc, analyse.lex())) {
            // l'occurrence sous le curseur ne compte pas : le mot tapé ne
            // se propose pas à lui-même
            if (mot != null && releve.offset() >= mot.start()
                    && releve.offset() < mot.end()) {
                continue;
            }
            ajouter(candidats, releve.nom(), prefixe);
        }
        for (String motCle : motsCles) {
            ajouter(candidats, motCle, prefixe);
        }
        candidats.sort((a, b) -> {
            int sansCasse = a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
            return sansCasse != 0 ? sansCasse : a.compareTo(b);
        });
        if (candidats.size() > MAX_CANDIDATS) {
            return List.copyOf(candidats.subList(0, MAX_CANDIDATS));
        }
        return List.copyOf(candidats);
    }

    private static void ajouter(List<String> candidats, String nom, String prefixe) {
        if (candidats.contains(nom)) {
            return;
        }
        if (nom.length() >= prefixe.length()
                && nom.regionMatches(true, 0, prefixe, 0, prefixe.length())) {
            candidats.add(nom);
        }
    }
}
