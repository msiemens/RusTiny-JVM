package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.Expression
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.Type
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object NativeGenerator {
    data class NativeMethod(
        val arguments: List<Type>,
        val returnType: Type,
        val implementation: (CodeGenerator, MethodVisitor, List<Node<Expression>>) -> Unit
    )

    val methods: Map<String, NativeMethod> = mapOf(
        "println" to NativeMethod(listOf(Type.Int), Type.Unit, this::println),
        "pow" to NativeMethod(listOf(Type.Int, Type.Int), Type.Int, this::pow)
    )

    fun println(codegen: CodeGenerator, writer: MethodVisitor, arguments: List<Node<Expression>>) {
        check(arguments.size == 1) { "Argument count mismatch for println" }

        writer.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")

        arguments.forEach {
            codegen.genExpression(it)
        }

        // TODO: Generate descriptor based on argument types

        writer.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false)
    }

    fun pow(codegen: CodeGenerator, writer: MethodVisitor, arguments: List<Node<Expression>>) {
        check(arguments.size == 2) { "Argument count mismatch for pow" }

        arguments.forEach {
            codegen.genExpression(it)
            writer.visitInsn(Opcodes.I2D)
        }

        codegen.variables.stack.forget(arguments.size)

        writer.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false)

        writer.visitInsn(Opcodes.D2I)

        codegen.variables.stack.push(Type.Int)
    }
}
