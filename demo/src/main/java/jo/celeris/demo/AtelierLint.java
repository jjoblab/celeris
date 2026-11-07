package jo.celeris.demo;

import java.util.List;
import java.util.function.Consumer;

import jo.celeris.android.host.spi.AnalyseurLint;
import jo.celeris.android.host.spi.CorrectifLint;
import jo.celeris.android.host.spi.CorrectifsLint;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier lint de la démonstration (C-12) : le faux build rend la
 * sortie au format d'Android Lint, l'analyseur la convertit, les
 * correctifs mécaniques s'appliquent en transactions annulables — une
 * vague à la fois, l'analyse se relance après chacune. L'activité ne
 * garde que le menu et la barre d'état.
 */
final class AtelierLint {

    /** Le garde-fou des vagues : chaque vague retire une occurrence. */
    private static final int LIMITE_CORRECTIFS = 50;

    private final String nomFichier;
    private final TextSession session;
    private final Consumer<List<DiagnosticHote>> surEtat;

    private List<DiagnosticHote> diagnostics = List.of();

    /**
     * Construit l'atelier du fichier ouvert.
     *
     * @param nomFichier le nom du fichier analysé
     * @param session    la session d'édition — les correctifs y deviennent
     *                   des transactions annulables
     * @param surEtat    l'écouteur des diagnostics après chaque analyse
     */
    AtelierLint(String nomFichier, TextSession session,
            Consumer<List<DiagnosticHote>> surEtat) {
        this.nomFichier = nomFichier;
        this.session = session;
        this.surEtat = surEtat;
    }

    /**
     * Lance l'analyse : la sortie du faux build se convertit en
     * diagnostics, l'écouteur en est prévenu.
     */
    void analyser() {
        diagnostics = AnalyseurLint.parser(
                FauxLint.sortie(nomFichier, lignes()));
        surEtat.accept(diagnostics);
    }

    /**
     * Applique tous les correctifs mécaniques disponibles, une vague à
     * la fois — chaque correctif remplace sa plage exacte.
     *
     * @return le nombre de correctifs appliqués
     */
    int corriger() {
        int corriges = 0;
        for (int vague = 0; vague < LIMITE_CORRECTIFS; vague++) {
            List<DiagnosticHote> restants = AnalyseurLint.parser(
                    FauxLint.sortie(nomFichier, lignes()));
            CorrectifLint correctif = premierCorrectif(restants);
            if (correctif == null) {
                break;
            }
            int[] debuts = debutsLignes();
            if (!session.replaceRange(correctif.debut(debuts),
                    correctif.fin(debuts), correctif.apres())) {
                break;
            }
            corriges++;
        }
        analyser();
        return corriges;
    }

    /**
     * Les lignes porteuses d'un diagnostic du fichier ouvert, la
     * matière des marques de gouttière.
     *
     * @return les lignes, numérotées à partir de zéro, distinctes
     */
    int[] lignesMarquees() {
        return diagnostics.stream()
                .filter(d -> d.chemin().nom().equals(nomFichier))
                .mapToInt(DiagnosticHote::ligne)
                .map(ligne -> ligne - 1)
                .distinct().toArray();
    }

    private List<String> lignes() {
        return FauxLint.lignes(session.document().text());
    }

    private int[] debutsLignes() {
        List<String> lignes = lignes();
        int[] debuts = new int[lignes.size()];
        int curseur = 0;
        for (int i = 0; i < lignes.size(); i++) {
            debuts[i] = curseur;
            curseur += lignes.get(i).length() + 1;
        }
        return debuts;
    }

    private CorrectifLint premierCorrectif(List<DiagnosticHote> diagnostiques) {
        List<String> lignes = lignes();
        for (DiagnosticHote diagnostic : diagnostiques) {
            if (!diagnostic.chemin().nom().equals(nomFichier)) {
                continue;
            }
            CorrectifLint correctif = CorrectifsLint
                    .pour(diagnostic, lignes).orElse(null);
            if (correctif != null) {
                return correctif;
            }
        }
        return null;
    }
}
