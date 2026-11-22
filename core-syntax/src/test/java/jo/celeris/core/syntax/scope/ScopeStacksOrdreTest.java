package jo.celeris.core.syntax.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le chantier C7 de la feuille de route coloration (prompt du
 * 2026-11-22) : le banc de reproductibilité des piles de portées —
 * l'ordre d'ouverture de N fichiers ne change RIEN aux piles servies
 * (la table est bornée à 50 000 nœuds, Javadoc « garde-fou, pas
 * cache » : au plafond, le push rend le parent — dégradation assumée).
 * Le banc ouvre des piles en masse dans DEUX ordres et compare.
 */
public class ScopeStacksOrdreTest {

    /** Les mêmes piles dans deux ordres d'ouverture : mêmes portées. */
    @Test
    public void lordreOuvertureNeChangeLesPiles() {
        int nombre = 2000;
        String[][] portees = new String[nombre][];
        for (int i = 0; i < nombre; i++) {
            portees[i] = new String[] {"source.test" + (i % 37),
                    "bloc.a" + i, "bloc.b" + i};
        }
        int[] direct = new int[nombre];
        for (int i = 0; i < nombre; i++) {
            int pile = ScopeStacks.root(portees[i][0]);
            pile = ScopeStacks.push(pile, portees[i][1]);
            direct[i] = ScopeStacks.push(pile, portees[i][2]);
        }
        int[] inverse = new int[nombre];
        for (int i = nombre - 1; i >= 0; i--) {
            int pile = ScopeStacks.root(portees[i][0]);
            pile = ScopeStacks.push(pile, portees[i][1]);
            inverse[i] = ScopeStacks.push(pile, portees[i][2]);
        }
        for (int i = 0; i < nombre; i++) {
            List<String> d = ScopeStacks.scopesOf(direct[i]);
            List<String> v = ScopeStacks.scopesOf(inverse[i]);
            assertEquals(d, v, "la pile " + i + " ne dépend pas de"
                    + " l'ordre d'ouverture");
            assertEquals(3, d.size(), "les trois portées vivent");
            assertEquals(portees[i][2], d.get(d.size() - 1),
                    "le sommet est la dernière poussée");
        }
        assertTrue(nombre > 0);
    }
}
