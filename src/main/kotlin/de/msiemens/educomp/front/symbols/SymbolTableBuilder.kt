package de.msiemens.educomp.front.symbols

import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.Program
import de.msiemens.educomp.front.ast.Symbol
import de.msiemens.educomp.front.ast.Visitor

object SymbolTableBuilder {
    fun run(program: Program, table: SymbolTable) {
        object : Visitor() {
            override fun visitSymbol(symbol: Node<Symbol>) {
                table.registerSymbol(symbol.value.name(), symbol)
            }
        }.visit(program)
    }
}