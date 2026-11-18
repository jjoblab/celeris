package com.example.consumer;

import android.app.Activity;
import android.os.Bundle;

import jo.celeris.android.host.CelerisEditor;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;
import jo.celeris.core.syntax.grammars.PackGrammars;

/**
 * L'activité de l'exemple (chantier B5) : le montage intégral TIENT
 * ICI — la façade compose tout, l'hôte ne connaît aucun interne. Le
 * chargeur de langues vit en champ : UNE instance par application,
 * le fil démon partagé sert l'activité recréée.
 */
public class MainActivity extends Activity {

    private static final String SOURCE = """
            class Premiere {
                void methode() {
                    System.out.println("Bonjour celeris");
                }
            }
            """;

    /** Le chargeur partagé : monté une fois, lu à chaque création. */
    private static final LanguageKit LANGUES = LanguageKit
            .builder(LanguageKit.sharedDaemon())
            .grammarFactory(PackGrammars.INSTANCE)
            .builtinPacks()
            .build();

    private CelerisEditor editeur;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        setContentView(R.layout.activity_main);
        EditorView vue = findViewById(R.id.editeur);
        PopupLayer couche = findViewById(R.id.couche_popups);
        // Le montage entier : trois lignes, la façade fait le reste.
        editeur = CelerisEditor.attach(vue, couche, LANGUES);
        editeur.open("Main.java", SOURCE);
        editeur.settings().setFontSizeSp(14f);
    }

    @Override
    protected void onDestroy() {
        // La libération suit la mort de l'activité : écouteurs partis,
        // fils éteints, la vue collectable (chantiers C1/C2).
        if (editeur != null) {
            editeur.close();
        }
        super.onDestroy();
    }
}
