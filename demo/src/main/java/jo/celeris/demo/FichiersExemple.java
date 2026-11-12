package jo.celeris.demo;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le catalogue des fichiers d'exemple : la matière première du terrain
 * de démonstration. Chaque langage P0 de la matrice (C-14, C-15) y
 * figure avec un fichier réaliste — l'analyse vit dans les paquets de
 * la plateforme (M7.6), la démo ne porte plus que la matière.
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
            <!DOCTYPE resources [
                <!-- Le catalogue des chaînes de l'application. -->
                <!ELEMENT resources (string | plurals)*>
                <!-- Une chaîne nommée, sa valeur en corps. -->
                <!ELEMENT string (#PCDATA)>
                <!ATTLIST string name CDATA #REQUIRED>
                <!-- Des chaînes au pluriel, une par quantité. -->
                <!ELEMENT plurals (item+)>
                <!ATTLIST plurals name CDATA #REQUIRED>
                <!-- Un pluriel pour une quantité donnée. -->
                <!ELEMENT item (#PCDATA)>
                <!ATTLIST item quantity CDATA #REQUIRED>
            ]>
            <resources>
                <string name="app_name"> exemple </string>
                <string name="titre_accueil">Bienvenue</string>
                <string name="action_reglages">Réglages</string>
                <plurals name="nombre_messages">
                    <item quantity="one">%d message</item>
                    <item quantity="other">%d messages</item>
                </plurals>
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

    private static final String YAML = """
            # Les dépendances de la démonstration : le pipeline des
            # versions se déclare, il ne s'écrit pas dans les scripts.
            catalogue:
              nom: exemples
              version: 3
              entrees:
                - id: A-13
                  libelle: annulation groupee
                  priorite: P0
                - id: B-03
                  libelle: completion de base
                  priorite: P0
              actif: true

            pipeline:
              etapes:
                - nom: compilation
                  duree: 42
                - nom: tests
                  duree: 17
            """;

    private static final String SQL = """
            -- L'inventaire de la démonstration : le plan sort des
            -- règles du paquet, les plis des crochets du lexique.
            CREATE TABLE clients (
                id INTEGER PRIMARY KEY,
                nom TEXT NOT NULL,
                ville TEXT
            );

            CREATE VIEW clients_actifs AS
                SELECT id, nom FROM clients
                WHERE ville IS NOT NULL;

            CREATE UNIQUE INDEX clients_nom ON clients (nom);

            INSERT INTO clients (id, nom, ville) VALUES
                (1, 'Ada', 'Paris'),
                (2, 'Grace', NULL);
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
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.tooling.preview.Preview
            import androidx.compose.ui.unit.dp

            /**
             * Les cartes de l'accueil : le repère @Preview de la
             * gouttière délègue son rendu à l'hôte (C-11).
             */
            @Composable
            fun TitreCarte(nom: String) {
                Text(text = nom, modifier = Modifier.padding(16.dp))
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

    /** L'échantillon du paquet jouet (H-04) : le Logo de la tortue, le
     *  langage que l'utilisateur ajoute sans recompiler. */
    private static final String LOGO = """
            ; logo — le dessin du jour (paquet jouet, H-04)
            pour carre taille
              baisse
              repete 4 [
                avance taille
                tourne 90
              ]
              leve
            fin

            pour triangle taille
              baisse
              repete 3 [
                avance taille
                tourne 120
              ]
              leve
            fin

            ; la scene principale
            couleur rouge
            carre 40
            couleur bleu
            triangle 30
            avance 20
            couleur vert
            carre 10
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
            new FichierExemple("dependances.yml", YAML),
            new FichierExemple("inventaire.sql", SQL),
            new FichierExemple("proguard-rules.pro", PROGUARD),
            new FichierExemple("couleurs.xml", COULEURS),
            new FichierExemple("Apercus.kt", APERCUS));

    /** Le fichier d'exemple du paquet jouet (H-04). */
    private static final FichierExemple JOUET =
            new FichierExemple("tortue.lgo", LOGO);

    private FichiersExemple() {
    }

    /**
     * Le catalogue complet, dans l'ordre d'affichage.
     *
     * @return les fichiers d'exemple, non modifiables
     */
    public static List<FichierExemple> catalogue() {
        return catalogue(false);
    }

    /**
     * Le catalogue complet, l'échantillon du paquet jouet compris quand
     * il est installé (H-04) : la carte n'apparaît que si le langage
     * peut se colorer — la démonstration se joue à chaud.
     *
     * @param paquetJouet vrai quand le paquet jouet vit au stockage
     * @return les fichiers d'exemple, non modifiables
     */
    public static List<FichierExemple> catalogue(boolean paquetJouet) {
        if (!paquetJouet) {
            return CATALOGUE;
        }
        return java.util.stream.Stream.concat(CATALOGUE.stream(),
                java.util.stream.Stream.of(JOUET)).toList();
    }

    /**
     * Le fichier d'exemple portant ce nom exact.
     *
     * @param nom le nom cherché, extension comprise
     * @return le fichier, null si le nom est inconnu
     */
    public static @Nullable FichierExemple parNom(String nom) {
        return parNom(nom, false);
    }

    /**
     * Le fichier d'exemple portant ce nom exact, l'échantillon du
     * paquet jouet compris quand il est installé (H-04).
     *
     * @param nom        le nom cherché, extension comprise
     * @param paquetJouet vrai quand le paquet jouet vit au stockage
     * @return le fichier, null si le nom est inconnu
     */
    public static @Nullable FichierExemple parNom(String nom,
            boolean paquetJouet) {
        for (FichierExemple fichier : catalogue(paquetJouet)) {
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

}

