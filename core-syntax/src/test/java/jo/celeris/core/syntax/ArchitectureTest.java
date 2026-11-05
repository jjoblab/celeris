package jo.celeris.core.syntax;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

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

    /** ADR-007 : le cœur ne déclare aucun champ volatile — moniteurs et confinement seulement. */
    @ArchTest
    public static final ArchRule aucunChampVolatile =
            noFields().should(new ArchCondition<JavaField>("être volatile") {
                @Override
                public void check(JavaField champ, ConditionEvents evenements) {
                    if (champ.getModifiers().contains(JavaModifier.VOLATILE)) {
                        evenements.add(SimpleConditionEvent.violated(champ,
                                champ.getFullName() + " est volatile"));
                    }
                }
            }).because("le cœur ne déclare ni volatile (ADR-007 : moniteurs et confinement)");

    /** ADR-007 : aucune classe atomique — l'annulation passe par le moniteur du jeton. */
    @ArchTest
    public static final ArchRule aucuneClasseAtomique =
            noClasses().should().dependOnClassesThat()
                    .resideInAnyPackage("java.util.concurrent.atomic..")
                    .because("pas d'atomiques dans le cœur (ADR-007)");

    /** ADR-007 : l'exécuteur s'injecte, jamais ne se déclare — la fabrique Executors est interdite au cœur. */
    @ArchTest
    public static final ArchRule aucunExecuteurDeclare =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("java.util.concurrent.Executors")
                    .because("l'exécuteur est un point d'injection, pas une déclaration (ADR-007)");
}
