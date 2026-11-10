# Règles de conservation de la frontière protocolaire (ADR-015, M7.6).
# Ce fichier est une règle CONSOMMATEUR : tout assemblage réduit (R8)
# d'une application qui dépend du module joint ces règles aux siennes —
# la frontière connaît ce que la réduction ne peut pas deviner.
#
# Le protocole vit sur la réflexion : le rouage JSON de la bibliothèque
# (Gson, EPL-2.0 OU BSD-3-Clause — voir NOTICE) lit et écrit les CHAMPS
# des types du protocole par introspection, jamais par des accesseurs
# visibles du compilateur ; le cadre jsonrpc de la bibliothèque LSP4J
# bâtit des PROXYS pour les interfaces de service — le client comme le
# serveur embarqué — et des sous-classes de TypeToken à l'exécution.
# La réduction retire ce qu'elle ne voit pas : ces chemins se gardent
# ici, parce que le fil les exige à l'exécution.

# Les types du protocole — requêtes, réponses, notifications, capacités :
# champs, constructeurs et accesseurs se gardent entiers, la
# sérialisation réflexive les parcourt.
-keep class org.eclipse.lsp4j.** { *; }

# Le cadre jsonrpc de la bibliothèque : proxys, adaptateurs des Either,
# résolveurs de méthodes — la mécanique réflexive du fil.
-keep class org.eclipse.lsp4j.jsonrpc.** { *; }

# Le rouage JSON : Gson bâtit ses adaptateurs par réflexion et ses
# TypeToken par sous-classement à l'exécution.
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.InstanceCreator
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Les signatures génériques se lisent à l'exécution (TypeToken, Either,
# proxys typés) — l'attribut ne se retire jamais.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Le rouage JSON sait optionnellement passer par les API internes du
# JDK (allocation sans constructeur) : la plateforme Android ne les
# porte pas, les chemins y sont morts — jamais manquants.
-dontwarn sun.misc.**
-dontwarn jdk.internal.**
