package jo.celeris.demo;

import java.util.List;
import java.util.Locale;

import jo.celeris.core.syntax.GroovySyntax;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.JsonSyntax;
import jo.celeris.core.syntax.KotlinSyntax;
import jo.celeris.core.syntax.KtsSyntax;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.XmlSyntax;
import jo.celeris.core.syntax.grammars.LangagesGrammaires;

import org.jspecify.annotations.Nullable;

/**
 * Le catalogue des fichiers d'exemple : la matière première du terrain
 * de démonstration. Chaque langage P0 de la matrice (C-14, C-15) y
 * figure avec un fichier réaliste — l'analyseur dédié quand il existe
 * (Kotlin, Java, XML, KTS, JSON, Groovy), la grammaire déclarative
 * sinon (TOML, Markdown, Python — C-16).
 */
public final class FichiersExemple {

    private static final String KOTLIN = """
            package com.exemple.outils

            /**
             * Conversions entre les unités d'interface : indépendantes
             * de densité en entrée, pixels exacts en sortie.
             */
            object Conversions {

                private const val SEUIL = 0.5f

                fun dpVersPx(dp: Float, densite: Float): Int {
                    return (dp * densite + SEUIL).toInt()
                }

                fun pxVersDp(px: Int, densite: Float): Float {
                    require(densite > 0f) { "densite doit etre positive" }
                    return px / densite
                }
            }

            data class Mesure(val largeur: Int, val hauteur: Int) {

                fun aire(): Int = largeur * hauteur

                fun ratio(): Float {
                    if (hauteur == 0) {
                        return 0f
                    }
                    return largeur.toFloat() / hauteur
                }
            }
            """;

    private static final String JAVA = """
            package com.exemple.app;

            import java.util.ArrayList;
            import java.util.List;

            /**
             * Le service des utilisateurs : registre en mémoire, doublons
             * refusés, historique des inscriptions consultable.
             */
            public final class ServiceUtilisateur {

                private final List<String> noms = new ArrayList<>();

                private int inscriptions;

                public boolean inscrire(String nom) {
                    if (nom == null || nom.isBlank()) {
                        throw new IllegalArgumentException("nom requis");
                    }
                    if (noms.contains(nom)) {
                        return false;
                    }
                    inscriptions++;
                    return noms.add(nom);
                }

                public List<String> inscrits() {
                    return List.copyOf(noms);
                }

                public int inscriptions() {
                    return inscriptions;
                }
            }
            """;

    private static final String XML = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name"> exemple </string>
                <string name="titre_accueil">Bienvenue</string>
                <string name="action_reglages">Réglages</string>
                <plurals name="nombre_messages">
                    <item quantity="one">%d message</item>
                    <item quantity="other">%d messages</item>
                </plurals>
                <string-array name="langues">
                    <item>fr</item>
                    <item>en</item>
                </string-array>
            </resources>
            """;

    private static final String KTS = """
            plugins {
                alias(libs.plugins.android.application)
            }

            android {
                namespace = "com.exemple.app"
                compileSdk = 36

                defaultConfig {
                    applicationId = "com.exemple.app"
                    minSdk = 26
                    targetSdk = 36
                    versionCode = 1
                    versionName = "1.0"
                }
            }

            dependencies {
                implementation(project(":core"))
                testImplementation(libs.junit.jupiter)
            }
            """;

    private static final String TOML = """
            [versions]
            agp = "9.5.0"
            kotlin = "2.2.20"
            junit = "6.1.3"

            [libraries]
            junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            androidx-core = { module = "androidx.core:core", version = "1.17.0" }

            [plugins]
            android-application = { id = "com.android.application", version.ref = "agp" }
            """;

    private static final String JSON = """
            {
              "catalogue": {
                "nom": "exemples",
                "version": 3,
                "entrees": [
                  { "id": "A-13", "libelle": "annulation groupee", "priorite": "P0" },
                  { "id": "B-03", "libelle": "completion de base", "priorite": "P0" },
                  { "id": "D-15", "libelle": "zoom au pincement", "priorite": "P0" }
                ],
                "actif": true
              }
            }
            """;

    private static final String MARKDOWN = """
            # Exemples de la démonstration

            Le terrain de démonstration ouvre de **vrais fichiers** dans
            l'éditeur complet : coloration, plis, recherche, plan.

            ## Langages couverts

            - Kotlin, Java, XML, KTS, JSON : analyseurs dédiés
            - TOML, Markdown, Python : voie de grammaires

            ## Essais suggérés

            1. Pincer pour zoomer, double appui pour un second curseur
            2. Ouvrir le plan et sauter à une déclaration
            3. Chercher « completion » puis parcourir les occurrences
            4. Lancer l'analyse lint... les points de suspension -- comme
               ceux-ci -- se corrigent d'un geste
            """;

    private static final String PYTHON = """
            # Outils d'outillage : comptage de lignes par catégorie.
            CATEGORIES = {
                "code": 0,
                "commentaire": 0,
                "vide": 0,
            }

