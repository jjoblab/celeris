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
public final class ApercuCorrectif {

    /** Le contexte par défaut : deux lignes intactes autour d'un bloc. */
    public static final int CONTEXTE_DEFAUT = 2;

    private final List<Ligne> lignes;

    private ApercuCorrectif(List<Ligne> lignes) {
        this.lignes = List.copyOf(lignes);
    }

    /** Le genre d'une ligne d'aperçu. */
    public enum GenreLigne {

        /** La ligne existe dans l'ancien texte seulement. */
        ENLEVEE,

        /** La ligne existe dans le nouveau texte seulement. */
        AJOUTEE,

        /** La ligne existe aux deux textes — le contexte du bloc. */
        INTACTE
    }

    /** Une ligne d'aperçu : son genre et son texte. */
    public record Ligne(GenreLigne genre, String texte) {

        /** Le préfixe de la ligne rendue : « - », « + » ou deux blancs. */
        public String prefixe() {
            return switch (genre) {
                case ENLEVEE -> "- ";
                case AJOUTEE -> "+ ";
                case INTACTE -> "  ";
            };
        }

        /** La ligne rendue : son préfixe et son texte. */
        public String rendue() {
            return prefixe() + texte;
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
    public static ApercuCorrectif de(String avant, String apres) {
        return de(avant, apres, CONTEXTE_DEFAUT);
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
    public static ApercuCorrectif de(String avant, String apres, int contexte) {
        Objects.requireNonNull(avant, "avant");
        Objects.requireNonNull(apres, "apres");
        if (contexte < 0) {
            throw new IllegalArgumentException("contexte must not be negative");
        }
        List<DiffBlocs.BlocDiff> blocs = DiffBlocs.de(avant, apres).blocs();
        List<String> anciennes = lignes(avant);
        List<Ligne> rendues = new ArrayList<>();
        // Le parcours unifié : chaque ligne ancienne se rend au plus une
        // fois — intacte de contexte, ou dans son bloc avec ses
        // ajoutées derrière ; les blocs d'ajout pur s'ancrent à leur
        // ligne — leurs lignes se rendent avant elle.
        for (int i = 0; i < anciennes.size(); i++) {
            ajoutsPurs(blocs, i).forEach(ligne ->
                    rendues.add(new Ligne(genre(ligne), ligne.texte())));
            DiffBlocs.BlocDiff bloc = blocDe(blocs, i);
            if (bloc != null) {
                for (DiffBlocs.LigneDiff ligne : bloc.lignes()) {
                    rendues.add(new Ligne(genre(ligne), ligne.texte()));
                }
                i = bloc.finAncien() - 1;
                continue;
            }
            if (presDe(blocs, i, contexte)) {
                rendues.add(new Ligne(GenreLigne.INTACTE, anciennes.get(i)));
            }
        }
        // L'ajout pur ancré à la fin du texte : le parcours ne l'atteint
        // jamais — il se rend en dernier.
        ajoutsPurs(blocs, anciennes.size()).forEach(ligne ->
                rendues.add(new Ligne(genre(ligne), ligne.texte())));
        return new ApercuCorrectif(rendues);
    }

    /**
     * Les lignes de l'aperçu, dans l'ordre du texte — le contexte
     * encadre les blocs.
     *
     * @return les lignes, non modifiables
     */
    public List<Ligne> lignes() {
        return lignes;
    }

    /**
     * Vrai quand l'aperçu porte un changement : les textes diffèrent.
     *
     * @return vrai si au moins une ligne changée se montre
     */
    public boolean porteChangement() {
        for (Ligne ligne : lignes) {
            if (ligne.genre() != GenreLigne.INTACTE) {
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
        List<String> sortie = new ArrayList<>();
        for (Ligne ligne : lignes) {
            sortie.add(ligne.rendue());
        }
        return sortie;
    }

    /** Les lignes d'ajout pur du bloc ancré à la ligne ancienne donnée. */
    private static List<DiffBlocs.LigneDiff> ajoutsPurs(List<DiffBlocs.BlocDiff> blocs,
            int ligneAncienne) {
        for (DiffBlocs.BlocDiff bloc : blocs) {
            if (bloc.debutAncien() == ligneAncienne
                    && bloc.finAncien() == ligneAncienne) {
                return bloc.lignes();
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
    private static GenreLigne genre(DiffBlocs.LigneDiff ligne) {
        return ligne.type() == DiffBlocs.TypeLigne.ENLEVEE
                ? GenreLigne.ENLEVEE : GenreLigne.AJOUTEE;
    }

    /** Les lignes du texte, la convention du diff : coupe au saut. */
    private static List<String> lignes(String texte) {
        List<String> lignes = new ArrayList<>();
        int debut = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                lignes.add(texte.substring(debut, i));
                debut = i + 1;
            }
        }
        lignes.add(texte.substring(debut));
        return lignes;
    }
}
