package de.msiemens.educomp.front.parser

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.CodeMap
import de.msiemens.educomp.front.ast.BinaryOp
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.UnaryOp

class Lexer(private val source: String, private val codeMap: CodeMap) {
    private var pos = 0
    private var curr: Char? = if (source.isNotEmpty()) {
        source[0]
    } else {
        null
    }
    private var lineNo = 1

    data class Spanned<T>(val value: T, val span: Node.Span) {
        constructor(value: T, lo: Int, hi: Int) : this(value, Node.Span(lo, hi - lo))
    }

    fun next(): Spanned<Token> {
        while (!eof()) {
            return read() ?: continue
        }

        return Spanned(EOFToken, pos, pos)
    }

    private fun read(): Spanned<Token>? {
        val c = curr ?: fatal("unexpected EOF")
        val lo = pos

        val token: Token? = when (c) {
            '+' -> emit(BinOpToken(BinaryOp.Add))
            '-' -> emitIf('>' to { RArrowToken }, default = BinOpToken(BinaryOp.Sub))
            '*' -> emitIf('*' to { BinOpToken(BinaryOp.Pow) }, default = BinOpToken(BinaryOp.Mul))
            '/' -> emitIf('/' to { skipComment(); null }, default = BinOpToken(BinaryOp.Div))
            '%' -> emit(BinOpToken(BinaryOp.Mod))
            '&' -> emitIf('&' to { BinOpToken(BinaryOp.And) }, default = BinOpToken(BinaryOp.BitAnd))
            '|' -> emitIf('|' to { BinOpToken(BinaryOp.Or) }, default = BinOpToken(BinaryOp.BitOr))
            '^' -> emit(BinOpToken(BinaryOp.BitXor))
            '<' -> emitIf(
                '<' to { BinOpToken(BinaryOp.Shl) },
                '=' to { BinOpToken(BinaryOp.Le) },
                default = BinOpToken(BinaryOp.Lt)
            )
            '>' -> emitIf(
                '>' to { BinOpToken(BinaryOp.Shr) },
                '=' to { BinOpToken(BinaryOp.Ge) },
                default = BinOpToken(BinaryOp.Gt)
            )
            '=' -> emitIf('=' to { BinOpToken(BinaryOp.Eq) }, default = EqToken)
            '!' -> emitIf('=' to { BinOpToken(BinaryOp.Ne) }, default = UnOpToken(UnaryOp.Not))
            '(' -> emit(LParenToken)
            ')' -> emit(RParenToken)
            '{' -> emit(LBraceToken)
            '}' -> emit(RBraceToken)
            ',' -> emit(CommaToken)
            ':' -> emit(ColonToken)
            ';' -> emit(SemicolonToken)
            '\'' -> tokenizeChar()
            '"' -> tokenizeString()
            else -> when {
                c.isLetter() -> {
                    tokenizeIdentifier()
                }
                c.isDigit() -> {
                    tokenizeInteger()
                }
                c.isWhitespace() -> {
                    if (c == '\n') {
                        lineNo++

                        codeMap.newLine(pos)
                    }

                    bump()

                    return null
                }
                else -> fatal("unexpected character: `$c`")
            }
        }

        return Spanned(token ?: return null, lo, pos)
    }

    private fun emit(token: Token): Token {
        bump()

        return token
    }

    private fun emitIf(vararg conditions: Pair<Char, () -> Token?>, default: Token): Token? {
        bump()

        val cond = conditions.find { it.first == curr } ?: return default

        bump()

        return cond.second()
    }

    // Tokenizers

    private fun skipComment() = eatAll { it != '\n' }

    private fun tokenizeIdentifier(): Token {
        val identifier = collect { it.isLetter() || it.isDigit() || it == '_' }

        val keyword = Keyword.values().find { it.value == identifier }
        if (keyword != null) {
            return KeywordToken(keyword)
        }

        return IdentToken(identifier.intern())
    }

    private fun tokenizeString(): Token {
        bump()

        val value = StringBuilder()

        var escaped = false
        var c = curr

        while (c != null) {
            if (!escaped && c == '"') {
                break
            }

            if (escaped) {
                when (curr) {
                    'n' -> value.append('\n')
                    '"' -> CharToken('"')
                    else -> {
                        val c = curr

                        if (c != null) {
                            fatal("unsupported or invalid escape sequence: \\$c")
                        } else {
                            fatal("expected escaped char, found EOF")
                        }
                    }
                }
            } else {
                value.append(c)
            }

            escaped = c == '\\'

            bump()
            c = curr
        }

        expect('"')

        return StringToken(value.toString())
    }

    private fun tokenizeInteger(): Token {
        val value = collect { it.isDigit() }
        try {
            return IntToken(value.toInt())
        } catch (e: NumberFormatException) {
            fatal("invalid integer: `$value`")
        }
    }

    private fun tokenizeChar(): Token {
        bump() // Matched the opening quote, move on

        val c = curr ?: fatal("expected a char, found EOF")
        val token = if (c == '\\') {
            tokenizeEscapedChar()
        } else {
            CharToken(c)
        }

        bump() // Matched a (possibly escaped) character, move along

        expect('\'') // Match closing quote

        return token
    }

    private fun tokenizeEscapedChar(): CharToken {
        bump() // Currently on the backslash, look at the next character

        return when (curr) {
            'n' -> CharToken('\n')
            '\'' -> CharToken('\'')
            else -> {
                val c = curr

                if (c != null) {
                    fatal("unsupported or invalid escape sequence: \\$c")
                } else {
                    fatal("expected escaped char, found EOF")
                }
            }
        }
    }

    // Character processing

    private fun bump() {
        curr = if (pos == source.lastIndex) {
            null
        } else {
            pos++

            source[pos]
        }
    }

    private fun currentEscaped(): String = when (curr) {
        '\t' -> "\\t"
        '\r' -> "\\r"
        '\n' -> "\\n"
        '\\' -> "\\\\"
        else -> curr.toString()
    }

    private fun expect(expected: Char) {
        if (curr != expected) {
            val found = if (curr != null) {
                "`${currentEscaped()}`"
            } else {
                "EOF"
            }

            fatal("Expected `$expected`, found $found")
        }

        bump()
    }

    private fun collect(cond: (Char) -> Boolean): String {
        val start = pos

        var c = curr

        while (c != null) {
            if (cond(c)) {
                bump()

                c = curr
            } else {
                break
            }
        }

        return source.substring(start, pos)
    }

    private fun eatAll(cond: (Char) -> Boolean) {
        collect(cond)
    }

    // Helpers

    private fun eof(): Boolean = curr == null

    private fun fatal(msg: String): Nothing {
        Driver.errorAt(msg, codeMap.resolve(pos))
        Driver.abort()
    }
}