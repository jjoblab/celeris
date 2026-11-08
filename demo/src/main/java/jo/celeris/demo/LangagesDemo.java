package jo.celeris.demo;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jo.celeris.core.intelligence.WordCompletion;
import jo.celeris.core.language.Completion;
import jo.celeris.core.language.LanguageDetection;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.grammars.PackGrammars;

import org.jspecify.annotations.Nullable;

/**
 * L'enregistrement des langages de la démonstration (M7.6) : le
 * registre assemblé de la plateforme — les seize paquets intégrés, la
 * fabrique de grammaires de la voie déclarative, la complétion
 * générique par les mots du code en repli de tous les langages. C'est
 * du code d'enregistrement : les littéraux d'identifiant y vivent (le
 * choix du serveur tiers retiré), comme dans les paquets, et nulle
 * part ailleurs — la règle {@code servers.json} des paquets les
 * remplacera au jalon LSP.
 *
 * <p>L'assemblement se fait une fois, à la première demande : le
 * chargeur de paquets travaille hors fil UI (ADR-007) — l'accueil le
 * précharge sur son exécuteur, l'atelier paie au pire le premier
 * dégelui.</p>
 */
public final class LangagesDemo {

    /** Les identifiants servis par le serveur tiers retiré (M8.6). */
    private static final Set<String> SERVES_XML_EMBARQUE = Set.of("xml");

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
     * lecture des seize paquets, compilation des grammaires — part sur
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
                present = LanguagePlatform.with(PackGrammars.INSTANCE)
                        .builtinPacks()
                        .generic(Completion.class, WordCompletion.INSTANCE)
                        .build();
                registre = present;
            }
            return present;
        }
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

    /**
     * Le serveur tiers retiré sert-il ce langage ? (M8.6 —
     * l'enregistrement de la démo ; la règle {@code servers.json} des
     * paquets remplacera cette table au jalon LSP.)
     *
     * @param languageId l'identifiant du langage
     * @return vrai quand le serveur tiers retiré le sert
     */
    public static boolean serveurRetire(String languageId) {
        return SERVES_XML_EMBARQUE.contains(languageId);
    }
}
