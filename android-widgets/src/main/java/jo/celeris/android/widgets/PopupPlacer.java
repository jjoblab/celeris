package jo.celeris.android.widgets;

/**
 * Le placement coordonné des popups unifiées (M5, D-10) : complétion,
 * documentation et signature partagent l'écran autour du curseur sans
 * jamais se superposer ni sortir — la complétion sous le curseur
 * (au-dessus en bord bas), la documentation au flanc de la complétion
 * (à droite, à gauche en bord droit), la signature du côté opposé du
 * curseur (au-dessus quand la complétion est en dessous, repliée sous
 * la complétion en bord haut). Pur et testable sans SDK : l'accessibilité
 * du contenu appartient au peintre, qui annonce chaque popup à
 * l'explorateur.
 */
public final class PopupPlacer {

    /** Un rectangle, en coordonnées d'écran entières. */
    public record Cadre(int x, int y, int largeur, int hauteur) {
    }

    /** Le placement des trois popups unifiées. */
    public record Placement(Cadre completion, Cadre documentation, Cadre signature) {
    }

    private PopupPlacer() {
    }

    /**
     * Place les popups autour du curseur.
     *
     * @param caret         le rectangle du curseur (fin de ligne)
     * @param completion    la taille de la popup de complétion
     * @param documentation la taille de la popup de documentation
     * @param signature     la taille de la popup de signature
     * @param ecranLargeur  la largeur de l'écran
     * @param ecranHauteur  la hauteur de l'écran
     * @return le placement coordonné, chaque popup dans l'écran
     */
    public static Placement placer(Cadre caret, Cadre completion, Cadre documentation,
            Cadre signature, int ecranLargeur, int ecranHauteur) {
        // la complétion : sous le curseur si elle tient, au-dessus sinon
        boolean dessous = caret.y() + caret.hauteur() + completion.hauteur()
                <= ecranHauteur;
        int completionY = dessous
                ? caret.y() + caret.hauteur()
                : Math.max(0, caret.y() - completion.hauteur());
        int completionX = borne(caret.x(), completion.largeur(), ecranLargeur);
        Cadre completionPlacee = new Cadre(completionX, completionY,
                completion.largeur(), completion.hauteur());

        // la documentation : au flanc droit de la complétion, à gauche en bord
        int docX = completionPlacee.x() + completionPlacee.largeur();
        if (docX + documentation.largeur() > ecranLargeur) {
            docX = completionPlacee.x() - documentation.largeur();
        }
        if (docX < 0) {
            docX = borne(docX, documentation.largeur(), ecranLargeur);
        }
        Cadre documentationPlacee = new Cadre(docX, completionPlacee.y(),
                documentation.largeur(), documentation.hauteur());

        // la signature : du côté opposé du curseur, repliée sous la complétion
        int signatureY = dessous
                ? caret.y() - signature.hauteur()
                : caret.y() + caret.hauteur();
        if (signatureY < 0 || signatureY + signature.hauteur() > ecranHauteur) {
            signatureY = dessous
                    ? completionPlacee.y() + completionPlacee.hauteur()
                    : Math.max(0, completionPlacee.y() - signature.hauteur());
        }
        int signatureX = borne(completionPlacee.x(), signature.largeur(), ecranLargeur);
        Cadre signaturePlacee = new Cadre(signatureX, signatureY,
                signature.largeur(), signature.hauteur());

        return new Placement(completionPlacee, documentationPlacee, signaturePlacee);
    }

    /** Borne une abscisse pour que le cadre tienne dans l'écran. */
    private static int borne(int x, int largeur, int ecranLargeur) {
        return Math.max(0, Math.min(x, Math.max(0, ecranLargeur - largeur)));
    }
}
