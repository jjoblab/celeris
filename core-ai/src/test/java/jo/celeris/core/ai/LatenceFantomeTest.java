package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'annulation et la latence du texte fantôme (F-09, ADR-012) : la
 * frappe suivante annule le vol en cours, l'attente d'un modèle ne
 * bloque jamais — la preuve structurelle d'abord, la mesure PIEGE-13
 * ensuite (minimum de trois tours, jamais un tour unique).
 */
class LatenceFantomeTest {

    /** Le réseau aveugle : rien ne revient jamais de ce modèle. */
    private static final Executor RESEAU_AVEUGLE = tache -> {
        // rien — la réponse n'arrive pas, la frappe ne doit rien attendre
    };

    /**
     * F-09 — le jeton de la première requête meurt à la frappe suivante :
     * frapper, redemander, frapper encore — chaque vol supplante le
     * précédent, aucun ne survit.
     */
    @Test
    void jetonAnnuleALaDeuxiemeFrappe() {
        FauxModeleDistant modele = new FauxModeleDistant(RESEAU_AVEUGLE);
        SessionTexteFantome session = new SessionTexteFantome(modele, Runnable::run);
        Document document = Document.of("int x = ");

        session.demander(document, 8);
        session.frappe();
        assertTrue(modele.requetes().get(0).jeton().estAnnule(),
                "la frappe suivante annule le vol en cours");

        session.demander(document, 8);
        session.frappe();
        assertTrue(modele.requetes().get(1).jeton().estAnnule(),
                "le second vol meurt pareil");
        assertNull(session.suggestionActive(),
                "rien ne s'affiche d'un modèle qui ne répond pas");
    }

    /**
     * F-09 — l'attente ne bloque jamais la frappe : mille demandes vers
     * un modèle qui ne répond jamais, la session rend la main à chaque
     * fois — la frappe reste libre, le fantôme absent.
     */
    @Test
    void milleDemandesAuModeleAveugleNeBloquentPas() {
        FauxModeleDistant modele = new FauxModeleDistant(RESEAU_AVEUGLE);
        SessionTexteFantome session = new SessionTexteFantome(modele, Runnable::run);
        Document document = Document.of("int x = ");

        for (int i = 0; i < 1_000; i++) {
            session.demander(document, 8);
            session.frappe();
        }
        assertEquals(1_000, modele.requetes().size(),
                "chaque demande est partie, aucune n'a attendu");
        assertTrue(modele.requetes().get(999).jeton().estAnnule());
        assertNull(session.suggestionActive());
    }

    /**
     * PIEGE-13 — la mesure du non-blocage : mille demandes vers le
     * modèle aveugle, minimum de trois tours, assertion sur le minimum.
     * Le budget (1 s) est une protection structurelle — le chemin est
     * fait d'appels purs, un facteur immense au-dessus de la mesure —
     * pas une chasse aux pourcents de bruit.
     */
    @Test
    void mesureMilleDemandesMinDeTroisTours() {
        FauxModeleDistant modele = new FauxModeleDistant(RESEAU_AVEUGLE);
        SessionTexteFantome session = new SessionTexteFantome(modele, Runnable::run);
        Document document = Document.of("int x = ");

        Runnable phaseMilleDemandes = () -> {
            for (int i = 0; i < 1_000; i++) {
                session.demander(document, 8);
            }
            session.frappe();
        };
        long minimum = minDeTroisTours(System::nanoTime, phaseMilleDemandes);
        System.out.printf("[benchmark] fantome : 1000 demandes au modele aveugle"
                        + " (min de 3 tours) : %.2f ms%n",
                minimum / 1e6);
        assertTrue(minimum < 1_000_000_000L,
                "demander bloque la frappe : " + (minimum / 1e6) + " ms (min de 3 tours)");
    }

    /**
     * Le protocole PIEGE-13 : trois tours de la phase, minimum conservé
     * — le bruit du runner ne fait qu'ajouter du temps, le minimum
     * l'écarte. (Le protocole vit en test de core-text ; le voilà, le
     * temps que la mesure IA gagne son harnais propre.)
     */
    private static long minDeTroisTours(java.util.function.LongSupplier horloge,
            Runnable phase) {
        long meilleur = Long.MAX_VALUE;
        for (int tour = 0; tour < 3; tour++) {
            long debut = horloge.getAsLong();
            phase.run();
            long duree = horloge.getAsLong() - debut;
            if (duree < meilleur) {
                meilleur = duree;
            }
        }
        return meilleur;
    }
}
