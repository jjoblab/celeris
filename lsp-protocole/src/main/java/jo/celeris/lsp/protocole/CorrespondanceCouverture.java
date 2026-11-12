package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.jspecify.annotations.Nullable;













/**
 * La correspondance des réponses de la COUVERTURE du jalon (H-12,
 * ADR-015) : références, symboles, plis, surlignages, renommage,
 * correctifs, formatage, jetons, indices et plages de sélection y
 * deviennent les types immuables du domaine — la suite naturelle de
 * {@link CorrespondanceDocuments}, chaque forme du protocole reçoit
 * sa lecture.
 */
final class CorrespondanceCouverture {

    private CorrespondanceCouverture() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    // ── La couverture du jalon (H-12) ────────────────────────────────

    /** Les lieux plats des références — la liste directe du protocole. */
    static List<LieuLsp> lireLieuxPlats(
            @Nullable List<? extends Location> reponse) {
        List<LieuLsp> lieux = new ArrayList<>();
        if (reponse != null) {
            for (Location lieu : reponse) {
                if (lieu != null) {
                    lieux.add(new LieuLsp(lieu.getUri(),
                            CorrespondanceLsp.lirePlage(lieu.getRange())));
                }
            }
        }
        return List.copyOf(lieux);
    }

    /**
     * Les symboles lus : chaque entrée porte sa forme — plate ou
     * hiérarchique — le plan et le fil d'Ariane y trouvent la même
     * matière, les enfants suivent la profondeur du serveur.
     */
    static List<SymboleLsp> lireSymboles(
            @Nullable List<? extends Either<SymbolInformation,
                    DocumentSymbol>> reponse) {
        List<SymboleLsp> symboles = new ArrayList<>();
        if (reponse != null) {
            for (Either<SymbolInformation, DocumentSymbol> symbole : reponse) {
                if (symbole == null) {
                    continue;
                }
                if (symbole.isLeft() && symbole.getLeft() != null) {
                    symboles.add(lireSymbolePlat(symbole.getLeft()));
                } else if (symbole.isRight() && symbole.getRight() != null) {
                    symboles.add(lireSymbole(symbole.getRight()));
                }
            }
        }
        return List.copyOf(symboles);
    }

    /** Le symbole plat : sa plage porte la sélection. */
    private static SymboleLsp lireSymbolePlat(SymbolInformation symbole) {
        return new SymboleLsp(symbole.getName() == null ? "" : symbole.getName(),
                symbole.getKind() == null ? 0 : symbole.getKind().getValue(),
                CorrespondanceLsp.lirePlage(symbole.getLocation() == null
                        ? null : symbole.getLocation().getRange()),
                null, List.of());
    }

    /** Le symbole hiérarchique : ses enfants suivent, la profondeur entière. */
    private static SymboleLsp lireSymbole(DocumentSymbol symbole) {
        List<SymboleLsp> enfants = new ArrayList<>();
        if (symbole.getChildren() != null) {
            for (DocumentSymbol enfant : symbole.getChildren()) {
                if (enfant != null) {
                    enfants.add(lireSymbole(enfant));
                }
            }
        }
        return new SymboleLsp(symbole.getName() == null ? "" : symbole.getName(),
                symbole.getKind() == null ? 0 : symbole.getKind().getValue(),
                CorrespondanceLsp.lirePlage(symbole.getRange()),
                CorrespondanceLsp.lirePlage(symbole.getSelectionRange()),
                enfants);
    }

    /**
     * Les plis lus : la fin inclusive du protocole devient EXCLUSIVE —
     * la convention de l'arbre commun, jamais deux conventions pour un
     * même consommateur.
     */
    static List<PliLsp> lirePlis(
            @Nullable List<? extends FoldingRange> reponse) {
        List<PliLsp> plis = new ArrayList<>();
        if (reponse != null) {
            for (FoldingRange pli : reponse) {
                if (pli == null) {
                    continue;
                }
                // la fin du protocole est INCLUSIVE : +1 la rend exclusive
                plis.add(new PliLsp(pli.getStartLine(), pli.getEndLine() + 1));
            }
        }
        return List.copyOf(plis);
    }

    /** Les surlignages d'occurrences lus — le genre absent vaut texte. */
    static List<SurlignageLsp> lireSurlignages(
            @Nullable List<? extends DocumentHighlight> reponse) {
        List<SurlignageLsp> surlignages = new ArrayList<>();
        if (reponse != null) {
            for (DocumentHighlight surlignage : reponse) {
                if (surlignage == null) {
                    continue;
                }
                surlignages.add(new SurlignageLsp(
                        CorrespondanceLsp.lirePlage(surlignage.getRange()),
                        surlignage.getKind() == null ? 0
                                : surlignage.getKind().getValue()));
            }
        }
        return List.copyOf(surlignages);
    }

