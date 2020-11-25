package de.msiemens.educomp.front.parser.parselet

import de.msiemens.educomp.front.ast.BinaryOp
import de.msiemens.educomp.front.parser.*

object ParseletManager {
    fun lookupPrefix(token: Token): PrefixParselet? = prefix[TokenType.fromToken(token)]

    fun lookupInfix(token: Token): InfixParselet? = infix[TokenType.fromToken(token)]

    private val prefix: Map<TokenType, PrefixParselet> = mapOf(
        Literal to LiteralParselet,
        Ident to IdentParselet,
        UnOp to PrefixOperatorParselet,
        BinOp(BinaryOp.Sub) to PrefixOperatorParselet,
        LParen to GroupParselet
    )

    private val infix: Map<TokenType, InfixParselet> = mapOf(
        BinOp(BinaryOp.Add) to BinaryOperatorParselet(Associativity.Left, Precedence.Sum),
        BinOp(BinaryOp.Sub) to BinaryOperatorParselet(Associativity.Left, Precedence.Sum),
        BinOp(BinaryOp.Mul) to BinaryOperatorParselet(Associativity.Left, Precedence.Product),
        BinOp(BinaryOp.Div) to BinaryOperatorParselet(Associativity.Left, Precedence.Product),
        BinOp(BinaryOp.Mod) to BinaryOperatorParselet(Associativity.Left, Precedence.Product),
        BinOp(BinaryOp.Pow) to BinaryOperatorParselet(Associativity.Right, Precedence.Exponent),
        BinOp(BinaryOp.And) to BinaryOperatorParselet(Associativity.Left, Precedence.And),
        BinOp(BinaryOp.Or) to BinaryOperatorParselet(Associativity.Left, Precedence.Or),
        BinOp(BinaryOp.BitXor) to BinaryOperatorParselet(Associativity.Left, Precedence.BitXor),
        BinOp(BinaryOp.BitAnd) to BinaryOperatorParselet(Associativity.Left, Precedence.BitAnd),
        BinOp(BinaryOp.BitOr) to BinaryOperatorParselet(Associativity.Left, Precedence.BitOr),
        BinOp(BinaryOp.Shl) to BinaryOperatorParselet(Associativity.Left, Precedence.Shift),
        BinOp(BinaryOp.Shr) to BinaryOperatorParselet(Associativity.Left, Precedence.Shift),
        BinOp(BinaryOp.Lt) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        BinOp(BinaryOp.Le) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        BinOp(BinaryOp.Ne) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        BinOp(BinaryOp.Ge) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        BinOp(BinaryOp.Gt) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        BinOp(BinaryOp.Eq) to BinaryOperatorParselet(Associativity.Left, Precedence.Compare),
        Eq to AssignParselet,
        LParen to CallParselet
    )
}

sealed class TokenType {
    companion object {
        fun fromToken(token: Token): TokenType = when (token) {
            is BinOpToken -> BinOp(token.op)
            is UnOpToken -> UnOp
            is LParenToken -> LParen
            is EqToken -> Eq
            is KeywordToken -> when (token.keyword) {
                Keyword.True -> Literal
                Keyword.False -> Literal
                else -> Other
            }
            is IdentToken -> Ident
            is IntToken -> Literal
            is CharToken -> Literal
            else -> Other
        }
    }
}

object Literal : TokenType()
object Ident : TokenType()
object LParen : TokenType()
object Eq : TokenType()
object UnOp : TokenType()
data class BinOp(val binaryOp: BinaryOp) : TokenType()
object Other : TokenType()
