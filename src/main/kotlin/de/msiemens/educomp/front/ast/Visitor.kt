package de.msiemens.educomp.front.ast

open class Visitor {
    fun visit(program: Program) {
        program.symbols.forEach { visitSymbol(it) }
    }

    protected open fun visitSymbol(symbol: Node<Symbol>) {
        when (symbol.value) {
            is Function -> {
                val fn = symbol.value
                visitIdent(fn.name)
                fn.bindings.forEach { visitBinding(it) }
                visitType(fn.returnType)
                visitBlock(fn.body)
            }
            is Static -> visitBinding(symbol.value.binding)
            is Constant -> visitBinding(symbol.value.binding)
            is NativeFunction -> Unit
        }
    }

    protected open fun visitBinding(binding: Node<Binding>) {
        visitType(binding.value.type)
        visitIdent(binding.value.name)
    }

    protected open fun visitBlock(block: Node<Block>) {
        block.value.statements.forEach { visitStatement(it) }
        visitExpression(block.value.expression)
    }

    protected open fun visitStatement(statement: Node<Statement>) {
        when (statement.value) {
            is DeclarationStatement -> {
                visitBinding(statement.value.binding)
                visitExpression(statement.value.value)
            }
            is ExpressionStatement -> visitExpression(statement.value.expression)
        }
    }

    protected open fun visitExpression(expression: Node<Expression>) {
        when (expression.value) {
            is BlockExpression -> visitBlock(expression.value.block)
            is VariableExpression -> visitIdent(expression.value.name)
            is AssignExpression -> {
                visitExpression(expression.value.left)
                visitExpression(expression.value.right)
            }
            is AssignOpExpression -> {
                visitExpression(expression.value.left)
                visitExpression(expression.value.right)
            }
            is ReturnExpression -> visitExpression(expression.value.value)
            is CallExpression -> {
                visitExpression(expression.value.function)
                expression.value.arguments.forEach { visitExpression(it) }
            }
            is GroupExpression -> visitExpression(expression.value.expression)
            is InfixExpression -> {
                visitExpression(expression.value.left)
                visitExpression(expression.value.right)
            }
            is PrefixExpression -> visitExpression(expression.value.expression)
            is IfExpression -> {
                visitExpression(expression.value.condition)
                visitBlock(expression.value.consequence)
                expression.value.alternative?.let { visitBlock(it) }
            }
            is WhileExpression -> {
                visitExpression(expression.value.condition)
                visitBlock(expression.value.body)
            }
            is LiteralExpression, BreakExpression, UnitExpression -> {
                // Skip
            }
        }
    }

    protected open fun visitIdent(ident: Node<String>) {
        // Nothing to do, it's a leaf node
    }

    protected open fun visitType(type: Type) {
        // Nothing to do, it's a leaf node
    }
}