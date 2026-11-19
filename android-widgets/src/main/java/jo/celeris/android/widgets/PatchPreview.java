package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * L'aperçu d'un correctif (B-18) : les lignes du document AVANT et
 * APRÈS l'édition proposée se lisent en préfixe — « - » l'enlevée,
 * « + » l'ajoutée — chaque bloc changé s'encadrant de lignes intactes
 * de contexte, la revue se voit AVANT l'acceptation. L'application
 * appartient à l'appelant : par la voie de transaction, diffable et
 * annulable (B-17) — l'aperçu n'engage à rien.
 *
 * <p>La coupe est celle du diff unifié : les blocs se calculent par
 * {@link DiffBlocs} (la plus longue sous-suite commune des lignes),
 * le contexte encadre chaque bloc — les blocs proches partagent leurs
 * lignes intactes, le hors-bloc ne se montre jamais ; le contexte nul
 * ne montre que les lignes changées. Les textes identiques ne montrent
 * rien : un correctif qui n'engage à rien ne se prévise pas.</p>
 */
public final class PatchPreview {

    /** Le contexte par défaut : deux lignes intactes autour d'un bloc. */
    public static final int CONTEXTE_DEFAUT = 2;

    private final List<Line> lines;

    private PatchPreview(List<Line> lines) {
        this.lines = List.copyOf(lines);
    }

    /** Le genre d'une ligne d'aperçu. */
    public enum LineKind {

        /** La ligne existe dans l'ancien texte seulement. */
        ENLEVEE,

        /** La ligne existe dans le nouveau texte seulement. */
        AJOUTEE,

        /** La ligne existe aux deux textes — le contexte du bloc. */
        INTACTE
    }

    /** Une ligne d'aperçu : son genre et son texte. */
    public record Line(LineKind kind, String text) {

        /** Le préfixe de la ligne rendue : « - », « + » ou deux blancs. */
        public String prefixe() {
            return switch (kind) {
                case ENLEVEE -> "- ";
                case AJOUTEE -> "+ ";
                case INTACTE -> "  ";
            };
        }

        /** La ligne rendue : son préfixe et son texte. */
        public String rendue() {
            return prefixe() + text;
        }
    }

    /**
     * L'aperçu des deux textes au contexte par défaut — deux lignes
     * intactes autour de chaque bloc changé.
     *
     * @param avant l'ancien texte
     * @param apres le texte après l'édition proposée
     * @return l'aperçu, ou l'aperçu vide quand les textes sont identiques
     */
    public static PatchPreview of(String avant, String apres) {
        return of(avant, apres, CONTEXTE_DEFAUT);
    }

    /**
     * L'aperçu des deux textes au contexte donné : chaque bloc changé
     * s'encadre de lignes intactes — au plus le contexte de chaque
     * côté, les blocs proches partagent leurs lignes, le hors-bloc ne
     * se montre jamais.
     *
     * @param avant     l'ancien texte
     * @param apres     le texte après l'édition proposée
     * @param contexte  les lignes intactes autour d'un bloc, zéro permis
     * @return l'aperçu, ou l'aperçu vide quand les textes sont identiques
     * @throws IllegalArgumentException si le contexte est négatif
     */
    public static PatchPreview of(String avant, String apres, int contexte) {
        Objects.requireNonNull(avant, "avant");
        Objects.requireNonNull(apres, "apres");
        if (contexte < 0) {
            throw new IllegalArgumentException("contexte must not be negative");
        }
        List<DiffBlocs.BlocDiff> blocs = DiffBlocs.of(avant, apres).blocs();
        List<String> anciennes = lines(avant);
        List<Line> rendues = new ArrayList<>();
        // Le parcours unifié : chaque ligne ancienne se rend au plus une
        // fois — intacte de contexte, ou dans son bloc avec ses
        // ajoutées derrière ; les blocs d'ajout pur s'ancrent à leur
        // ligne — leurs lignes se rendent avant elle.
        for (int i = 0; i < anciennes.size(); i++) {
            ajoutsPurs(blocs, i).forEach(line ->
                    rendues.add(new Line(kind(line), line.text())));
            DiffBlocs.BlocDiff bloc = blocDe(blocs, i);
            if (bloc != null) {
                for (DiffBlocs.LineDiff line : bloc.lines()) {
                    rendues.add(new Line(kind(line), line.text()));
                }
                i = bloc.finAncien() - 1;
                continue;
            }
            if (presDe(blocs, i, contexte)) {
                rendues.add(new Line(LineKind.INTACTE, anciennes.get(i)));
            }
        }
        // L'ajout pur ancré à la fin du texte : le parcours ne l'atteint
        // jamais — il se rend en dernier.
        ajoutsPurs(blocs, anciennes.size()).forEach(line ->
                rendues.add(new Line(kind(line), line.text())));
        return new PatchPreview(rendues);
    }

    /**
     * Les lignes de l'aperçu, dans l'ordre du texte — le contexte
     * encadre les blocs.
     *
     * @return les lignes, non modifiables
     */
    public List<Line> lines() {
        return lines;
    }

    /**
     * Vrai quand l'aperçu porte un changement : les textes diffèrent.
     *
     * @return vrai si au moins une ligne changée se montre
     */
    public boolean porteChangement() {
        for (Line line : lines) {
            if (line.kind() != LineKind.INTACTE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Les lignes rendues : préfixe et texte, prêtes à la vue.
     *
     * @return les lignes préfixées, non modifiables
     */
    public List<String> rendues() {
        List<String> output = new ArrayList<>();
        for (Line line : lines) {
            output.add(line.rendue());
        }
        return output;
    }

    /** Les lignes d'ajout pur du bloc ancré à la ligne ancienne donnée. */
    private static List<DiffBlocs.LineDiff> ajoutsPurs(List<DiffBlocs.BlocDiff> blocs,
            int ligneAncienne) {
        for (DiffBlocs.BlocDiff bloc : blocs) {
            if (bloc.debutAncien() == ligneAncienne
                    && bloc.finAncien() == ligneAncienne) {
                return bloc.lines();
            }
        }
        return List.of();
    }

    /** Le bloc qui commence à la ligne ancienne donnée, ou null. */
    private static DiffBlocs.BlocDiff blocDe(List<DiffBlocs.BlocDiff> blocs,
            int ligneAncienne) {
        for (DiffBlocs.BlocDiff bloc : blocs) {
            if (bloc.debutAncien() == ligneAncienne && bloc.finAncien() > ligneAncienne) {
                return bloc;
            }
        }
        return null;
    }

    /** Vrai quand la ligne ancienne vit à portée de contexte d'un bloc. */
    private static boolean presDe(List<DiffBlocs.BlocDiff> blocs, int ligneAncienne,
            int contexte) {
        for (DiffBlocs.BlocDiff bloc : blocs) {
            if (ligneAncienne >= bloc.debutAncien() - contexte
                    && ligneAncienne < bloc.finAncien() + contexte) {
                return true;
            }
        }
        return false;
    }

    /** Le genre d'aperçu d'une ligne de diff. */
    private static LineKind kind(DiffBlocs.LineDiff line) {
        return line.type() == DiffBlocs.LineKind.ENLEVEE
                ? LineKind.ENLEVEE : LineKind.AJOUTEE;
    }

    /** Les lignes du texte, la convention du diff : coupe au saut. */
    private static List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i));
                start = i + 1;
            }
        }
        lines.add(text.substring(start));
        return lines;
    }
}
