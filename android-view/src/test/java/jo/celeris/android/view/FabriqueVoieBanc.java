package jo.celeris.android.view;

import java.util.concurrent.Executor;

import jo.celeris.core.syntax.EnginePathFactory;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.MoteurSyntaxe;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La fabrique de la voie de BANC (feuille de route A1.2) : inscrite
 * par le META-INF/services des ressources de TEST de android-view —
 * le {@code ServiceLoader} du pont la charge dans les bancs, le
 * moteur maquette y prend la place de la voie native. Elle ne sert
 * QUE le banc {@link RattrapageVoieNatifTest} : la classe vit dans
 * les sources de test, jamais dans les artefacts.
 *
 * <p>Le pont délègue à la fabrique la DERNIÈRE session attachée —
 * le banc n'ouvre qu'une session à la fois, la fabrique rend un
 * moteur neuf par demande (comme la vraie : un moteur par
 * rattachement).</p>
 */
public final class FabriqueVoieBanc implements EnginePathFactory {

    /** La dernière session demandée — le banc y lit son moteur. */
    static volatile TextSession derniereSession;

    /** L'arme du banc : la fabrique ne sert QUE le banc A1.2 — les
     *  autres bancs du module gardent la voie du socle, la fabrique
     *  muette ne change RIEN pour eux. */
    static volatile boolean armee;

    /** Le dernier moteur servi — le banc y lit ses publications. */
    static volatile MoteurSyntaxe dernierMoteur;

    /** Compteur des moteurs servis — le banc asserte le rattachement. */
    static volatile int moteurs;

    @Override
    public @Nullable MoteurSyntaxe creer(@Nullable LanguageSyntax source,
            TextSession session, @Nullable Executor executeur) {
        // La voie ne sert que les sources de la langue de banc : la
        // JavaSyntax marque la voie (toute source non nulle y passe —
        // le banc n'attache qu'elle).
        if (source == null || !armee) {
            return null;
        }
        derniereSession = session;
        MoteurSyntaxe moteur = new RattrapageVoieNatifTest.MoteurMaquette(
                session, executeur);
        dernierMoteur = moteur;
        moteurs++;
        return moteur;
    }
}
