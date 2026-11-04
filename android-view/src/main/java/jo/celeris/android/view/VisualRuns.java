package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Bidi;

/**
 * Les segments visuels bidirectionnels d'une rangée (ADR-008) : le
 * découpage en plages de niveau homogène, rendues de gauche à droite
 * dans l'ordre visuel — la règle L2 du cœur (UAX #9) rejouée sur les
 * segments. Le texte LOGIQUE ne bouge jamais : seules les positions
 * de tracé changent (ADR-003 — la vue peut se tromper sur l'affichage,
 * jamais sur l'état).
 *
 * <p>Le tracé concret d'un segment droite-à-gauche revient au moteur
 * de rendu de la plateforme : le segment se trace tel quel à sa
 * position visuelle, le façonnage interne s'occupe du sens. Une
 * rangée sans unité droite-à-gauche tient en un seul segment — le
 * chemin rapide, sans analyse.</p>
 */
public final class VisualRuns {

    private VisualRuns() {
    }

    /**
     * Un segment visuel : une plage LOGIQUE de la rangée, avec son
     * sens de lecture.
     *
     * @param start le début de la plage, en colonnes de la rangée
     * @param end   la fin exclusive de la plage
     * @param rtl   vrai si le segment se lit de droite à gauche
     */
    public record Run(int start, int end, boolean rtl) {
    }

    /** Un segment de travail : la plage et son niveau directionnel. */
    private record Segment(int start, int end, int level) {
    }

    /**
     * Vrai si le texte contient au moins une unité forte
     * droite-à-gauche — le test qui court-circuite l'analyse.
     *
     * @param text le texte de la rangée
     * @return vrai si une unité droite-à-gauche s'y trouve
     */
    public static boolean containsRtl(String text) {
        for (int i = 0; i < text.length(); i++) {
            byte directionalite = Character.getDirectionality(text.charAt(i));
            if (directionalite == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || directionalite
                            == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    /**
     * Les segments visuels de la rangée, de gauche à droite.
     *
     * @param line le texte de la rangée, sans séparateur de paragraphe
     * @return les plages logiques dans l'ordre visuel
     */
    public static List<Run> runs(String line) {
        int longueur = line.length();
        if (longueur == 0) {
            return List.of();
        }
        if (!containsRtl(line)) {
            return List.of(new Run(0, longueur, false));
        }
        List<Segment> segments = segmentsDeNiveau(line);
        reorganiser(segments);
        List<Run> ordonnes = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            ordonnes.add(new Run(segment.start(), segment.end(),
                    (segment.level() & 1) == 1));
        }
        return List.copyOf(ordonnes);
    }

    /** Les plages logiques de niveau directionnel homogène. */
    private static List<Segment> segmentsDeNiveau(String line) {
        int[] niveaux = Bidi.analyze(line).levels();
        List<Segment> segments = new ArrayList<>();
        int debut = 0;
        for (int i = 1; i <= niveaux.length; i++) {
            if (i == niveaux.length || niveaux[i] != niveaux[debut]) {
                segments.add(new Segment(debut, i, niveaux[debut]));
                debut = i;
            }
        }
        return segments;
    }

    /**
     * La règle L2 sur les segments : du niveau le plus haut au niveau
     * 1, chaque séquence contiguë de segments de niveau au moins égal
     * s'inverse — le miroir exact de la réorganisation du cœur.
     */
    private static void reorganiser(List<Segment> segments) {
        int maximum = 0;
        for (Segment segment : segments) {
            maximum = Math.max(maximum, segment.level());
        }
        for (int niveau = maximum; niveau >= 1; niveau--) {
            inverserSequences(segments, niveau);
        }
    }

    /** Inverse chaque séquence contiguë de segments de niveau au moins égal. */
    private static void inverserSequences(List<Segment> segments, int niveau) {
        int i = 0;
        while (i < segments.size()) {
            if (segments.get(i).level() < niveau) {
                i++;
                continue;
            }
            int debut = i;
            while (i < segments.size() && segments.get(i).level() >= niveau) {
                i++;
            }
            for (int a = debut, b = i - 1; a < b; a++, b--) {
                Segment temp = segments.get(a);
                segments.set(a, segments.get(b));
                segments.set(b, temp);
            }
        }
    }
}
