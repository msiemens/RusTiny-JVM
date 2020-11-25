package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ConditionalGenerator(
    private val variables: VariablesStack,
    private val codegen: CodeGenerator,
    private val writer: MethodVisitor
) {
    fun generateComparison(
        op: BinaryOp,
        left: Node<Expression>,
        right: Node<Expression>,
        conseq: () -> Unit,
        altern: () -> Unit
    ) {
        val opcode = when (op) {
            BinaryOp.Eq -> Opcodes.IF_ICMPEQ
            BinaryOp.Lt -> Opcodes.IF_ICMPLT
            BinaryOp.Le -> Opcodes.IF_ICMPLE
            BinaryOp.Ne -> Opcodes.IF_ICMPNE
            BinaryOp.Ge -> Opcodes.IF_ICMPGE
            BinaryOp.Gt -> Opcodes.IF_ICMPGT
            else -> error("Invalid comparison operator $op")
        }

        val isTrue = Label()
        val end = Label()

        codegen.genExpression(left)
        codegen.genExpression(right)

        writer.visitJumpInsn(opcode, isTrue)
        variables.stack.forget(2)

        altern()

        writer.visitJumpInsn(Opcodes.GOTO, end)

        writer.visitLabel(isTrue)

        conseq()

        writer.visitLabel(end)
    }
}