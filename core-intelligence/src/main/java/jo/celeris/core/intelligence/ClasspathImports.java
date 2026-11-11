package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * L'import automatique adossé au classpath (chantier C8) : le nom
 * simple sous le curseur se résout contre l'index de l'hôte — les
 * noms qualifiés candidats se proposent, celui déjà importé ne se
 * re-propose jamais. L'insertion vit chez {@link ImportAuto} : une
 * édition de position unique à la tête du bloc des imports.
 */
public final class ClasspathImports {

    private ClasspathImports() {
    }

    /**
     * Les candidats d'import du nom simple sous le curseur : le nom
     * qualifié de l'index dont le nom simple est l'identifiant qui
     * l'entoure, dans l'ordre de déclaration de l'hôte — l'ordre est
     * déterministe. L'import déjà présent à la ligne d'import ne se
     * re-propose pas ; les autres candidats du même nom restent
     * proposés — le choix reste à l'utilisateur.
     *
     * @param doc       le document porteur
     * @param caret     la position du curseur
     * @param classpath l'index de classpath de l'hôte
     * @return les noms qualifiés candidats, vides hors identifiant ou
     *         sans candidat
     */
    public static List<String> candidats(Document doc, int caret,
            ClasspathIndex classpath) {
        String nom = identifiantSous(doc, caret);
        if (nom == null) {
            return List.of();
        }
        List<String> candidats = new ArrayList<>();
        for (String fqn : classpath.fqnDe(nom)) {
            if (!dejaImporte(doc, fqn)) {
                candidats.add(fqn);
            }
        }
        return List.copyOf(candidats);
    }

    /**
     * Le plan d'import d'un nom qualifié : la ligne d'import se pose
     * en tête du bloc par {@link ImportAuto} — une seule édition, la
     * sélection au début de l'insert.
     *
     * @param doc le document porteur
     * @param fqn le nom pleinement qualifié à importer
     * @return le plan, ou null quand l'import est déjà présent
     */
    public static @Nullable EditPlan importer(Document doc, String fqn) {
        return ImportAuto.importer(doc, "import " + fqn + ";\n");
    }

    /**
     * Vrai quand la ligne d'import du nom qualifié existe déjà — la
     * présence se vérifie à la ligne d'import, jamais dans une chaîne.
     */
    private static boolean dejaImporte(Document doc, String fqn) {
        String ligne = "import " + fqn + ";";
        for (int l = 0; l < doc.lineCount(); l++) {
            if (doc.lineText(l).strip().equals(ligne)) {
                return true;
            }
        }
        return false;
    }

    /**
     * L'identifiant qui entoure la position : ses caractères
     * d'identifiant Java contigus, la position peut vivre à ses deux
     * bornes — null hors identifiant.
     */
    private static @Nullable String identifiantSous(Document doc, int caret) {
        int fin = caret;
        while (fin < doc.length()
                && Character.isJavaIdentifierPart(doc.charAt(fin))) {
            fin++;
        }
        int debut = caret;
        while (debut > 0
                && Character.isJavaIdentifierPart(doc.charAt(debut - 1))) {
            debut--;
        }
        if (debut == fin) {
            return null;
        }
        String nom = doc.subText(debut, fin);
        return Character.isJavaIdentifierStart(nom.charAt(0)) ? nom : null;
    }
}
