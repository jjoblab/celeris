/**
 * La plateforme de langages (M7.6) : descripteurs, configurations et
 * paquets de données, registre et assemblage — et la déclaration des
 * sources syntaxiques. Les analyseurs écrits s'enregistrent
 * ({@link jo.celeris.core.language.WrittenLanguages}) ; la case
 * tree-sitter du niveau 2 s'y déclare aussi
 * ({@link jo.celeris.core.language.packs.TreeSitterLanguages}) — vide à
 * M7.6 (ADR-016), prête à l'hôte qui embarquerait l'arrière natif.
 */
@NullMarked
package jo.celeris.core.language;

import org.jspecify.annotations.NullMarked;
