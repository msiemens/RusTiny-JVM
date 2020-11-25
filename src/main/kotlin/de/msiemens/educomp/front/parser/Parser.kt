package de.msiemens.educomp.front.parser

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.CodeMap
import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.ast.Function
import de.msiemens.educomp.front.parser.parselet.ParseletManager

class Parser(private val lexer: Lexer, private val codeMap: CodeMap) {
    private val init = lexer.next()

    internal var token = init.value
    var span = init.span

    fun parse(): Program {
        val source = mutableListOf<Node<Symbol>>()

        while (token != EOFToken) {
            source.add(parseSymbol())
        }

        return Program(source, codeMap)
    }

    // Symbols

    private fun parseSymbol(): Node<Symbol> {
        val t = token

        return if (t is KeywordToken) when (t.keyword) {
            Keyword.Fn -> parseFunction()
            Keyword.Static -> parseStatic()
            Keyword.Const -> parseConst()
            else -> unexpectedToken("a symbol")
        }
        else unexpectedToken("a symbol")
    }

    private fun parseFunction(): Node<Symbol> {
        // Grammar:  k_fn IDENT LPAREN (binding COMMA)* binding? RPAREN (RARROW TYPE)? block

        val lo = span

        // Parse `fn <name>`
        expect(KeywordToken(Keyword.Fn))
        val name = parseIdent()

        expect(LParenToken)

        // Parse the arguments declaration
        val bindings = mutableListOf<Node<Binding>>()
        while (token != RParenToken) {
            bindings.add(parseBinding())

            if (!eat(CommaToken)) {
                break
            }
        }

        expect(RParenToken)

        // Parse the return type
        val returnType = if (eat(RArrowToken)) {
            parseType()
        } else {
            Type.Unit
        }

        val body = parseBlock()

        return Node(Function(name, bindings, returnType, body), lo + span)
    }

    private fun parseStatic(): Node<Symbol> {
        // Grammar: k_static binding EQ literal

        val lo = span

        expect(KeywordToken(Keyword.Static))

        val binding = parseBinding()

        expect(EqToken)

        val value = parseLiteral()

        expect(SemicolonToken)

        return Node(Static(binding, value), lo + span)
    }

    private fun parseConst(): Node<Symbol> {
        // Grammar: k_const binding EQ literal

        val lo = span

        expect(KeywordToken(Keyword.Const))

        val binding = parseBinding()

        expect(EqToken)

        val value = parseLiteral()

        expect(SemicolonToken)

        return Node(Constant(binding, value), lo + span)
    }

    // Shared

    private fun parseBinding(): Node<Binding> {
        // Grammar: IDENT COLON TYPE

        val lo = span

        val name = parseIdent()

        expect(ColonToken)

        val type = parseType()

        return Node(Binding(type, name), lo + span)
    }

    private fun parseBlock(): Node<Block> {
        // Grammar: LBRACE (statement SEMICOLON)* expr? RBRACE

        val lo = span

        // Blocks are funny things in Rust. They contain:
        // - a list of semicolon-separated statements,
        // - and an optional expression that acts as the block's value.
        // It requires a little work to get this right.

        expect(LBraceToken)

        val statements = mutableListOf<Node<Statement>>()
        var expression: Node<Expression>

        // Parse all statements
        while (true) {
            val curr = span

            if (eat(RBraceToken)) {
                expression = Node(UnitExpression, curr)

                break
            }

            val statement = parseStatement()

            if (exprStatementNeedsSemicolon(statement)) {
                val closingToken = expect(listOf(SemicolonToken, RBraceToken))
                if (closingToken == RBraceToken) {
                    expression = (statement.value as ExpressionStatement).expression

                    break
                }
            } else {
                if (eat(RBraceToken)) {
                    expression = (statement.value as ExpressionStatement).expression

                    break
                }
            }

            while (eat(SemicolonToken)) {
                // Eat all remaining semicolons
            }

            statements += statement
        }

        return Node(Block(statements, expression), lo + span)
    }

    private fun exprStatementNeedsSemicolon(statement: Node<Statement>): Boolean {
        return when (val s = statement.value) {
            is DeclarationStatement -> true
            is ExpressionStatement -> {
                val expr = s.expression.value

                expr !is BlockExpression && expr !is IfExpression && expr !is WhileExpression
            }
        }
    }

    // Statements

    private fun parseStatement(): Node<Statement> {
        val lo = span

        val t = token

        if (t is KeywordToken && t.keyword == Keyword.Let) {
            return parseDeclaration()
        }

        val expr = parseExpression()

        return Node(ExpressionStatement(expr), lo + span)
    }

