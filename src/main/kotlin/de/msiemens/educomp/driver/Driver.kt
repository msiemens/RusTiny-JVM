package de.msiemens.educomp.driver

import de.msiemens.educomp.CompilationFailure
import de.msiemens.educomp.back.ClassGenerator
import de.msiemens.educomp.front.CodeMap
import de.msiemens.educomp.front.ast.Node
import de.msiemens.educomp.front.parser.Lexer
import de.msiemens.educomp.front.parser.Parser
import de.msiemens.educomp.front.semck.SemanticCheck
import de.msiemens.educomp.front.symbols.SymbolTable
import de.msiemens.educomp.front.typeck.TypeCheck
import java.nio.file.Path
import kotlin.system.exitProcess

object Driver {
    private val errors: MutableList<CompilationFailure.Error> = mutableListOf()

    fun compile(source: String, file: Path) {
        val className = className(file)
        val output = file.parent.resolve("$className.class").toFile()

        val code = compile(source, className)

        output.writeBytes(code)
    }

    fun run(source: String, file: Path) {
        val className = className(file)
        val code = compile(source, className)

        val loader = object : ClassLoader() {
            fun define(className: String, bytecode: ByteArray): Class<*> {
                return super.defineClass(className, bytecode, 0, bytecode.size)
            }
        }

        val clazz = loader.define(className, code)

        val method = clazz.getMethod("main", Array<String>::class.java)
        method.invoke(null, arrayOf<String>() as Any)
    }

    private fun compile(source: String, className: String): ByteArray {
        errors.clear()

        val codeMap = CodeMap()
        val lexer = Lexer(source, codeMap)
        val parser = Parser(lexer, codeMap)

        val program = parser.parse()

        SemanticCheck.run(program)

        val symbolTable = SymbolTable.build(program)

        val typeTable = TypeCheck(program, symbolTable).run()

        return ClassGenerator(program, typeTable, className).run()
    }

    internal fun errorAt(msg: String, span: Node.Span, codeMap: CodeMap) = errorAt(
        msg,
        codeMap.resolve(span.pos)
    )

    internal fun errorAt(msg: String, location: CodeMap.Location) {
        println("Error in line ${location.line}:${location.column}: $msg")

        errors += CompilationFailure.Error(msg, location)
    }

    internal fun error(msg: String) {
        println("Error: $msg")

        errors += CompilationFailure.Error(msg)
    }

    internal fun abort(): Nothing {
        throw CompilationFailure(errors)
    }

    internal fun abortOnErrors() {
        if (errors.isNotEmpty()) {
            abort()
        }
    }

    private fun className(file: Path) = file.toFile()
        .nameWithoutExtension
        .split('-')
        .joinToString("") { it.capitalize() }
}