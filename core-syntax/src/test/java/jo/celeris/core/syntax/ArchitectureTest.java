package jo.celeris.core.syntax;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture du module core-syntax (ADR-002) : aucune dépendance
 * Android, et seules les dépendances descendantes déclarées sont
 * importables — le socle syntaxique lit le cœur texte, rien au-dessus.
 */
@AnalyzeClasses(packages = "jo.celeris.core.syntax", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

    /** Le socle ne connaît pas Android : aucun import android.* ici. */
    @ArchTest
    public static final ArchRule aucuneDependanceAndroid =
            noClasses().should().dependOnClassesThat()
                    .resideInAPackage("android..")
                    .because("le socle syntaxique ne connaît pas Android (ADR-002)");

    /** Les dépendances ne vont que vers le bas : paquets autorisés uniquement. */
    @ArchTest
    public static final ArchRule dependancesDescendantesUniquement =
            classes().should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "javax..",
                            "org.jspecify.annotations..",
                            "jo.celeris.core.text..",
                            "jo.celeris.core.syntax..")
                    .because("les dépendances ne vont que vers le bas (ADR-002)");
}
