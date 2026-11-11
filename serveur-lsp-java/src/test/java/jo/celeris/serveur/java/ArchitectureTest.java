package jo.celeris.serveur.java;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture du module serveur-lsp-java (ADR-002, ADR-015) :
 * aucune dépendance Android, aucune dépendance montante — le serveur
 * est autonome, la bibliothèque de protocole est sa seule tierce
 * (détail interne, jamais dans l'API, comme dans lsp-protocole).
 */
@AnalyzeClasses(packages = "jo.celeris.serveur.java", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

    /** Le serveur ne connaît pas Android : aucun import android.* ici. */
    @ArchTest
    public static final ArchRule aucuneDependanceAndroid =
            noClasses().should().dependOnClassesThat()
                    .resideInAPackage("android..")
                    .because("le serveur ne connaît pas Android (ADR-002)");

    /** Le serveur est autonome : la bibliothèque exceptée par l'ADR-015. */
    @ArchTest
    public static final ArchRule aucuneDependanceMontante =
            classes().should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "javax..",
                            "org.jspecify.annotations..",
                            // la bibliothèque de protocole : la seule
                            // dépendance tierce du module, détail
                            // interne (ADR-015) — son rouage JSON en
                            // fait partie.
                            "org.eclipse.lsp4j..",
                            "jo.celeris.serveur.java..")
                    .because("le serveur est autonome (ADR-002) ; la"
                            + " bibliothèque de protocole est sa seule"
                            + " tierce, exceptée par l'ADR-015");
}
