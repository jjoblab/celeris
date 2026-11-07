# Corpus R8/ProGuard — les règles d'un éditeur embarquable
# La réduction garde l'API publique et les réflections.

-verbose

-dontwarn org.slf4j.**

-keep public class com.celeris.demo.MainActivity {
    public static *** main(java.lang.String[]);
}

-keepclassmembers class * extends android.app.Activity {
    <init>();
    void onCreate(...);
}

-keepnames class com.celeris.core.text.**

-keepattributes Signature, *Annotation*

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontobfuscate
