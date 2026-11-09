package jo.celeris.lsp.protocole;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture du module lsp-protocole (ADR-002, ADR-015) :
 * aucune dépendance Android, seules les dépendances descendantes
 * déclarées et LA bibliothèque de la frontière sont importables —
 * elle est la seule tierce du module, interne, jamais dans l'API
 * (comme joni dans core-syntax-grammars, ADR-014).
 */
@AnalyzeClasses(packages = "jo.celeris.lsp.protocole", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

    /** Le cœur ne connaît pas Android : aucun import android.* ici. */
    @ArchTest
    public static final ArchRule aucuneDependanceAndroid =
            noClasses().should().dependOnClassesThat()
                    .resideInAPackage("android..")
                    .because("le coeur ne connaît pas Android (ADR-002)");

    /** Les dépendances descendent, la bibliothèque exceptée par l'ADR-015. */
    @ArchTest
    public static final ArchRule dependancesDescendantesUniquement =
            classes().should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "javax..",
                            "org.jspecify.annotations..",
                            // la bibliothèque de la frontière : la seule
                            // dépendance tierce du module, détail interne
                            // (ADR-015) — son rouage JSON en fait partie.
                            "org.eclipse.lsp4j..",
                            "jo.celeris.core.lsp..",
                            "jo.celeris.lsp.protocole..")
                    .because("les dépendances ne vont que vers le bas (ADR-002) ;"
                            + " la bibliothèque est la seule tierce, exceptée par"
                            + " l'ADR-015");
}
