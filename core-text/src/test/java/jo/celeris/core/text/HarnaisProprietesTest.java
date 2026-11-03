package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Harnais de propriétés (jqwik, ADR-010) : démontre le motif à oracle qui
 * servira au fuzz du modèle de texte à M2 — l'oracle naïf reste la
 * référence, les invariants croisés le complètent. Remplacé par les
 * propriétés réelles dès l'arrivée du code.
 */
public final class HarnaisProprietesTest {

    /** La longueur d'une concaténation est la somme des longueurs (oracle naïf). */
    @Property
    void laLongueurDUneConcatenationEstLaSommeDesLongueurs(
            @ForAll String gauche, @ForAll String droite) {
        assertEquals(gauche.length() + droite.length(), (gauche + droite).length());
    }
}
