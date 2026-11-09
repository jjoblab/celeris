# Règles de conservation de la démonstration (ADR-015, M7.6) — côté
# terrain. Le serveur XML réel embarqué (identité, licence et provenance
# au NOTICE) s'y lance PAR DÉSIGNATION : la classe du serveur et
# l'interface cliente se nomment en toutes lettres chez
# ServeurEmbarque.monter, l'instanciation et le proxy passent par la
# réflexion, ses points d'extension se chargent par ServiceLoader —
# invisibles au réducteur.
#
# L'archive du serveur est déjà réduite au octet près par sa découpe
# reproductible (ADR-015, décision 3) : ce qu'elle porte se garde
# ENTIER — la réduire à nouveau casserait en silence les chemins
# réflexifs et de service que la découpe a justement vérifiés.

# Le serveur et ses points d'extension (ServiceLoader IXMLExtension,
# participants DTD, XSD, extraits de code…).
-keep class org.eclipse.lemminx.** { *; }

# Ses analyseurs embarqués : Xerces (analyse et validation), Relax NG
# et ses ponts (thaiopensource, iso_relax, relaxng, Relaxer), jsoup,
# Remark, la StAX du bureau qu'Android ne porte pas, et la fermeture
# de portée de la bibliothèque de collections issue de la découpe.
-keep class org.apache.xerces.** { *; }
-keep class org.apache.html.** { *; }
-keep class org.apache.wml.** { *; }
-keep class org.apache.xml.** { *; }
-keep class com.thaiopensource.** { *; }
-keep class org.iso_relax.** { *; }
-keep class org.relaxng.** { *; }
-keep class jp.gr.xml.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.overzealous.** { *; }
-keep class javax.xml.stream.** { *; }
-keep class com.google.common.** { *; }

# Les analyseurs du bureau touchent des API que la plateforme Android
# ne porte pas : annotations de compilation (JSR-305, j2objc), ponts
# optionnels (Saxon, Xalan, Ant, commons-cli de la ligne de commande
# de Remark), modules DOM des niveaux 2/3 (html, events, ranges,
# traversal, ls, views), ponts StAX de la transformation et le RELAX
# « swift » absent de la découpe — les chemins y sont morts, jamais
# manquants ; le réducteur n'a pas à s'en inquiéter.
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.xml.transform.stax.**
-dontwarn jp.co.swiftinc.relax.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.xalan.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.commons.cli.**
-dontwarn org.w3c.dom.html.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.ranges.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.w3c.dom.ls.**
-dontwarn org.w3c.dom.views.**
-dontwarn org.w3c.dom.ElementTraversal
# La surveillance du processus parent ne sert que lancée en ligne de
# commande : la démo monte le serveur en processus par son API, le
# chemin y est mort.
-dontwarn java.lang.ProcessHandle
