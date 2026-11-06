package jo.celeris.core.intelligence;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture du module core-intelligence (ADR-002) : aucune dépendance
 * Android, et seules les dépendances descendantes déclarées sont
 * importables. Les paquets autorisés encodent la topologie du module ;
 * les faire évoluer, c'est faire évoluer l'architecture.
 */
@AnalyzeClasses(packages = "jo.celeris.core.intelligence", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

    /** Le cœur ne connaît pas Android : aucun import android.* ici. */
    @ArchTest
    public static final ArchRule aucuneDependanceAndroid =
            noClasses().should().dependOnClassesThat()
                    .resideInAPackage("android..")
                    .because("le coeur ne connaît pas Android (ADR-002)");

    /** Les dépendances ne vont que vers le bas : paquets autorisés uniquement. */
    @ArchTest
    public static final ArchRule dependancesDescendantesUniquement =
            classes().should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "javax..",
                            "org.jspecify.annotations..",
                            "jo.celeris.core.text..",
                            "jo.celeris.core.syntax..",
                            "jo.celeris.core.language..",
                            "jo.celeris.core.intelligence..")
                    .because("les dépendances ne vont que vers le bas (ADR-002)");
}
