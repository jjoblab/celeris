; Requête de surbrillance Kotlin — écrite pour celeris sur les noms
; de nœuds de la grammaire tree-sitter-kotlin (Amaan Qureshi et
; contributeurs, MIT) : la grammaire ne publie pas de requête à la
; version embarquée, celle-ci couvre les constructions courantes
; (le lecteur de requêtes y lit noms et captures).

(line_comment) @comment
(string_literal) @string
(multiline_string_literal) @string
(number_literal) @number
(character_literal) @string
(annotation) @attribute
(file_annotation) @attribute
(label) @label

(type) @type
(function_declaration) @function
(class_declaration) @type
(object_declaration) @type

; Les mots réservés de la grammaire se lisent en littéraux.
[
  "as"
  "break"
  "by"
  "catch"
  "class"
  "constructor"
  "continue"
  "do"
  "else"
  "false"
  "finally"
  "for"
  "fun"
  "get"
  "if"
  "import"
  "in"
  "interface"
  "is"
  "null"
  "object"
  "package"
  "return"
  "set"
  "super"
  "this"
  "throw"
  "true"
  "try"
  "typealias"
  "val"
  "var"
  "when"
  "while"
] @keyword
