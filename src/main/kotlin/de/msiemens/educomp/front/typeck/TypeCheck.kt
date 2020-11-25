package de.msiemens.educomp.front.typeck

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.ast.Function
import de.msiemens.educomp.front.symbols.SymbolTable

class TypeCheck(private val program: Program, private val symbolTable: SymbolTable) {
    data class FunctionContext(val returnType: Type, var explicitReturn: Boolean)

    private val types = mutableMapOf<Node.Id, Type>()
    private var scope: Node.Id = Node.Id.EMPTY
    private var ctx = FunctionContext(Type.Unit, false)

    private fun checkFunction(returnType: Type, body: Node<Block>) {
        ctx = FunctionContext(returnType, false)

        val implicitReturnType = checkBlock(body)

        if (ctx.explicitReturn) {
            // There was an explicit return. Thus, the body has to evaluate to ().
            typeCheck(implicitReturnType, Type.Unit, body)
        } else if (implicitReturnType == Type.Unit && returnType != Type.Unit) {
            // There was NO explicit return. Thus, the body has to evaluate
            // to the return type from the function signature.
            Driver.errorAt("missing return value/return statement", body.span, program.codeMap)
        } else {
            typeCheck(implicitReturnType, returnType, body)
        }
    }

    private fun checkBlock(block: Node<Block>, expected: Type? = null): Type {
        val oldScope = scope
        scope = block.id

        block.value.statements.forEach { checkStatement(it) }

        val blockType = checkExpression(block.value.expression, expected)
        types[block.id] = blockType

        scope = oldScope

        return blockType
    }

    private fun checkStatement(statement: Node<Statement>) {
        when (statement.value) {
            is DeclarationStatement -> {
                val variableName = statement.value.binding.value.name.value
                val variable = symbolTable.resolveVariable(scope, variableName)
                    ?: error("Variable $variableName missing in symbol table")

                checkExpression(statement.value.value, variable.type)
            }
            is ExpressionStatement -> checkExpression(statement.value.expression)
        }
    }

    private fun checkExpression(expression: Node<Expression>, expected: Type? = null): Type {
        val type = when (expression.value) {
            is LiteralExpression -> expression.value.value.type
            is VariableExpression -> resolveVariable(expression.value.name)
            is AssignExpression -> checkAssign(expression.value.left, expression.value.right)
            is AssignOpExpression -> checkAssignOp(expression.value.op, expression.value.left, expression.value.right)
            is ReturnExpression -> checkReturn(expression.value.value)
            is CallExpression -> checkCall(expression.value.function, expression.value.arguments)
            is GroupExpression -> checkExpression(expression.value.expression, expected)
            is InfixExpression -> checkInfix(expression.value.op, expression.value.left, expression.value.right)
            is PrefixExpression -> checkPrefix(expression.value.op, expression.value.expression)
            is IfExpression -> checkIf(
                expression.value.condition,
                expression.value.consequence,
                expression.value.alternative,
                expected
            )
            is WhileExpression -> checkWhile(expression.value.condition, expression.value.body)
            BreakExpression, UnitExpression -> Type.Unit
        }

        types[expression.id] = type

        if (expected != null) {
            return typeCheck(type, expected, expression)
        }

        return type
    }

    private fun resolveVariable(name: Node<String>): Type {
        val variable = symbolTable.resolveVariable(scope, name.value)
        if (variable == null) {
            Driver.errorAt("no variable named ${name.value}", name.span, program.codeMap)

            return Type.Err
        }

        return variable.type
    }

    private fun checkAssign(lhs: Node<Expression>, rhs: Node<Expression>): Type {
        // Infer from left hand side
        val expected = checkExpression(lhs)
        checkExpression(rhs, expected)

        return Type.Unit
    }

    private fun checkAssignOp(op: BinaryOp, lhs: Node<Expression>, rhs: Node<Expression>): Type {
        checkInfix(op, lhs, rhs)

        return Type.Unit
    }

    private fun checkReturn(value: Node<Expression>): Type {
        ctx.explicitReturn = true

        return checkExpression(value, ctx.returnType)
    }

