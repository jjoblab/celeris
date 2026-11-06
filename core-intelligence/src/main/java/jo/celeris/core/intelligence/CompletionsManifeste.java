package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Map;

/**
 * La complétion du manifeste Android (C-06) : les attributs connus de
 * chaque balise — manifeste, application, activité, service,
 * récepteur, permission — et les permissions usuelles suggérées par
 * leur préfixe. Le schéma vit ici, embarqué, sans dépendance.
 */
public final class CompletionsManifeste {

    private static final List<String> PERMISSIONS = List.of(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.VIBRATE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.NFC");

    private static final List<String> COMMUNS = List.of(
            "android:name", "android:label");

    private static final Map<String, List<String>> BALISES = Map.of(
            "manifest", List.of("package", "android:versionCode",
                    "android:versionName", "android:installLocation"),
            "application", List.of("android:label", "android:icon",
                    "android:theme", "android:allowBackup",
                    "android:supportsRtl", "android:hardwareAccelerated"),
            "activity", List.of("android:name", "android:exported",
                    "android:label", "android:configChanges",
                    "android:launchMode", "android:screenOrientation",
                    "android:windowSoftInputMode"),
            "service", List.of("android:name", "android:exported",
                    "android:foregroundServiceType"),
            "receiver", List.of("android:name", "android:exported",
                    "android:permission"),
            "uses-permission", List.of("android:name",
                    "android:maxSdkVersion"));

    private CompletionsManifeste() {
    }

    /**
     * Les attributs suggérés pour une balise du manifeste : les siens
     * puis les communs, sans doublon, filtrés par le préfixe tapé.
     *
     * @param balise  le nom de la balise, sans chevrons
     * @param prefixe le préfixe tapé (peut être vide)
     * @return les attributs suggérés (jamais null)
     */
    public static List<String> attributs(String balise, String prefixe) {
        List<String> propres = BALISES.getOrDefault(balise, List.of());
        List<String> candidats = new java.util.ArrayList<>();
        for (String attribut : propres) {
            ajouter(candidats, attribut, prefixe);
        }
        for (String commun : COMMUNS) {
            ajouter(candidats, commun, prefixe);
        }
        return List.copyOf(candidats);
    }

    /**
     * Les permissions usuelles suggérées, filtrées par le préfixe.
     *
     * @param prefixe le préfixe tapé (peut être vide)
     * @return les permissions suggérées (jamais null)
     */
    public static List<String> permissions(String prefixe) {
        List<String> candidats = new java.util.ArrayList<>();
        for (String permission : PERMISSIONS) {
            ajouter(candidats, permission, prefixe);
        }
        return List.copyOf(candidats);
    }

    /** Ajoute le candidat s'il porte le préfixe et n'y figure pas déjà. */
    private static void ajouter(List<String> candidats, String candidat, String prefixe) {
        if (candidat.startsWith(prefixe) && !candidats.contains(candidat)) {
            candidats.add(candidat);
        }
    }
}
