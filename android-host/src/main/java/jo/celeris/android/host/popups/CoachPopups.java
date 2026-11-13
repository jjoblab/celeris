package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.SignatureAideModel;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Le coach des popups de l'hôte : le contenu de REPLI LOCAL quand
 * aucun serveur de langage ne parle — les mots réservés du langage,
 * les badges des candidats bruts, la documentation et les signatures
 * connues de son terrain. Les algorithmes génériques vivent chez
 * {@link ContenuPopups} ; le coach ne porte que les TABLES de
 * l'hôte.
 *
 * <p>Le coach MUET ({@link #muet()}) rend le minimum : aucune table,
 * les mots du document seuls en variables — la complétion sans
 * serveur vit déjà, l'hôte enrichit quand il veut.</p>
 */
public interface CoachPopups {

    /**
     * Les mots réservés du langage ouvert — la complétion y joint les
     * mots du code du fichier ouvert.
     *
     * @return les mots réservés servis à la complétion
     */
    List<String> motsReserves();

    /**
     * Les candidats TYPÉS des mots bruts du document : chaque libellé
     * gagne son genre de badge — l'hôte qui connaît ses fonctions les
     * badge en méthodes.
     *
     * @param bruts les candidats libellés, déjà filtrés
     * @return les candidats typés, même ordre
     */
    List<CompletionListView.Candidat> candidats(List<String> bruts);

    /**
     * La documentation d'un symbole du terrain — le repli du survol
     * sans serveur.
     *
     * @param symbole le nom du symbole
     * @return la documentation structurée, null si le coach ne connaît pas
     */
    @Nullable CompletionDocModel docDe(String symbole);

    /**
     * La signature de l'appel ouvert sous le curseur — le repli de
     * l'aide aux paramètres sans serveur.
     *
     * @param document le document ouvert
     * @param caret    la position du curseur
     * @return le modèle d'aide aux paramètres, null sans appel connu
     */
    @Nullable SignatureAideModel signatureDe(Document document, int caret);

    /** Le coach muet : aucune table, les mots du document en variables —
     *  la complétion sans serveur vit déjà, dépouillée. */
    static CoachPopups muet() {
        return new CoachPopups() {
            @Override
            public List<String> motsReserves() {
                return List.of();
            }

            @Override
            public List<CompletionListView.Candidat> candidats(
                    List<String> bruts) {
                List<CompletionListView.Candidat> types =
                        new java.util.ArrayList<>(bruts.size());
                for (String nom : bruts) {
                    types.add(new CompletionListView.Candidat(nom, "",
                            jo.celeris.android.widgets.BadgeGenre.Genre.VARIABLE));
                }
                return types;
            }

            @Override
            public @Nullable CompletionDocModel docDe(String symbole) {
                return null;
            }

            @Override
            public @Nullable SignatureAideModel signatureDe(
                    Document document, int caret) {
                return null;
            }
        };
    }
}