    private fun parseDeclaration(): Node<Statement> {
        // Grammar: k_let binding EQ expression

        val lo = span

        expect(KeywordToken(Keyword.Let))

        val name = parseBinding()

        expect(EqToken)

        val value = parseExpression()

        return Node(DeclarationStatement(name, value), lo + span)
    }

    // Expressions

    internal fun parseExpression(): Node<Expression> = parseExpressionWithPrecedence(0)

    internal fun parseExpressionWithPrecedence(precedence: Int): Node<Expression> {
        return when (val t = token) {
            is LBraceToken -> {
                val lo = span

                val block = parseBlock()

                Node(BlockExpression(block), lo + span)
            }
            is KeywordToken -> when (t.keyword) {
                Keyword.If -> parseIf()
                Keyword.While -> parseWhile()
                Keyword.Return -> {
                    val lo = span

                    bump()

                    // Parse the return value
                    if (token == RBraceToken || token == SemicolonToken) {
                        Node(ReturnExpression(Node(UnitExpression, span)), lo + span)
                    } else {
                        Node(ReturnExpression(parseExpression()), lo + span)
                    }
                }
                Keyword.Break -> {
                    val lo = span

                    bump()

                    Node(BreakExpression, lo + span)
                }
                else -> prattParser(precedence)
            }
            else -> prattParser(precedence)
        }
    }

    private fun prattParser(precedence: Int): Node<Expression> {
        val parselet = ParseletManager.lookupPrefix(token) ?: unexpectedToken("a prefix expression")

        val prefixToken = token
        val prefixSpan = span

        bump()

        var left = parselet.parse(this, prefixToken, prefixSpan)

        while (precedence < currentPrecedence()) {
            val infixParselet = ParseletManager.lookupInfix(token) ?: unexpectedToken("an infix expression")

            val infixToken = token
            val infixSpan = span

            bump()

            left = infixParselet.parse(this, left, infixToken, infixSpan)
        }

        return left
    }

    private fun currentPrecedence(): Int = ParseletManager.lookupInfix(token)?.precedence ?: 0

    private fun parseIf(): Node<Expression> {
        // Grammar: k_if expression block (k_else block)?

        val lo = span

        expect(KeywordToken(Keyword.If))

        val condition = parseExpression()
        val consequence = parseBlock()
        val alternative = if (eat(KeywordToken(Keyword.Else))) {
            parseBlock()
        } else {
            null
        }

        return Node(IfExpression(condition, consequence, alternative), lo + span)
    }

    private fun parseWhile(): Node<Expression> {
        // Grammar: k_while expression block

        val lo = span

        expect(KeywordToken(Keyword.While))

        val condition = parseExpression()
        val body = parseBlock()

        return Node(WhileExpression(condition, body), lo + span)
    }

    // Tokens

    private fun parseIdent(): Node<String> {
        val t = token
        val s = span

        val ident = if (t is IdentToken) {
            t.value
        } else {
            unexpectedToken("an identifier")
        }

        bump()

        return Node(ident, s)
    }

    private fun parseLiteral(): Node<Expression> {
        val s = span
        val value = when (val t = token) {
            is IntToken -> IntVal(t.value)
            is CharToken -> CharVal(t.value)
            is StringToken -> StringVal(t.value)
            is KeywordToken -> when (t.keyword) {
                Keyword.True -> BoolVal(true)
                Keyword.False -> BoolVal(false)
                else -> unexpectedToken("true or false")
            }
            else -> unexpectedToken("a literal")
        }

        bump()

        return Node(LiteralExpression(value), s)
    }

    private fun parseType(): Type {
        val identifier = parseIdent()

        return Type.values().find { it.display == identifier.value } ?: unexpectedToken("a type")
    }

    // Token processing

    internal fun expect(t: Token) {
        if (!eat(t)) {
            fatal("expected `${t.display}`, found `${token.display}`")
        }
    }

    private fun expect(tokens: List<Token>): Token {
        val t = tokens.find { it == token }
        if (t != null) {
            bump()

            return t
        }

        fatal("expected one of ${tokens.joinToString { "`${it.display}`" }}, found `${token.display}`")
    }

    internal fun eat(t: Token): Boolean {
        return if (token == t) {
            bump()
            true
        } else {
            false
        }
    }

    internal fun bump() {
        val next = lexer.next()

        token = next.value
        span = next.span
    }

    // Error handling

    internal fun unexpectedToken(expected: String?): Nothing {
        if (expected != null) {
            fatal("unexpected token: `${token.display}`, expected $expected")
        } else {
            fatal("unexpected token: `${token.display}`")
        }
    }

    private fun fatal(msg: String): Nothing {
        Driver.errorAt(msg, span, codeMap)
        Driver.abort()
    }
}