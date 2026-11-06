package jo.celeris.android.host.spi;

/**
 * La gravité d'un diagnostic d'hôte (E-05) : issue de la sortie de
 * build ou de l'outillage, indépendante du protocole LSP.
 */
public enum GraviteDiagnostic {

    /** L'exécution ne passe pas : erreur de compilation, d'outillage. */
    ERREUR,

    /** Suspicion : avertissement du compilateur, note de linte. */
    AVERTISSEMENT,

    /** Information sans gravité. */
    INFO
}
