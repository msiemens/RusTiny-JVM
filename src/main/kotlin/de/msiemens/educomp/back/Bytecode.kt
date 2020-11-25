package de.msiemens.educomp.back

import de.msiemens.educomp.front.Signature
import de.msiemens.educomp.front.ast.Type
import org.objectweb.asm.Opcodes

object Bytecode {
    fun store(type: Type): Int = when (type) {
        Type.Bool -> Opcodes.ISTORE
        Type.Int -> Opcodes.ISTORE
        Type.Char -> Opcodes.ISTORE
        Type.Str -> Opcodes.ASTORE
        Type.Unit -> error("Cannot store () type")
        else -> error("Invalid JVM type $type")
    }

    fun load(type: Type): Int = when (type) {
        Type.Bool -> Opcodes.ILOAD
        Type.Int -> Opcodes.ILOAD
        Type.Char -> Opcodes.ILOAD
        Type.Str -> Opcodes.ALOAD
        Type.Unit -> error("Cannot load () type")
        else -> error("Invalid JVM type $type")
    }

    fun ret(type: Type): Int = when (type) {
        Type.Bool -> Opcodes.IRETURN
        Type.Int -> Opcodes.IRETURN
        Type.Char -> Opcodes.IRETURN
        Type.Str -> Opcodes.ARETURN
        Type.Unit -> error("Cannot return () type")
        else -> error("Invalid JVM type $type")
    }

    fun descriptor(signature: Signature): String {
        return descriptor(signature.first, signature.second)
    }

    fun descriptor(types: List<Type> = emptyList(), returnType: Type = Type.Unit): String {
        val args = types.joinToString("") { type(it) }
        val ret = type(returnType)

        return "($args)$ret"
    }

    fun type(type: Type): String = when (type) {
        Type.Bool -> "Z"
        Type.Int -> "I"
        Type.Char -> "C"
        Type.Str -> "Ljava/lang/String;"
        Type.Unit -> "V"
        else -> error("Invalid JVM type $type")
    }
}