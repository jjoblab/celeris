package jo.celeris.core.syntax;

/**
 * Exception d'annulation (ADR-007) : levée par un point de contrôle
 * d'un travail supplanté ou fermé. Non vérifiée — l'annulation est un
 * cheminement normal, pas une erreur du moteur ; l'appelant qui pilote
 * le travail l'intercepte et se tait.
 */
public final class AnalyseAnnulee extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /** L'annulation générique du moteur d'analyse. */
    public AnalyseAnnulee() {
        super("analysis cancelled");
    }
}
