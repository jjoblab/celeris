package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;


import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
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
final class CorrespondanceDocuments {

    private CorrespondanceDocuments() {
        // la correspondance est un choix de fonctions pures, jamais une instance
    }

    /** L'élément de complétion lu — le libellé manque, la lecture refuse. */
    static ElementCompletion lireElement(CompletionItem element) {
        String libelle = element.getLabel();
        if (libelle == null) {
            throw new IllegalArgumentException("completion item requires label");
        }
        CompletionItemKind genre = element.getKind();
        return new ElementCompletion(libelle,
                genre == null ? 0 : genre.getValue(),
                element.getDetail(),
                CorrespondanceLsp.documentationDe(element.getDocumentation()),
                element.getSortText(),
                element.getFilterText(),
                element.getInsertText(),
                Boolean.TRUE.equals(element.getDeprecated()));
    }

    /** La liste de complétion lue : le tableau nu est complet, l'objet porte sa coupure. */
    static ListeCompletion lireListe(
            @Nullable Either<List<CompletionItem>, CompletionList> reponse) {
        if (reponse == null) {
            return ListeCompletion.de(false, List.of());
        }
        if (reponse.isRight()) {
            CompletionList liste = reponse.getRight();
            if (liste == null) {
                return ListeCompletion.de(false, List.of());
            }
            return ListeCompletion.de(liste.isIncomplete(),
                    lireElements(liste.getItems()));
        }
        return ListeCompletion.de(false, lireElements(reponse.getLeft()));
    }

    private static List<ElementCompletion> lireElements(
            @Nullable List<CompletionItem> elements) {
        List<ElementCompletion> lus = new ArrayList<>();
        if (elements != null) {
            for (CompletionItem element : elements) {
                lus.add(lireElement(element));
            }
        }
        return List.copyOf(lus);
    }

    /** Le survol lu : les deux formes de contenu et la plage qui le porte. */
    static SurvolLsp lireSurvol(@Nullable Hover survol) {
        if (survol == null) {
            return SurvolLsp.VIDE;
        }
        String texte = "";
        String genre = "";
        Either<List<Either<String, MarkedString>>, MarkupContent> contenu =
                survol.getContents();
        if (contenu != null && contenu.isRight()) {
            MarkupContent balise = contenu.getRight();
            if (balise != null) {
                texte = balise.getValue() == null ? "" : balise.getValue();
                genre = balise.getKind() == null ? "" : balise.getKind();
            }
        } else if (contenu != null) {
            texte = joindre(contenu.getLeft());
        }
        return new SurvolLsp(texte, genre, lirePlageNullable(survol.getRange()));
    }

    /** La plage qui porte le survol — null reste null, jamais devinée. */
    private static @Nullable PlageLsp lirePlageNullable(@Nullable Range plage) {
        return plage == null ? null : CorrespondanceLsp.lirePlage(plage);
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
            joint.append(texteDe(morceau));
        }
        return joint.toString();
    }

    private static String texteDe(@Nullable Either<String, MarkedString> morceau) {
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
    static SignatureLsp lireSignature(@Nullable SignatureHelp aide) {
        if (aide == null) {
            return SignatureLsp.de(List.of(), -1, -1);
        }
        List<SignatureLsp.Entree> entrees = new ArrayList<>();
        if (aide.getSignatures() != null) {
            for (SignatureInformation signature : aide.getSignatures()) {
                entrees.add(lireEntree(signature));
            }
        }
        return SignatureLsp.de(List.copyOf(entrees),
                entierOuAbsent(aide.getActiveSignature()),
                entierOuAbsent(aide.getActiveParameter()));
    }

    private static SignatureLsp.Entree lireEntree(SignatureInformation signature) {
        String libelle = signature.getLabel() == null ? "" : signature.getLabel();
        List<SignatureLsp.Parametre> parametres = new ArrayList<>();
        if (signature.getParameters() != null) {
            for (org.eclipse.lsp4j.ParameterInformation parametre
                    : signature.getParameters()) {
                parametres.add(lireParametre(parametre, libelle));
            }
        }
        return new SignatureLsp.Entree(libelle, List.copyOf(parametres),
                CorrespondanceLsp.documentationDe(signature.getDocumentation()));
    }

    private static SignatureLsp.Parametre lireParametre(
            org.eclipse.lsp4j.ParameterInformation parametre, String libelleSignature) {
        String libelle = "";
        Either<String, Tuple.Two<Integer, Integer>> forme = parametre.getLabel();
        if (forme != null && forme.isLeft() && forme.getLeft() != null) {
            libelle = forme.getLeft();
        } else if (forme != null && forme.getRight() != null) {
            libelle = tranche(libelleSignature, forme.getRight().getFirst(),
                    forme.getRight().getSecond());
        }
        return new SignatureLsp.Parametre(libelle,
                CorrespondanceLsp.documentationDe(parametre.getDocumentation()));
    }

    /** La tranche du libellé de signature, bornée au libellé. */
    private static String tranche(String libelle, @Nullable Integer debut,
            @Nullable Integer fin) {
        if (debut == null || fin == null) {
            return "";
        }
        int d = Math.max(0, Math.min(debut, libelle.length()));
        int f = Math.max(d, Math.min(fin, libelle.length()));
        return libelle.substring(d, f);
    }

    private static int entierOuAbsent(@Nullable Integer valeur) {
        return valeur == null ? -1 : valeur;
    }

    /** Les lieux de définition lus : la liste directe ou les liens cibles. */
    static List<LieuLsp> lireLieux(
            @Nullable Either<List<? extends Location>, List<? extends LocationLink>> reponse) {
        if (reponse == null) {
            return List.of();
        }
        List<LieuLsp> lieux = new ArrayList<>();
        if (reponse.isLeft() && reponse.getLeft() != null) {
            for (Location lieu : reponse.getLeft()) {
                lieux.add(new LieuLsp(lieu.getUri(), lirePlageNullable(lieu.getRange())));
            }
        } else if (reponse.getRight() != null) {
            for (LocationLink lien : reponse.getRight()) {
                lieux.add(new LieuLsp(lien.getTargetUri(),
                        lirePlageNullable(lien.getTargetRange())));
            }
        }
        return List.copyOf(lieux);
    }

}
