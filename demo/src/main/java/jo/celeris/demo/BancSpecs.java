package jo.celeris.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.PasteReindenter;
import jo.celeris.core.intelligence.Renommage;
import jo.celeris.core.intelligence.SmartTyping;
import jo.celeris.core.intelligence.TypingRules;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * Le banc des vérifications automatiques (M7.5, §9) : chaque
 * spécification rejoue les gestes du fil RÉEL de la démonstration —
 * les moteurs branchés par le concentrateur et les ateliers, jamais
 * des figures — et rend son verdict. La démonstration qui démontre :
 * le catalogue affiche ses preuves, la vérification tourne à
 * l'ouverture, un écart se voit.
 */
final class BancSpecs {

    private static final String URI = "file:///exemple.java";
    private static final IndentStyle STYLE = IndentStyle.ESPACES_4;

    /** Les règles du banc : la configuration du paquet java (M7.6). */
    private static TypingRules regles() {
        return TypingRules.of(LangagesDemo.registre()
                .byFilename("exemple.java").configuration());
    }

    /** L'identifiant du langage du banc — la détection du registre. */
    private static String languageId() {
        return LangagesDemo.registre().byFilename("exemple.java").id();
    }

    private BancSpecs() {
    }

    /** Les spécifications vérifiées, dans l'ordre de la matrice. */
    static List<ScenarioSpec> specs() {
        return List.of(
                new ScenarioSpec("A-05",
                        "L'ouvrant pose sa paire, le fermant se saute",
                        BancSpecs::paires),
                new ScenarioSpec("A-06",
                        "L'Entrée entre accolades fait les deux sauts",
                        BancSpecs::entree),
                new ScenarioSpec("A-07",
                        "Le retour arrière mange la paire vide entière",
                        BancSpecs::retourArriere),
                new ScenarioSpec("A-08",
                        "L'ouvrant sur la sélection l'entoure",
                        BancSpecs::entourage),
                new ScenarioSpec("A-09",
                        "Le collage multiligne s'aligne à l'accueil",
                        BancSpecs::collage),
                new ScenarioSpec("A-22",
                        "Le gabarit sélectionne son premier trou",
                        BancSpecs::gabarit),
                new ScenarioSpec("B-03",
                        "La complétion sert les mots du code, jamais les chaînes",
                        BancSpecs::completion),
                new ScenarioSpec("B-07",
                        "La signature arrive par le fil du protocole",
                        BancSpecs::signature),
                new ScenarioSpec("B-09",
                        "Le survol sert la documentation par le fil",
                        BancSpecs::survol),
                new ScenarioSpec("B-19",
                        "Le renommage porte les usages entiers, annulable",
                        BancSpecs::renommage),
                new ScenarioSpec("B-25",
                        "Le formatage redresse l'indentation, annulable",
                        BancSpecs::formatage),
                new ScenarioSpec("B-27",
                        "L'import se pose à la tête, le doublon se refuse",
                        BancSpecs::importation));
    }

    /** Les verdicts rejoués au banc — l'identifiant et sa preuve. */
    static Map<String, Boolean> verdicts() {
        Map<String, Boolean> tableau = new LinkedHashMap<>();
        for (ScenarioSpec spec : specs()) {
            tableau.put(spec.identifiant(), spec.verification().getAsBoolean());
        }
        return tableau;
    }

