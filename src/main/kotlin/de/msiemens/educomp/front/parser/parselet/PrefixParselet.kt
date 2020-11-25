package de.msiemens.educomp.front.parser.parselet

import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.parser.*

abstract class PrefixParselet(val name: String) {
    abstract fun parse(parser: Parser, token: Token, span: Node.Span): Node<Expression>
}

object IdentParselet : PrefixParselet("IdentParselet") {
    override fun parse(parser: Parser, token: Token, span: Node.Span): Node<Expression> {
        if (token is IdentToken) {
            return Node(VariableExpression(Node(token.value, span)), span)
        }

        parser.unexpectedToken("an identifier")
    }
}

object LiteralParselet : PrefixParselet("LiteralParselet") {
    override fun parse(parser: Parser, token: Token, span: Node.Span): Node<Expression> {
        val value =  when(token) {
            is IntToken -> IntVal(token.value)
            is CharToken -> CharVal(token.value)
            is StringToken -> StringVal(token.value)
            is KeywordToken -> when (token.keyword) {
                Keyword.True -> BoolVal(true)
                Keyword.False -> BoolVal(false)
                else -> parser.unexpectedToken("true or false")
            }
            else -> parser.unexpectedToken("a literal")
        }

        return Node(LiteralExpression(value), span)
    }
}

object PrefixOperatorParselet : PrefixParselet("PrefixOperatorParselet") {
    override fun parse(parser: Parser, token: Token, span: Node.Span): Node<Expression> {
        val operand = parser.parseExpression()
        val op = when (token) {
            is UnOpToken -> token.op
            is BinOpToken -> when (token.op) {
                BinaryOp.Sub -> UnaryOp.Neg
                else -> parser.unexpectedToken("a unary operator")
            }
            else -> parser.unexpectedToken("a unary operator")
        }

        return Node(PrefixExpression(op, operand), span + operand.span)
    }
}

object GroupParselet : PrefixParselet("GroupParselet") {
    override fun parse(parser: Parser, token: Token, span: Node.Span): Node<Expression> {
        val expr = parser.parseExpression()
        parser.expect(RParenToken)

        return Node(GroupExpression(expr), span + parser.span)
    }
}