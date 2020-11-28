package de.msiemens.educomp.front.ast

import de.msiemens.educomp.front.CodeMap
import de.msiemens.educomp.front.Signature

data class Program(val symbols: List<Node<Symbol>>, val codeMap: CodeMap)

/**
 * A top level symbol.
 */
sealed class Symbol {
    abstract fun name(): String
}

/**
 * A function.
 */
data class Function(
    val name: Node<String>,
    val bindings: List<Node<Binding>>,
    val returnType: Node<Type>,
    val body: Node<Block>,
) : Symbol() {
    override fun name(): String = name.value

    val signature: Signature
        get() = bindings.map { it.value.type.value } to returnType.value
}

data class NativeFunction(val name: String, val arguments: List<Type>, val returnType: Type) : Symbol() {
    override fun name(): String = name

    val signature: Signature
        get() = arguments to returnType
}

/**
 * A static value (can be modified at runtime).
 */
data class Static(
    val binding: Node<Binding>,
    val value: Node<Expression>,
) : Symbol() {
    override fun name(): String = binding.value.name.value
}

/**
 * A constant value.
 *
 * Usages will be replaced with the value at compilation time.
 */
data class Constant(
    val binding: Node<Binding>,
    val value: Node<Expression>,
) : Symbol() {
    override fun name(): String = binding.value.name.value
}

/**
 * A block of statements (e.g. function body, if body, ...).
 */
data class Block(
    val statements: List<Node<Statement>>,
    val expression: Node<Expression>,
)

/**
 * A binding of a value to a name (e.g. local variable, function argument).
 */
data class Binding(
    val type: Node<Type>,
    val name: Node<String>,
)

/**
 * A declaration or an expression terminated with a semicolon.
 */
sealed class Statement

data class DeclarationStatement(
    val name: Node<String>,
    val type: Node<Type>,
    val value: Node<Expression>,
) : Statement()

data class ExpressionStatement(val expression: Node<Expression>) : Statement()

sealed class Expression

/**
 * A block expression.
 */
data class BlockExpression(
    val block: Node<Block>,
) : Expression()

/**
 * A literal value.
 */
data class LiteralExpression(
    val value: Value
) : Expression()

/**
 * A variable referenced by name.
 */
data class VariableExpression(
    val name: Node<String>,
) : Expression()

/**
 * An assignment expression.
 */
data class AssignExpression(
    val left: Node<Expression>,
    val right: Node<Expression>
) : Expression()

/**
 * An assignment expression with an additional operator (ex: `a += 1`).
 */
data class AssignOpExpression(
    val op: BinaryOp,
    val left: Node<Expression>,
    val right: Node<Expression>
) : Expression()

/**
 * Exit the function with an optional return value.
 */
data class ReturnExpression(
    val value: Node<Expression>,
) : Expression()

/**
 * A function call.
 */
data class CallExpression(
    val function: Node<Expression>,
    val arguments: List<Node<Expression>>,
) : Expression()

/**
 * A grouped expression.
 *
 * Used for operator precedence, ex: `2 * (3 + 5)`, where `(3 + 5)` is stored in a group.
 */
data class GroupExpression(
    val expression: Node<Expression>,
) : Expression()

/**
 * An expression with an infix operator (`2 + 5`, `a == false`).
 */
data class InfixExpression(
    val op: BinaryOp,
    val left: Node<Expression>,
    val right: Node<Expression>
) : Expression()

/**
 * An expression with a prefix operator (`-2`, `!a`).
 */
data class PrefixExpression(
    val op: UnaryOp,
    val expression: Node<Expression>,
) : Expression()

/**
 * A conditional with an optional `else` branch.
 */
data class IfExpression(
    val condition: Node<Expression>,
    val consequence: Node<Block>,
    val alternative: Node<Block>?,
) : Expression()

/**
 * A while loop.
 */
data class WhileExpression(
    val condition: Node<Expression>,
    val body: Node<Block>,
) : Expression()

/**
 * Break out of a loop.
 */
object BreakExpression : Expression()

/**
 * An expression without any content.
 */
object UnitExpression : Expression()
