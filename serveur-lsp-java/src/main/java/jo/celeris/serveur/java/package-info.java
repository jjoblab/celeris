/**
 * Le mini serveur de langage Java de celeris : un serveur LSP complet
 * mais volontairement petit — diagnostics poussés (équilibre des
 * accolades, littéraux non fermés, point-virgule probable), complétion
 * par mots-clés et symboles du document, survol documenté, symboles
 * hiérarchiques, plis et formatage d'indentation.
 *
 * <p>Pourquoi ce module : la démonstration a besoin d'un serveur de
 * langage réel pour tester l'éditeur de bout en bout — la poignée de
 * main, les diagnostics poussés au fil du protocole, la complétion
 * négociée — sans embarquer d'archive tierce ni sortir du mandat de
 * licences. Le serveur parle la bibliothèque de protocole en direct
 * (montage par objet opaque ou par déclaration, convention
 * {@code setClient}), ne connaît ni réseau ni système de fichiers, et
 * se réduit à des analyses déterministes : un même document rend
 * toujours les mêmes réponses.</p>
 *
 * <p>Le module est JVM pure : constructible et testable sans SDK
 * Android (critère de sortie M1). Son banc de bout en bout monte le
 * serveur par la session cliente de la frontière protocolaire — les
 * deux bouts de la même bibliothèque s'y prouvent l'un à l'autre.</p>
 */
package jo.celeris.serveur.java;
