package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * L'hôte des plugins isolés (M5, G-04) : l'erreur d'un plugin ne tue
 * jamais l'éditeur. Les extensions enregistrées reçoivent chaque
 * image ; celle qui lève est journalisée — une ligne par défaillance,
 * le journal est borné — et les suivantes courent : l'image suivante
 * est intacte. L'hôte ne connaît ni Android ni le peintre : la charge
 * de l'image est opaque.
 */
public final class PluginHost {

    /** Une extension : reçoit la charge de chaque image. */
    public interface Extension {

        /**
         * Traite une image.
         *
         * @param image la charge opaque portée à l'extension
         */
        void executer(String image);
    }

    private static final int JOURNAL_MAX_DEFAUT = 100;

    private final List<Extension> extensions = new ArrayList<>();

    private final List<String> journal = new ArrayList<>();

    private final int journalMax;

    /**
     * Un hôte au journal borné par défaut.
     */
    public PluginHost() {
        this(JOURNAL_MAX_DEFAUT);
    }

    /**
     * Un hôte au journal borné.
     *
     * @param journalMax les lignes de journal retenues, au moins 1
     * @throws IllegalArgumentException si {@code journalMax < 1}
     */
    public PluginHost(int journalMax) {
        if (journalMax < 1) {
            throw new IllegalArgumentException("journalMax must be >= 1: " + journalMax);
        }
        this.journalMax = journalMax;
    }

    /**
     * Enregistre une extension.
     *
     * @param extension l'extension à appeler à chaque image
     */
    public void enregistrer(Extension extension) {
        extensions.add(java.util.Objects.requireNonNull(extension, "extension"));
    }

    /**
     * Porte la charge à toutes les extensions : celle qui lève est
     * journalisée, les suivantes courent.
     *
     * @param image la charge de l'image
     * @return vrai si toutes les extensions ont passé
     */
    public boolean executer(String image) {
        boolean toutes = true;
        for (Extension extension : extensions) {
            try {
                extension.executer(image);
            } catch (RuntimeException echec) {
                journal.add("extension " + extension.getClass().getName()
                        + " : " + echec);
                if (journal.size() > journalMax) {
                    journal.remove(0);
                }
                toutes = false;
            }
        }
        return toutes;
    }

    /**
     * Le journal des défaillances, borné.
     *
     * @return les lignes, de la plus ancienne à la plus récente
     */
    public List<String> journal() {
        return List.copyOf(journal);
    }
}
