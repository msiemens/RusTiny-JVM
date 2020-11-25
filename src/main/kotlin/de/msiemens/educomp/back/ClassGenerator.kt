package de.msiemens.educomp.back

import de.msiemens.educomp.front.Signature
import de.msiemens.educomp.front.ast.*
import de.msiemens.educomp.front.ast.Function
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter
import java.io.PrintWriter

class ClassGenerator(
    private val program: Program,
    private val types: Map<Node.Id, Type>,
    private val clazz: String
) {
    private val constants: MutableMap<String, Value> = mutableMapOf()

    fun run(): ByteArray {
        val impls: MutableList<Function> = mutableListOf()
        val functions: MutableMap<String, Signature> = mutableMapOf()
        val staticFields: MutableMap<String, Pair<Type, Node<Expression>>> = mutableMapOf()

        program.symbols.forEach { node ->
            when (val symbol = node.value) {
                is Static -> {
                    staticFields[symbol.name()] = symbol.binding.value.type to symbol.value
                }
                is Constant -> {
                    val value = if (symbol.value.value is LiteralExpression) {
                        symbol.value.value.value
                    } else {
                        error("$node is not constant")
                    }

                    constants[symbol.name()] = value
                }
                is Function -> {
                    functions[symbol.name()] = symbol.bindings.map { it.value.type } to symbol.returnType
                    impls += symbol
                }
                is NativeFunction -> functions[symbol.name()] = symbol.arguments to symbol.returnType
            }
        }

        // Generate the class
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        writer.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            clazz,
            null,
            "java/lang/Object",
            null
        )

        // Generate static fields
        genStaticFields(staticFields, writer)
        genConstructor(writer)

        // Generate all functions
        impls.forEach {
            genFunction(it.name(), it, staticFields, functions, writer)
        }

        // Generate main function
        genMain(writer)

        writer.visitEnd()

        val code = writer.toByteArray()
        verify(code)

        return code
    }

    private fun verify(code: ByteArray) {
        CheckClassAdapter.verify(ClassReader(code), true, PrintWriter(System.err, true))
    }

    private fun genConstructor(writer: ClassWriter) {
        val method = writer.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            Bytecode.descriptor(),
            null,
            null
        )

        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            Bytecode.descriptor(),
            false
        )
        method.visitInsn(Opcodes.RETURN)

        method.visitMaxs(0, 0)

        method.visitEnd()
    }

    private fun genMain(writer: ClassWriter) {
        val method = writer.visitMethod(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
            "main",
            "([Ljava/lang/String;)V",
            null,
            null
        )

        method.visitCode()

        method.visitTypeInsn(Opcodes.NEW, clazz)
        method.visitInsn(Opcodes.DUP)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", Bytecode.descriptor(), false)
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "main", Bytecode.descriptor(), false)
        method.visitInsn(Opcodes.RETURN)

        method.visitMaxs(0, 0)

        method.visitEnd()
    }

    private fun genStaticFields(
        fields: Map<String, Pair<Type, Node<Expression>>>,
        writer: ClassWriter
    ) {
        fields.entries.forEach {
            writer.visitField(Opcodes.ACC_STATIC, it.key, Bytecode.type(it.value.first), null, null)
        }

        val method = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", Bytecode.descriptor(), null, null)

        val variables = VariablesStack(static = true, writer = method)

        method.visitCode()

        fields.entries.forEach {
            CodeGenerator(
                clazz = clazz,
                functions = emptyMap(),
                fields = fields,
                constants = constants,
                variables = variables,
                types = types,
                returnType = it.value.first,
                writer = method
            ).genExpression(it.value.second)

            method.visitFieldInsn(Opcodes.PUTSTATIC, clazz, it.key, Bytecode.type(it.value.first))
        }

        method.visitInsn(Opcodes.RETURN)

        method.visitMaxs(0, 0)

        method.visitEnd()
    }

    private fun genFunction(
        name: String,
        function: Function,
        fields: Map<String, Pair<Type, Node<Expression>>>,
        functions: Map<String, Signature>,
        writer: ClassWriter
    ) {
        val method = writer.visitMethod(
            0,
            name,
            Bytecode.descriptor(function.signature),
            null,
            null
        )

        val variables = VariablesStack(function, writer = method)

        method.visitCode()

        val generator = CodeGenerator(
            clazz = clazz,
            functions = functions,
            fields = fields,
            constants = constants,
            variables = variables,
            types = types,
            returnType = function.returnType,
            writer = method
        )
        generator.generate(function.body)

        // No explicit return, we need to add the final return operation
        if (function.body.value.expression.value != UnitExpression) {
            method.visitInsn(Bytecode.ret(function.returnType))
        } else {
            method.visitInsn(Opcodes.RETURN)
        }

        method.visitMaxs(0, 0)

        method.visitEnd()
    }
}
