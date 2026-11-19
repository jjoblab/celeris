package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.SignatureLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * L'aide aux paramètres du domaine (B-07) sur sa voie typée (ADR-015) :
 * le paramètre courant BORNÉ à l'arité de la signature active, la
 * signature active hors bornes qui rend null — les serveurs généreux
 * ne cassent pas le surlignage. Le décodage du fil vit à la frontière
 * protocolaire (CorrespondanceLspTest), la règle reste au cœur.
 */
class SignatureLspTest {

    private static SignatureLsp.Parameter parametre(String label) {
        return new SignatureLsp.Parameter(label, null);
    }

    /** Le paramètre courant borné : l'indice généreux retombe sur le dernier. */
    @Test
    void leParametreCourantBorne() {
        SignatureLsp genereux = SignatureLsp.of(List.of(
                new SignatureLsp.Entry("ajouter(int a, int b)",
                        List.of(parametre("a"), parametre("b")), null)),
                0, 7);
        assertEquals(1, genereux.parametreCourant(),
                "borne au dernier paramètre");
        SignatureLsp absent = SignatureLsp.of(List.of(
                new SignatureLsp.Entry("ajouter(int a, int b)",
                        List.of(parametre("a"), parametre("b")), null)),
                -1, -1);
        assertEquals(-1, absent.signatureActiveIndex());
        assertNull(absent.signatureActive());
        assertEquals(-1, absent.parametreCourant());
        // sans paramètre : aucun courant, même si le serveur en annonce un
        SignatureLsp vide = SignatureLsp.of(List.of(
                new SignatureLsp.Entry("fermer()", List.of(), null)),
                0, 0);
        assertEquals(-1, vide.parametreCourant());
    }

    /** La signature active hors bornes : null, le repli appartient à l'hôte. */
    @Test
    void laSignatureActiveHorsBornes() {
        SignatureLsp horsBornes = SignatureLsp.of(List.of(
                new SignatureLsp.Entry("f(a)", List.of(parametre("a")), null)),
                4, 0);
        assertNull(horsBornes.signatureActive(),
                "l'index hors bornes ne choisit rien");
        assertEquals(-1, horsBornes.parametreCourant());
    }

    /** La réponse vide : aucune signature, jamais une exception. */
    @Test
    void laReponseVide() {
        assertEquals(0, SignatureLsp.of(List.of(), -1, -1).entrees().size());
        assertNull(SignatureLsp.of(List.of(), -1, -1).signatureActive());
    }
}
