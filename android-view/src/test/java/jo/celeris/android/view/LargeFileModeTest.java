package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le régime des fonctions lourdes (chantier C10) : la complétion au
 * curseur — qui balaye les mots du document — se coupe sur un gros
 * document, l'édition reste entière. Tri-état à la manière du dépôt de
 * départ : AUTO suit les seuils (2,5 M caractères, 50 000 lignes), ON
 * force les coupures, OFF les ferme même au-delà des seuils, null
 * retombe sur AUTO.
 */
@RunWith(RobolectricTestRunner.class)
public class LargeFileModeTest {

    private Context contexte;

    /** Le fournisseur fixe : la complétion servie, mesurable sans moteur. */
    private static final List<String> SERVIS = List.of("alpha", "beta");

    private final IntelligenceHub.Provider fournisseurFixe =
            new IntelligenceHub.Provider() {
                @Override
                public List<String> completionCandidates(Document doc,
                        @Nullable Analysis analysis,
                        int caret, List<String> keywords) {
                    return SERVIS;
                }
            };

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Un texte de {@code lignes} lignes courtes. */
    private static String texte(int lignes) {
        StringBuilder contenu = new StringBuilder(lignes * 3);
        for (int i = 0; i < lignes; i++) {
            if (i > 0) {
                contenu.append('\n');
            }
            contenu.append("ab");
        }
        return contenu.toString();
    }

    /** AUTO sur un petit document : le régime dort, la complétion sert. */
    @Test
    public void autoSurPetitDocumentSertLaCompletion() {
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(100));
        IntelligenceHub intelligence = banc.vue().intelligence();
        intelligence.setProvider(fournisseurFixe);
        assertEquals("le régime défaut est AUTO",
                LargeFileMode.AUTO, intelligence.largeFileMode());
        assertFalse("un petit document ne coupe rien",
                intelligence.isLargeFileModeActive());
        assertEquals("la complétion sert",
                SERVIS, intelligence.completionCandidates(List.of()));
    }

    /** ON force les coupures, même sur un petit document. */
    @Test
    public void onCoupeMemeSurPetitDocument() {
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(100));
        IntelligenceHub intelligence = banc.vue().intelligence();
        intelligence.setProvider(fournisseurFixe);
        intelligence.setLargeFileMode(LargeFileMode.ON);
        assertTrue("ON force les coupures", intelligence.isLargeFileModeActive());
        assertTrue("la complétion se coupe",
                intelligence.completionCandidates(List.of()).isEmpty());
    }

    /** AUTO sur un gros document (50 001 lignes, le seuil du dépôt de
     *  départ) : les coupures suivent, la session reste éditable. */
    @Test
    public void autoSurGrosDocumentCoupeLaCompletion() {
        // 50 001 lignes : une de plus que le seuil, le texte reste petit
        // en caractères — le seuil de lignes seul décide.
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(50_001));
        EditorView vue = banc.vue();
        assertTrue("le document dépasse le seuil de lignes",
                vue.session().document().isLarge());
        IntelligenceHub intelligence = vue.intelligence();
        intelligence.setProvider(fournisseurFixe);
        assertEquals(LargeFileMode.AUTO, intelligence.largeFileMode());
        assertTrue("AUTO suit les seuils", intelligence.isLargeFileModeActive());
        assertTrue("la complétion se coupe",
                intelligence.completionCandidates(List.of()).isEmpty());

        // L'édition reste entière : la frappe passe par la voie unique.
        TextSession session = vue.session();
        assertTrue(session.replaceRange(0, 0, "x"));
        assertEquals(1, session.selection().ranges().size());
    }

    /** OFF ferme les coupures, même sur un gros document. */
    @Test
    public void offFermeLesCoupuresSurGrosDocument() {
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(50_001));
        IntelligenceHub intelligence = banc.vue().intelligence();
        intelligence.setProvider(fournisseurFixe);
        intelligence.setLargeFileMode(LargeFileMode.OFF);
        assertFalse("OFF ferme les coupures",
                intelligence.isLargeFileModeActive());
        assertEquals("la complétion sert au-delà des seuils",
                SERVIS, intelligence.completionCandidates(List.of()));
    }

    /** Null retombe sur AUTO — jamais de régime fantôme. */
    @Test
    public void nullRetombeSurAuto() {
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(100));
        IntelligenceHub intelligence = banc.vue().intelligence();
        intelligence.setLargeFileMode(LargeFileMode.ON);
        intelligence.setLargeFileMode(null);
        assertEquals("null retombe sur AUTO",
                LargeFileMode.AUTO, intelligence.largeFileMode());
        assertFalse(intelligence.isLargeFileModeActive());
    }

    /** La sélection se pose sur le gros document — la voie reste unique. */
    @Test
    public void selectionSePoseSurGrosDocument() {
        EditorTestRig banc = EditorTestRig.sur(contexte, texte(50_001));
        TextSession session = banc.vue().session();
        session.setSelection(MultiSelection.of(SelRange.caretAt(5)));
        assertEquals(5, session.selection().ranges().get(0).caret());
    }
}
