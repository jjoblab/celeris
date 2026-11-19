package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
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
 * {@link DocumentsMapping}, chaque forme du protocole reçoit
 * sa lecture.
 */
final class CoverageMapping {

    private CoverageMapping() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    // ── La couverture du jalon (H-12) ────────────────────────────────

    /** Les lieux plats des références — la liste directe du protocole. */
    static List<LocationLsp> readFlatLocations(
            @Nullable List<? extends Location> reponse) {
        List<LocationLsp> lieux = new ArrayList<>();
        if (reponse != null) {
            for (Location lieu : reponse) {
                if (lieu != null) {
                    lieux.add(new LocationLsp(lieu.getUri(),
                            LspMapping.readRange(lieu.getRange())));
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
    static List<SymbolLsp> readSymbols(
            @Nullable List<? extends Either<SymbolInformation,
                    DocumentSymbol>> reponse) {
        List<SymbolLsp> symbols = new ArrayList<>();
        if (reponse != null) {
            for (Either<SymbolInformation, DocumentSymbol> symbole : reponse) {
                if (symbole == null) {
                    continue;
                }
                if (symbole.isLeft() && symbole.getLeft() != null) {
                    symbols.add(lireSymbolePlat(symbole.getLeft()));
                } else if (symbole.isRight() && symbole.getRight() != null) {
                    symbols.add(lireSymbole(symbole.getRight()));
                }
            }
        }
        return List.copyOf(symbols);
    }

    /** Le symbole plat : sa plage porte la sélection. */
    private static SymbolLsp lireSymbolePlat(SymbolInformation symbole) {
        return new SymbolLsp(symbole.getName() == null ? "" : symbole.getName(),
                symbole.getKind() == null ? 0 : symbole.getKind().getValue(),
                LspMapping.readRange(symbole.getLocation() == null
                        ? null : symbole.getLocation().getRange()),
                null, List.of());
    }

    /** Le symbole hiérarchique : ses enfants suivent, la profondeur entière. */
    private static SymbolLsp lireSymbole(DocumentSymbol symbole) {
        List<SymbolLsp> enfants = new ArrayList<>();
        if (symbole.getChildren() != null) {
            for (DocumentSymbol enfant : symbole.getChildren()) {
                if (enfant != null) {
                    enfants.add(lireSymbole(enfant));
                }
            }
        }
        return new SymbolLsp(symbole.getName() == null ? "" : symbole.getName(),
                symbole.getKind() == null ? 0 : symbole.getKind().getValue(),
                LspMapping.readRange(symbole.getRange()),
                LspMapping.readRange(symbole.getSelectionRange()),
                enfants);
    }

    /**
     * Les plis lus : la fin inclusive du protocole devient EXCLUSIVE —
     * la convention de l'arbre commun, jamais deux conventions pour un
     * même consommateur.
     */
    static List<FoldLsp> readFolds(
            @Nullable List<? extends FoldingRange> reponse) {
        List<FoldLsp> folds = new ArrayList<>();
        if (reponse != null) {
            for (FoldingRange pli : reponse) {
                if (pli == null) {
                    continue;
                }
                // la fin du protocole est INCLUSIVE : +1 la rend exclusive
                folds.add(new FoldLsp(pli.getStartLine(), pli.getEndLine() + 1));
            }
        }
        return List.copyOf(folds);
    }

    /** Les surlignages d'occurrences lus — le genre absent vaut texte. */
    static List<HighlightLsp> readHighlights(
            @Nullable List<? extends DocumentHighlight> reponse) {
        List<HighlightLsp> highlights = new ArrayList<>();
        if (reponse != null) {
            for (DocumentHighlight surlignage : reponse) {
                if (surlignage == null) {
                    continue;
                }
                highlights.add(new HighlightLsp(
                        LspMapping.readRange(surlignage.getRange()),
                        surlignage.getKind() == null ? 0
                                : surlignage.getKind().getValue()));
            }
        }
        return List.copyOf(highlights);
    }

    /**
     * La préparation de renommage lue : la plage seule ou l'objet
     * amorce — la troisième forme (comportement par défaut) ne porte
     * ni plage ni amorce : le renommage qui ne dit rien ne devine pas.
     */
    static @Nullable RenamePrepareLsp lirePreparationRennomage(
            @Nullable Either3<Range, PrepareRenameResult,
                    PrepareRenameDefaultBehavior> reponse) {
        if (reponse == null) {
            return null;
        }
        if (reponse.isFirst() && reponse.getFirst() != null) {
            return new RenamePrepareLsp(
                    LspMapping.readRange(reponse.getFirst()), null);
        }
        if (reponse.isSecond() && reponse.getSecond() != null) {
            PrepareRenameResult prepare = reponse.getSecond();
            return new RenamePrepareLsp(
                    LspMapping.readRange(prepare.getRange()),
                    prepare.getPlaceholder());
        }
        return null;
    }

    /**
     * Les correctifs lus : la commande nue (sans édition) reste un
     * correctif — l'hôte montre le titre, l'édition vide ne fait rien.
     */
    static List<CodeActionLsp> readCodeActions(
            @Nullable List<? extends Either<Command, CodeAction>> reponse) {
        List<CodeActionLsp> correctifs = new ArrayList<>();
        if (reponse != null) {
            for (Either<Command, CodeAction> correctif : reponse) {
                if (correctif == null) {
                    continue;
                }
                if (correctif.isLeft() && correctif.getLeft() != null) {
                    Command command = correctif.getLeft();
                    correctifs.add(new CodeActionLsp(
                            command.getTitle() == null ? ""
                                    : command.getTitle(),
                            WorkspaceEditLsp.EMPTY));
                } else if (correctif.isRight() && correctif.getRight() != null) {
                    CodeAction action = correctif.getRight();
                    correctifs.add(new CodeActionLsp(
                            action.getTitle() == null ? "" : action.getTitle(),
                            WorkspaceMapping.readWorkspace(action.getEdit())));
                }
            }
        }
        return List.copyOf(correctifs);
    }

    /** Les éditions du formatage : la liste du protocole, l'ordre du serveur. */
    static List<TextEditLsp> readFormatting(
            @Nullable List<? extends TextEdit> reponse) {
        return WorkspaceMapping.readEdits(reponse);
    }

    /** Les jetons sémantiques lus : la charge d'entiers, le décodage au domaine. */
    static SemanticTokensLsp lireTokens(@Nullable SemanticTokens tokens) {
        if (tokens == null || tokens.getData() == null) {
            return SemanticTokensLsp.of(null, List.of());
        }
        return SemanticTokensLsp.of(tokens.getResultId(), tokens.getData());
    }

    /** Les indices en ligne lus : les parties typées, la cible de navigation. */
    static List<InlayHintLsp> readInlayHints(
            @Nullable List<? extends InlayHint> reponse) {
        List<InlayHintLsp> inlayHints = new ArrayList<>();
        if (reponse != null) {
            for (InlayHint indice : reponse) {
                if (indice == null) {
                    continue;
                }
                List<InlayHintLsp.Part> parties = new ArrayList<>();
                Either<String, List<org.eclipse.lsp4j.InlayHintLabelPart>> label =
                        indice.getLabel();
                if (label != null && label.isLeft()) {
                    parties.add(new InlayHintLsp.Part(
                            label.getLeft() == null ? "" : label.getLeft(),
                            null));
                } else if (label != null && label.getRight() != null) {
                    for (org.eclipse.lsp4j.InlayHintLabelPart partie
                            : label.getRight()) {
                        if (partie == null) {
                            continue;
                        }
                        parties.add(new InlayHintLsp.Part(
                                partie.getValue() == null ? ""
                                        : partie.getValue(),
                                lireCible(partie.getLocation())));
                    }
                }
                inlayHints.add(new InlayHintLsp(
                        LspMapping.readPosition(indice.getPosition()),
                        parties,
                        indice.getKind() == null ? 0 : indice.getKind().getValue(),
                        Boolean.TRUE.equals(indice.getPaddingLeft()),
                        Boolean.TRUE.equals(indice.getPaddingRight())));
            }
        }
        return List.copyOf(inlayHints);
    }

    /** La cible d'une partie d'indice — le lieu cliquable vers sa déclaration. */
    private static @Nullable LocationLsp lireCible(@Nullable Location lien) {
        if (lien == null) {
            return null;
        }
        return new LocationLsp(lien.getUri() == null ? "" : lien.getUri(),
                LspMapping.readRange(lien.getRange()));
    }

    /**
     * Les plages de sélection lues : la chaîne des englobantes, de la
     * plus intime à la plus large — l'extension de sélection y descend
     * palier après palier.
     */
    static List<RangeLsp> readSelectionRanges(
            @Nullable List<? extends SelectionRange> reponse) {
        List<RangeLsp> ranges = new ArrayList<>();
        if (reponse != null) {
            for (SelectionRange selection : reponse) {
                SelectionRange palier = selection;
                while (palier != null && palier.getRange() != null) {
                    ranges.add(LspMapping.readRange(palier.getRange()));
                    palier = palier.getParent();
                }
            }
        }
        return List.copyOf(ranges);
    }
}
