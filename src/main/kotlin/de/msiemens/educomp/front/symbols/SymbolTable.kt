package de.msiemens.educomp.front.symbols

import de.msiemens.educomp.back.NativeGenerator
import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.CodeMap
import de.msiemens.educomp.front.Signature
import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.ast.Function

class SymbolTable private constructor(val codeMap: CodeMap) {
    data class BlockScope(var variables: MutableMap<String, Variable> = mutableMapOf(), var parent: Node.Id? = null)
    data class Variable(val type: Type)

    private val scopes = mutableMapOf<Node.Id, BlockScope>()
    private val symbols = mutableMapOf<String, Symbol>()

    fun registerSymbol(name: String, symbol: Node<Symbol>) {
        if (name in symbols) {
            Driver.errorAt("cannot redeclare `$name`", symbol.span, codeMap)

            return
        }

        symbols[name] = symbol.value
    }

    fun registerScope(scope: Node.Id) {
        if (scope in scopes) {
            error("Scope $scope already exists")
        }

        scopes[scope] = BlockScope()
    }

    fun registerVariable(scope: Node.Id, binding: Node<Binding>) {
        val variables = (scopes[scope] ?: error("Unregistered scope $scope")).variables

        if (binding.value.name.value in variables) {
            Driver.errorAt("cannot redeclare `${binding.value.name.value}`", binding.span, codeMap)

            return
        }

        variables[binding.value.name.value] = Variable(binding.value.type)
    }

    fun lookupSymbol(name: String): Symbol? = symbols[name]

    fun lookupFunction(name: String): Signature? {
        val symbol = symbols[name]

        if (symbol != null && symbol is Function) {
            return symbol.signature
        }

        if (symbol != null && symbol is NativeFunction) {
            return symbol.signature
        }

        return null
    }

    fun resolveVariable(scope: Node.Id, name: String): Variable? {
        // First, look in the current block and its parents
        var s = scope

        while (true) {
            val variable = scopes[s]?.variables?.get(name)
            if (variable != null) {
                return variable
            }

            s = parentScope(s) ?: break
        }

        // Look up in static/const symbols
        val symbol = lookupSymbol(name)

        if (symbol != null && symbol is Static) {
            return Variable(symbol.binding.value.type)
        }

        if (symbol != null && symbol is Constant) {
            return Variable(symbol.binding.value.type)
        }

        return null
    }

    fun setParentScope(scope: Node.Id, parent: Node.Id) {
        (scopes[scope] ?: error("Unregistered scope $scope")).parent = parent
    }

    private fun parentScope(scope: Node.Id): Node.Id? = scopes[scope]?.parent

    companion object {
        fun build(program: Program): SymbolTable {
            val table = SymbolTable(program.codeMap)

            // Register all native methods
            NativeGenerator.methods.forEach {
                val name = it.key
                val function = it.value

                table.registerSymbol(name, Node.dummy(NativeFunction(name, function.arguments, function.returnType)))
            }

            // Build symbol table and scope table
            SymbolTableBuilder.run(program, table)
            ScopeTableBuilder(program, table).run()

            Driver.abortOnErrors()

            return table
        }
    }
}