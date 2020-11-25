package de.msiemens.educomp.front.parser.parselet

import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.parser.*

abstract class InfixParselet(val precedence: Int, val name: String) {
    abstract fun parse(parser: Parser, left: Node<Expression>, token: Token, span: Node.Span): Node<Expression>
}

class BinaryOperatorParselet(private val associativity: Associativity, precedence: Int) :
    InfixParselet(precedence, "BinaryOperatorParselet") {

    constructor(associativity: Associativity, precedence: Precedence) : this(associativity, precedence.value)

    override fun parse(parser: Parser, left: Node<Expression>, token: Token, span: Node.Span): Node<Expression> {
        val op = if (token is BinOpToken) {
            token.op
        } else {
            parser.unexpectedToken("a binary operator")
        }

        val precedence = precedence - associativity.precedence

        if (parser.token == EqToken && op != BinaryOp.And && op != BinaryOp.Or) {
            parser.bump()

            val right = parser.parseExpressionWithPrecedence(precedence)

            return Node(AssignOpExpression(op, left, right), left.span + right.span)
        }

        val right = parser.parseExpressionWithPrecedence(precedence)

        return Node(InfixExpression(op, left, right), left.span + right.span)
    }
}

object AssignParselet : InfixParselet(Precedence.Assignment.value, "AssignParselet") {
    override fun parse(parser: Parser, left: Node<Expression>, token: Token, span: Node.Span): Node<Expression> {
        val right = parser.parseExpression()

        return Node(AssignExpression(left, right), left.span + right.span)
    }
}

object CallParselet : InfixParselet(Precedence.Call.value, "CallParselet") {
    override fun parse(parser: Parser, left: Node<Expression>, token: Token, span: Node.Span): Node<Expression> {
        val args = mutableListOf<Node<Expression>>()

        while (parser.token != RParenToken) {
            args.add(parser.parseExpression())

            if (!parser.eat(CommaToken)) {
                break
            }
        }

        parser.expect(RParenToken)

        return Node(CallExpression(left, args), left.span + parser.span)
    }
}