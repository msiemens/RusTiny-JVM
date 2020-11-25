package de.msiemens.educomp.back

import de.msiemens.educomp.front.ast.Expression
import de.msiemens.educomp.front.ast.Node
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
            UnaryOp.Not -> TODO()
            UnaryOp.Neg -> {
                codegen.genExpression(expression)
                writer.visitInsn(Opcodes.INEG)
            }
        }
    }

}
