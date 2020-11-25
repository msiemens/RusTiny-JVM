package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class FrameStack(private val writer: MethodVisitor) {
    private val stack: MutableList<Type> = mutableListOf()

    val size: Int
        get() = stack.size

    fun push(value: Value, type: Type) {
        push(value)
        push(type)
    }

    fun push(value: Value) {
        when (value) {
            is BoolVal -> push(value.value)
            is IntVal -> push(value.value)
            is CharVal -> push(value.value.toInt())
            is StringVal -> writer.visitLdcInsn(value.value)
        }
    }

    private fun push(value: Int) = when (value) {
        0 -> writer.visitInsn(Opcodes.ICONST_0)
        1 -> writer.visitInsn(Opcodes.ICONST_1)
        2 -> writer.visitInsn(Opcodes.ICONST_2)
        3 -> writer.visitInsn(Opcodes.ICONST_3)
        4 -> writer.visitInsn(Opcodes.ICONST_4)
        5 -> writer.visitInsn(Opcodes.ICONST_5)
        else -> if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            writer.visitIntInsn(Opcodes.BIPUSH, value)
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            writer.visitIntInsn(Opcodes.SIPUSH, value)
        } else {
            writer.visitLdcInsn(value)
        }
    }

    private fun push(value: Boolean) {
        writer.visitInsn(if (value) Opcodes.ICONST_1 else Opcodes.ICONST_0)
    }

    fun push(type: Type) {
        stack += type
    }

    fun pop() {
        popDownTo(stack.lastIndex)
    }

    fun popDownTo(elements: Int) {
        var last = stack.size

        while (last > elements) {
            last--

            stack.removeLast()
            writer.visitInsn(Opcodes.POP)
        }
    }

    fun forget(count: Int = 1) {
        repeat(count) { stack.removeLast() }
    }

    fun load(variable: VariablesStack.Variable) {
        writer.visitVarInsn(Bytecode.load(variable.type), variable.index)
        push(variable.type)
    }

    fun store(variable: VariablesStack.Variable) {
        writer.visitVarInsn(Bytecode.store(variable.type), variable.index)
        forget()
    }
}