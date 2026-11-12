package jo.celeris.demo;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jo.celeris.core.intelligence.WordCompletion;
import jo.celeris.core.language.Completion;
import jo.celeris.core.language.LanguageDetection;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.Packs;


import jo.celeris.core.syntax.grammars.PackGrammars;

import org.jspecify.annotations.Nullable;

/**
 * L'enregistrement des langages de la démonstration (M7.6) : le
 * registre assemblé de la plateforme — les vingt-deux paquets intégrés, la
 * fabrique de grammaires de la voie déclarative, la complétion
 * générique par les mots du code en repli de tous les langages, les
 * serveurs déclarés par la donnée de la démonstration (H-13,
 * {@code serveurs/servers.json} — l'éditeur n'en connaît pas un mot).
 * C'est du code d'enregistrement : la donnée des paquets et des
 * déclarations y coule, aucun littéral d'identifiant n'y vit.
 *
 * <p>L'assemblement se fait une fois, à la première demande : le
 * chargeur de paquets travaille hors fil UI (ADR-007) — l'accueil le
 * précharge sur son exécuteur, l'atelier paie au pire le premier
 * dégelui. Le dossier des PAQUETS DE L'UTILISATEUR s'y ajoute quand
 * l'hôte le pose (H-04 : les réglages y copient le paquet jouet, le
 * registre se RECHARGE à chaud — ajouter un langage ne recompile
 * rien).</p>
 */
public final class LangagesDemo {

    /** Le dossier des paquets de l'utilisateur, posé par l'hôte (H-04). */
    private static volatile @Nullable Path dossierPaquets;

    /** Le fil démon du préchargement (ADR-007) — un seul, il ne sert
     *  qu'au premier dégelui de l'application. */
    private static final ExecutorService FIL_PRECHARGEMENT =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread fil = new Thread(runnable, "celeris-langages");
                fil.setDaemon(true);
                return fil;
            });

    private static volatile @Nullable LanguageRegistry registre;

    private LangagesDemo() {
    }

    /**
     * Précharge le registre hors fil UI (ADR-007) : l'assemblage —
     * lecture des vingt-deux paquets, compilation des grammaires — part sur
     * le fil démon ; l'accueil soumet ses cartes quand il vit.
     */
    public static void precharger() {
        FIL_PRECHARGEMENT.execute(LangagesDemo::registre);
    }

    /** Le registre assemblé — bâti une fois, relu ensuite. */
    public static LanguageRegistry registre() {
        LanguageRegistry present = registre;
        if (present != null) {
            return present;
        }
        synchronized (LangagesDemo.class) {
            present = registre;
            if (present == null) {
                present = assembler();
                registre = present;
            }
            return present;
        }
    }

    /**
     * Pose le dossier des paquets de l'utilisateur (H-04) : le registre
     * lira tout paquet qui y vit au prochain assemblage. À appeler AVANT
     * le préchargement — l'accueil le fait à sa création.
     *
     * @param dossier le dossier des paquets de l'utilisateur, ou null
     */
    public static void dossierPaquets(@Nullable Path dossier) {
        dossierPaquets = dossier;
    }

    /**
     * Recharge le registre à chaud (H-04) : l'assemblement suivant relit
     * les paquets intégrés ET le dossier de l'utilisateur — le langage
     * ajouté au stockage entre, le retiré sort. La session ouverte ne
     * bouge pas : les éditeurs ouverts gardent leur langage, les
     * nouveaux fichiers parlent au registre rechargé.
     */
    public static void recharger() {
        synchronized (LangagesDemo.class) {
            registre = null;
        }
    }

    /** L'assemblement : paquets intégrés, paquets de l'utilisateur,
     *  serveurs déclarés, complétion générique de repli. */
    private static LanguageRegistry assembler() {
        LanguagePlatform plateforme = LanguagePlatform.with(
                PackGrammars.INSTANCE)
                .builtinPacks()
                .servers(DeclaredServers.declarations())
                .generic(Completion.class, WordCompletion.INSTANCE);
        Path paquets = dossierPaquets;
        if (paquets != null) {
            plateforme.addPackSource(Packs.folder(paquets));
        }
        return plateforme.build();
    }

    /**
     * Le support d'un fichier de la démo : la détection du registre —
     * nom, première ligne, échantillon — sans aucune table locale.
     *
     * @param nomFichier  le nom du fichier, extension comprise
     * @param premiereLigne la première ligne, ou null
     * @param echantillon un échantillon du contenu, ou null
     * @return le support résolu, ou null sans langage
     */
    public static @Nullable LanguageSupport langageDe(String nomFichier,
            @Nullable String premiereLigne, @Nullable String echantillon) {
        LanguageDetection detection = registre().detect(nomFichier,
                premiereLigne, echantillon);
        return detection instanceof LanguageDetection.Resolved resolu
                ? resolu.support()
                : null;
    }
}
