/**
 * L'artefact agrégateur de celeris : une dépendance, l'éditeur entier
 * — la vue ({@code android-view}), les widgets ({@code android-widgets}),
 * la couche hôte ({@code android-host} : la façade
 * {@code CelerisEditor}, l'orchestrateur des popups) et la voie de
 * grammaires ({@code core-syntax-grammars}).
 *
 * <p>Ce module ne porte aucun code : il agrège. Les règles de
 * dépendance d'ARCHITECTURE.md n'y voient qu'un assemblage — tout va
 * vers le bas, rien ne remonte. Le LSP embarqué ({@code lsp-protocole})
 * reste optionnel : il porte désucrage et règles R8, l'hôte qui n'en
 * veut pas ne le paie pas (ADR-0022).</p>
 *
 * <p>Le montage court vit dans USAGE § 4.2 : la façade
 * {@code CelerisEditor.attach(view, layer, kit)} compose tout.</p>
 */
package jo.celeris;
