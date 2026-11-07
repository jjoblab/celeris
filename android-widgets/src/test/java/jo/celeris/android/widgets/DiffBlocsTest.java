package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La différence ligne à ligne par blocs (D-12) : la plus longue
 * sous-suite commune découpe les changements en blocs aux plages
 * exactes, la lecture en ligne préfixe, la lecture côte à côte
 * aligne — et la revue décide bloc par bloc avec réalignement.
 */
class DiffBlocsTest {

    private static final String ANCIEN = """
            premiere ligne
            deuxieme ligne
            troisieme ligne
            quatrieme ligne
            cinquieme ligne
            """;

    private static final String NOUVEAU = """
            premiere ligne
            ligne remplacee
            troisieme ligne
            cinquieme ligne
            sixieme ligne
            """;

    @Test
    void lesBlocsPortentLeursPlagesExactes() {
        List<DiffBlocs.BlocDiff> blocs = DiffBlocs.de(ANCIEN, NOUVEAU).blocs();
        assertEquals(3, blocs.size(),
                "un remplacement, une suppression, un ajout — trois changements indépendants");
        DiffBlocs.BlocDiff remplacement = blocs.get(0);
        assertEquals(1, remplacement.debutAncien());
        assertEquals(2, remplacement.finAncien());
        assertEquals(1, remplacement.debutNouveau());
        assertEquals(2, remplacement.finNouveau());
        assertEquals(2, remplacement.lignes().size());
        assertEquals(DiffBlocs.TypeLigne.ENLEVEE,
                remplacement.lignes().get(0).type());
        assertEquals(DiffBlocs.TypeLigne.AJOUTEE,
                remplacement.lignes().get(1).type());
        DiffBlocs.BlocDiff suppression = blocs.get(1);
        assertEquals(3, suppression.debutAncien());
        assertEquals(4, suppression.finAncien());
        assertEquals(3, suppression.debutNouveau());
        assertEquals(3, suppression.finNouveau(), "aucune ligne nouvelle");
        DiffBlocs.BlocDiff ajout = blocs.get(2);
        assertEquals(5, ajout.debutAncien(), "aucune ligne ancienne");
        assertEquals(5, ajout.finAncien());
        assertEquals(4, ajout.debutNouveau());
        assertEquals(5, ajout.finNouveau());
    }

    @Test
    void laLectureEnLignePrefixeLesLignes() {
        List<String> lignes = DiffBlocs.de(ANCIEN, NOUVEAU).enLigne();
        assertEquals(List.of(
                "- deuxieme ligne",
                "+ ligne remplacee",
                "- quatrieme ligne",
                "+ sixieme ligne"), lignes);
    }

    @Test
    void laLectureCoteACoteAligneLesColonnes() {
        List<String> rendues = DiffBlocs.de(ANCIEN, NOUVEAU).coteACote(16);
        assertEquals(3, rendues.size());
        assertEquals("deuxieme ligne   | ligne remplacee", rendues.get(0));
        assertEquals("quatrieme ligne  | ", rendues.get(1));
        assertEquals("                 | sixieme ligne", rendues.get(2));
    }

    @Test
    void lesTextesEgauxNePortentAucunBloc() {
        assertEquals(0, DiffBlocs.de(ANCIEN, ANCIEN).blocs().size());
    }

    @Test
    void laRevueDecideBlocParBlocAvecRealignement() {
        RevueDiff revue = RevueDiff.de(ANCIEN, NOUVEAU);
        assertEquals(3, revue.nombreDeBlocs());
        // Tout en attente : le texte reste l'ancien.
        assertEquals(ANCIEN, revue.texte());

        // Le remplacement accepté : il s'applique, les blocs suivants
        // restent alignés malgré le décalage.
        RevueDiff remplacementAccepte = revue.decider(0,
                RevueDiff.Decision.ACCEPTE);
        assertEquals("""
                premiere ligne
                ligne remplacee
                troisieme ligne
                quatrieme ligne
                cinquieme ligne
                """, remplacementAccepte.texte());

        // La suppression refusée : l'ancienne ligne reste.
        RevueDiff suppressionRefusee = remplacementAccepte.decider(1,
                RevueDiff.Decision.REFUSE);
        assertEquals("""
                premiere ligne
                ligne remplacee
                troisieme ligne
                quatrieme ligne
                cinquieme ligne
                """, suppressionRefusee.texte());

        // L'ajout accepté par-dessus : le texte devient le nouveau.
        RevueDiff tousDecides = suppressionRefusee.decider(1,
                RevueDiff.Decision.ACCEPTE).decider(2,
                RevueDiff.Decision.ACCEPTE);
        assertEquals(NOUVEAU, tousDecides.texte());
    }

    @Test
    void leRefusDuMilieuRealigneLesSuivants() {
        String ancien = "a\nb\nc\nd\ne\n";
        String nouveau = "a\nB\nc\nD\ne\n";
        RevueDiff revue = RevueDiff.de(ancien, nouveau);
        assertEquals(2, revue.nombreDeBlocs());

        // Refus du premier bloc, acceptation du second : le second
        // s'applique à sa place exacte, malgré le refus qui précède.
        RevueDiff revueDecidee = revue
                .decider(0, RevueDiff.Decision.REFUSE)
                .decider(1, RevueDiff.Decision.ACCEPTE);
        assertEquals("a\nb\nc\nD\ne\n", revueDecidee.texte());
    }

    @Test
    void lePointDeControleRetrouveSonEtat() {
        RevueDiff depart = RevueDiff.de(ANCIEN, NOUVEAU);
        RevueDiff point = depart.decider(0, RevueDiff.Decision.ACCEPTE)
                .pointDeControle();
        RevueDiff apres = point.decider(1, RevueDiff.Decision.ACCEPTE)
                .decider(0, RevueDiff.Decision.REFUSE);
        assertEquals(RevueDiff.Decision.REFUSE, apres.decision(0));
        // Le retour au point de contrôle restaure l'état enregistré.
        RevueDiff revenu = apres.retourAuPointDeControle(point);
        assertEquals(RevueDiff.Decision.ACCEPTE, revenu.decision(0));
        assertEquals(RevueDiff.Decision.EN_ATTENTE, revenu.decision(1));
    }
}
