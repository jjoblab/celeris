package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;


import jo.celeris.core.lsp.types.CompletionItemLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;
import org.jspecify.annotations.Nullable;








/**
 * La correspondance des réponses de document (ADR-015) : complétion,
 * survol, signature, définition et jetons y deviennent les types
 * immuables du domaine — chaque forme du protocole reçoit sa lecture,
 * la panoplie des {@code Either} décodée jusqu'au texte (PIEGE-16, le
 * redressement M8.6 : la panne réelle du lecteur de documentation est
 * exactement ce que ces décodages éliminent).
 */
final class DocumentsMapping {

    private DocumentsMapping() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** L'élément de complétion lu — le libellé manque, la lecture refuse. */
    static CompletionItemLsp readItem(CompletionItem element) {
        String label = element.getLabel();
        if (label == null) {
            throw new IllegalArgumentException("completion item requires label");
        }
        CompletionItemKind kind = element.getKind();
        return new CompletionItemLsp(label,
                kind == null ? 0 : kind.getValue(),
                element.getDetail(),
                LspMapping.documentationOf(element.getDocumentation()),
                element.getSortText(),
                element.getFilterText(),
                element.getInsertText(),
                Boolean.TRUE.equals(element.getDeprecated()));
    }

    /** La liste de complétion lue : le tableau nu est complet, l'objet porte sa coupure. */
    static CompletionListLsp readList(
            @Nullable Either<List<CompletionItem>, CompletionList> reponse) {
        if (reponse == null) {
            return CompletionListLsp.of(false, List.of());
        }
        if (reponse.isRight()) {
            CompletionList liste = reponse.getRight();
            if (liste == null) {
                return CompletionListLsp.of(false, List.of());
            }
            return CompletionListLsp.of(liste.isIncomplete(),
                    readItems(liste.getItems()));
        }
        return CompletionListLsp.of(false, readItems(reponse.getLeft()));
    }

    private static List<CompletionItemLsp> readItems(
            @Nullable List<CompletionItem> elements) {
        List<CompletionItemLsp> lus = new ArrayList<>();
        if (elements != null) {
            for (CompletionItem element : elements) {
                lus.add(readItem(element));
            }
        }
        return List.copyOf(lus);
    }

    /** Le survol lu : les deux formes de contenu et la plage qui le porte. */
    static HoverLsp readHover(@Nullable Hover hover) {
        if (hover == null) {
            return HoverLsp.EMPTY;
        }
        String text = "";
        String kind = "";
        Either<List<Either<String, MarkedString>>, MarkupContent> contenu =
                hover.getContents();
        if (contenu != null && contenu.isRight()) {
            MarkupContent balise = contenu.getRight();
            if (balise != null) {
                text = balise.getValue() == null ? "" : balise.getValue();
                kind = balise.getKind() == null ? "" : balise.getKind();
            }
        } else if (contenu != null) {
            text = joindre(contenu.getLeft());
        }
        return new HoverLsp(text, kind, lirePlageNullable(hover.getRange()));
    }

    /** La plage qui porte le survol — null reste null, jamais devinée. */
    private static @Nullable RangeLsp lirePlageNullable(@Nullable Range range) {
        return range == null ? null : LspMapping.readRange(range);
    }

    private static String joindre(
            @Nullable List<Either<String, MarkedString>> morceaux) {
        if (morceaux == null || morceaux.isEmpty()) {
            return "";
        }
        StringBuilder joint = new StringBuilder();
        for (Either<String, MarkedString> morceau : morceaux) {
            if (joint.length() > 0) {
                joint.append('\n');
            }
            joint.append(textOf(morceau));
        }
        return joint.toString();
    }

    private static String textOf(@Nullable Either<String, MarkedString> morceau) {
        if (morceau == null) {
            return "";
        }
        if (morceau.isRight()) {
            MarkedString balise = morceau.getRight();
            return balise == null || balise.getValue() == null ? ""
                    : balise.getValue();
        }
        return morceau.getLeft() == null ? "" : morceau.getLeft();
    }

    /** L'aide aux paramètres lue — absente, aucune signature. */
    static SignatureLsp readSignature(@Nullable SignatureHelp help) {
        if (help == null) {
            return SignatureLsp.of(List.of(), -1, -1);
        }
        List<SignatureLsp.Entry> entrees = new ArrayList<>();
        if (help.getSignatures() != null) {
            for (SignatureInformation signature : help.getSignatures()) {
                entrees.add(readEntry(signature));
            }
        }
        return SignatureLsp.of(List.copyOf(entrees),
                entierOuAbsent(help.getActiveSignature()),
                entierOuAbsent(help.getActiveParameter()));
    }

    private static SignatureLsp.Entry readEntry(SignatureInformation signature) {
        String label = signature.getLabel() == null ? "" : signature.getLabel();
        List<SignatureLsp.Parameter> parameters = new ArrayList<>();
        if (signature.getParameters() != null) {
            for (org.eclipse.lsp4j.ParameterInformation parametre
                    : signature.getParameters()) {
                parameters.add(readParameter(parametre, label));
            }
        }
        return new SignatureLsp.Entry(label, List.copyOf(parameters),
                LspMapping.documentationOf(signature.getDocumentation()));
    }

    private static SignatureLsp.Parameter readParameter(
            org.eclipse.lsp4j.ParameterInformation parametre, String libelleSignature) {
        String label = "";
        Either<String, Tuple.Two<Integer, Integer>> forme = parametre.getLabel();
        if (forme != null && forme.isLeft() && forme.getLeft() != null) {
            label = forme.getLeft();
        } else if (forme != null && forme.getRight() != null) {
            label = tranche(libelleSignature, forme.getRight().getFirst(),
                    forme.getRight().getSecond());
        }
        return new SignatureLsp.Parameter(label,
                LspMapping.documentationOf(parametre.getDocumentation()));
    }

    /** La tranche du libellé de signature, bornée au libellé. */
    private static String tranche(String label, @Nullable Integer start,
            @Nullable Integer end) {
        if (start == null || end == null) {
            return "";
        }
        int d = Math.max(0, Math.min(start, label.length()));
        int f = Math.max(d, Math.min(end, label.length()));
        return label.substring(d, f);
    }

    private static int entierOuAbsent(@Nullable Integer value) {
        return value == null ? -1 : value;
    }

    /** Les lieux de définition lus : la liste directe ou les liens cibles. */
    static List<LocationLsp> readLocations(
            @Nullable Either<List<? extends Location>, List<? extends LocationLink>> reponse) {
        if (reponse == null) {
            return List.of();
        }
        List<LocationLsp> lieux = new ArrayList<>();
        if (reponse.isLeft() && reponse.getLeft() != null) {
            for (Location lieu : reponse.getLeft()) {
                lieux.add(new LocationLsp(lieu.getUri(), lirePlageNullable(lieu.getRange())));
            }
        } else if (reponse.getRight() != null) {
            for (LocationLink lien : reponse.getRight()) {
                lieux.add(new LocationLsp(lien.getTargetUri(),
                        lirePlageNullable(lien.getTargetRange())));
            }
        }
        return List.copyOf(lieux);
    }

}
