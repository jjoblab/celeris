package jo.celeris.android.view;

/**
 * La conversion des tailles d'interface (PIEGE-23) : les tailles vivent
 * en sp, la mesure et le tracé vivent en pixels — jamais un pixel cru
 * ne se poste dans une mesure d'interface, la conversion passe par la
 * densité de l'appareil.
 *
 * <p>Le piège hérité : les tailles de police se lisaient en pixels
 * bruts — minuscules sur écran dense. La discipline : une taille
 * d'interface se déclare en sp et se convertit une fois pour toutes à
 * la densité de l'appareil, l'échelle de police de l'accessibilité
 * comprise — {@code px = sp × densité × échelle}.</p>
 */
public final class TaillesSp {

    private TaillesSp() {
    }

    /**
     * Convertit une taille en sp vers des pixels à la densité donnée,
     * échelle de police comprise.
     *
     * @param sp            la taille logique, en sp
     * @param densite       la densité de l'appareil (1,0 sur mdpi,
     *                      3,0 sur xxhdpi)
     * @param echellePolice l'échelle de police de l'accessibilité,
     *                      lue dans la configuration (1,0 par défaut)
     * @return la taille en pixels
     */
    public static float enPixels(float sp, float densite, float echellePolice) {
        return sp * densite * echellePolice;
    }
}