    private fun checkCall(callee: Node<Expression>, arguments: List<Node<Expression>>): Type {
        val name = if (callee.value is VariableExpression) {
            callee.value.name.value
        } else {
            error("Function call on non-function")
        }

        val function = symbolTable.lookupFunction(name)
        if (function == null) {
            Driver.errorAt("no function named $name", callee.span, program.codeMap)

            return Type.Err
        }

        // Check argument count
        if (arguments.size != function.first.size) {
            Driver.errorAt(
                "mismatching argument count: expected ${function.first.size}, got ${arguments.size}",
                callee.span,
                program.codeMap
            )
        }

        // Check argument types
        arguments.zip(function.first).forEach {
            checkExpression(it.first, it.second)
        }

        return function.second
    }

    private fun checkInfix(op: BinaryOp, lhs: Node<Expression>, rhs: Node<Expression>): Type {
        return when (op.type()) {
            BinaryOp.Type.Arithmetic -> {
                checkExpression(lhs, Type.Int)
                checkExpression(rhs, Type.Int)

                Type.Int
            }
            BinaryOp.Type.Logic -> {
                checkExpression(lhs, Type.Bool)
                checkExpression(rhs, Type.Bool)

                Type.Bool
            }
            BinaryOp.Type.Bitwise -> {
                // Both Ints and Bools are accepted here, thus we infer the
                // used type from the left hand side

                val type = checkExpression(lhs)
                if (type == Type.Bool || type == Type.Int) {
                    checkExpression(rhs, type)
                } else {
                    Driver.errorAt(
                        "binary operation `${op.display}` cannot be applied to ${type.display}",
                        lhs.span,
                        program.codeMap
                    )

                    Type.Err
                }
            }
            BinaryOp.Type.Comparison -> {
                val type = checkExpression(lhs)
                checkExpression(rhs, type)

                Type.Bool
            }
        }
    }

    private fun checkPrefix(op: UnaryOp, item: Node<Expression>): Type {
        return when (op) {
            UnaryOp.Neg -> checkExpression(item, Type.Int)
            UnaryOp.Not -> {
                val type = checkExpression(item)
                if (type == Type.Bool || type == Type.Int) {
                    type
                } else {
                    Driver.errorAt(
                        "unary operation `${op.display}` cannot be applied to ${type.display}",
                        item.span,
                        program.codeMap
                    )

                    Type.Err
                }
            }
        }
    }

    private fun checkIf(
        condition: Node<Expression>,
        consequence: Node<Block>,
        alternative: Node<Block>?,
        expected: Type?
    ): Type {
        checkExpression(condition, Type.Bool)

        // Verify that the conseq type is matches `expected` ...
        // ... or infer it if `expected` is None
        val consequenceType = checkBlock(consequence, expected)

        if (alternative != null) {
            checkBlock(alternative, consequenceType)
        } else if (expected != null && expected != Type.Unit) {
            Driver.errorAt("missing else clause", consequence.span, program.codeMap)
        }

        if (expected != null) {
            // The containing expr/statement would usually check the consequence type
            // again. But if there was a type error, it will have been reported
            // by checkBlock(consequence, expected) above. Thus, we return a type
            // error to not throw multiple errors.

            if (expected != consequenceType) {
                return Type.Err
            }
        }

        return consequenceType
    }

    private fun checkWhile(condition: Node<Expression>, body: Node<Block>): Type {
        checkExpression(condition, Type.Bool)
        checkBlock(body, Type.Unit)

        return Type.Unit
    }

    private fun <T> typeCheck(actual: Type, expected: Type, node: Node<T>): Type {
        if (actual == Type.Err) {
            // Assume there's nothing wrong to collect more type errors
            return actual
        }

        if (actual != expected) {
            Driver.errorAt(
                "type mismatch: expected ${expected.display}, got ${actual.display}",
                node.span,
                program.codeMap
            )

            return Type.Err
        }

        return actual
    }

    fun run(): Map<Node.Id, Type> {
        program.symbols.forEach {
            when (it.value) {
                is Function -> checkFunction(it.value.returnType, it.value.body)
                is Static -> checkExpression(it.value.value, it.value.binding.value.type)
                is Constant -> checkExpression(it.value.value, it.value.binding.value.type)
                is NativeFunction -> {
                    // Do nothing
                }
            }
        }

        Driver.abortOnErrors()

        return types
    }
}
