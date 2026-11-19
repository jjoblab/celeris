package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La table des guides verticaux structurels (H-09 — l'analyse
 * structurelle niveau 2 se sert par les niveaux 1 et 3, ADR-016 ;
 * A-17 relue) : un guide par BLOC du document, jamais un trait par
 * cran de blancs. La ligne qui porte l'accolade ouvrante donne SA
 * colonne au guide — le trait se tient devant le premier caractère de
 * cette ligne ; le trait part de la ligne APRÈS l'ouvrante et meurt à
 * la ligne AVANT la fermante : jamais sur l'ouvrante, jamais sur la
 * fermante, et les lignes vides du corps se joignent en lui — une
 * seule ligne continue par bloc.
 *
 * <p>La structure décide, pas la géométrie des blancs : les accolades
 * appariées des jetons servent les langages à crochets — une accolade
 * dans une chaîne ou un commentaire n'ouvre rien ; les suites de
 * commentaires (javadoc comprise) servent leur propre trait, à la
 * colonne de leur première ligne, comme les blocs qu'elles
 * documentent ; les régions d'indentation ({@link IndentationFolds})
 * servent les langages à décalage — la table suit les mêmes sources
 * que les plis de la gouttière. La table vit en LIGNES du document :
 * le pli replié ne la change pas (PIEGE-04), la projection règle les
 * rangées à l'image.</p>
 *
 * <p>La table porte SA révision : le peintre ne la lit que collée à
 * l'instantané affiché — la structure d'une autre révision ne se trace
 * jamais (PIEGE-20). L'édition la remappe comme les plis (A-15) :
 * décalée du différentiel de lignes, elle survit au vol de publication
 * (PIEGE-26) ; la ligne qui ouvre meurt, le guide meurt avec
 * elle.</p>
 */
public final class GuidesStructurels {

    /**
     * Un guide structurel : sa colonne de tracé (le retrait de la
     * ligne qui ouvre le bloc), la ligne qui ouvre, la ligne APRES la
     * dernière couverte — le guide couvre les lignes strictement
     * entre les deux — et les colonnes des crochets d'ouverture et de
     * fermeture dans leurs lignes, pour le calcul des rangées quand
     * la coupure les déplace.
     *
     * @param column         la colonne du trait, en colonnes visuelles
     * @param colonneOuvrante la colonne de l'accolade ouvrante dans sa
     *                        ligne (zéro pour un guide d'indentation)
     * @param ligneOuverture  la ligne qui porte l'ouverture du bloc
     * @param colonneFermante la colonne de l'accolade fermante dans sa
     *                        ligne (zéro pour un guide d'indentation)
     * @param ligneApresFin   la première ligne APRÈS le corps du bloc
     */
    public record Guide(int column, int colonneOuvrante, int ligneOuverture,
            int colonneFermante, int ligneApresFin) {
    }

    /** La table vide : aucune révision ne la porte, rien ne se trace. */
    public static final GuidesStructurels EMPTY =
            new GuidesStructurels(-1, List.of());

    private final int revision;
    private final List<Guide> guides;

    private GuidesStructurels(int revision, List<Guide> guides) {
        this.revision = revision;
        this.guides = guides;
    }

    /** La révision de l'instantané qui a porté la dérivation. */
    public int revision() {
        return revision;
    }

    /** Le nombre de guides de la table. */
    public int nombre() {
        return guides.size();
    }

    /**
     * Le guide d'indice donné — la table se lit à l'indice, jamais
     * d'itérateur alloué dans la boucle de dessin (PIEGE-15).
     *
     * @param indice l'indice du guide, dans {@code [0, nombre()[}
     * @return le guide
     */
    public Guide guide(int indice) {
        return guides.get(indice);
    }