            def classifier(ligne):
                if not ligne.strip():
                    return "vide"
                if ligne.lstrip().startswith("#"):
                    return "commentaire"
                return "code"

            def compter(texte):
                for ligne in texte.splitlines():
                    CATEGORIES[classifier(ligne)] += 1
                return dict(CATEGORIES)
            """;

    private static final String PROGUARD = """
            # Règles R8 de la démonstration : l'API publique et les
            # entrées réfléchies survivent à la réduction.

            -verbose

            -keep public class com.celeris.demo.MainActivity {
                public static *** main(java.lang.String[]);
            }

            -keepclassmembers class * extends android.app.Activity {
                <init>();
                void onCreate(...);
            }

            -keepnames class com.celeris.core.text.**

            -keepattributes Signature, *Annotation*

            -keepclassmembers enum * {
                public static **[] values();
                public static ** valueOf(java.lang.String);
            }

            -dontwarn org.slf4j.**
            -dontobfuscate
            """;

    private static final String COULEURS = """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- La palette de la démonstration : les pastilles de la
                 gouttière rendent chaque valeur résolue (C-05). -->
            <resources>
                <color name="marque">#FF5722</color>
                <color name="fond_sombre">#121212</color>
                <color name="texte">#E3E3E3</color>
                <color name="accent">#03DAC6</color>
                <color name="voile">#80FFFFFF</color>
                <color name="alerte">#CF6679</color>
                <color name="ok">#4CAF50</color>
                <color name="rappel">#F00</color>
            </resources>
            """;

    private static final String APERCUS = """
            package com.exemple.atelier

            import androidx.compose.foundation.layout.padding
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.ui.unit.dp

            /**
             * Les cartes de l'accueil : le repère @Preview de la
             * gouttière délègue son rendu à l'hôte (C-11).
             */
            @Composable
            fun TitreCarte(nom: String) {
                Text(text = nom)
            }

            @Preview
            @Composable
            fun TitreCarteApercu() {
                TitreCarte(nom = "Conversions.kt")
            }

            @Preview(showBackground = true)
            @Composable
            fun EcranAccueilApercu() {
                TitreCarte(nom = "Celeris")
            }
            """;

    private static final List<FichierExemple> CATALOGUE = List.of(
            new FichierExemple("Conversions.kt", KOTLIN),
            new FichierExemple("ServiceUtilisateur.java", JAVA),
            new FichierExemple("strings.xml", XML),
            new FichierExemple("build.gradle.kts", KTS),
            new FichierExemple("libs.versions.toml", TOML),
            new FichierExemple("catalogue.json", JSON),
            new FichierExemple("README.md", MARKDOWN),
            new FichierExemple("outils.py", PYTHON),
            new FichierExemple("proguard-rules.pro", PROGUARD),
            new FichierExemple("couleurs.xml", COULEURS),
            new FichierExemple("Apercus.kt", APERCUS));

    private FichiersExemple() {
    }

    /**
     * Le catalogue complet, dans l'ordre d'affichage.
     *
     * @return les fichiers d'exemple, non modifiables
     */
    public static List<FichierExemple> catalogue() {
        return CATALOGUE;
    }

    /**
     * Le fichier d'exemple portant ce nom exact.
     *
     * @param nom le nom cherché, extension comprise
     * @return le fichier, null si le nom est inconnu
     */
    public static @Nullable FichierExemple parNom(String nom) {
        for (FichierExemple fichier : CATALOGUE) {
            if (fichier.nom().equals(nom)) {
                return fichier;
            }
        }
        return null;
    }

    /**
     * Le premier fichier du catalogue — celui ouvert quand l'écran est
     * lancé sans nom.
     *
     * @return le premier fichier, toujours présent
     */
    public static FichierExemple premier() {
        return CATALOGUE.get(0);
    }

    /**
     * L'analyseur du fichier selon son extension : dédié pour les P0,
     * grammaire déclarative pour la longue traîne (C-14 à C-16).
     *
     * @param nom le nom du fichier, extension comprise
     * @return le langage, null si l'extension est inconnue
     */
    public static @Nullable LanguageSyntax languePour(String nom) {
        int point = nom.lastIndexOf('.');
        if (point < 0) {
            return null;
        }
        String extension = nom.substring(point + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "kt" -> KotlinSyntax.INSTANCE;
            case "java" -> JavaSyntax.INSTANCE;
            case "xml" -> XmlSyntax.INSTANCE;
            case "kts" -> KtsSyntax.INSTANCE;
            case "gradle" -> GroovySyntax.INSTANCE;
            case "json" -> JsonSyntax.INSTANCE;
            case "toml" -> LangagesGrammaires.toml();
            case "md" -> LangagesGrammaires.markdown();
            case "py" -> LangagesGrammaires.python();
            case "pro" -> LangagesGrammaires.proguard();
            default -> null;
        };
    }
}
