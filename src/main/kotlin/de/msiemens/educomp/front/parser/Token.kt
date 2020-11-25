package de.msiemens.educomp.front.parser

import de.msiemens.educomp.front.ast.BinaryOp
import de.msiemens.educomp.front.ast.UnaryOp

sealed class Token(val display: String)

data class BinOpToken(val op: BinaryOp) : Token(op.display)
data class UnOpToken(val op: UnaryOp) : Token(op.display)
object LParenToken : Token("(")
object RParenToken : Token(")")
object LBraceToken : Token("{")
object RBraceToken : Token("}")
object CommaToken : Token(",")
object ColonToken : Token(":")
object SemicolonToken : Token(";")
object RArrowToken : Token("->")
object EqToken : Token("=")
data class KeywordToken(val keyword: Keyword) : Token(keyword.value)
data class IdentToken(val value: String) : Token(value)
data class IntToken(val value: Int) : Token(value.toString())
data class CharToken(val value: Char) : Token(value.toString())
data class StringToken(val value: String): Token(value)
object EOFToken : Token("EOF")

enum class Keyword(val value: String) {
    Break("break"),
    Const("const"),
    Else("else"),
    False("false"),
    Fn("fn"),
    If("if"),
    Impl("impl"),
    Let("let"),
    Return("return"),
    Static("static"),
    True("true"),
    While("while"),
}