package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.LexDocument;
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
 * dans une chaîne ou un commentaire n'ouvre rien ; les régions
 * d'indentation ({@link IndentationFolds}) servent les langages à
 * décalage — la table suit les mêmes sources que les plis de la
 * gouttière. La table vit en LIGNES du document : le pli replié ne la
 * change pas (PIEGE-04), la projection règle les rangées à
 * l'image.</p>
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
     * @param colonne         la colonne du trait, en colonnes visuelles
     * @param colonneOuvrante la colonne de l'accolade ouvrante dans sa
     *                        ligne (zéro pour un guide d'indentation)
     * @param ligneOuverture  la ligne qui porte l'ouverture du bloc
     * @param colonneFermante la colonne de l'accolade fermante dans sa
     *                        ligne (zéro pour un guide d'indentation)
     * @param ligneApresFin   la première ligne APRÈS le corps du bloc
     */
    public record Guide(int colonne, int colonneOuvrante, int ligneOuverture,
            int colonneFermante, int ligneApresFin) {
    }

    /** La table vide : aucune révision ne la porte, rien ne se trace. */
    public static final GuidesStructurels VIDE =
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
     * @param langue   le support du langage, ou null
     * @param document l'instantané analysé
     * @param analyse  l'analyse courante du document
     * @return la table triée par ligne d'ouverture puis colonne
     */
    public static GuidesStructurels de(@Nullable LanguageSupport langue,
            Document document, Analysis analyse) {
        if (langue == null) {
            return VIDE;
        }
        int taquet = langue.configuration().indentSize();
        if (langue.configuration().folding().byIndentation()) {
            return new GuidesStructurels(analyse.revision(),
                    guidesParIndentation(document, analyse.lex(), taquet,
                            langue.configuration().folding().offSide()));
        }
        return new GuidesStructurels(analyse.revision(),
                guidesParCrochets(document, analyse.lex(), taquet));
    }

    /**
     * Les guides des accolades appariées : un balayage des jetons du
     * document, pile des ouvrantes en main — chaînes et commentaires
     * ne portent pas de jetons de crochets, une accolade dedans
     * n'ouvre rien. Le jeton de crochet soude ses blancs de tête : le
     * GLYPHE se cherche au premier caractère non blanc de sa portée —
     * jamais au début du jeton (la convention du lexeur). La fermante
     * apparie la dernière ouvrante concordante : les intrus
     * intermédiaires perdent leur tour, un document imparfait rend
     * ses guides quand même.
     */
    private static List<Guide> guidesParCrochets(Document document,
            LexDocument lex, int taquet) {
        List<int[]> pile = new ArrayList<>();
        List<Guide> guides = new ArrayList<>();
        int lignes = Math.min(lex.lineCount(), document.lineCount());
        for (int ligne = 0; ligne < lignes; ligne++) {
            String texte = document.lineText(ligne);
            for (Token jeton : lex.line(ligne).tokens()) {
                if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                    int colonne = colonneDuCrochet(texte, jeton);
                    if (colonne < texte.length()) {
                        pile.add(new int[] {ligne, colonne,
                                texte.charAt(colonne)});
                    }
                } else if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                    int colonne = colonneDuCrochet(texte, jeton);
                    if (colonne < texte.length()) {
                        apparier(document, pile, guides, ligne, colonne,
                                texte.charAt(colonne), taquet);
                    }
                }
            }
        }
        return trier(guides);
    }

    /**
     * La colonne du glyphe de crochet : le premier caractère non
     * blanc de la portée du jeton — les blancs soudés ne sont pas le
     * crochet.
     */
    private static int colonneDuCrochet(String texte, Token jeton) {
        int colonne = jeton.start();
        int fin = Math.min(jeton.end(), texte.length());
        while (colonne < fin
                && (texte.charAt(colonne) == ' ' || texte.charAt(colonne) == '\t')) {
            colonne++;
        }
        return colonne;
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
        return trier(guides);
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
            return VIDE;
        }
        int delta = sautsDeLigne(inserted) - sautsDeLigne(removed);
        int ligne = avant.lineForOffset(start);
        int premiereDecalee;
        int premiereDetruite;
        int derniereDetruite;
        if (start == end) {
            premiereDecalee = start == avant.lineEnd(ligne) ? ligne + 1 : ligne;
            premiereDetruite = Integer.MAX_VALUE;
            derniereDetruite = -1;
        } else {
            derniereDetruite = avant.lineForOffset(end - 1);
            premiereDetruite = ligne;
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
            return new Guide(guide.colonne(), guide.colonneOuvrante(),
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
        return new Guide(guide.colonne(), guide.colonneOuvrante(), ouverture,
                guide.colonneFermante(), apresFin);
    }

    /** Les guides triés par ligne d'ouverture puis colonne. */
    private static List<Guide> trier(List<Guide> guides) {
        guides.sort(Comparator.comparingInt(Guide::ligneOuverture)
                .thenComparingInt(Guide::colonne));
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
    private static int indentation(String texte, int taquet) {
        int largeur = 0;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == ' ') {
                largeur++;
            } else if (c == '\t') {
                largeur += taquet - (largeur % taquet);
            } else {
                break;
            }
        }
        return largeur;
    }

    /** Le nombre de sauts de ligne d'un texte d'édition. */
    private static int sautsDeLigne(String texte) {
        int nombre = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                nombre++;
            }
        }
        return nombre;
    }
}
