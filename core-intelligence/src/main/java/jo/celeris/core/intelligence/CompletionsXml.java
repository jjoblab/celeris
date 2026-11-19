package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La complétion XML guidée par le schéma (C-02) : le contexte du
 * curseur décide — nom de balise après le chevron, attribut dans
 * l'en-tête de balise, valeur d'attribut entre guillemets — et le
 * schéma embarqué répond. Les unités (dp, sp), les constantes de
 * taille et les préfixes de référence ({@code @string/}) suivent
 * l'attribut en cours.
 *
 * <p>Le contexte se lit en remontant du curseur : la balise ouverte
 * la plus proche non refermée, puis la position dans son en-tête.
 * Hors d'une balise — dans le texte — rien ne se suggère.</p>
 */
public final class CompletionsXml {

    /** Le plafond de candidats — la complétion guidée reste courte. */
    public static final int MAX_CANDIDATS = 100;

    private CompletionsXml() {
    }

    /**
     * Les candidats de complétion au curseur, guidés par le schéma.
     *
     * @param text le texte XML porteur
     * @param caret la position du curseur, dans les bornes du texte
     * @return les candidats distincts (peut être vide, jamais null)
     */
    public static List<String> completer(String text, int caret) {
        Contexte contexte = contexte(text, caret);
        if (contexte == null) {
            return List.of();
        }
        List<String> candidats = new ArrayList<>();
        for (String candidat : contexte.candidats()) {
            if (candidat.startsWith(contexte.prefixe()) && !candidats.contains(candidat)) {
                candidats.add(candidat);
            }
        }
        if (candidats.size() > MAX_CANDIDATS) {
            return List.copyOf(candidats.subList(0, MAX_CANDIDATS));
        }
        return List.copyOf(candidats);
    }

    /** Le contexte de complétion : la source des candidats et le préfixe. */
    private static @Nullable Contexte contexte(String text, int caret) {
        int chevron = ouvertureLaPlusProche(text, caret);
        if (chevron < 0) {
            return null;
        }
        String balise = nomDeBalise(text, chevron);
        int guillemet = text.lastIndexOf('"', Math.max(0, caret - 1));
        if (guillemet > chevron) {
            return valueContext(text, chevron, guillemet, caret);
        }
        return contexteNom(text, chevron, caret, balise);
    }

    /** Contexte de nom : la balise elle-même ou ses attributs. */
    private static Contexte contexteNom(String text, int chevron, int caret,
            String balise) {
        String apresChevron = text.substring(chevron + 1, caret);
        int dernierBlanc = Math.max(apresChevron.lastIndexOf(' '),
                apresChevron.lastIndexOf('\n'));
        if (dernierBlanc < 0) {
            // le nom de balise se tape : les balises connues répondent
            return new Contexte(SchemaAndroid.BALISES_CONNUES, apresChevron);
        }
        String prefixe = apresChevron.substring(dernierBlanc + 1);
        return new Contexte(SchemaAndroid.attributs(balise), prefixe);
    }

    /** Contexte de valeur : constantes, unités ou références. */
    private static Contexte valueContext(String text, int chevron, int guillemet,
            int caret) {
        String value = text.substring(Math.min(guillemet + 1, caret), caret);
        if (value.startsWith("@") || value.startsWith("?")) {
            return new Contexte(SchemaAndroid.references(), value);
        }
        String attribut = attributEnCours(text, chevron, guillemet);
        return new Contexte(SchemaAndroid.valeurs(attribut, value), value);
    }

    /** Le nom de l'attribut dont la valeur s'écrit après le guillemet. */
    private static String attributEnCours(String text, int chevron, int guillemet) {
        int egal = text.lastIndexOf('=', guillemet);
        if (egal <= chevron) {
            return "";
        }
        int start = egal - 1;
        while (start > chevron && !Character.isWhitespace(text.charAt(start))) {
            start--;
        }
        return text.substring(start + 1, egal).strip();
    }

    /** L'indice du «&#x2f;» ouvrant la balise la plus proche, non refermée. */
    private static int ouvertureLaPlusProche(String text, int caret) {
        int dernierFerme = Math.max(text.lastIndexOf('>', caret - 1), -1);
        int ouverture = text.lastIndexOf('<', caret - 1);
        if (ouverture <= dernierFerme) {
            return -1;
        }
        if (ouverture + 1 < text.length() && text.charAt(ouverture + 1) == '/') {
            return -1;
        }
        return ouverture;
    }

    /** Le nom de la balise ouverte à l'indice donné, vide si encore vide. */
    private static String nomDeBalise(String text, int ouverture) {
        int end = ouverture + 1;
        while (end < text.length()
                && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }
        return text.substring(ouverture + 1, end);
    }

    /** Un contexte de complétion : candidats source et préfixe tapé. */
    private record Contexte(List<String> candidats, String prefixe) {
    }
}
