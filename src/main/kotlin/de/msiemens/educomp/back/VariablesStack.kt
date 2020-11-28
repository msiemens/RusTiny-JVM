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
        val name: Pair<String, Node.Id>,
        val type: Type,
    )

    data class Scope(
        val scope: Node.Id,
        val onStack: MutableMap<Pair<String, Node.Id>, Variable>,
        val `break`: Label?,
    )

    var stack = FrameStack(writer)
        private set

    var `break`: Label? = null
        private set

    var scope: Node.Id = Node.Id.EMPTY
        private set

    private var parentScopes = mutableMapOf<Node.Id, Node.Id>()

    private var currentVarIndex: Int = if (static) 0 else 1

    private val used = mutableListOf<Variable>()
    private var onStack = mutableMapOf<Pair<String, Node.Id>, Variable>()

    private val states = mutableListOf<Scope>()

    init {
        if (function != null) {
            onStack = function.bindings
                .map { (it.value.name.value to function.body.id) to it.value.type }
                .map { it.first to define(it.first, it.second.value) }
                .toMap()
                .toMutableMap()
        }
    }

    fun push(id: Node.Id) {
        check(id != scope) { "New scope and current scope is equal" }

        parentScopes[id] = scope
        states += Scope(id, onStack, `break`)

        scope = id
    }

    fun pop() {
        states.removeLast()

        if (states.isNotEmpty()) {
            val restored = states.last()

            scope = restored.scope
            onStack = restored.onStack
            `break` = restored.`break`
        }
    }

    fun defineVariable(name: Pair<String, Node.Id>, type: Type, initFromStack: Boolean) {
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

    operator fun get(name: Pair<String, Node.Id>): Variable? {
        return get(name.first, name.second)
    }

    operator fun get(name: String, scope: Node.Id): Variable? {
        if (scope == Node.Id.EMPTY) {
            return null
        }

        val variable = onStack[name to scope]
        if (variable != null) {
            return variable
        }

        return get(name, parentScopes[scope] ?: return null)
    }

    operator fun get(name: String): Variable? = onStack[name to scope]

    private fun define(name: Pair<String, Node.Id>, type: Type): Variable {
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
