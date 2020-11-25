package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class InfixExpressionGenerator(
    private val variables: VariablesStack,
    private val codegen: CodeGenerator,
    private val writer: MethodVisitor
) {
    private val conditional = ConditionalGenerator(variables, codegen, writer)

    fun generate(op: BinaryOp, left: Node<Expression>, right: Node<Expression>) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/BinaryExpressionHelper.java#L134-L329

        when (op) {
            // Arithmetic
            BinaryOp.Add -> genArithmetic(Opcodes.IADD, left, right)
            BinaryOp.Sub -> genArithmetic(Opcodes.ISUB, left, right)
            BinaryOp.Mul -> genArithmetic(Opcodes.IMUL, left, right)
            BinaryOp.Div -> genArithmetic(Opcodes.IDIV, left, right)
            BinaryOp.Mod -> genArithmetic(Opcodes.IREM, left, right)
            BinaryOp.Pow -> NativeGenerator.pow(codegen, writer, listOf(left, right))
            BinaryOp.Shl -> genArithmetic(Opcodes.ISHL, left, right)
            BinaryOp.Shr -> genArithmetic(Opcodes.ISHR, left, right)

            // Bitwise
            BinaryOp.BitXor -> genArithmetic(Opcodes.IXOR, left, right)
            BinaryOp.BitAnd -> genArithmetic(Opcodes.IAND, left, right)
            BinaryOp.BitOr -> genArithmetic(Opcodes.IOR, left, right)

            // Logic
            BinaryOp.And -> genLogic(Opcodes.IFEQ, left, right)
            BinaryOp.Or -> genLogic(Opcodes.IFNE, left, right)

            // Comparisons
            BinaryOp.Eq, BinaryOp.Lt, BinaryOp.Le, BinaryOp.Ne, BinaryOp.Ge, BinaryOp.Gt -> {
                conditional.generateComparison(
                    op = op,
                    left = left,
                    right = right,
                    conseq = { variables.stack.push(BoolVal(true)) },
                    altern = { variables.stack.push(BoolVal(false)) }
                )

                variables.stack.push(Type.Bool)
            }
        }
    }

    private fun genArithmetic(opcode: Int, left: Node<Expression>, right: Node<Expression>) {
        codegen.genExpression(left)
        codegen.genExpression(right)

        check(variables.stack.size >= 2) { "Arithmetic operation needs 2 arguments" }

        writer.visitInsn(opcode)

        variables.stack.forget()
    }

    private fun genLogic(opcode: Int, left: Node<Expression>, right: Node<Expression>) {
        val isTrue = Label()
        val isFalse = Label()
        val end = Label()

        // Evaluate left hand side
        codegen.genExpression(left)
        writer.visitJumpInsn(opcode, isTrue)
        variables.stack.forget()

        // Evaluate right hand side
        codegen.genExpression(right)
        writer.visitJumpInsn(Opcodes.IFEQ, isFalse)
        variables.stack.forget()

        writer.visitLabel(isTrue)
        variables.stack.push(BoolVal(true))
        writer.visitJumpInsn(Opcodes.GOTO, end)

        writer.visitLabel(isFalse)
        variables.stack.push(BoolVal(false))

        variables.stack.push(Type.Bool)

        writer.visitLabel(end)
    }

//    private fun genComparison(opcode: Int, left: Node<Expression>, right: Node<Expression>) {
//        conditional.generateComparison()
//
//        codegen.genExpression(left)
//        codegen.genExpression(right)
//
//        val isTrue = Label()
//        val end = Label()
//
//        writer.visitJumpInsn(opcode, isTrue)
//        variables.stack.forget(2)
//
//        variables.stack.push(BoolVal(false))
//        writer.visitJumpInsn(Opcodes.GOTO, end)
//
//        writer.visitLabel(isTrue)
//        variables.stack.push(BoolVal(true))
//
//        variables.stack.push(Type.Bool)
//
//        writer.visitLabel(end)
//    }
}