    /**
     * La préparation de renommage lue : la plage seule ou l'objet
     * amorce — la troisième forme (comportement par défaut) ne porte
     * ni plage ni amorce : le renommage qui ne dit rien ne devine pas.
     */
    static @Nullable PreparationRennomageLsp lirePreparationRennomage(
            @Nullable Either3<Range, PrepareRenameResult,
                    PrepareRenameDefaultBehavior> reponse) {
        if (reponse == null) {
            return null;
        }
        if (reponse.isFirst() && reponse.getFirst() != null) {
            return new PreparationRennomageLsp(
                    CorrespondanceLsp.lirePlage(reponse.getFirst()), null);
        }
        if (reponse.isSecond() && reponse.getSecond() != null) {
            PrepareRenameResult prepare = reponse.getSecond();
            return new PreparationRennomageLsp(
                    CorrespondanceLsp.lirePlage(prepare.getRange()),
                    prepare.getPlaceholder());
        }
        return null;
    }

    /**
     * Les correctifs lus : la commande nue (sans édition) reste un
     * correctif — l'hôte montre le titre, l'édition vide ne fait rien.
     */
    static List<CorrectifLsp> lireCorrectifs(
            @Nullable List<? extends Either<Command, CodeAction>> reponse) {
        List<CorrectifLsp> correctifs = new ArrayList<>();
        if (reponse != null) {
            for (Either<Command, CodeAction> correctif : reponse) {
                if (correctif == null) {
                    continue;
                }
                if (correctif.isLeft() && correctif.getLeft() != null) {
                    Command commande = correctif.getLeft();
                    correctifs.add(new CorrectifLsp(
                            commande.getTitle() == null ? ""
                                    : commande.getTitle(),
                            EditionEspaceLsp.VIDE));
                } else if (correctif.isRight() && correctif.getRight() != null) {
                    CodeAction action = correctif.getRight();
                    correctifs.add(new CorrectifLsp(
                            action.getTitle() == null ? "" : action.getTitle(),
                            CorrespondanceEspace.lireEspace(action.getEdit())));
                }
            }
        }
        return List.copyOf(correctifs);
    }

    /** Les éditions du formatage : la liste du protocole, l'ordre du serveur. */
    static List<EditionTexteLsp> lireFormatage(
            @Nullable List<? extends TextEdit> reponse) {
        return CorrespondanceEspace.lireEditions(reponse);
    }

    /** Les jetons sémantiques lus : la charge d'entiers, le décodage au domaine. */
    static SemanticTokensLsp lireJetons(@Nullable SemanticTokens jetons) {
        if (jetons == null || jetons.getData() == null) {
            return SemanticTokensLsp.de(null, List.of());
        }
        return SemanticTokensLsp.de(jetons.getResultId(), jetons.getData());
    }

    /** Les indices en ligne lus : les parties typées, la cible de navigation. */
    static List<IndiceLsp> lireIndices(
            @Nullable List<? extends InlayHint> reponse) {
        List<IndiceLsp> indices = new ArrayList<>();
        if (reponse != null) {
            for (InlayHint indice : reponse) {
                if (indice == null) {
                    continue;
                }
                List<IndiceLsp.Partie> parties = new ArrayList<>();
                Either<String, List<org.eclipse.lsp4j.InlayHintLabelPart>> libelle =
                        indice.getLabel();
                if (libelle != null && libelle.isLeft()) {
                    parties.add(new IndiceLsp.Partie(
                            libelle.getLeft() == null ? "" : libelle.getLeft(),
                            null));
                } else if (libelle != null && libelle.getRight() != null) {
                    for (org.eclipse.lsp4j.InlayHintLabelPart partie
                            : libelle.getRight()) {
                        if (partie == null) {
                            continue;
                        }
                        parties.add(new IndiceLsp.Partie(
                                partie.getValue() == null ? ""
                                        : partie.getValue(),
                                lireCible(partie.getLocation())));
                    }
                }
                indices.add(new IndiceLsp(
                        CorrespondanceLsp.lirePosition(indice.getPosition()),
                        parties,
                        indice.getKind() == null ? 0 : indice.getKind().getValue(),
                        Boolean.TRUE.equals(indice.getPaddingLeft()),
                        Boolean.TRUE.equals(indice.getPaddingRight())));
            }
        }
        return List.copyOf(indices);
    }

    /** La cible d'une partie d'indice — le lieu cliquable vers sa déclaration. */
    private static @Nullable LieuLsp lireCible(@Nullable Location lien) {
        if (lien == null) {
            return null;
        }
        return new LieuLsp(lien.getUri() == null ? "" : lien.getUri(),
                CorrespondanceLsp.lirePlage(lien.getRange()));
    }

    /**
     * Les plages de sélection lues : la chaîne des englobantes, de la
     * plus intime à la plus large — l'extension de sélection y descend
     * palier après palier.
     */
    static List<PlageLsp> lirePlagesSelection(
            @Nullable List<? extends SelectionRange> reponse) {
        List<PlageLsp> plages = new ArrayList<>();
        if (reponse != null) {
            for (SelectionRange selection : reponse) {
                SelectionRange palier = selection;
                while (palier != null && palier.getRange() != null) {
                    plages.add(CorrespondanceLsp.lirePlage(palier.getRange()));
                    palier = palier.getParent();
                }
            }
        }
        return List.copyOf(plages);
    }
}
