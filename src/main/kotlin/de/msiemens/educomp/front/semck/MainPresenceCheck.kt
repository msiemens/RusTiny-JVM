package de.msiemens.educomp.front.semck

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.ast.Function
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.Program
import de.msiemens.educomp.front.ast.Symbol
import de.msiemens.educomp.front.ast.Visitor

object MainPresenceCheck {
    fun run(program: Program) {
        var found = false

        object : Visitor() {
            override fun visitSymbol(symbol: Node<Symbol>) {
                found = found || (symbol.value is Function && symbol.value.name.value == "main")
            }
        }.visit(program)

        if (!found) {
            Driver.error("main function not found")
        }
    }
}