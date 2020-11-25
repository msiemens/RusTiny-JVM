package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.Expression
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.ast.Type
import de.msiemens.educomp.front.ast.UnaryOp
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class PrefixExpressionGenerator(
    private val codegen: CodeGenerator,
    private val writer: MethodVisitor
) {
    fun generate(op: UnaryOp, expression: Node<Expression>) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/UnaryExpressionHelper.java#L50-L84
        when (op) {
            UnaryOp.Not -> when (val type = codegen.types[expression.id]) {
                Type.Bool -> {
                    codegen.genExpression(expression)
                    writer.visitInsn(Opcodes.ICONST_1)
                    writer.visitInsn(Opcodes.IXOR)
                }
                Type.Int -> {
                    codegen.genExpression(expression)
                    writer.visitInsn(Opcodes.ICONST_M1)
                    writer.visitInsn(Opcodes.IXOR)
                }
                else -> {
                    error("Called UnaryOp.Not with unsupported type $type")
                }
            }
            UnaryOp.Neg -> {
                codegen.genExpression(expression)
                writer.visitInsn(Opcodes.INEG)
            }
        }
    }

}
