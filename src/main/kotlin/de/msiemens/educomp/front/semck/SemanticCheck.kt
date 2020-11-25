package de.msiemens.educomp.front.semck

import de.msiemens.educomp.front.ast.Program

object SemanticCheck {
    fun run(program: Program) {
        MainPresenceCheck.run(program)
        LValueCheck.run(program)
        BreakVerifier.run(program)
    }
}