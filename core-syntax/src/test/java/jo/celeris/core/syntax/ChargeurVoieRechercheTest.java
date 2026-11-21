package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * Le chargeur de la voie native au banc (audit coloration, piste H7) :
 * sa Javadoc promet un chargeur « paresseux et unique par
 * classloader » — mais le résultat NUL ne se mémorise pas : sans le
 * module opt-in (le cas de l'exemple consommateur), chaque
 * rattachement RELIT le descripteur de service par
 * {@code ServiceLoader.load} — l'ouverture de chaque onglet paie la
 * sonde, encore et encore.
 *
 * <p>La preuve compte les lectures du descripteur de service : le
 * chargeur vit dans un classloader sonde qui définit SES PROPRES
 * copies du chargeur et de son contrat (enfant d'abord pour ces deux
 * classes, parent pour le reste) — chaque lecture de
 * {@code META-INF/services/...EnginePathFactory} s'y comptabilise.
 * Deux appels ne doivent payer QU'UNE lecture.</p>
 */
class ChargeurVoieRechercheTest {

    /** Le classloader sonde : enfant d'abord pour le chargeur de voie
     *  et son contrat, parent pour tout le reste ; chaque lecture du
     *  descripteur de service se compte. */
    private static final class SondeDeServices extends java.net.URLClassLoader {

        private final Set<String> definisParMoi;
        private final AtomicInteger lectures = new AtomicInteger();

        SondeDeServices(java.net.URL[] urls, ClassLoader parent,
                Set<String> definisParMoi) {
            super(urls, parent);
            this.definisParMoi = definisParMoi;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> chargee = findLoadedClass(name);
                if (chargee == null) {
                    if (definisParMoi.contains(name)) {
                        chargee = findClass(name);
                    } else {
                        chargee = getParent().loadClass(name);
                    }
                }
                if (resolve) {
                    resolveClass(chargee);
                }
                return chargee;
            }
        }

        @Override
        public Enumeration<java.net.URL> getResources(String name)
                throws java.io.IOException {
            if (name.endsWith("EnginePathFactory")) {
                lectures.incrementAndGet();
            }
            return super.getResources(name);
        }
    }

    /** Deux demandes sans module opt-in : le résultat nul se paie UNE
     *  fois — la seconde lit le champ, pas le META-INF. */
    @Test
    void leResultatNulSeMemorise() throws Exception {
        java.net.URL codeSource = EnginePathFactoryLoader.class
                .getProtectionDomain().getCodeSource().getLocation();
        SondeDeServices sonde = new SondeDeServices(
                new java.net.URL[] {codeSource},
                EnginePathFactoryLoader.class.getClassLoader(),
                Set.of("jo.celeris.core.syntax.EnginePathFactoryLoader",
                        "jo.celeris.core.syntax.EnginePathFactory"));
        Class<?> chargeur = Class.forName(
                "jo.celeris.core.syntax.EnginePathFactoryLoader", true, sonde);
        TextSession session = TextSession.of("class A {}", () -> 0L);
        java.lang.reflect.Method create = chargeur.getMethod("create",
                LanguageSyntax.class, TextSession.class, Executor.class);

        Object premiere = create.invoke(null, null, session, null);
        assertNull(premiere, "sans module opt-in, la voie rend null");
        int lecturesApresPremiere = sonde.lectures.get();
        assertEquals(1, lecturesApresPremiere,
                "la première demande lit le descripteur de service");

        create.invoke(null, null, session, null);
        assertEquals(lecturesApresPremiere, sonde.lectures.get(),
                "le résultat NUL se mémorise : la seconde demande ne "
                        + "relit pas le META-INF — le chargeur est unique, "
                        + "comme sa Javadoc le promet");
    }
}
