package jo.celeris.lsp.protocole;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SemanticTokenLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * L'écriture du domaine vers le protocole (ADR-015) : les
 * convertisseurs du monteur de référence — plages, signatures,
 * symboles, éditions, espaces et jetons sémantiques — nés de la
 * scission des plafonds de ServeurDocuments (ADR-002).
 */
final class EcritureDocuments {

    private EcritureDocuments() {
    }

/** La position du domaine des paramètres de la bibliothèque. */
    static PositionLsp position(
        org.eclipse.lsp4j.TextDocumentPositionParams params) {
    return new PositionLsp(params.getPosition().getLine(),
            params.getPosition().getCharacter());
    }

/** La plage du protocole au domaine — jamais nulle ici, l'appel est né d'elle. */
    static PlageLsp lirePlage(Range plage) {
    return new PlageLsp(
            new PositionLsp(plage.getStart().getLine(),
                    plage.getStart().getCharacter()),
            new PositionLsp(plage.getEnd().getLine(),
                    plage.getEnd().getCharacter()));
    }

/**
     * La chaîne des plages englobantes au protocole : la plus large
     * devient le parent de la précédente, la plus intime répond.
     */
    static org.eclipse.lsp4j.SelectionRange ecrirePlagesSelection(
        List<PlageLsp> plages) {
    org.eclipse.lsp4j.SelectionRange chaine =
            new org.eclipse.lsp4j.SelectionRange(
                    plage(plages.get(plages.size() - 1)), null);
    for (int i = plages.size() - 2; i >= 0; i--) {
        chaine = new org.eclipse.lsp4j.SelectionRange(
                plage(plages.get(i)), chaine);
    }
    return chaine;
    }

/** La plage du domaine au Range de la bibliothèque — null reste null. */
    static @Nullable Range plage(@Nullable PlageLsp plage) {
    return plage == null ? null : new Range(
            new Position(plage.debut().ligne(), plage.debut().colonne()),
            new Position(plage.fin().ligne(), plage.fin().colonne()));
    }

/** L'aide aux paramètres du domaine à la réponse du protocole. */
    static SignatureHelp ecrireSignature(SignatureLsp aide) {
    List<SignatureInformation> signatures = new ArrayList<>();
    for (SignatureLsp.Entree entree : aide.entrees()) {
        List<ParameterInformation> parametres = new ArrayList<>();
        for (SignatureLsp.Parametre parametre : entree.parametres()) {
            parametres.add(new ParameterInformation(parametre.libelle()));
        }
        signatures.add(new SignatureInformation(entree.libelle(),
                entree.documentation(), parametres));
    }
    SignatureHelp reponse = new SignatureHelp();
    reponse.setSignatures(signatures);
    if (aide.signatureActiveIndex() >= 0
            && aide.signatureActiveIndex() < signatures.size()) {
        reponse.setActiveSignature(aide.signatureActiveIndex());
    }
    if (aide.parametreCourant() >= 0) {
        reponse.setActiveParameter(aide.parametreCourant());
    }
    return reponse;
    }

/** Le symbole du domaine au symbole du protocole, ses enfants suivent. */
    static DocumentSymbol ecrireSymbole(
            SymboleLsp symbole) {
        List<DocumentSymbol> enfants = new ArrayList<>();
        for (SymboleLsp enfant : symbole.enfants()) {
            enfants.add(ecrireSymbole(enfant));
        }
        return new DocumentSymbol(symbole.nom(),
                SymbolKind.forValue(symbole.genre()),
                plage(symbole.plage()),
                symbole.plageSelection() == null ? plage(symbole.plage())
                        : plage(symbole.plageSelection()),
                null, enfants);
    }

/** Les éditions du domaine aux éditions du protocole. */
    static List<TextEdit> ecrireEditions(
            List<EditionTexteLsp> editions) {
        List<TextEdit> protocole = new ArrayList<>();
        for (EditionTexteLsp edition : editions) {
            protocole.add(new TextEdit(
                    plage(edition.plage()), edition.nouveauTexte()));
        }
        return protocole;
    }

/**
     * L'édition d'espace du domaine au protocole : la forme moderne
     * {@code documentChanges} porte les versions, les gabarits y
     * voyagent en édits de gabarit — le aller-retour ne perd rien.
     */
    static @Nullable WorkspaceEdit ecrireEspace(
            EditionEspaceLsp espace) {
        if (espace.documents().isEmpty() && espace.gabarits().isEmpty()) {
            return null;
        }
        WorkspaceEdit protocole = new WorkspaceEdit();
        List<Either<TextDocumentEdit,
                org.eclipse.lsp4j.ResourceOperation>> changements =
                        new ArrayList<>();
        for (EditionDocumentLsp document : espace.documents()) {
            List<Either<TextEdit,
                    org.eclipse.lsp4j.SnippetTextEdit>> editions =
                            new ArrayList<>();
            for (EditionTexteLsp edition : document.editions()) {
                editions.add(Either.forLeft(new TextEdit(
                        plage(edition.plage()), edition.nouveauTexte())));
            }
            changements.add(Either.forLeft(new TextDocumentEdit(
                    new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                            document.uri(), document.version()),
                    editions)));
        }
        for (jo.celeris.core.lsp.types.EditionGabaritLsp gabarit
                : espace.gabarits()) {
            List<Either<TextEdit,
                    org.eclipse.lsp4j.SnippetTextEdit>> editions =
                            new ArrayList<>();
            Range ancrage = new Range(
                    new Position(gabarit.position().ligne(),
                            gabarit.position().colonne()),
                    new Position(gabarit.position().ligne(),
                            gabarit.position().colonne()));
            org.eclipse.lsp4j.StringValue corps =
                    new org.eclipse.lsp4j.StringValue();
            corps.setValue(gabarit.corps());
            editions.add(Either.forRight(
                    new org.eclipse.lsp4j.SnippetTextEdit(ancrage,
                            corps)));
            changements.add(Either.forLeft(new TextDocumentEdit(
                    new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(
                            gabarit.uri(), -1),
                    editions)));
        }
        protocole.setDocumentChanges(changements);
        return protocole;
    }

/** Les options de formatage du protocole au domaine. */
    static OptionsFormatageLsp optionsFormatage(
            org.eclipse.lsp4j.FormattingOptions options) {
        return new OptionsFormatageLsp(options.getTabSize(),
                options.isInsertSpaces());
    }

/** Les jetons absolus du domaine redeviennent la charge encodée du protocole. */
    static List<Integer> encoderJetons(SemanticTokensLsp jetons) {
        List<Integer> donnees = new ArrayList<>();
        int lignePrecedente = 0;
        int colonnePrecedente = 0;
        for (SemanticTokenLsp jeton : jetons.jetons()) {
            donnees.add(jeton.ligne() - lignePrecedente);
            donnees.add(jeton.ligne() == lignePrecedente
                    ? jeton.colonne() - colonnePrecedente
                    : jeton.colonne());
            donnees.add(jeton.longueur());
            donnees.add(jeton.type());
            donnees.add(jeton.modificateurs());
            lignePrecedente = jeton.ligne();
            colonnePrecedente = jeton.colonne();
        }
        return donnees;
    }
}
