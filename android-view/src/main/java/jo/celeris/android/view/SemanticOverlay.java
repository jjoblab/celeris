package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * La surcouche sémantique (ADR-0027, B1) : les jetons du serveur LSP
 * RAFFINENT la coloration syntaxique — jamais ils ne la remplacent.
 * Un jeton ne survit à l'habillage que sur les plages où l'éditeur a
 * une preuve DES DEUX : la span syntaxique dessous, le jeton dessus ;
 * l'intersection seule se colore à la sorte raffinée, le reste garde
 * la couleur syntaxique. Un jeton orphelin (sans span dessous —
 * chaîne, commentaire, blanc) s'ignore : la syntaxe fait foi.
 *
 * <p>Pure et testable sans SDK : les jetons entrent déjà transposés en
 * offsets du document (la transposition LSP vit chez l'hôte, frontière
 * ADR-005) ; la fraîcheur suit la même règle que tout le reste
 * (PIEGE-20) — les jetons portent la révision du document que le
 * serveur a vu, une publication périmée s'ignore —
 * jamais elle ne colore une révision plus récente (le recalage dit
 * ici « vide ou sert », rien entre les deux).</p>
 *
 * <p>La sorte se résout par la légende du serveur (les index de types
 * convenus à l'initialisation) : un type inconnu du mapping garde la
 * sorte syntaxique — raffiner ne peut pas inventer.</p>
 */
public final class SemanticOverlay {

    /** Un jeton sémantique transposé : plage absolue et sorte. */
    private record Token(int start, int end, TokenKind kind) {
    }

    /** La surcouche vide : aucun jeton, tout retombe à la syntaxe. */
    private static final SemanticOverlay VIDE =
            new SemanticOverlay(List.of(), -1L);

    /** Les jetons triés par début, sans recouvrement. */
    private final List<Token> jetons;

    /** La révision du document que le serveur a vue. */
    private final long revisionServeur;

    private SemanticOverlay(List<Token> jetons, long revisionServeur) {
        this.jetons = jetons;
        this.revisionServeur = revisionServeur;
    }

    /**
     * La surcouche vide — la pose initiale, ou l'effacement à la
     * déconnexion du serveur.
     *
     * @return la surcouche sans effet
     */
    public static SemanticOverlay vide() {
        return VIDE;
    }

    /**
     * bâtit la surcouche d'une publication du serveur : les plages
     * absolues {@code [debut, fin[} prennent leur sorte par la
     * légende — un type inconnu de la légende ou du mapping rend le
     * jeton muet, les plages vides ou renversées aussi. Les jetons
     * qui se recouvrent : le PREMIER l'emporte, le suivant se rogne à
     * ce qui reste — le serveur ordonne, l'éditeur ne devine jamais.
     *
     * @param plages          les plages {@code [debut, fin[} des
     *                        jetons, aplaties deux à deux
     * @param types           la sorte LSP de chaque plage : l'index
     *                        dans la légende convenue
     * @param legende         la légende des types convenue avec le
     *                        serveur, vide sans
     * @param revisionServeur la révision du document que le serveur
     *                        a vue
     * @return la surcouche prête à raffiner
     */
    public static SemanticOverlay of(int[] plages, int[] types,
            List<String> legende, long revisionServeur) {
        if (plages.length != types.length * 2 || legende.isEmpty()) {
            return VIDE;
        }
        List<Token> retenus = new ArrayList<>(types.length);
        int finPrecedent = Integer.MIN_VALUE;
        for (int i = 0; i < types.length; i++) {
            int debut = plages[i * 2];
            int fin = plages[i * 2 + 1];
            if (fin <= debut) {
                continue;
            }
            TokenKind sorte = sorteDe(types[i], legende);
            if (sorte == null) {
                continue;
            }
            if (debut < finPrecedent) {
                debut = finPrecedent;
                if (fin <= debut) {
                    continue;
                }
            }
            retenus.add(new Token(debut, fin, sorte));
            finPrecedent = fin;
        }
        if (retenus.isEmpty()) {
            return VIDE;
        }
        return new SemanticOverlay(List.copyOf(retenus), revisionServeur);
    }

    /**
     * La sorte d'un index de légende — le mapping LSP vers les sortes
     * communes (ADR-004) : les types de la spécification s'y
     * ramènent, les types propriétaires restent muets.
     */
    private static @Nullable TokenKind sorteDe(int index,
            List<String> legende) {
        if (index < 0 || index >= legende.size()) {
            return null;
        }
        return SORTES.get(legende.get(index));
    }

    /** Le mapping des types LSP standard vers les sortes communes. */
    private static final Map<String, TokenKind> SORTES = Map.ofEntries(
            Map.entry("namespace", TokenKind.TYPE),
            Map.entry("class", TokenKind.TYPE),
            Map.entry("interface", TokenKind.TYPE),
            Map.entry("enum", TokenKind.TYPE),
            Map.entry("struct", TokenKind.TYPE),
            Map.entry("type", TokenKind.TYPE),
            Map.entry("typeParameter", TokenKind.TYPE),
            Map.entry("function", TokenKind.KEYWORD),
            Map.entry("method", TokenKind.KEYWORD),
            Map.entry("macro", TokenKind.KEYWORD),
            Map.entry("variable", TokenKind.IDENTIFIER),
            Map.entry("parameter", TokenKind.IDENTIFIER),
            Map.entry("property", TokenKind.IDENTIFIER),
            Map.entry("event", TokenKind.IDENTIFIER),
            Map.entry("label", TokenKind.IDENTIFIER),
            Map.entry("enumMember", TokenKind.CONSTANT),
            Map.entry("decorator", TokenKind.ANNOTATION),
            Map.entry("string", TokenKind.STRING),
            Map.entry("keyword", TokenKind.KEYWORD),
            Map.entry("number", TokenKind.NUMBER),
            Map.entry("regexp", TokenKind.STRING),
            Map.entry("operator", TokenKind.OPERATOR),
            Map.entry("comment", TokenKind.COMMENT));

    /**
     * Raffine les plages d'une rangée : chaque plage syntaxique qui
     * rencontre des jetons se COUPE à leurs intersections — chaque
     * part couverte prend la sorte du jeton qui la couvre, les parts
     * non couvertes gardent la sorte syntaxique. Jamais une plage ne
     * s'ajoute : sans preuve syntaxique dessous, le jeton ne colore
     * rien (ADR-0027, règle 1).
     *
     * @param ranges        les plages syntaxiques de la rangée,
     *                      offsets relatifs au début de la tranche
     * @param debutTranche  l'offset absolu du début de la tranche
     * @param finTranche    l'offset absolu de la fin de la tranche
     * @return les plages raffinées, ou la MÊME liste sans jeton actif
     */
    public List<ColoredSpan> raffiner(List<ColoredSpan> ranges,
            int debutTranche, int finTranche) {
        if (jetons.isEmpty() || ranges.isEmpty()) {
            return ranges;
        }
        List<ColoredSpan> rendues = null;
        for (int i = 0; i < ranges.size(); i++) {
            ColoredSpan plage = ranges.get(i);
            List<ColoredSpan> coupees = couper(plage, debutTranche);
            if (rendues != null) {
                rendues.addAll(coupees != null ? coupees : List.of(plage));
                continue;
            }
            if (coupees != null) {
                rendues = new ArrayList<>(ranges.size() + 4);
                rendues.addAll(ranges.subList(0, i));
                rendues.addAll(coupees);
            }
        }
        return rendues != null ? rendues : ranges;
    }

    /**
     * La plage coupée aux jetons qui la recouvrent — null si aucun
     * jeton ne la change : une à trois parts par jeton rencontré,
     * dans l'ordre, chaque part couverte porte la sorte de son jeton.
     */
    private @Nullable List<ColoredSpan> couper(ColoredSpan plage,
            int debutTranche) {
        int debutAbsolu = debutTranche + plage.start();
        int finAbsolue = debutTranche + plage.end();
        int premier = premierJetonApres(debutAbsolu);
        if (premier < 0 || jetons.get(premier).start() >= finAbsolue) {
            return null;
        }
        boolean changee = false;
        List<ColoredSpan> parts = new ArrayList<>(4);
        int curseur = plage.start();
        for (int j = premier; j < jetons.size(); j++) {
            Token token = jetons.get(j);
            if (token.start() >= finAbsolue) {
                break;
            }
            int coupeRelDebut = Math.max(token.start(), debutAbsolu)
                    - debutTranche;
            int coupeRelFin = Math.min(token.end(), finAbsolue)
                    - debutTranche;
            if (coupeRelFin <= coupeRelDebut) {
                continue;
            }
            if (token.kind() != plage.kind()) {
                changee = true;
            }
            if (curseur < coupeRelDebut) {
                parts.add(new ColoredSpan(plage.kind(), curseur,
                        coupeRelDebut, plage.scope()));
            }
            parts.add(new ColoredSpan(token.kind(), coupeRelDebut,
                    coupeRelFin, plage.scope()));
            curseur = coupeRelFin;
        }
        if (!changee) {
            return null;
        }
        if (curseur < plage.end()) {
            parts.add(new ColoredSpan(plage.kind(), curseur, plage.end(),
                    plage.scope()));
        }
        return parts;
    }

    /**
     * L'index du premier jeton dont la fin dépasse l'offset donné —
     * recherche binaire sur la liste triée, -1 si aucun.
     */
    private int premierJetonApres(int offset) {
        int bas = 0;
        int haut = jetons.size() - 1;
        int candidat = -1;
        while (bas <= haut) {
            int milieu = (bas + haut) >>> 1;
            if (jetons.get(milieu).end() <= offset) {
                bas = milieu + 1;
            } else {
                candidat = milieu;
                haut = milieu - 1;
            }
        }
        return candidat;
    }

    /**
     * La surcouche recalée sur le document affiché (PIEGE-20) : la
     * publication périmée survit au recalage seulement si le contenu
     * n'a pas bougé entre la révision du serveur et l'affichée — le
     * texte identique porte les mêmes offsets, la révision signe la
     * fraîcheur rendue. Toute édition qui change le texte invalide :
     * le serveur re-dira à sa synchronisation suivante, jamais la
     * vue ne recolle un jeton à devinant.
     *
     * @param affiche le document affiché
     * @return la surcouche validée sur cette révision, ou vide
     */
    public SemanticOverlay recaler(Document affiche) {
        if (jetons.isEmpty()) {
            return this;
        }
        if (affiche.revision() == revisionServeur) {
            return this;
        }
        return VIDE;
    }

    /** La révision du document que le serveur a vue. */
    public long revisionServeur() {
        return revisionServeur;
    }

    /** Vrai si la surcouche ne porte aucun jeton. */
    public boolean sansJetons() {
        return jetons.isEmpty();
    }
}
