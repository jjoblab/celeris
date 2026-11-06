package jo.celeris.demo;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

/**
 * L'adaptateur des cartes de fichiers de l'accueil : monogramme teinté
 * de la couleur du langage, nom en police à chasse fixe, langage et
 * compte de lignes en sous-titre. Le clic remonte au consommateur
 * donné à la construction.
 */
public final class AdaptateurFichiers
        extends ListAdapter<CarteFichier, AdaptateurFichiers.Tenu> {

    /** La teinte du fond de badge : la couleur du langage, translucide. */
    private static final int ALPHA_BADGE = 0x26000000;

    /** La fabrique de différences : les cartes sont des enregistrements. */
    private static final DiffUtil.ItemCallback<CarteFichier> DIFF =
            new DiffUtil.ItemCallback<CarteFichier>() {
                @Override
                public boolean areItemsTheSame(@NonNull CarteFichier a,
                        @NonNull CarteFichier b) {
                    return a.nom().equals(b.nom());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CarteFichier a,
                        @NonNull CarteFichier b) {
                    return a.equals(b);
                }
            };

    private final Consumer<CarteFichier> surOuverture;

    /**
     * @param surOuverture appelé quand une carte est ouverte
     */
    public AdaptateurFichiers(Consumer<CarteFichier> surOuverture) {
        super(DIFF);
        this.surOuverture = surOuverture;
    }

    @NonNull
    @Override
    public Tenu onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View carte = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fichier, parent, false);
        return new Tenu(carte);
    }

    @Override
    public void onBindViewHolder(@NonNull Tenu tenu, int position) {
        CarteFichier carte = getItem(position);
        tenu.monogramme.setText(carte.monogramme());
        tenu.monogramme.setTextColor(carte.couleur());
        tenu.monogramme.setBackgroundTintList(
                ColorStateList.valueOf(teinteBadge(carte)));
        tenu.nom.setText(carte.nom());
        tenu.sousTitre.setText(tenu.itemView.getContext().getResources()
                .getQuantityString(R.plurals.sous_titre_fichier, carte.lignes(),
                        carte.langage(), carte.lignes()));
        tenu.itemView.setOnClickListener(vue -> surOuverture.accept(carte));
    }

    private int teinteBadge(CarteFichier carte) {
        return ALPHA_BADGE | (carte.couleur() & 0x00FFFFFF);
    }

    /** Le titulaire d'une carte de fichier. */
    static final class Tenu extends RecyclerView.ViewHolder {

        private final TextView monogramme;
        private final TextView nom;
        private final TextView sousTitre;

        Tenu(@NonNull View carte) {
            super(carte);
            monogramme = carte.findViewById(R.id.monogramme);
            nom = carte.findViewById(R.id.nom_fichier);
            sousTitre = carte.findViewById(R.id.sous_titre_fichier);
        }
    }
}
