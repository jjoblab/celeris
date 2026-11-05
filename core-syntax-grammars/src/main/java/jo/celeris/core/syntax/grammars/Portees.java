package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.TokenKind;

/**
 * Le mapping portées → sortes de jetons (voie héritée du dépôt de
 * départ, ADR-004) : les portées pointées du format TextMate se
 * rabattent sur les quatorze sortes communes du socle — la coloration
 * d'une grammaire vit dans les mêmes paniers que celle des analyseurs
 * dédiés. La portée la plus longue gagne : {@code keyword.operator}
 * avant {@code keyword}, {@code punctuation.section.<x>.begin} en
 * crochet ouvrant apparié.
 */
final class Portees {

    private Portees() {
    }

    /**
     * La sorte de jeton d'une portée.
     *
     * @param portee la portée pointée (par exemple keyword.operator.toml)
     * @return la sorte commune, IDENTIFIER par défaut
     */
    static TokenKind de(String portee) {
        int point = portee.indexOf('.');
        String tete = point < 0 ? portee : portee.substring(0, point);
        return switch (tete) {
            case "comment" -> commentaire(portee);
            case "string" -> TokenKind.STRING;
            case "constant" -> constante(portee);
            case "keyword" -> motCle(portee);
            case "storage" -> TokenKind.KEYWORD;
            case "entity" -> entite(portee);
            case "support" -> support(portee);
            case "punctuation" -> ponctuation(portee);
            case "markup" -> markup(portee);
            case "meta" -> meta(portee);
            default -> TokenKind.IDENTIFIER;
        };
    }

    /** La documentation se distingue du commentaire ordinaire. */
    private static TokenKind commentaire(String portee) {
        return portee.startsWith("comment.block.documentation")
                ? TokenKind.DOC : TokenKind.COMMENT;
    }

    /** Le nombre se distingue des autres constantes. */
    private static TokenKind constante(String portee) {
        return portee.startsWith("constant.numeric")
                ? TokenKind.NUMBER : TokenKind.CONSTANT;
    }

    /** L'opérateur se distingue du mot réservé. */
    private static TokenKind motCle(String portee) {
        return portee.startsWith("keyword.operator")
                ? TokenKind.OPERATOR : TokenKind.KEYWORD;
    }

    /** Le soutien : types et constantes nommées se distinguent. */
    private static TokenKind support(String portee) {
        if (portee.startsWith("support.type")) {
            return TokenKind.TYPE;
        }
        return portee.startsWith("support.constant")
                ? TokenKind.CONSTANT : TokenKind.IDENTIFIER;
    }

    /** L'annotation se distingue du reste, transparent. */
    private static TokenKind meta(String portee) {
        return portee.startsWith("meta.annotation")
                ? TokenKind.ANNOTATION : TokenKind.IDENTIFIER;
    }

    /** Les entités nommées : types, sections et balises se distinguent. */
    private static TokenKind entite(String portee) {
        if (portee.startsWith("entity.name.type")
                || portee.startsWith("entity.name.section")
                || portee.startsWith("entity.name.tag")) {
            return TokenKind.TYPE;
        }
        return TokenKind.IDENTIFIER;
    }

    /** La ponctuation : les sections début/fin portent les crochets appariés. */
    private static TokenKind ponctuation(String portee) {
        if (portee.startsWith("punctuation.section.")) {
            if (aSegment(portee, "begin")) {
                return TokenKind.OPEN_BRACKET;
            }
            if (aSegment(portee, "end")) {
                return TokenKind.CLOSE_BRACKET;
            }
        }
        return TokenKind.PUNCTUATION;
    }

    /** Vrai quand la portée compte un segment exactement égal au mot donné. */
    private static boolean aSegment(String portee, String mot) {
        int depuis = 0;
        int point = portee.indexOf('.');
        while (point >= 0) {
            if (portee.substring(depuis, point).equals(mot)) {
                return true;
            }
            depuis = point + 1;
            point = portee.indexOf('.', depuis);
        }
        return portee.substring(depuis).equals(mot);
    }

    /** Le balisage léger : le brut en encre de chaîne, la citation en commentaire. */
    private static TokenKind markup(String portee) {
        if (portee.startsWith("markup.raw") || portee.startsWith("markup.underline")) {
            return TokenKind.STRING;
        }
        if (portee.startsWith("markup.quote")) {
            return TokenKind.COMMENT;
        }
        if (portee.startsWith("markup.heading")) {
            return TokenKind.TYPE;
        }
        return TokenKind.CONSTANT;
    }
}
