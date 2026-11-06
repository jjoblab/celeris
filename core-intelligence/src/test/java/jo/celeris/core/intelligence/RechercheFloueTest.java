package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La recherche floue de symboles (B-13) : l'aller à symbole hors ligne
 * tape quelques lettres, l'index répond dans l'ordre — identique,
 * préfixe, début de mot interne, sous-chaîne, sous-suite — insensible
 * à la casse, jamais au-delà.
 */
class RechercheFloueTest {

    /** L'identique insensible à la casse : le score le plus haut. */
    @Test
    void lidentiqueInsensibleALaCasse() {
        assertEquals(RechercheFloue.SCORE_IDENTIQUE, RechercheFloue.score("Client", "client"));
        assertEquals(RechercheFloue.SCORE_IDENTIQUE, RechercheFloue.score("CLIENT", "client"));
        assertTrue(RechercheFloue.correspond("Client", "clIeNt"));
    }

    /** Le préfixe : la requête ouvre le nom. */
    @Test
    void lePrefixe() {
        assertEquals(RechercheFloue.SCORE_PREFIXE, RechercheFloue.score("ClientService", "cl"));
        assertEquals(RechercheFloue.SCORE_PREFIXE, RechercheFloue.score("clone", "CL"));
        // « lient » n'ouvre pas « Client » : c'est une sous-chaîne, pas un préfixe
        assertEquals(RechercheFloue.SCORE_SOUS_CHAINE, RechercheFloue.score("Client", "lient"));
    }

    /** Le début de mot interne : camelCase ou après un souligné. */
    @Test
    void leDebutDeMotInterne() {
        assertEquals(RechercheFloue.SCORE_DEBUT_MOT, RechercheFloue.score("MonClient", "cl"));
        assertEquals(RechercheFloue.SCORE_DEBUT_MOT, RechercheFloue.score("mon_client", "cl"));
        // « cl » au milieu d'un mot n'est PAS un début de mot
        assertEquals(RechercheFloue.SCORE_SOUS_CHAINE, RechercheFloue.score("Xyclone", "cl"));
    }

    /** La sous-chaîne : la requête vit à l'intérieur d'un mot. */
    @Test
    void laSousChaine() {
        assertEquals(RechercheFloue.SCORE_SOUS_CHAINE, RechercheFloue.score("Xyclone", "cl"));
        assertTrue(RechercheFloue.correspond("Seclore", "eclo"));
    }

    /** La sous-suite : les caractères dans l'ordre, pas forcément voisins. */
    @Test
    void laSousSuite() {
        assertEquals(RechercheFloue.SCORE_SOUS_SUITE, RechercheFloue.score("Colocalise", "cl"));
        assertEquals(RechercheFloue.SCORE_SOUS_SUITE, RechercheFloue.score("recule", "rl"));
        // l'ordre compte : « lc » n'est pas dans « recule » en sous-suite
        assertTrue(!RechercheFloue.correspond("recule", "lc"));
        // « drr » échoue : après le d et deux r, il n'y a plus rien
        assertTrue(!RechercheFloue.correspond("abracadabra", "drr"));
    }

    /** Le non-match : les caractères absents, la requête plus longue que le nom. */
    @Test
    void leNonMatch() {
        assertTrue(!RechercheFloue.correspond("Client", "zz"));
        assertTrue(!RechercheFloue.correspond("Client", "clientx"));
        assertTrue(!RechercheFloue.correspond("", "c"));
    }

    /** La hiérarchie : identique > préfixe > début de mot > sous-chaîne > sous-suite. */
    @Test
    void laHierarchieDesScores() {
        assertTrue(RechercheFloue.SCORE_IDENTIQUE > RechercheFloue.SCORE_PREFIXE);
        assertTrue(RechercheFloue.SCORE_PREFIXE > RechercheFloue.SCORE_DEBUT_MOT);
        assertTrue(RechercheFloue.SCORE_DEBUT_MOT > RechercheFloue.SCORE_SOUS_CHAINE);
        assertTrue(RechercheFloue.SCORE_SOUS_CHAINE > RechercheFloue.SCORE_SOUS_SUITE);
        // la requête vide correspond à tout, à score nul — l'aller à symbole montre tout
        assertEquals(0, RechercheFloue.score("Nimporte", ""));
        assertTrue(RechercheFloue.correspond("Nimporte", ""));
    }
}
