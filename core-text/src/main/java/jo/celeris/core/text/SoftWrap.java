package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de retour à la ligne souple, JVM pure : la mesure de texte est
 * injectée ({@link Measurer} — l'adaptateur du rendu côté vue), le modèle
 * se teste sans SDK ni appareil.
 *
 * <p>PIEGE-08 : le retour à la ligne souple du dépôt de départ ne coupait
 * pas un mot plus large que l'espace disponible — le mot débordait du
 * panneau. La coupe par caractères avec recherche dichotomique est
 * obligatoire : l'accumulation est gloutonne mot à mot (UNE mesure par
 * mot), un mot plus large que la ligne est coupé par caractères (une
 * mesure par itération de dichotomie), et la queue d'un mot coupé ouvre la
 * rangée courante — le mot suivant peut la rejoindre.</p>
 *
 * <p>Portage de la logique pure du kit de popups du dépôt de départ
 * (registre de portage) ; adaptations : aucune tolérance à {@code null}
 * (contract {@code @NullMarked}), découpage en méthoses courtes pour la
 * porte de complexité. Sémantique héritée : retours à la ligne explicites
 * respectés, paragraphe vide = rangée vide de respiration, paragraphes
 * vides de queue ignorés, aucune espace en tête de rangée coupée, au plus
 * une espace entre deux mots conservés, un paragraphe tout blanc rendu tel
 * quel, et jamais moins d'un caractère par rangée. La variante code
 * (préservation des blancs, coupes par offsets dans la ligne du document)
 * vient avec la vue (M3) — le registre de portage en suit l'écart.</p>
 */
public final class SoftWrap {

    /**
     * Mesure de la largeur d'un texte (px) : injectée par l'hôte — le
     * modèle reste pur et déterministe en test.
     */
    public interface Measurer {

        /**
         * Mesure la largeur rendue du texte.
         *
         * @param text le texte à mesurer
         * @return la largeur en pixels
         */
        float width(String text);
    }

    private SoftWrap() {
    }

    /**
     * Découpe le texte en rangées tenant dans la largeur donnée.
     *
     * @param text     le texte à découper (non nul)
     * @param maxWidth la largeur maximale d'une rangée (px)
     * @param measurer la mesure de texte injectée
     * @return les rangées, vide pour un texte vide
     */
    public static List<String> wrap(String text, float maxWidth, Measurer measurer) {
        List<String> rangees = new ArrayList<>();
        int longueur = text.length();
        int debutParagraphe = 0;
        while (debutParagraphe < longueur) {
            int saut = text.indexOf('\n', debutParagraphe);
            int finParagraphe = saut < 0 ? longueur : saut;
            wrapParagraph(text, debutParagraphe, finParagraphe, maxWidth, measurer, rangees);
            if (saut < 0) {
                break;
            }
            debutParagraphe = saut + 1;
        }
        // Paragraphes vides de queue ignorés : un saut final ne crée pas de
        // rangée vide (compatibilité du découpage hérité).
        while (!rangees.isEmpty() && rangees.get(rangees.size() - 1).isEmpty()) {
            rangees.remove(rangees.size() - 1);
        }
        return rangees;
    }

    /**
     * Découpe un paragraphe : vide = rangée vide de respiration ; tout
     * blanc = rendu tel quel (aucun mot à couper) ; sinon mot à mot.
     */
    private static void wrapParagraph(String text, int start, int end,
            float maxWidth, Measurer measurer, List<String> out) {
        if (start >= end) {
            out.add("");
            return;
        }
        if (isAllWhitespace(text, start, end)) {
            out.add(text.substring(start, end));
            return;
        }
        wrapWords(text, start, end, maxWidth, measurer, out);
    }

    private static boolean isAllWhitespace(String text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!isSpace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accumulation gloutonne mot à mot : un mot plus large que la ligne est
     * coupé par caractères (sa queue ouvre la rangée courante), un mot
     * normal rejoint la rangée s'il y tient avec son séparateur, sinon la
     * rangée part et le mot ouvre la suivante.
     */
    private static void wrapWords(String text, int start, int end,
            float maxWidth, Measurer measurer, List<String> out) {
        StringBuilder ligne = new StringBuilder();
        int i = start;
        while (i < end) {
            while (i < end && isSpace(text.charAt(i))) {
                i++;
            }
            if (i >= end) {
                break;
            }
            int debutMot = i;
            while (i < end && !isSpace(text.charAt(i))) {
                i++;
            }
            if (measure(measurer, text, debutMot, i) > maxWidth) {
                cutOversizedWord(text, debutMot, i, maxWidth, measurer, out, ligne);
            } else {
                accumulateWord(text, debutMot, i, maxWidth, measurer, out, ligne);
            }
        }
        if (ligne.length() > 0) {
            out.add(ligne.toString());
        }
    }

    /**
     * Coupe un mot plus large que la ligne : la rangée courante part
     * d'abord, puis le mot est coupé par caractères (dichotomie) ; la queue
     * du mot ouvre la rangée courante — le mot suivant pourra la rejoindre.
     */
    private static void cutOversizedWord(String text, int debutMot, int finMot,
            float maxWidth, Measurer measurer, List<String> out, StringBuilder ligne) {
        if (ligne.length() > 0) {
            out.add(ligne.toString());
            ligne.setLength(0);
        }
        int coupe = debutMot;
        while (coupe < finMot) {
            int borne = longestPrefix(text, coupe, finMot, maxWidth, measurer);
            if (borne <= coupe) {
                // Garde-fou hérité : au moins un caractère par rangée, même
                // plus étroite qu'un caractère.
                borne = coupe + 1;
            }
            if (borne >= finMot) {
                ligne.append(text, coupe, finMot);
                break;
            }
            out.add(text.substring(coupe, borne));
            coupe = borne;
        }
    }

    /** Le mot rejoint la rangée s'il y tient AVEC le séparateur. */
    private static void accumulateWord(String text, int debutMot, int finMot,
            float maxWidth, Measurer measurer, List<String> out, StringBuilder ligne) {
        int longueurAvant = ligne.length();
        if (longueurAvant > 0) {
            ligne.append(' ');
        }
        ligne.append(text, debutMot, finMot);
        if (longueurAvant > 0
                && measure(measurer, ligne, 0, ligne.length()) > maxWidth) {
            ligne.setLength(longueurAvant);
            out.add(ligne.toString());
            ligne.setLength(0);
            ligne.append(text, debutMot, finMot);
        }
    }

    /**
     * Plus grand {@code k <= end} tel que la largeur de
     * {@code text[begin..k)} soit {@code <= maxWidth} — dichotomie : la
     * largeur croît avec le préfixe, une mesure par itération.
     */
    private static int longestPrefix(String text, int begin, int end,
            float maxWidth, Measurer measurer) {
        if (maxWidth <= 0f) {
            return begin;
        }
        int lo = begin;
        int hi = end;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (measure(measurer, text, begin, mid) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /** Mesure d'une plage du texte (une seule sous-chaîne allouée). */
    private static float measure(Measurer measurer, CharSequence text, int begin, int end) {
        if (begin >= end) {
            return 0f;
        }
        return measurer.width(text.subSequence(begin, end).toString());
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c);
    }
}
