package de.msiemens.educomp.back

import de.msiemens.educomp.front.Signature
import de.msiemens.educomp.front.ast.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class CodeGenerator(
    private val clazz: String,
    private val functions: Map<String, Signature>,
    private val fields: Map<String, Pair<Type, Node<Expression>>>,
    private val constants: Map<String, Value>,
    internal val variables: VariablesStack,
    private val types: Map<Node.Id, Type>,
    private val returnType: Type,
    private val writer: MethodVisitor
) {
    private val infix = InfixExpressionGenerator(variables, this, writer)
    private val prefix = PrefixExpressionGenerator(this, writer)
    private val conditional = ConditionalGenerator(variables, this, writer)

    fun generate(block: Node<Block>) {
        variables.push(block.id)

        block.value.statements.forEach {
            when (it.value) {
                is DeclarationStatement -> genDeclaration(it.value)
                is ExpressionStatement -> {
                    val mark = variables.stack.size

                    genExpression(it.value.expression)

                    variables.stack.popDownTo(mark)
                }
            }
        }

        genExpression(block.value.expression)

        variables.pop()
    }

    private fun genDeclaration(declaration: DeclarationStatement) {
        genExpression(declaration.value)

        val binding = declaration.binding.value

        variables.defineVariable(binding.name.value, binding.type, initFromStack = true)
    }

    internal fun genExpression(expression: Node<Expression>) {
        when (val expr = expression.value) {
            is BlockExpression -> generate(expr.block)
            is LiteralExpression -> variables.stack.push(expr.value, expr.value.type)
            is VariableExpression -> genVariable(expr)
            is AssignExpression -> genAssign(expr.left, expr.right)
            is AssignOpExpression -> genAssignOp(expr.op, expr.left, expr.right)
            is ReturnExpression -> genReturn(expr.value)
            is CallExpression -> genCall(expr.function, expr.arguments)
            is GroupExpression -> genExpression(expr.expression)
            is InfixExpression -> infix.generate(expr.op, expr.left, expr.right)
            is PrefixExpression -> prefix.generate(expr.op, expr.expression)
            is IfExpression -> genIf(expr.condition, expr.consequence, expr.alternative)
            is WhileExpression -> genWhile(expr.condition, expr.body)
            BreakExpression -> genBreak()
            UnitExpression -> return
            else -> TODO()
        }
    }

    private fun genVariable(value: VariableExpression) {
        val name = value.name.value

        val variable = variables[name]
        if (variable != null) {
            return variables.stack.load(variable)
        }

        // Try resolving static variable
        val field = fields[name]
        if (field != null) {
            writer.visitFieldInsn(Opcodes.GETSTATIC, clazz, name, Bytecode.type(field.first))
            variables.stack.push(field.first)

            return
        }

        // Try resolving constant
        val constant = constants[name]
        if (constant != null) {
            return variables.stack.push(constant, constant.type)
        }

        error("Undefined variable $name")
    }

    private fun genAssign(left: Node<Expression>, right: Node<Expression>) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/BinaryExpressionHelper.java#L366-L488
        genExpression(right)

        store(left)
    }

    private fun genAssignOp(op: BinaryOp, left: Node<Expression>, right: Node<Expression>) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/BinaryExpressionHelper.java#L641-L662
        infix.generate(op, left, right)

        store(left)
    }

    private fun store(lhs: Node<Expression>) {
        val name = if (lhs.value is VariableExpression) {
            lhs.value.name.value
        } else {
            error("Assignment to a non-variable value")
        }
        val variable = variables[name]
        if (variable != null) {
            variables.stack.store(variable)

            return
        }

        val static = fields[name]
        if (static != null) {
            writer.visitFieldInsn(Opcodes.PUTSTATIC, clazz, name, Bytecode.type(static.first))
            variables.stack.forget()

            return
        }

        error("Variable $name not defined")
    }

    private fun genReturn(value: Node<Expression>) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/StatementWriter.java#L602-L634
        if (value.value is UnitExpression) {
            return writer.visitInsn(Opcodes.RETURN)
        }

        genExpression(value)

        writer.visitInsn(Bytecode.ret(returnType))

        variables.stack.pop()
    }

    private fun genCall(callee: Node<Expression>, arguments: List<Node<Expression>>) {
        // https://github.com/apache/groovy/blob/GROOVY_2_4_15/src/main/org/codehaus/groovy/classgen/asm/InvocationWriter.java#L482-L499

        // Resolve function name
        val name = if (callee.value is VariableExpression) {
            callee.value.name.value
        } else {
            error("Calling a non-function variable")
        }

        val native = NativeGenerator.methods[name]
        if (native != null) {
            native.implementation(this, writer, arguments)

            variables.stack.forget(arguments.size)

            return
        }

        writer.visitVarInsn(Opcodes.ALOAD, 0)

        arguments.forEach {
            genExpression(it)
        }

        val function = functions[name] ?: error("Function $name is not defined")
        val declaration = Bytecode.descriptor(function)

        writer.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, name, declaration, false)

        variables.stack.forget(arguments.size)

        val returnType = function.second

        if (returnType != Type.Unit) {
            variables.stack.push(returnType)
        }
    }

    private fun genIf(condition: Node<Expression>, consequence: Node<Block>, alternative: Node<Block>?) {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/StatementWriter.java#L305-L333
        val conseq = Label()
        val end = Label()

        if (condition.value is InfixExpression && condition.value.op.type() == BinaryOp.Type.Comparison) {
            conditional.generateComparison(
                condition.value.op,
                condition.value.left,
                condition.value.right,
                conseq = { generate(consequence) },
                altern = { alternative?.let { generate(it) } }
            )
        } else {
            genExpression(condition)

            writer.visitJumpInsn(Opcodes.IFNE, conseq)
            variables.stack.forget()

            if (alternative != null) {
                generate(alternative)

                writer.visitJumpInsn(Opcodes.GOTO, end)
            } else {
                writer.visitJumpInsn(Opcodes.GOTO, end)
            }

            writer.visitLabel(conseq)
            generate(consequence)
        }

        val targetType = types[consequence.id] ?: error("No type for node ${consequence.id}")
        if (targetType != Type.Unit) {
            check(alternative != null) { "`if` block with target type without `else`" }

            // The target type is stored on the stack twice, once for the consequence
            // and once for the alternative. As they should have the same type we
            // can safely drop one of them to restore the proper stack size.
            variables.stack.forget()
        }

        writer.visitLabel(end)
    }

    private fun genWhile(condition: Node<Expression>, body: Node<Block>) {
        val mark = variables.stack.size

        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/StatementWriter.java#L263-L282
        val continueLabel = Label()
        val breakLabel = variables.loop()

        writer.visitLabel(continueLabel)

        genExpression(condition)
        writer.visitJumpInsn(Opcodes.IFEQ, breakLabel)
        variables.stack.forget()

        generate(body)

        writer.visitJumpInsn(Opcodes.GOTO, continueLabel)

        writer.visitLabel(breakLabel)

        check(mark == variables.stack.size) { "Dropped to many values during `if` generation" }
    }

    private fun genBreak() {
        // https://github.com/apache/groovy/blob/12dca398e711ce6c703998b46c928f6bc87f0018/src/main/java/org/codehaus/groovy/classgen/asm/StatementWriter.java#L513-L522

        writer.visitJumpInsn(Opcodes.GOTO, variables.`break`!!)
    }
}