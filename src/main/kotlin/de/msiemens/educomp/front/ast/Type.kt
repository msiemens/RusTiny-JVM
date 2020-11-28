package de.msiemens.educomp.front.ast

enum class Type(val display: String? = null) {
    Bool("bool"),
    Int("int"),
    Char("char"),
    Str("str"),
    Unit("()"),
    Err, // Special type used for expressions with type errors
    Infer
}