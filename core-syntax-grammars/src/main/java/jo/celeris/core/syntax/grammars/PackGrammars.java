package jo.celeris.core.syntax.grammars;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.syntax.LanguageSyntax;


/**
 * La fabrique de sources syntaxiques par grammaire (M7.6) :
 * l'implémentation de la couture {@link GrammarFactory} du module du
 * bas — le chargeur de paquets compile la grammaire JSON par la voie
 * de grammaires, la préférence {@code "grammar"} du
 * {@link jo.celeris.core.language.packs.LanguagePack.SyntaxPreference}
 * la résout. Le code de compilation vit ici (Grammaires), le paquet
 * reste de la donnée.
 *
 * <p>Les inclusions externes (chantier C4, prompt du 2026-11-22) : le
 * registre partagé par portée racine — une grammaire qui inclut
 * {@code source.css} y trouve celle du paquet css du même montage ; le
 * registre se remplit au fil des compilations (chaque grammaire
 * compilée s'y pose), les inclusions inconnues restent sautées,
 * comptées au rapport — comme avant, sans refus.</p>
 */
public final class PackGrammars implements GrammarFactory {

    /** L'instance sans registre — les inclusions externes se sautent. */
    public static final PackGrammars INSTANCE = new PackGrammars();

    /** Les grammaires compilées par portée racine, partagées par les
     *  inclusions externes du même montage. La mise à jour est
     *  paresseuse et sûre sous cours : une portée absente se compile
     *  par le fil qui la demande, les autres attendent, la grammaire
     *  se pose UNE fois. */
    private final Map<String, Grammar> registre =
            new ConcurrentHashMap<>();

    private PackGrammars() {
    }

    /** La fabrique à registre partagé — les grammaires du montage s'y
     *  posent, les inclusions externes s'y résolvent (html colore
     *  {@code <style>} par la grammaire css du même montage).
     *
     * @return la fabrique avec registre
     */
    public static PackGrammars avecRegistre() {
        return new PackGrammars();
    }

    /**
     * Compile la grammaire JSON d'un langage en source syntaxique —
     * les erreurs de Grammaires remontent telles quelles : le message
     * précis du refus. La grammaire compilée se pose au registre par
     * sa portée racine : les inclusions externes des grammaires
     * suivantes s'y résolvent.
     *
     * @param languageId  l'identifiant du langage du paquet
     * @param grammarJson le texte JSON de la grammaire
     * @return la source syntaxique de la voie de grammaires
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    @Override
    public LanguageSyntax compile(String languageId, String grammarJson) {
        Grammar grammaire = Grammars.read(grammarJson, registre);
        // La grammaire se pose au registre : les inclusions externes
        // des langages suivants la trouvent (le fil de l'assemblage
        // est unique — le kit y passe paquet par paquet ; une pose
        // concurrente ne change rien : la même grammaire y retourne).
        registre.putIfAbsent(grammaire.porteeRacine(), grammaire);
        return new GrammarSyntax(languageId, grammaire);
    }
}