    /**
     * La table des guides d'un document selon son langage : accolades
     * appariées pour les langages à crochets, régions d'indentation
     * pour les langages à décalage — null rend la table vide (sans
     * analyse, pas de structure, pas de guide).
     *
     * @param language   le support du langage, ou null
     * @param document l'instantané analysé
     * @param analyse  l'analyse courante du document
     * @return la table triée par ligne d'ouverture puis colonne
     */
    public static GuidesStructurels of(@Nullable LanguageSupport language,
            Document document, Analysis analyse) {
        if (language == null) {
            return EMPTY;
        }
        int taquet = language.configuration().indentSize();
        if (language.configuration().folding().byIndentation()) {
            return new GuidesStructurels(analyse.revision(),
                    guidesParIndentation(document, analyse.lex(), taquet,
                            language.configuration().folding().offSide()));
        }
        return new GuidesStructurels(analyse.revision(),
                guidesParCrochets(document, analyse.lex(), taquet));
    }

    /**
     * Les guides des accolades appariées ET des commentaires de bloc
     * (le signal du propriétaire du 2026-11-13 : « la ligne verticale
     * doit aussi suivre les commentaires ou javadocs ») : un balayage
     * des jetons du document, pile des ouvrantes en main — chaînes et
     * commentaires ne portent pas de jetons de crochets, une accolade
     * dedans n'ouvre rien ; chaque suite de lignes de commentaire ouvre
     * son propre trait à la colonne de sa première ligne. Le jeton de
     * crochet soude ses blancs de tête : le GLYPHE se cherche au premier
     * caractère non blanc de sa portée — jamais au début du jeton (la
     * convention du lexeur). La fermante apparie la dernière ouvrante
     * concordante : les intrus intermédiaires perdent leur tour, un
     * document imparfait rend ses guides quand même.
     */
    private static List<Guide> guidesParCrochets(Document document,
            LexDocument lex, int taquet) {
        List<int[]> pile = new ArrayList<>();
        List<Guide> guides = new ArrayList<>();
        int lines = Math.min(lex.lineCount(), document.lineCount());
        for (int line = 0; line < lines; line++) {
            String text = document.lineText(line);
            for (Token token : lex.line(line).tokens()) {
                if (token.kind() == TokenKind.OPEN_BRACKET) {
                    int column = colonneDuCrochet(text, token);
                    if (column < text.length()) {
                        pile.add(new int[] {line, column,
                                text.charAt(column)});
                    }
                } else if (token.kind() == TokenKind.CLOSE_BRACKET) {
                    int column = colonneDuCrochet(text, token);
                    if (column < text.length()) {
                        apparier(document, pile, guides, line, column,
                                text.charAt(column), taquet);
                    }
                }
            }
        }
        guides.addAll(guidesDeCommentaires(document, lex, taquet));
        return sort(guides);
    }

    /**
     * Les guides des suites de commentaires (javadoc comprise) : chaque
     * suite de lignes dont la tête parle en commentaire ouvre son trait
     * à la colonne de SA première ligne, de la ligne après l'ouverture à
     * la ligne avant la fermeture — même géométrie que les blocs
     * d'accolades, le trait suit la construction multi-lignes. Une suite
     * d'une ligne n'ouvre rien : le commentaire seul aligne déjà.
     */
    private static List<Guide> guidesDeCommentaires(Document document,
            LexDocument lex, int taquet) {
        List<Guide> guides = new ArrayList<>();
        int ouverture = -1;
        int lines = Math.min(lex.lineCount(), document.lineCount());
        for (int line = 0; line <= lines; line++) {
            boolean commentaire = line < lines
                    && porteCommentaire(lex, line);
            if (commentaire && ouverture < 0) {
                ouverture = line;
            } else if (!commentaire && ouverture >= 0) {
                if (line - 1 > ouverture) {
                    guides.add(guideDeCommentaire(document, lex, taquet,
                            ouverture, line - 1));
                }
                ouverture = -1;
            }
        }
        return guides;
    }

