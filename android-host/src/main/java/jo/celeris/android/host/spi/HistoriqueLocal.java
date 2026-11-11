package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * L'historique local par fichier (A-14) : chaque écriture enregistre le
 * contenu qui s'en va, chaque fichier garde ses révisions pour soi —
 * l'histoire d'un fichier ne fuit jamais dans celle d'un autre. La
 * restauration réécrit le contenu d'une révision passée par la porte
 * commune : la restauration s'enregistre à son tour, elle se défait
 * comme elle s'est faite. L'horloge injectée rend l'ordonnancement
 * déterministe pour les bancs.
 *
 * <p>Le décorateur est mince par construction : l'historique ne connaît
 * ni fichier ni stockage — il enveloppe un
 * {@link SystemeFichiersVirtuel} et délègue tout, n'interceptant que
 * l'écriture. L'écriture qui ne change rien n'enregistre rien : le
 * passé n'archive pas le présent.</p>
 */
public final class HistoriqueLocal implements SystemeFichiersVirtuel {

    private final SystemeFichiersVirtuel systeme;
    private final LongSupplier horloge;
    private final Map<CheminDoc, List<RevisionLocale>> passe = new HashMap<>();

    /**
     * Construit l'historique sur le système donné.
     *
     * @param systeme le système de fichiers enveloppé
     * @param horloge l'horloge des horodatages, injectée pour le
     *                déterminisme des bancs
     */
    public HistoriqueLocal(SystemeFichiersVirtuel systeme, LongSupplier horloge) {
        this.systeme = Objects.requireNonNull(systeme, "systeme");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    @Override
    public FichierVirtuel ouvrir(CheminDoc chemin) {
        return systeme.ouvrir(chemin);
    }

    @Override
    public void ecrire(CheminDoc chemin, String contenu) {
        Objects.requireNonNull(contenu, "contenu");
        enregistrer(chemin, contenu);
        systeme.ecrire(chemin, contenu);
    }

    @Override
    public List<CheminDoc> lister(CheminDoc dossier) {
        return systeme.lister(dossier);
    }

    @Override
    public boolean estFichier(CheminDoc chemin) {
        return systeme.estFichier(chemin);
    }

    /**
     * Les révisions passées du fichier, dans l'ordre chronologique —
     * chaque écriture qui change le contenu archive le contenu
     * remplacé.
     *
     * @param chemin le chemin du fichier
     * @return les révisions, vide pour un fichier sans passé
     */
    public List<RevisionLocale> revisions(CheminDoc chemin) {
        return List.copyOf(passe.getOrDefault(chemin, List.of()));
    }

    /**
     * Restaure une révision passée du fichier : le contenu de la
     * révision se réécrit par la porte commune — la restauration
     * s'enregistre à son tour, elle se défait comme elle s'est faite.
     *
     * @param chemin   le chemin du fichier
     * @param revision la révision à restaurer, appartenant à l'histoire
     *                 du fichier
     * @throws IllegalArgumentException si la révision n'appartient pas
     *         à l'histoire du fichier
     */
    public void restaurer(CheminDoc chemin, RevisionLocale revision) {
        Objects.requireNonNull(revision, "revision");
        if (!revisions(chemin).contains(revision)) {
            throw new IllegalArgumentException(
                    "revision does not belong to the history of " + chemin.ecriture());
        }
        ecrire(chemin, revision.contenu());
    }

    /** Archive le contenu remplacé quand l'écriture change le fichier. */
    private void enregistrer(CheminDoc chemin, String contenu) {
        if (!systeme.estFichier(chemin)) {
            return;
        }
        String remplace = systeme.ouvrir(chemin).contenu();
        if (remplace.equals(contenu)) {
            return;
        }
        passe.computeIfAbsent(chemin, c -> new ArrayList<>())
                .add(new RevisionLocale(chemin, remplace, horloge.getAsLong()));
    }
}
