package jo.celeris.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jo.celeris.android.host.popups.LanguageSession;
import jo.celeris.core.language.model.LanguageServerDeclaration;
import jo.celeris.core.language.model.LanguageSupport;



import org.jspecify.annotations.Nullable;

/**
 * Le choix du serveur de langage de la démonstration (M7.6, H-13) :
 * la DÉCLARATION décide, jamais le code. Le fichier
 * {@code serveurs/servers.json} de la démonstration — de la donnée,
 * la sienne, l'éditeur n'en connaît pas un mot — dit quel serveur
 * sert quel langage ; ce choix monte le serveur déclaré quand il est
 * DISPONIBLE (sa classe vit dans l'application), sinon la
 * démonstration replie sur son serveur de référence (M7.5, §8) —
 * l'index répond vite et hors ligne, le serveur répond juste, la vue
 * ne sait pas qui a répondu (ADR-005).
 */
final class DeclaredServers {

    /** Le fichier des déclarations de la démonstration, aux ressources. */
    private static final String FICHIER = "/serveurs/servers.json";

    private static volatile @Nullable List<LanguageServerDeclaration> declarations;

    private DeclaredServers() {
    }

    /**
     * Les déclarations de la démonstration, lues une fois : le fichier
     * vit aux ressources du module, lu hors fil UI à la première
     * demande.
     *
     * @return les déclarations lues, éventuellement vides
     */
    static List<LanguageServerDeclaration> declarations() {
        List<LanguageServerDeclaration> presentes = declarations;
        if (presentes != null) {
            return presentes;
        }
        synchronized (DeclaredServers.class) {
            presentes = declarations;
            if (presentes == null) {
                presentes = lire();
                declarations = presentes;
            }
            return presentes;
        }
    }

    /**
     * La session de langage d'un langage ouvert : la première
     * déclaration DISPONIBLE de l'HÔTE gagne — la classe du serveur
     * embarqué charge, le montage l'exécute —, sans déclaration
     * servable le serveur de référence répond.
     *
     * <p>Seules les déclarations de l'hôte se montent (revue de
     * sécurité M9) : un paquet qui déclare un serveur embarqué ne fait
     * pas instancier sa classe — la donnée du paquet voyage jusqu'à
     * l'hôte, la décision de monter reste la sienne (H-13, ADR-005).
     * Un paquet malveillant ne choisit pas quelle classe du chemin de
     * classes s'exécute dans le processus.</p>
     *
     * @param language le support du langage ouvert, ou null sans langage
     * @return la session démarrée, prête à ouvrir un document
     */
    static LanguageSession open(@Nullable LanguageSupport language) {
        if (language != null) {
            List<LanguageServerDeclaration> deLhote = declarations();
            for (LanguageServerDeclaration declaration
                    : language.serverDeclarations()) {
                if (LanguageServerDeclaration.EMBEDDED
                        .equals(declaration.transport())
                        && deLhote.contains(declaration)
                        && disponible(declaration)) {
                    return DeclaredServerSession.start(declaration);
                }
            }
        }
        return ServeurLangage.demarrer();
    }

    /** Vrai quand la classe du serveur déclaré vit au chemin de classes. */
    private static boolean disponible(LanguageServerDeclaration declaration) {
        try {
            Class.forName(declaration.serverClass(), false,
                    DeclaredServers.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            // le serveur déclaré n'est pas embarqué ici : la
            // démonstration replie, silencieusement — la donnée reste
            // juste, la disponibilité ne l'est pas toujours
            return false;
        }
    }

    /** Lit le fichier des déclarations aux ressources du module. */
    private static List<LanguageServerDeclaration> lire() {
        try (InputStream flux = DeclaredServers.class
                .getResourceAsStream(FICHIER)) {
            if (flux == null) {
                throw new IllegalStateException(
                        "demo servers.json not found: " + FICHIER);
            }
            // la lecture par tampon reste au niveau 26 : le flux des
            // ressources n'offre ses octets entiers qu'au niveau 33
            StringBuilder texte = new StringBuilder();
            byte[] tampon = new byte[8192];
            int lus;
            while ((lus = flux.read(tampon)) > 0) {
                texte.append(new String(tampon, 0, lus,
                        StandardCharsets.UTF_8));
            }
            return List.copyOf(LanguageServerDeclaration.parse(
                    texte.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "demo servers.json unreadable: " + FICHIER, e);
        }
    }
}
