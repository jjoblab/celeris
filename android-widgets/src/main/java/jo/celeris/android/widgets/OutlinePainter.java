package jo.celeris.android.widgets;

import java.util.List;

import jo.celeris.core.syntax.NodeCategory;

/**
 * Peintre du plan consommateur (B-16) : une rangée par entrée — glyphe
 * de catégorie en primitives (le carré des types, le cercle des
 * fonctions, le tiret des champs) et le nom en retrait d'un pas par
 * niveau d'imbrication. Le peintre ne construit rien : il lit le modèle
 * et trace sur la surface — aucune allocation dans la boucle de dessin
 * (PIEGE-15).
 */
public final class OutlinePainter {

    /** La respiration gauche du plan, en pixels. */
    public static final float MARGE_GAUCHE = 8f;

    /** La respiration entre le glyphe et le nom, en pixels. */
    private static final float RESPIRATION_GLYPHE = 4f;

    private final float hauteurLigne;
    private final float pasIndentation;
    private final float tailleGlyph;
    private final int encreTexte;
    private final int encreGlyph;

    /**
     * Construit le peintre aux métriques et encres données.
     *
     * @param hauteurLigne  la hauteur d'une rangée, en pixels
     * @param pasIndentation le retrait par niveau d'imbrication, en pixels
     * @param tailleGlyph   le côté du glyphe, en pixels
     * @param encreTexte    la couleur du texte, en ARGB
     * @param encreGlyph    la couleur des glyphes, en ARGB
     */
    public OutlinePainter(float hauteurLigne, float pasIndentation, float tailleGlyph,
            int encreTexte, int encreGlyph) {
        this.hauteurLigne = hauteurLigne;
        this.pasIndentation = pasIndentation;
        this.tailleGlyph = tailleGlyph;
        this.encreTexte = encreTexte;
        this.encreGlyph = encreGlyph;
    }

    /**
     * Peint le plan sur la surface : une rangée par entrée.
     *
     * @param modele  le modèle du plan
     * @param surface la surface de dessin
     */
    public void dessiner(OutlineModel modele, OutlineSurface surface) {
        List<OutlineModel.Ligne> lignes = modele.lignes();
        for (int i = 0; i < lignes.size(); i++) {
            OutlineModel.Ligne ligne = lignes.get(i);
            float x = MARGE_GAUCHE + ligne.profondeur() * pasIndentation;
            float centre = (i + 0.5f) * hauteurLigne;
            glyphe(ligne.entree().category(), x, centre, surface);
            surface.drawText(ligne.entree().name(),
                    x + tailleGlyph + RESPIRATION_GLYPHE,
                    (i + 0.8f) * hauteurLigne, encreTexte);
        }
    }

    /**
     * La hauteur du plan : une hauteur de rangée par entrée.
     *
     * @param nombre le nombre d'entrées
     * @return la hauteur totale, en pixels
     */
    public float hauteur(int nombre) {
        return nombre * hauteurLigne;
    }

    /** Le glyphe de catégorie — en primitives, sans allocation. */
    private void glyphe(NodeCategory categorie, float x, float centre,
            OutlineSurface surface) {
        switch (categorie) {
            case TYPE, TAG -> surface.drawRect(x, centre - tailleGlyph / 2f,
                    x + tailleGlyph, centre + tailleGlyph / 2f, encreGlyph);
            case FUNCTION -> surface.drawCircle(x + tailleGlyph / 2f, centre,
                    tailleGlyph / 2f, encreGlyph);
            case FIELD -> surface.drawLine(x, centre, x + tailleGlyph, centre, encreGlyph);
            default -> surface.drawCircle(x + tailleGlyph / 4f, centre,
                    tailleGlyph / 4f, encreGlyph);
        }
    }
}
