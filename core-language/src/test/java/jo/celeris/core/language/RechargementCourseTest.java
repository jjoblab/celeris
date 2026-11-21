package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.syntax.clike.JavaSyntax;

/**
 * Le rechargement à chaud PENDANT l'assemblage (audit coloration,
 * piste H15) : {@code LanguageKit.reload()} n'est pas synchronisé et
 * ne réinitialise pas {@code finAssemblage} — un rechargement demandé
 * pendant qu'un assemblage court laisse les DEUX assemblages se
 * courir après : le premier qui finit POSE son registre, le second se
 * jette ({@code if (registre == null)}), et le kit sert la
 * génération d'AVANT le rechargement — silencieusement.
 *
 * <p>Le banc fige la course : une source bloquante sert la première
 * génération, le rechargement change la source, la résolution relance
 * l'assemblage — la NOUVELLE génération doit être servie, pas
 * l'ancienne.</p>
 */
final class RechargementCourseTest {

    /** La fabrique de banc : compile n'importe quoi en la façade Java. */
    private static final GrammarFactory FABRIQUE =
            (id, json) -> JavaSyntax.INSTANCE;

    /** Un paquet minimal en mémoire, pour l'extension donnée. */
    private static PackSource.PackFiles paquet(String id) {
        byte[] manifeste = ("{\"id\": \"" + id + "\", \"version\": \"1.0.0\","
                + " \"schemaVersion\": 1, \"license\": \"Apache-2.0\","
                + " \"provenance\": \"banc\"}")
                .getBytes(StandardCharsets.UTF_8);
        byte[] descripteur = ("{\"id\": \"" + id + "\", \"name\": \"" + id
                + "\", \"extensions\": [\"." + id + "\"], \"badge\": \"X\","
                + " \"badgeColor\": \"#123456\","
                + " \"syntax\": {\"preferred\": \"grammar\"},"
                + " \"comments\": {\"lineComment\": \"# \"},"
                + " \"brackets\": [[\"(\", \")\"]],"
                + " \"autoClosingPairs\": [{\"open\": \"(\", \"close\": \")\"}]}")
                .getBytes(StandardCharsets.UTF_8);
        byte[] grammaire = ("{\"name\": \"X" + id + "\", \"scopeName\": "
                + "\"source." + id + "\", \"patterns\": []}")
                .getBytes(StandardCharsets.UTF_8);
        return new PackSource.PackFiles() {

            @Override
            public String name() {
                return id;
            }

            @Override
            public boolean has(String file) {
                return file.equals("pack.json")
                        || file.equals("language.json")
                        || file.equals("grammar.json");
            }

            @Override
            public byte[] read(String file) {
                return switch (file) {
                    case "pack.json" -> manifeste;
                    case "language.json" -> descripteur;
                    default -> grammaire;
                };
            }
        };
    }

    /** Le rechargement pendant l'assemblage : la génération posée
     *  APRÈS le reload doit être servie — l'assemblage d'avant ne
     *  doit pas gagner la course en posant son registre périmé. */
    @Test
    void leRechargementPendantAssemblageNePerdPasLaNouvelleGeneration()
            throws InterruptedException {
        CountDownLatch premierAppelVu = new CountDownLatch(1);
        CountDownLatch libererPremier = new CountDownLatch(1);
        AtomicInteger generation = new AtomicInteger(1);
        PackSource source = () -> {
            int servie = generation.get();
            if (servie == 1) {
                premierAppelVu.countDown();
                try {
                    if (!libererPremier.await(10L, TimeUnit.SECONDS)) {
                        throw new java.io.IOException(
                                "banc : assemblage 1 bloqué");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("banc interrompu");
                }
            }
            return List.of(paquet("g" + servie));
        };
        ExecutorService fil = Executors.newSingleThreadExecutor(tache -> {
            Thread filBanc = new Thread(tache, "banc-assemblage");
            filBanc.setDaemon(true);
            return filBanc;
        });
        try {
            LanguageKit kit = LanguageKit.builder(fil)
                    .grammarFactory(FABRIQUE)
                    .addSource(source)
                    .build();
            kit.preload();
            assertTrue(premierAppelVu.await(10L, TimeUnit.SECONDS),
                    "l'assemblage 1 a bien commencé (bloqué dans la source)");
            kit.reload();
            generation.set(2);
            // L'assemblage 1 se libère : il pose SA génération — puis la
            // résolution relance l'assemblage 2 : la nouvelle génération
            // doit gagner, pas l'ancienne.
            libererPremier.countDown();
            LanguageSupport neuve = kit.languageFor("neuf.g2");
            assertNotNull(neuve,
                    "la génération posée après reload doit être servie — "
                            + "l'assemblage d'avant le rechargement ne doit "
                            + "pas imposer son registre périmé");
        } finally {
            libererPremier.countDown();
            fil.shutdownNow();
        }
    }
}
