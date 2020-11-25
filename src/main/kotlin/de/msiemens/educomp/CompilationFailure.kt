package de.msiemens.educomp

import de.msiemens.educomp.front.CodeMap

class CompilationFailure(val errors: List<Error>) : Exception() {
    data class Error(val message: String, val location: CodeMap.Location? = null)
}