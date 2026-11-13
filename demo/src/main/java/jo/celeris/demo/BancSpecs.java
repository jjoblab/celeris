package jo.celeris.demo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jo.celeris.android.host.popups.ContenuPopups;
import jo.celeris.android.host.popups.LanguageSession;
import jo.celeris.android.widgets.PatchPreview;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.Packs;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.PasteReindenter;
import jo.celeris.core.intelligence.Renommage;
import jo.celeris.core.intelligence.SmartTyping;
import jo.celeris.core.intelligence.TypingRules;


import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.structure.TreeFolds;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.SyntaxCoordinator;

import jo.celeris.core.syntax.grammars.PackGrammars;



import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

import jo.celeris.core.lsp.EditsEspaceLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;

import org.jspecify.annotations.Nullable;

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
        List<ScenarioSpec> specs = new ArrayList<>(specsDeFrappeEtRefactor());
        specs.addAll(specsDeLangage());
        specs.addAll(specsDesPopupsUnifiees());
        specs.addAll(specsDuFilEtDuPaquet());
        return List.copyOf(specs);
    }

    /** Les spécifications des popups unifiées (D) — le clavier
     *  délégué du chantier d'unification. */
    private static List<ScenarioSpec> specsDesPopupsUnifiees() {
        return List.of(
                new ScenarioSpec("D-10",
                        "Le clavier délégué des popups unifiées : la frappe"
                                + " ouvre, Enter accepte, Escape referme",
                        BancSpecs::clavierPopup));
    }

    /** Les spécifications de langage (C) — la documentation servie au survol. */
    private static List<ScenarioSpec> specsDeLangage() {
        return List.of(
                new ScenarioSpec("C-10",
                        "Le survol d'un modificateur sert sa documentation",
                        BancSpecs::survolModificateur));
    }

    /** Les spécifications de frappe et de refactorisation
     *  (A, B) — les paires aux gabarits. */
    private static List<ScenarioSpec> specsDeFrappeEtRefactor() {
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
                new ScenarioSpec("B-18",
                        "L'aperçu du correctif se voit avant l'acceptation",
                        BancSpecs::apercuCorrectif),
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

    /** Les spécifications du fil du protocole et du paquet (H) — les
     *  méthodes nouvelles, le serveur déclaré, le paquet jouet. */
    private static List<ScenarioSpec> specsDuFilEtDuPaquet() {
        return List.of(
                new ScenarioSpec("H-12",
                        "Les méthodes nouvelles servent le plan, le pli et"
                                + " le formatage par le fil",
                        BancSpecs::couverture),
                new ScenarioSpec("H-13",
                        "Le serveur Java déclaré sert chaque capacité"
                                + " annoncée — complétion, survol, signature,"
                                + " symboles, plis, formatage, diagnostics",
                        BancSpecs::serveurDeclare),
                new ScenarioSpec("H-04",
                        "Le paquet jouet se charge à chaud, se colore, se"
                                + " plie et se commente",
                        BancSpecs::paquetJouet));
    }

    /**
     * H-04 — le paquet jouet de l'utilisateur : copié au dossier des
     * paquets, chargé à chaud par la plateforme — l'échantillon se
     * détecte, se colore (commentaires et mots-clés par la grammaire du
     * paquet), se plie (l'indentation de la configuration) et se
     * commente (le point-virgule de la configuration) — sans un mot de
     * Logo dans le code du banc.
     */
    private static boolean paquetJouet() {
        try {
            java.nio.file.Path bac = java.nio.file.Files.createTempDirectory(
                    "celeris-h04");
            PaquetJouet.installer(bac);
            LanguageRegistry registre = LanguagePlatform
                    .with(PackGrammars.INSTANCE)
                    .addPackSource(Packs.folder(bac))
                    .build();
            LanguageSupport logo = registre.byFilename("tortue.lgo");
            if (logo == null || !PaquetJouet.LANGAGE.equals(logo.id())
                    || logo.syntax() == null) {
                return false;
            }
            String source = FichiersExemple.parNom("tortue.lgo", true).contenu();
            Analysis analyse = SyntaxService.of(
                    logo.syntax()).analyseFull(Document.of(source));
            boolean commentee = false;
            boolean motCle = false;
            for (int ligne = 0; ligne < analyse.lex().lineCount(); ligne++) {
                for (Token jeton : analyse.lex().line(ligne).tokens()) {
                    commentee |= jeton.kind() == TokenKind.COMMENT;
                    motCle |= jeton.kind() == TokenKind.KEYWORD;
                }
            }
            LanguageConfiguration.Folding pliage =
                    logo.configuration().folding();
            List<FoldSpan> plis = TreeFolds.regions(Document.of(source),
                    analyse, pliage.byIndentation()
                            ? new IndentationFolds.Settings(
                                    logo.configuration().indentSize(),
                                    pliage.offSide())
                            : null);
            return commentee && motCle && !plis.isEmpty()
                    && "; ".equals(logo.configuration().comments().lineComment());
        } catch (java.io.IOException echec) {
            return false;
        }
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
                session.selection(), AtelierRefactor.GABARIT_METHODE,
                java.util.Map.of("FICHIER", "Banc.java"));
        session.apply(plan.transaction(), plan.selection());
        // la macro FICHIER se résout au rendu (C7) : le premier trou est
        // le trou à choix du genre, son premier choix rendu
        SelRange premiere = session.selection().ranges().get(0);
        return "void".equals(session.document()
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
            DemoIntelligence fournisseur = new DemoIntelligence(
                    () -> LangagesDemo.langageDe("Banc.java", null,
                            session.document().text()));
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

    /** D-10 — le clavier délégué des popups unifiées (le chantier
     *  d'unification) : la frappe OUVRE (le prédicat de l'éditeur, les
     *  mots du code par le fournisseur), la liste SURVIT à la frappe
     *  suivante (le régime du filtre — la frappe referme les bulles,
     *  jamais la liste), Enter ACCEPTE (le candidat surligné à sa
     *  naissance remplace le mot au curseur, la liste ferme et SA
     *  DOCUMENTATION suit — la cascade du manager), Escape REFERME
     *  (la cascade encore). Les flèches déplacent la surbrillance par
     *  la même délégation — leur preuve vit au banc du kit
     *  ({@code PopupManagerTest.leClavierDelegueSertLaCompletion}). */
    private static boolean clavierPopup() {
        // la frappe ouvre : « no » au curseur, le coach propose « nombre »
        TextSession session = TextSession.of(
                "int nombre = 0;\nint no", () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        try {
            DemoIntelligence fournisseur = new DemoIntelligence(
                    () -> LangagesDemo.langageDe("Banc.java", null,
                            session.document().text()));
            int caret = session.document().text().length();
            if (!ContenuPopups.doitOuvrirALaFrappe(session.document(), caret)
                    || !laFrappePropose(
                            fournisseur.completionCandidates(
                                    session.document(), coordinateur.analysis(),
                                    caret, PopupCoach.motsReserves()))) {
                return false;
            }
            PopupController popups = new PopupController();
            // l'état s'ouvre : la liste, SA documentation suit (la machine
            // l'ouvre sur elle — contrôleur neuf, l'ouverture ne peut que
            // réussir)
            popups.open(PopupController.PopupKind.COMPLETION_LIST);
            popups.open(PopupController.PopupKind.COMPLETION_DOC);
            // la frappe suivante FILTRE : le régime ne referme PAS la
            // liste — elle vit pour filtrer, jamais de zombie
            if (popups.onKeystroke().contains(
                    PopupController.PopupKind.COMPLETION_LIST)) {
                return false;
            }
            return enterAccepte(popups, session, "nombre", caret)
                    && escapeReferme(popups);
        } finally {
            coordinateur.close();
        }
    }

    /** La frappe ouvre : la liste servie porte le candidat attendu,
     *  SEUL — la surbrillance naît à la tête, jamais vide. */
    private static boolean laFrappePropose(List<String> candidats) {
        return List.of("nombre").equals(candidats);
    }

    /** Enter accepte : le candidat remplace le mot au curseur
     *  (choisirCompletion de l'atelier — la cascade vit au manager). */
    private static boolean enterAccepte(PopupController popups,
            TextSession session, String retenu, int caret) {
        int debut = ContenuPopups.debutMot(session.document(), caret);
        // le remplacement qui échoue se dénonce : le texte attendu ne
        // tiendrait pas — l'assertion finale le subsume
        session.replaceRange(debut, caret, retenu);
        boolean cascade = popups.closeRendus(
                PopupController.PopupKind.COMPLETION_LIST).equals(
                EnumSet.of(PopupController.PopupKind.COMPLETION_LIST,
                        PopupController.PopupKind.COMPLETION_DOC));
        session.setSelection(MultiSelection.of(
                SelRange.caretAt(debut + retenu.length())));
        return cascade && "int nombre = 0;\nint nombre".equals(
                session.document().text());
    }

    /** Escape referme : la liste rouverte, SA documentation suit — la
     *  cascade du manager, encore, jamais codée deux fois. */
    private static boolean escapeReferme(PopupController popups) {
        popups.open(PopupController.PopupKind.COMPLETION_LIST);
        popups.open(PopupController.PopupKind.COMPLETION_DOC);
        return popups.closeRendus(
                PopupController.PopupKind.COMPLETION_LIST).equals(
                EnumSet.of(PopupController.PopupKind.COMPLETION_LIST,
                        PopupController.PopupKind.COMPLETION_DOC));
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

    /** B-18 — l'aperçu du correctif se voit avant l'acceptation, l'application reste annulable. */
    private static boolean apercuCorrectif() {
        String document = "int total = ajouter(1, 2);\n";
        int debut = document.indexOf("ajouter(1, 2)");
        ServeurLangage serveur = ServeurLangage.demarrer();
        try {
            serveur.ouvrir(URI, languageId(), document);
            Document pur = TextSession.of(document, () -> 0L).document();
            List<CorrectifLsp> correctifs = serveur.correctifs(URI,
                    PositionsLsp.plage(pur, debut, debut + "ajouter(1, 2)".length()))
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);
            if (correctifs == null || correctifs.isEmpty()
                    || correctifs.get(0).edition().documents().isEmpty()) {
                return false;
            }
            // l'épreuve porte l'aperçu : la session reste intacte tant
            // que l'acceptation n'a pas parlé
            Transaction edition = EditsEspaceLsp.transaction(pur,
                    correctifs.get(0).edition().documents().get(0));
            TextSession epreuve = TextSession.of(document, () -> 0L);
            if (!epreuve.apply(edition)) {
                return false;
            }
            PatchPreview apercu = PatchPreview.de(document, epreuve.text());
            boolean seVoit = apercu.rendues().contains("- int total = ajouter(1, 2);")
                    && apercu.rendues().contains("+ int total = {ajouter(1, 2)};");
            // l'acceptation applique par la voie de transaction — un
            // geste défait tout
            TextSession session = TextSession.of(document, () -> 0L);
            session.apply(edition);
            boolean applique = session.text().contains("{ajouter(1, 2)}");
            session.undo();
            return seVoit && applique && document.equals(session.text());
        } catch (Exception echec) {
            return false;
        } finally {
            serveur.close();
        }
    }

    /** C-10 — le modificateur d'agencement survolé sert sa documentation (Compose). */
    private static boolean survolModificateur() {
        FichierExemple fichier = FichiersExemple.parNom("Previews.kt");
        if (fichier == null) {
            return false;
        }
        String texte = fichier.contenu();
        String uri = "file:///Previews.kt";
        String langue = LangagesDemo.registre().byFilename("Previews.kt").id();
        int caret = texte.indexOf("padding(16.dp)") + 2;
        ServeurLangage serveur = ServeurLangage.demarrer();
        try {
            serveur.ouvrir(uri, langue, texte);
            Document document = TextSession.of(texte, () -> 0L).document();
            CompletionDocModel doc = serveur.survol(uri, document, "padding", caret)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);
            return doc != null && "padding".equals(doc.titre())
                    && doc.texteLibre().contains("L'espace autour du contenu");
        } catch (Exception echec) {
            return false;
        } finally {
            serveur.close();
        }
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

    /**
     * H-12 — la couverture nouvelle par le fil : les symboles du plan,
     * les plis du document, le formatage de l'alignement — chacun
     * servi par le protocole du serveur de référence, rendu en domaine.
     */
    private static boolean couverture() {
        String document = "int total = ajouter(1, 2);\n";
        java.util.List<jo.celeris.core.lsp.types.SymboleLsp> symboles = auFil(
                document, 0, (serveur, doc, position) -> serveur.symboles(URI)
                        .get(2, java.util.concurrent.TimeUnit.SECONDS));
        boolean lePlan = symboles != null && symboles.stream()
                .anyMatch(s -> "total".equals(s.nom()));
        String imbrique = "int ajouter(int a, int b) {\n    return a + b;\n}\n";
        java.util.List<jo.celeris.core.lsp.types.PliLsp> plis = auFil(
                imbrique, 0, (serveur, doc, position) -> serveur.plis(URI)
                        .get(2, java.util.concurrent.TimeUnit.SECONDS));
        boolean lePli = plis != null && !plis.isEmpty();
        java.util.List<jo.celeris.core.lsp.types.EditionTexteLsp> editions = auFil(
                "  int total = 1;\n", 0,
                (serveur, doc, position) -> serveur.formatage(URI,
                        jo.celeris.core.lsp.types.OptionsFormatageLsp.DEFAUT)
                        .get(2, java.util.concurrent.TimeUnit.SECONDS));
        boolean leFormat = editions != null && !editions.isEmpty()
                && editions.get(0).nouveauTexte().startsWith("int");
        return lePlan && lePli && leFormat;
    }

    /**
     * H-13 — la couverture du serveur DÉCLARÉ : le mini serveur Java
     * de la démonstration monte par sa déclaration ({@code
     * servers.json}), et chaque capacité annoncée à la poignée de main
     * répond au fil — complétion, survol, signature, symboles, plis,
     * formatage, diagnostics poussés. La démo qui démontre : le
     * serveur embarqué se teste entier, à l'ouverture.
     */
    private static boolean serveurDeclare() {
        // l'accolade fermante de « corps » manque : les diagnostics
        // poussés portent un constat réel, jamais un vent vide
        String texte = "class Exemple {\n"
                + "    int ajouter(int a, int b) {\n"
                + "        return a + b;\n"
                + "    }\n"
                + "    void corps() {\n"
                + "        int total = ajouter(1, 2);\n"
                + "    }\n";
        LanguageSupport langue = LangagesDemo.langageDe("exemple.java",
                null, texte);
        if (langue == null) {
            return false;
        }
        LanguageSession session = DeclaredServers.open(langue);
        try {
            if (!(session instanceof DeclaredServerSession)) {
                // le serveur déclaré n'a pas monté : la couverture ne
                // se prouve pas sur le serveur de repli
                return false;
            }
            Document document = TextSession.of(texte, () -> 0L).document();
            return diagnosticsPousses(session, texte)
                    && capacitesServies(session, document, texte);
        } catch (Exception echec) {
            return false;
        } finally {
            session.close();
        }
    }

    /** Les diagnostics poussés du serveur déclaré : un constat réel
     *  arrive au fil — jamais un vent vide (l'accolade manquante
     *  porte la preuve). */
    private static boolean diagnosticsPousses(LanguageSession session,
            String texte) {
        java.util.concurrent.CountDownLatch poussee =
                new java.util.concurrent.CountDownLatch(1);
        java.util.List<DiagnosticLsp> constatsPousses =
                new java.util.ArrayList<>();
        session.poserAuditeurDiagnostics((uri, constats) -> {
            constatsPousses.addAll(constats);
            poussee.countDown();
        });
        session.ouvrir(URI, languageId(), texte);
        try {
            return poussee.await(2, java.util.concurrent.TimeUnit.SECONDS)
                    && !constatsPousses.isEmpty();
        } catch (InterruptedException echec) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Les capacités servies au fil : complétion, survol, signature,
     *  symboles, plis et formatage — l'ouverture les éprouve ; tout
     *  échec du fil rend la capacité non servie. */
    private static boolean capacitesServies(LanguageSession session,
            Document document, String texte) {
        try {
            boolean laCompletion = session.sertCompletion()
                    && session.completion(URI, document,
                            texte.indexOf("int total = a"))
                            .get(2, java.util.concurrent.TimeUnit.SECONDS)
                            .elements().stream()
                            .anyMatch(c -> "ajouter".equals(c.libelle()));
            boolean leSurvol = survolDu(session, document,
                    texte.indexOf("ajouter(1,") + 3) != null;
            SignatureAideModel laSignature = signatureDu(session, document,
                    texte.indexOf("ajouter(1,") + "ajouter(1,".length());
            boolean laBonneSignature = laSignature != null
                    && "ajouter(int a, int b)".equals(laSignature.libelle())
                    && laSignature.parametreCourant() == 1;
            boolean lePlan = porteLeNom(session.symboles(URI)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS), "ajouter");
            boolean lePli = !session.plis(URI)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS).isEmpty();
            boolean leFormat = !session.formatage(URI,
                    jo.celeris.core.lsp.types.OptionsFormatageLsp.DEFAUT)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS).isEmpty();
            return laCompletion && leSurvol && laBonneSignature
                    && lePlan && lePli && leFormat;
        } catch (Exception echec) {
            return false;
        }
    }

    /**
     * Le plan porte-t-il le nom, PREMIER RANG OU ENFANTS : la classe
     * enveloppe ses méthodes — le plan du fichier descend l'arbre des
     * symboles, jamais il ne reste au premier étage.
     */
    private static boolean porteLeNom(
            java.util.List<jo.celeris.core.lsp.types.SymboleLsp> symboles,
            String nom) {
        for (jo.celeris.core.lsp.types.SymboleLsp symbole : symboles) {
            if (nom.equals(symbole.nom())) {
                return true;
            }
            if (porteLeNom(symbole.enfants(), nom)) {
                return true;
            }
        }
        return false;
    }

    /** Le survol d'une coupe joint borné — null au fil muet. */
    private static @Nullable CompletionDocModel survolDu(
            LanguageSession session, Document document, int caret) {
        try {
            return session.survol(URI, document, "ajouter", caret)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception echec) {
            return null;
        }
    }

    /** La signature d'une coupe jointe bornée — null au fil muet. */
    private static @Nullable SignatureAideModel signatureDu(
            LanguageSession session, Document document, int caret) {
        try {
            return session.signature(URI, document, caret)
                    .get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception echec) {
            return null;
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
