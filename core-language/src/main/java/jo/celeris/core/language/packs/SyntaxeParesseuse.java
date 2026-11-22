package jo.celeris.core.language.packs;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;

import org.jspecify.annotations.Nullable;

/**
 * La source syntaxique PARESSEUSE du chantier A de la coloration
 * (prompt du 2026-11-22) : le registre se bâtit des DESCRIPTeurs des
 * paquets — la grammaire d'un langage ne se compile qu'à la PREMIÈRE
 * DEMANDE (la vue qui pose la langue, le coach qui lit les mots
 * réservés), jamais à l'assemblage. La langue du fichier ouvert
 * n'attend plus les grammaires des autres langages.
 *
 * <p>La compilation se joue une fois, se MÉMORISE, et reste CORRECTE
 * SOUS COURS : un fil à la fois compile (le fil d'assemblage du kit en
 * production, le fil du banc en test), les autres fils en vol pendant
 * la compilation servent le REMPLI DECLARÉ (la source écrite du
 * langage, ou null — la vue reste éditable, le RATTRAPAGE la recolorera
 * dès l'arrivée) ; une compilation défaillante se MÉMORISE AUSSI — le
 * langage reste sans grammaire pour la vie du kit (l'erreur va au
 * consommateur du kit, l'éditeur ne retente pas à chaque frappe,
 * jamais il ne compile en boucle sur le fil de l'interface).</p>
 *
 * <p>La classe vit dans le paquet des chargeurs : elle est la couture
 * entre le paquet (sa donnée : le texte de la grammaire, son repli
 * écrit) et la fabrique de l'hôte. Le lexique et l'arbre restent au
 * socle ({@code core-syntax}) : ici, aucune dépendance à une voie.</p>
 */
final class SyntaxeParesseuse implements LanguageSyntax {

    /** L'état de la source paresseuse : REPLI avant compilation,
     *  COMPILÉE ensuite, DÉFAILLANTE si la fabrique refuse — chaque
     *  état est FINAL, la mémoire ne se retourne jamais (le repli
     *  n'existe qu'avant la première compilation réussie). */
    private enum Etat {
        REPLI, COMPILEE, DEFAILLANTE
    }

    private final String id;
    private final Supplier<LanguageSyntax> compilation;
    private final @Nullable LanguageSyntax repli;
    private final AtomicReference<Etat> etat =
            new AtomicReference<>(Etat.REPLI);
    private volatile @Nullable LanguageSyntax compilee;

    /**
     * La source paresseuse d'un langage.
     *
     * @param id         l'identifiant du langage (celui du paquet)
     * @param compilation la compilation de la grammaire (fabrique de
     *                    l'hôte, appelée UNE fois, sur le fil du
     *                    premier besoin)
     * @param repli      la source déclarée du langage en attendant la
     *                   compilation (l'analyseur écrit du paquet, ou
     *                   null : la vue reste sans coloration jusqu'à
     *                   l'arrivée)
     */
    SyntaxeParesseuse(String id, Supplier<LanguageSyntax> compilation,
            @Nullable LanguageSyntax repli) {
        this.id = id;
        this.compilation = compilation;
        this.repli = repli;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LanguageLexer lexer() {
        LanguageSyntax servie = servie();
        if (servie == null) {
            // Sans grammaire compilée ni repli : le lexeur muet rend
            // la ligne SANS jeton — la vue reste éditable sans couleur,
            // le rattrapage recolorera à l'arrivée de la publication.
            return LEXEUR_MUET;
        }
        return servie.lexer();
    }

    @Override
    public LanguageParser parser() {
        LanguageSyntax servie = servie();
        if (servie == null) {
            return PARSEUR_MUET;
        }
        return servie.parser();
    }

    @Override
    public List<String> reservedWords() {
        LanguageSyntax servie = servie();
        return servie == null ? List.of() : servie.reservedWords();
    }

    /** La source qui SERT : la compilée si elle vit, le repli sinon,
     *  null si rien (compilation en cours OU défaillante — les deux
     *  disparaîtront : la première devient compilée, la seconde est
     *  définitive et le paquet fautif l'a dite au consommateur). */
    private @Nullable LanguageSyntax servie() {
        return servir(etat.get());
    }

    /** La lecture d'un état connu : la compilée si elle vit, le repli
     *  sinon, null si rien — l'état NULL n'existe pas au champ, la
     *  lecture de servie() le garantit. */
    private @Nullable LanguageSyntax servir(@Nullable Etat lu) {
        if (lu == null) {
            return repli;
        }
        return switch (lu) {
            case COMPILEE -> compilee;
            case DEFAILLANTE -> null;
            case REPLI -> tenter();
        };
    }

    /** La première demande tente la compilation : le fil qui y arrive
     *  compile, les autres servent le repli — l'atomicité de l'état
     *  fait la course, le gagnant ne recompile jamais, les perdants
     *  ne servent jamais une compilation à moitié faite. */
    private @Nullable LanguageSyntax tenter() {
        if (!etat.compareAndSet(Etat.REPLI, Etat.COMPILEE)) {
            Etat lu = etat.get();
            return lu == Etat.COMPILEE ? compilee
                    : lu == Etat.DEFAILLANTE ? null : repli;
        }
        LanguageSyntax compilee = null;
        boolean defaillante = false;
        try {
            compilee = compilation.get();
            defaillante = compilee == null;
        } finally {
            if (defaillante) {
                etat.set(Etat.DEFAILLANTE);
            }
        }
        if (defaillante) {
            // La fabrique n'a rien rendu : défaillance mémoire, la
            // grammaire n'existe pas pour ce kit — le repli ne revient
            // PAS (l'état reste défaillant).
            return null;
        }
        this.compilee = compilee;
        return compilee;
    }

    /** Le lexeur sans couleur : la ligne entière sans jeton. */
    private static final LanguageLexer LEXEUR_MUET = new LanguageLexer() {
        @Override
        public jo.celeris.core.syntax.LexState initialState() {
            return ETAT_MUET;
        }

        @Override
        public jo.celeris.core.syntax.LexedLine lex(CharSequence lineText,
                jo.celeris.core.syntax.LexState entry) {
            return new jo.celeris.core.syntax.LexedLine(
                    new jo.celeris.core.syntax.Token[0], ETAT_MUET);
        }
    };

    /** L'état unique du lexeur muet — toujours égal à lui-même. */
    private static final jo.celeris.core.syntax.LexState ETAT_MUET =
            new jo.celeris.core.syntax.LexState() {
            };

    /** Le parseur sans arbre : la racine vide des chunks muets. */
    private static final LanguageParser PARSEUR_MUET = new LanguageParser() {
        @Override
        public int chunkLevel() {
            return 0;
        }

        @Override
        public boolean startsChunk(
                jo.celeris.core.syntax.TokenSlice slice, int index, int depth) {
            return index == 0 && depth == 0;
        }

        @Override
        public jo.celeris.core.syntax.SynNode parseChunk(
                jo.celeris.core.syntax.TokenSlice slice) {
            return jo.celeris.core.syntax.SynNode.of(
                    MUE,
                    jo.celeris.core.syntax.NodeCategory.BLOCK,
                    0, slice.size(), new jo.celeris.core.syntax.SynNode[0],
                    null, false);
        }
    };

    /** La sorte neutre des nœuds du parseur muet. */
    private static final jo.celeris.core.syntax.NodeKind MUE =
            new jo.celeris.core.syntax.NodeKind() {
            };
}