    /** A-05 — l'ouvrant pose sa paire, curseur au milieu ; le fermant se saute. */
    private static boolean paires() {
        TextSession session = TextSession.of("int valeur", () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(10)));
        if (!frapper(session, "(") || !"int valeur()".equals(
                session.document().text()) || caret(session) != 11) {
            return false;
        }
        return frapper(session, ")") && "int valeur()".equals(
                session.document().text()) && caret(session) == 12;
    }

    /** A-06 — l'Entrée entre accolades produit les deux sauts indentés. */
    private static boolean entree() {
        TextSession session = TextSession.of("class Exemple {}", () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(15)));
        EditPlan plan = SmartTyping.enter(session.document(), analyse(session),
                session.selection(), STYLE);
        session.apply(plan.transaction(), plan.selection());
        return "class Exemple {\n    \n}".equals(session.document().text())
                && caret(session) == 20;
    }

    /** A-07 — le retour arrière sur une paire vide la retire entière. */
    private static boolean retourArriere() {
        TextSession session = TextSession.of("f()", () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(2)));
        Optional<EditPlan> plan = SmartTyping.backspace(session.document(),
                session.selection(), regles(), STYLE);
        return plan.isPresent() && session.apply(plan.get().transaction(),
                plan.get().selection()) && "f".equals(
                        session.document().text()) && caret(session) == 1;
    }

    /** A-08 — l'ouvrant sur une sélection l'entoure, le contenu reste sélectionné. */
    private static boolean entourage() {
        TextSession session = TextSession.of("int valeur", () -> 0L);
        session.setSelection(MultiSelection.of(new SelRange(4, 10)));
        if (!frapper(session, "(") || !"int (valeur)".equals(
                session.document().text())) {
            return false;
        }
        SelRange plage = session.selection().ranges().get(0);
        return plage.start() == 5 && plage.end() == 11;
    }

    /** A-09 — le collage multiligne s'aligne sur l'indentation d'accueil. */
    private static boolean collage() {
        TextSession session = TextSession.of("class Exemple {\n}", () -> 0L);
        int accueil = 15;
        session.setSelection(MultiSelection.of(SelRange.caretAt(accueil)));
        String colle = PasteReindenter.reindent(session.document(),
                analyse(session), accueil, "int un;\nint deux;\n", STYLE);
        EditPlan plan = new EditPlan(
                Transaction.of(new TextEdit(accueil, "", colle)),
                MultiSelection.of(SelRange.caretAt(accueil + colle.length())));
        session.apply(plan.transaction(), plan.selection());
        return "class Exemple {int un;\n    int deux;\n    \n}".equals(
                session.document().text());
    }

    /** A-22 — le gabarit rend ses défauts, le premier trou vit sélectionné. */
    private static boolean gabarit() {
        TextSession session = TextSession.of("class Exemple {\n}\n", () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(16)));
        EditPlan plan = AtelierRefactor.planGabarit(session.document(),
                session.selection(), AtelierRefactor.GABARIT_METHODE);
        session.apply(plan.transaction(), plan.selection());
        SelRange premiere = session.selection().ranges().get(0);
        return "nouvelleMethode".equals(session.document()
                .subText(premiere.start(), premiere.end()));
    }

    /** B-03 — la complétion analytique : les mots du code, jamais les chaînes. */
    private static boolean completion() {
        String document = "int nombreLu = 3;\n"
                + "String secret = \"nombreCache\";\n"
                + "int copie = nombreLu;\n";
        TextSession session = TextSession.of(document, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            DemoIntelligence fournisseur = new DemoIntelligence();
            int caret = document.indexOf("nombreLu;")
                    + "nombreLu".length();
            List<String> candidats = fournisseur.completionCandidates(
                    session.document(), coordinateur.analysis(), caret,
                    PopupCoach.motsReserves());
            return candidats.contains("nombreLu") && !candidats.contains(
                    "nombreCache");
        } finally {
            coordinateur.close();
        }
    }

    /** B-07 — la signature de l'appel ouvert arrive par le fil du protocole. */
    private static boolean signature() {
        String document = "int total = ajouter(1, 2);\n";
        int caret = document.indexOf("ajouter(1,") + "ajouter(1,".length();
        SignatureAideModel modele = auFil(document, caret,
                (serveur, doc, position) -> serveur.signature(URI, doc,
                        position).get(2, java.util.concurrent.TimeUnit.SECONDS));
        return modele != null && "ajouter(int a, int b)".equals(
                modele.libelle()) && modele.parametreCourant() == 1;
    }

    /** B-09 — la documentation du symbole survolé arrive par le fil. */
    private static boolean survol() {
        String document = "int total = ajouter(1, 2);\n";
        int caret = document.indexOf("ajouter(1,") + 3;
        CompletionDocModel doc = auFil(document, caret,
                (serveur, documentPur, position) -> serveur.survol(URI,
                        documentPur, "ajouter", position)
                        .get(2, java.util.concurrent.TimeUnit.SECONDS));
        return doc != null && "ajouter".equals(doc.titre());
    }

    /** B-19 — le renommage porte les usages entiers, un geste défait tout. */
    private static boolean renommage() {
        String document = "class Exemple {\n    int total = 0;\n"
                + "    void corps() {\n        int nombre = total;\n    }\n}\n";
        TextSession session = TextSession.of(document, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        Renommage.PlanRenommage plan;
        try {
            plan = AtelierRefactor.planRenommage(session.document(),
                    coordinateur.analysis(), URI, "total", "somme");
        } finally {
            coordinateur.close();
        }
        if (!plan.conflits().isEmpty() || !AtelierRefactor.appliquerRenommage(
                session, URI, plan)) {
            return false;
        }
        boolean porte = !session.document().text().contains("total");
        session.undo();
        return porte && document.equals(session.document().text());
    }

    /** B-25 — le formatage redresse l'indentation, un geste défait tout. */
    private static boolean formatage() {
        String document = "class Exemple {\nvoid corps() {\n}\n}\n";
        TextSession session = TextSession.of(document, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        EditPlan plan;
        try {
            plan = AtelierRefactor.planFormat(session.document(),
                    coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
        if (plan == null || !session.apply(plan.transaction(),
                plan.selection())) {
            return false;
        }
        boolean redresse = session.document().text().contains(
                "\n    void corps() {");
        session.undo();
        return redresse && document.equals(session.document().text());
    }

    /** B-27 — l'import se pose à la tête du bloc, le doublon se refuse. */
    private static boolean importation() {
        TextSession session = TextSession.of(
                "package jo.exemple;\n\nclass Exemple {\n}\n", () -> 0L);
        EditPlan plan = AtelierRefactor.planImport(session.document(),
                "import java.util.List;\n");
        if (plan == null || !session.apply(plan.transaction(),
                plan.selection())) {
            return false;
        }
        boolean pose = session.document().text().startsWith(
                "package jo.exemple;\nimport java.util.List;\n");
        return pose && AtelierRefactor.planImport(session.document(),
                "import java.util.List;\n") == null;
    }

    /** La frappe d'un texte par le moteur — la voie du concentrateur. */
    private static boolean frapper(TextSession session, String texte) {
        Optional<EditPlan> plan = SmartTyping.insert(session.document(),
                session.selection(), texte, regles());
        return plan.isPresent() && session.apply(plan.get().transaction(),
                plan.get().selection());
    }

    private static int caret(TextSession session) {
        return session.selection().ranges().get(0).caret();
    }

    /** L'analyse fraîche d'une session — coordinateur ponctuel, fermé d'entrée. */
    private static Analysis analyse(TextSession session) {
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            return coordinateur.analysis();
        } finally {
            coordinateur.close();
        }
    }

    /** Le fil du protocole : la session de langage démarrée, la demande jointe. */
    private static <T> T auFil(String texte, int caret,
            DemandeAuFil<T> demande) {
        ServeurLangage serveur = ServeurLangage.demarrer();
        try {
            serveur.ouvrir(URI, languageId(), texte);
            Document document = TextSession.of(texte, () -> 0L).document();
            return demande.demander(serveur, document, caret);
        } catch (Exception echec) {
            return null;
        } finally {
            serveur.close();
        }
    }

    /** Une demande au fil — la signature ou le survol, joints bornés. */
    private interface DemandeAuFil<T> {

        /** La demande jointe — le futur du protocole, borné par l'appelant. */
        T demander(ServeurLangage serveur, Document document, int caret)
                throws Exception;
    }
}
