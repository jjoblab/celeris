package jo.celeris.demo;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;

import jo.celeris.core.syntax.structure.OutlineEntry;

import jo.celeris.core.text.TextSession;

/**
 * Le pont provisoire des surfaces réveillées (la réconciliation du
 * 2026-11-14) : les surfaces du signal « terminez le reste » vivent
 * encore dans la démonstration — le temps de leur déménagement chez
 * l'orchestrateur de l'éditeur, le pont les construit sur le manager
 * de l'éditeur et expose leurs ouvertures au catalogue. La
 * démonstration n'y fait que TESTER les cartes du kit, son rôle
 * exact ; le pont meurt pièce par pièce — chaque surface portée chez
 * l'hôte (moteur en core, câblage dans l'orchestrateur) retire sa
 * ligne et son atelier : l'info de type, les usages, la
 * documentation externe, les hiérarchies, les gabarits, la sûreté et le
masquage d'inspection y vivent déjà.
 */
final class PontSurfaces {

    private final AtelierException exception;

    /**
     * Construit le pont sur le manager de l'éditeur : les ateliers des
     * surfaces réveillées y naissent, chacun lié à son genre.
     *
     * @param editeur       la vue d'éditeur ouverte
     * @param session       la session d'édition courante
     * @param popups        le manager des popups de l'éditeur
     * @param margePx       la respiration des ancres aux bords, en pixels
     * @param palette       la palette courante des popups
     * @param plan          le plan du fichier ouvert, vif au document
     * @param nomDuFichier  le nom du fichier ouvert, vif
     * @param annoncer      la voix des annonces de l'éditeur
     */
    PontSurfaces(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette,
            Supplier<List<OutlineEntry>> plan,
            Supplier<String> nomDuFichier,
            BiConsumer<String, String> annoncer) {
        exception = new AtelierException(editeur, session, popups,
                margePx, palette, plan, nomDuFichier, annoncer);
    }

    /** La palette tourne : les cartes du pont la suivent. */
    void surPalette(PopupPalette palette) {
        exception.surPalette(palette);
    }

    /** Exécute le fichier ouvert — l'échec simulé porte la trace
     *  réelle du document, la carte des détails se pose. */
    void executerFichier() {
        exception.executer();
    }
}