    /**
     * Le guide d'une suite de commentaires : colonne du retrait de sa
     * première ligne, ouverture à sa première ligne, colonnes des
     * glyphes d'ouverture et de fermeture pour la coupure des rangées.
     */
    private static Guide guideDeCommentaire(Document document,
            LexDocument lex, int taquet, int ouverture, int fermeture) {
        String texteOuverture = document.lineText(ouverture);
        String texteFermeture = document.lineText(fermeture);
        return new Guide(indentation(texteOuverture, taquet),
                colonneDuCrochet(texteOuverture, lex.line(ouverture).tokens()[0]),
                ouverture,
                colonneDuCrochet(texteFermeture,
                        dernierCommentaire(lex.line(fermeture))),
                fermeture);
    }

    /**
     * Vrai quand la tête de la ligne parle en commentaire ou
     * documentation — le commentaire de FIN de ligne ne compte pas : il
     * n'ouvre rien, il aligne avec le code qui le porte.
     */
    private static boolean porteCommentaire(LexDocument lex, int line) {
        Token[] tokens = lex.line(line).tokens();
        return tokens.length > 0 && (tokens[0].kind() == TokenKind.COMMENT
                || tokens[0].kind() == TokenKind.DOC);
    }

    /** Le dernier jeton de commentaire d'une ligne — sa ligne même sinon. */
    private static Token dernierCommentaire(LexedLine panier) {
        Token dernier = panier.tokens()[panier.tokens().length - 1];
        for (Token token : panier.tokens()) {
            if (token.kind() == TokenKind.COMMENT
                    || token.kind() == TokenKind.DOC) {
                dernier = token;
            }
        }
        return dernier;
    }

    /**
     * La colonne du glyphe de crochet : le premier caractère non
     * blanc de la portée du jeton — les blancs soudés ne sont pas le
     * crochet.
     */
    private static int colonneDuCrochet(String text, Token token) {
        int column = token.start();
        int end = Math.min(token.end(), text.length());
        while (column < end
                && (text.charAt(column) == ' ' || text.charAt(column) == '\t')) {
            column++;
        }
        return column;
    }

    /**
     * Apparie une fermante : la pile se dépile jusqu'à l'ouvrante
     * concordante — les crochets intermédiaires, jamais fermés, se
     * posent. Seule la paire d'accolades sur des lignes distinctes
     * ouvre un guide : son trait couvre les lignes de l'ouvrante
     * exclusive à la fermante exclusive.
     */
    private static void apparier(Document document, List<int[]> pile,
            List<Guide> guides, int ligneFermante, int colonneFermante,
            char fermante, int taquet) {
        char ouvrante = crochetOuvrantDe(fermante);
        if (ouvrante == 0) {
            return;
        }
        while (!pile.isEmpty()) {
            int[] sommet = pile.remove(pile.size() - 1);
            if (sommet[2] != ouvrante) {
                continue;
            }
            if (ouvrante == '{' && ligneFermante > sommet[0] + 1) {
                guides.add(new Guide(
                        indentation(document.lineText(sommet[0]), taquet),
                        sommet[1], sommet[0], colonneFermante, ligneFermante));
            }
            return;
        }
    }

    /**
     * Les guides des régions d'indentation : chaque région pliable
     * d'indentation porte un guide à la colonne de son en-tête, du
     * corps entier — les langages à décalage n'ont pas de fermante à
     * écarter.
     */
    private static List<Guide> guidesParIndentation(Document document,
            LexDocument lex, int taquet, boolean offSide) {
        List<Guide> guides = new ArrayList<>();
        for (FoldSpan region : IndentationFolds.regions(document, lex,
                new IndentationFolds.Settings(taquet, offSide))) {
            guides.add(new Guide(
                    indentation(document.lineText(region.startLine()), taquet),
                    0, region.startLine(), 0, region.endLine() + 1));
        }
        return sort(guides);
    }

