package de.msiemens.educomp.front.semck

import de.msiemens.educomp.driver.Driver
import de.msiemens.educomp.front.ast.*

object LValueCheck {
    fun run(program: Program) {
        object : Visitor() {
            override fun visitExpression(expression: Node<Expression>) {
                when (expression.value) {
                    is AssignExpression -> checkExpression(expression.value.left)
                    is AssignOpExpression -> checkExpression(expression.value.left)
                    else -> {}
                }

                super.visitExpression(expression)
            }

            fun checkExpression(expression: Node<Expression>) {
                if (expression.value !is VariableExpression) {
                    Driver.errorAt("left-hand side of assignment is not a variable", expression.span, program.codeMap)
                }
            }
        }.visit(program)
    }
}
