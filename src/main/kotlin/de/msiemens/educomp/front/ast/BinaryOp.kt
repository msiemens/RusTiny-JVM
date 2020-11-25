package de.msiemens.educomp.front.ast

enum class BinaryOp(val display: String) {
    Add("+"),
    Sub("-"),
    Mul("*"),
    Div("/"),
    Mod("%"),
    Pow("**"),
    And("&&"),
    Or("||"),
    BitXor("^"),
    BitAnd("&"),
    BitOr("|"),
    Shl("<<"),
    Shr(">>"),
    Eq("=="),
    Lt("<"),
    Le("<="),
    Ne("!="),
    Ge(">="),
    Gt(">");

    enum class Type {
        Arithmetic, Logic, Bitwise, Comparison
    }

    fun type(): Type {
        return when (this) {
            Add, Sub, Mul, Div, Mod, Pow, Shl, Shr -> Type.Arithmetic
            And, Or -> Type.Logic
            BitXor, BitAnd, BitOr -> Type.Bitwise
            Eq, Lt, Le, Ne, Ge, Gt -> Type.Comparison
        }
    }
}