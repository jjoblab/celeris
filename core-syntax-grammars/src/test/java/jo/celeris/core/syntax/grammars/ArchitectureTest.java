package jo.celeris.core.syntax.grammars;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture du module core-syntax-grammars (ADR-002) :
 * aucune dépendance Android, et seules les dépendances descendantes
 * déclarées sont importables — la voie de grammaires lit le socle
 * syntaxique et les contrats de langage, rien au-dessus.
 */
@AnalyzeClasses(packages = "jo.celeris.core.syntax.grammars", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

    /** La voie de grammaires ne connaît pas Android. */
    @ArchTest
    public static final ArchRule aucuneDependanceAndroid =
            noClasses().should().dependOnClassesThat()
                    .resideInAPackage("android..")
                    .because("la voie de grammaires ne connaît pas Android (ADR-002)");

    /** Les dépendances ne vont que vers le bas : paquets autorisés uniquement. */
    @ArchTest
    public static final ArchRule dependancesDescendantesUniquement =
            classes().should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "javax..",
                            "org.jspecify.annotations..",
                            // joni et son rouage d'encodages : moteur des
                            // expressions des grammaires (ADR-014) — la
                            // seule dépendance tierce du module, interne.
                            "org.joni..",
                            "org.jcodings..",
                            "jo.celeris.core.text..",
                            "jo.celeris.core.syntax..",
                            "jo.celeris.core.language..",
                            "jo.celeris.core.syntax.grammars..")
                    .because("les dépendances ne vont que vers le bas (ADR-002), "
                            + "joni exceptée par l'ADR-014");
}