    /**
     * La table remappée après une édition : les guides au-dessus de la
     * zone touchée ne bougent pas, ceux au-dessous se décalent du
     * différentiel net de sauts de ligne, ceux qui chevauchent la
     * zone s'étendent ou se rétractent du même différentiel — celui
     * dont la ligne d'ouverture meurt disparaît. Le remappage suit la
     * monotonie de l'indexation des lignes, celle des plis (A-15).
     *
     * @param revisionApres la révision du document APRÈS l'édition
     * @param avant         le document AVANT l'édition
     * @param start         le début de la plage remplacée, dans l'état
     *                      pré-éditon
     * @param end           la fin (exclusive) de la plage remplacée
     * @param removed       le texte qui occupait la plage
     * @param inserted      le texte qui la remplace
     * @return la table remappée, vide si elle l'était déjà
     */
    public GuidesStructurels remapper(int revisionApres, Document avant,
            int start, int end, String removed, String inserted) {
        if (guides.isEmpty()) {
            return EMPTY;
        }
        int delta = lineJumps(inserted) - lineJumps(removed);
        int line = avant.lineForOffset(start);
        int premiereDecalee;
        int premiereDetruite;
        int derniereDetruite;
        if (start == end) {
            premiereDecalee = start == avant.lineEnd(line) ? line + 1 : line;
            premiereDetruite = Integer.MAX_VALUE;
            derniereDetruite = -1;
        } else {
            derniereDetruite = avant.lineForOffset(end - 1);
            premiereDetruite = line;
            premiereDecalee = derniereDetruite + 1;
        }
        List<Guide> remplaces = new ArrayList<>(guides.size());
        for (Guide guide : guides) {
            Guide remplace = remapper(guide, premiereDecalee, premiereDetruite,
                    derniereDetruite, delta);
            if (remplace != null) {
                remplaces.add(remplace);
            }
        }
        return new GuidesStructurels(revisionApres, List.copyOf(remplaces));
    }

    /**
     * Remappe un guide : null quand il meurt, ses bornes neuves sinon.
     */
    private static Guide remapper(Guide guide, int premiereDecalee,
            int premiereDetruite, int derniereDetruite, int delta) {
        int ouverture = guide.ligneOuverture();
        if (ouverture >= premiereDecalee) {
            return new Guide(guide.column(), guide.colonneOuvrante(),
                    ouverture + delta, guide.colonneFermante(),
                    guide.ligneApresFin() + delta);
        }
        if (ouverture >= premiereDetruite && ouverture <= derniereDetruite) {
            return null;
        }
        int apresFin = guide.ligneApresFin();
        if (apresFin < Math.min(premiereDecalee, premiereDetruite)) {
            return guide;
        }
        apresFin += delta;
        if (apresFin <= ouverture + 1) {
            return null;
        }
        return new Guide(guide.column(), guide.colonneOuvrante(), ouverture,
                guide.colonneFermante(), apresFin);
    }

    /** Les guides triés par ligne d'ouverture puis colonne. */
    private static List<Guide> sort(List<Guide> guides) {
        guides.sort(Comparator.comparingInt(Guide::ligneOuverture)
                .thenComparingInt(Guide::column));
        return List.copyOf(guides);
    }

    /** L'ouvrante concordante d'une fermante, 0 si le caractère n'en est pas une. */
    private static char crochetOuvrantDe(char fermante) {
        switch (fermante) {
            case ')': return '(';
            case ']': return '[';
            case '}': return '{';
            default: return 0;
        }
    }

    /** Le retrait visuel d'une ligne : l'espace compte un, la tabulation au taquet. */
    private static int indentation(String text, int taquet) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += taquet - (width % taquet);
            } else {
                break;
            }
        }
        return width;
    }

    /** Le nombre de sauts de ligne d'un texte d'édition. */
    private static int lineJumps(String text) {
        int nombre = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                nombre++;
            }
        }
        return nombre;
    }
}
