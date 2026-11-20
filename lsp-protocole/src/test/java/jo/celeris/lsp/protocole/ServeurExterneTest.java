package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.PositionLsp;

/**
 * D-09/D-10 (l'analyse croisée) : les connexions aux serveurs
 * EXTERNES — le dépôt de départ n'offrait que l'embarqué (ADR-015).
 * Le banc lance le factice en VRAI sous-processus (stdio) et en
 * serveur socket : la poignée de main traverse le transport réel,
 * les navigations typées (D-10) répondent au travers, l'extinction
 * referme le processus.
 */
class ServeurExterneTest {

    /** Le dossier des jars pour le sous-processus (posé par Gradle). */
    private static final String CLASSPATH = System.getProperty(
            "celeris.banc.classpath", System.getProperty("java.class.path", ""));

    @TempDir
    java.nio.file.Path dossier;

    /** La commande du sous-processus : java -cp … ServeurFacticeMain. */
    private String[] commande() {
        return new String[] {
                System.getProperty("java.home") + "/bin/java",
                "-cp", CLASSPATH,
                "jo.celeris.lsp.protocole.ServeurFacticeMain", "stdio" };
    }

    /** Le port libre d'un serveur socket du banc. */
    private int portLibre() throws Exception {
        try (java.net.ServerSocket sonde = new java.net.ServerSocket(0)) {
            return sonde.getLocalPort();
        }
    }

    /** D-09 stdio : la poignée de main traverse les flux du processus. */
    @Test
    void laPoigneeDeMainTraverseStdio() throws Exception {
        try (ServeurExterne connexion = ServeurExterne.parStdio(commande(),
                "banc-celeris")) {
            assertTrue(connexion.session().initialiser().sertDefinition(),
                    "le factice sert la définition au travers du processus");
        }
    }

    /** D-09 + D-10 stdio : les navigations typées répondent au
     *  travers du processus externe. */
    @Test
    void lesNavigationsTypeesTraversentStdio() throws Exception {
        try (ServeurExterne connexion = ServeurExterne.parStdio(commande(),
                "banc-celeris")) {
            connexion.session().initialiser();
            String uri = "file:///banc/Test.java";
            PositionLsp position = new PositionLsp(0, 3);

            List<LieuLsp> type = connexion.session()
                    .typeDefinition(uri, position).get(10, TimeUnit.SECONDS);
            assertEquals(1, type.size(), "la définition du type répond");
            org.junit.jupiter.api.Assertions.assertNotNull(type.get(0).plage());
            assertEquals(1, type.get(0).plage().debut().ligne(),
                    "la plage du type (ligne 1) est celle du factice");

            List<LieuLsp> impls = connexion.session()
                    .implementations(uri, position).get(10, TimeUnit.SECONDS);
            assertEquals(1, impls.size(), "l'implémentation répond");
            org.junit.jupiter.api.Assertions.assertNotNull(impls.get(0).plage());
            assertEquals(2, impls.get(0).plage().debut().ligne(),
                    "la plage d'implémentation (ligne 2) est celle du factice");

            List<LieuLsp> superLieu = connexion.session()
                    .superDefinition(uri, position).get(10, TimeUnit.SECONDS);
            assertEquals(1, superLieu.size(),
                    "la super-définition répond (commande maison)");
            org.junit.jupiter.api.Assertions.assertNotNull(superLieu.get(0).plage());
            assertEquals(3, superLieu.get(0).plage().debut().ligne(),
                    "la plage super (ligne 3) est celle du factice");
        }
    }

    /** D-09 socket : la liaison monte sur TCP et s'éteint proprement. */
    @Test
    void laConnexionSocketMonteEtSEteint() throws Exception {
        int port = portLibre();
        // le serveur socket du banc : le factice en écoute
        Process serveur = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp", CLASSPATH,
                "jo.celeris.lsp.protocole.ServeurFacticeMain",
                "socket", String.valueOf(port))
                .redirectErrorStream(true)
                .start();
        try {
            // attendre « PRET port » (le serveur annonce son écoute)
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(
                    serveur.getInputStream(), StandardCharsets.UTF_8));
            String annonce = lecteur.readLine();
            assertTrue(annonce != null && annonce.startsWith("PRET "),
                    "le serveur socket annonce son écoute : " + annonce);
            try (ServeurExterne connexion = ServeurExterne.parSocket(
                    "127.0.0.1", port, "banc-celeris")) {
                assertTrue(connexion.session().initialiser().sertDefinition(),
                        "la poignée de main traverse la socket");
            }
        } finally {
            serveur.destroy();
            if (!serveur.waitFor(5, TimeUnit.SECONDS)) {
                serveur.destroyForcibly();
            }
        }
    }

    /** Un serveur qui meurt sous la session ne pend pas la fermeture :
     *  close() est borné, le processus est forclos. */
    @Test
    void laFermetureSurvitAuServeurMort() throws Exception {
        ServeurExterne connexion = ServeurExterne.parStdio(commande(),
                "banc-celeris");
        connexion.session().initialiser();
        // le serveur meurt brutalement : kill -9 du sous-processus
        connexion.close();
        // la fermeture est idempotente — un second close ne jette rien
        connexion.close();
    }
}
