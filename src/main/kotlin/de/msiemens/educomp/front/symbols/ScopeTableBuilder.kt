package de.msiemens.educomp.front.symbols

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.ast.Function

class ScopeTableBuilder(private val program: Program, private val table: SymbolTable) : Visitor() {
    private var currentScope: Node.Id? = null
    private var currentSymbol: String? = null

    override fun visitSymbol(symbol: Node<Symbol>) {
        currentSymbol = symbol.value.name()

        super.visitSymbol(symbol)
    }

    override fun visitBlock(block: Node<Block>) {
        // Register the new block
        table.registerScope(block.id)

        // Set the parent if present
        val current = currentScope
        if (current != null) {
            table.setParentScope(block.id, current)
        } else {
            // Top-level block of a function -> insert args into symbol table
            initFunctionScope(block.id)
        }

        // Set the current scope (needed in visit_statement/expression)
        currentScope = block.id

        super.visitBlock(block)

        currentScope = current
    }

    override fun visitStatement(statement: Node<Statement>) {
        if (statement.value is DeclarationStatement) {
            registerDeclaration(statement.value.binding)
        }

        super.visitStatement(statement)
    }

    override fun visitExpression(expression: Node<Expression>) {
        if (expression.value is CallExpression) {
            resolveCall(expression.value.function)

            return
        }

        if (expression.value is VariableExpression) {
            resolveVariable(expression.value.name)
        }

        super.visitExpression(expression)
    }

    private fun initFunctionScope(scope: Node.Id) {
        val symbol = table.lookupSymbol(currentSymbol ?: error("Current symbol is empty"))
            ?: error("Current symbol is not registered")

        // Get the function's arguments
        val bindings = if (symbol is Function) {
            symbol.bindings
        } else {
            error("Current symbol is not a function")
        }

        // Check for duplicate arguments
        bindings.groupingBy { it.value.name.value }
            .eachCount()
            .filter { it.value > 1 }
            .map { (name, count) ->
                (1 until count).forEach { i ->
                    Driver.errorAt(
                        "multiple parameters with name: `$name`",
                        bindings.drop(i).find { it.value.name.value == name }!!.span,
                        program.codeMap
                    )
                }
            }.forEach { _ ->
                return
            }

        // Register arguments in scope table
        bindings.forEach { table.registerVariable(scope, it) }
    }

    private fun resolveCall(expression: Node<Expression>) {
        // Get function name
        val name = if (expression.value is VariableExpression) {
            expression.value.name.value
        } else {
            Driver.errorAt("cannot call non-function", expression.span, program.codeMap)

            return
        }

        val symbol =
            table.lookupSymbol(name) ?: Driver.errorAt("no such function: `$name`", expression.span, program.codeMap)

        // Verify the symbol is a function
        if (symbol !is Function && symbol !is NativeFunction) {
            Driver.errorAt("cannot call non-function", expression.span, program.codeMap)
        }
    }

    private fun resolveVariable(name: Node<String>) {
        val scope = currentScope ?: error("Resolving a variable without a containing scope")

        if (table.resolveVariable(scope, name.value) == null) {
            Driver.errorAt("variable `${name.value}` not declared", name.span, program.codeMap)
        }
    }

    private fun registerDeclaration(binding: Node<Binding>) {
        val scope = currentScope ?: error("Resolving a variable without a containing scope")

        table.registerVariable(scope, binding)
    }

    fun run() {
        visit(program)
    }
}
