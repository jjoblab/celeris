package jo.celeris.android.view;

import android.content.ClipData;
import android.net.Uri;
import android.view.DragEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * Le glisser-déposer (chantier C13, D-20) : le collaborateur de la vue
 * qui reçoit les dépôts. Le TEXTE déposé s'insère au point de chute —
 * l'offset se résout par le viseur, l'insertion passe par la voie de
 * transaction (ADR-003) ; l'URI de FICHIER déposée va à l'HÔTE par le
 * rappel — la vue ne décide jamais ce qu'ouvrir (le scénario de la
 * matrice : dépôt de fichier → ouverture). Le texte a la
 * précédence : un dépôt qui porte les deux insère le texte, l'uri
 * suit le rappel.
 *
 * <p>Pur sur l'état de la vue : le décodage du presse-papiers vit
 * dans la colle Android (fine), la décision et l'insertion vivent ici —
 * le banc les couvre sans appareil.</p>
 */
final class GlisserDeposer implements View.OnDragListener {

    /** Un dépôt reçu : le texte et les uris, décodés du geste. */
    record Recu(String texte, List<String> uris) {

        /** Constructeur canonique : composantes figées, jamais nulles. */
        Recu {
            java.util.Objects.requireNonNull(texte, "texte");
            java.util.Objects.requireNonNull(uris, "uris");
            uris = List.copyOf(uris);
        }

        /** Un dépôt vide : ni texte ni uri — rien à déposer. */
        static Recu vide() {
            return new Recu("", List.of());
        }
    }

    private final EditorView vue;

    /** Le rappel des fichiers déposés — l'uri va à l'hôte. */
    private @Nullable Consumer<String> surFichier;

    GlisserDeposer(EditorView vue) {
        this.vue = vue;
    }

    /**
     * Pose (ou retire) le rappel des fichiers déposés : chaque uri
     * déposée y part, l'hôte décide de l'ouverture.
     *
     * @param rappel le consommateur d'uri, null pour couper
     */
    void poserSurFichier(@Nullable Consumer<String> rappel) {
        surFichier = rappel;
    }

    @Override
    public boolean onDrag(View cible, DragEvent evenement) {
        switch (evenement.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // la vue n'accepte que ce qu'elle sait déposer
                return !decoder(evenement.getClipData()).equals(Recu.vide());
            case DragEvent.ACTION_DROP:
                return deposer(decoder(evenement.getClipData()),
                        evenement.getX(), evenement.getY());
            default:
                // les autres moments du geste se regardent, rien à faire
                return true;
        }
    }

    /**
     * Dépose un contenu reçu à un point de la vue : le texte
     * s'insère au point de chute (la voie de transaction, ADR-003),
     * l'uri va à l'hôte par le rappel — la précédence au texte, les
     * deux se consomment.
     *
     * @param recu le contenu décodé du geste
     * @param x    l'abscisse du point de chute, dans la vue
     * @param y    l'ordonnée du point de chute, dans la vue
     * @return vrai quand quelque chose se dépose
     */
    boolean deposer(Recu recu, float x, float y) {
        boolean depose = false;
        if (!recu.texte().isEmpty()) {
            int offset = vue.viseur.offsetAt(vue.session.document(),
                    vue.projection, vue.metrics, vue.largeurGouttiere,
                    vue.scrollX, vue.scrollY, x, y);
            if (vue.session.replaceRange(offset, offset, recu.texte())) {
                vue.invalidate();
                depose = true;
            }
        }
        if (surFichier != null) {
            for (String uri : recu.uris()) {
                surFichier.accept(uri);
                depose = true;
            }
        }
        return depose;
    }

    /**
     * Décode le presse-papiers du geste : le texte assemblé de ses
     * tranches TEXT, les uris de ses tranches porteuses d'uri.
     */
    private static Recu decoder(@Nullable ClipData pressePapiers) {
        if (pressePapiers == null || pressePapiers.getItemCount() == 0) {
            return Recu.vide();
        }
        StringBuilder texte = new StringBuilder();
        List<String> uris = new ArrayList<>();
        for (int i = 0; i < pressePapiers.getItemCount(); i++) {
            ClipData.Item tranche = pressePapiers.getItemAt(i);
            CharSequence brut = tranche.getText();
            if (brut != null) {
                texte.append(brut);
            }
            Uri uri = tranche.getUri();
            if (uri != null) {
                uris.add(uri.toString());
            }
        }
        return new Recu(texte.toString(), List.copyOf(uris));
    }
}
