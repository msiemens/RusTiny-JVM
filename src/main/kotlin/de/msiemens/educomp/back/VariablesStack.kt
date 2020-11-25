package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.Function
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.Type
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// https://github.com/apache/groovy/blob/GROOVY_2_4_15/src/main/org/codehaus/groovy/classgen/asm/CompileStack.java

class VariablesStack(
    function: Function? = null,
    static: Boolean = false,
    private val writer: MethodVisitor
) {
    data class Variable(
        val index: Int,
        val name: String,
        val type: Type,
    )

    data class Scope(
        val scope: Node.Id,
        val onStack: MutableMap<String, Variable>,
        val `break`: Label?,
    )

    var stack = FrameStack(writer)
        private set

    var `break`: Label? = null
        private set

    private var scope: Node.Id = Node.Id.EMPTY

    private var currentVarIndex: Int = if (static) 0 else 1

    private val used = mutableListOf<Variable>()
    private var onStack = function?.bindings
        ?.map { it.value.name.value to it.value.type }
        ?.map { it.first to define(it.first, it.second) }
        ?.toMap()
        ?.toMutableMap() ?: mutableMapOf()

    private val states = mutableListOf<Scope>()

    fun push(id: Node.Id) {
        states += Scope(id, onStack, `break`)
    }

    fun pop() {
        val state = states.removeLast()

        scope = state.scope
        onStack = state.onStack
        `break` = state.`break`
    }

    fun defineVariable(name: String, type: Type, initFromStack: Boolean) {
        if (!initFromStack) {
            when (type) {
                Type.Bool, Type.Int, Type.Char -> writer.visitLdcInsn(0)
                Type.Str -> writer.visitInsn(Opcodes.ACONST_NULL)
                else -> error("Invalid variable type $type")
            }

            stack.push(type)
        }

        val variable = define(name, type)
        onStack[name] = variable

        stack.store(variable)
    }

    operator fun get(name: String): Variable? = onStack[name]

    private fun define(name: String, type: Type): Variable {
        val index = currentVarIndex
        currentVarIndex++

        val variable = Variable(index, name, type)
        used += variable

        return variable
    }

    fun loop(): Label {
        val label = Label()
        `break` = label

        return label
    }
}
