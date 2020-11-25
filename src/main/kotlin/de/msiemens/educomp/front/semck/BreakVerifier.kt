package de.msiemens.educomp.front.semck

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.ast.*

object BreakVerifier {
    fun run(program: Program) {
        var whileDepth = 0

        object : Visitor() {
            override fun visitExpression(expression: Node<Expression>) {
                when (expression.value) {
                    is WhileExpression -> {
                        whileDepth++

                        super.visitExpression(expression)

                        whileDepth--
                    }
                    is BreakExpression -> if (whileDepth == 0) {
                        Driver.errorAt("`break` outside of loop", expression.span, program.codeMap)
                    } else {
                        super.visitExpression(expression)
                    }
                    else -> super.visitExpression(expression)
                }
            }
        }.visit(program)
    }
}
