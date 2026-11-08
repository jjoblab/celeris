package jo.celeris.core.language;

import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La capacité de complétion (M7.6) : les candidats d'un langage à une
 * position. Le fournisseur générique (les mots du code) sert tout
 * langage sans fournisseur propre — la complétion ne disparaît jamais
 * parce qu'un langage manque d'intelligence dédiée.
 */
public interface Completion extends LanguageCapability {

    /**
     * Les candidats du langage à la position donnée.
     *
     * @param document     le document édité
     * @param analysis     l'analyse publiée du document, ou null tant
     *                     qu'aucune n'existe
     * @param caretOffset  la position du curseur, en décalage du document
     * @return les candidats, éventuellement vides, jamais nuls
     */
    List<String> candidates(Document document, @Nullable Analysis analysis, int caretOffset);
}
