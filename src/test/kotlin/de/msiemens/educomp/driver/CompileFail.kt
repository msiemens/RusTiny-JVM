package de.msiemens.educomp.driver

import de.msiemens.educomp.CompilationFailure
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class CompileFail : BaseTest() {
    private val errorRe = Regex(".*//! ERROR: (?<error>.*)")
    private val errorWithLineRe = Regex(".*//! ERROR\\((?<line>\\d+):(?<col>\\d+)\\): ?(?<error>.*)")

    private data class ExpectedError(val message: String, val line: Int? = null, val column: Int? = null)
    private data class TestCase(
        val file: Path,
        val source: String,
        val errors: List<ExpectedError>,
        val category: String
    )

    @TestFactory
    fun testCompileFail(): Stream<DynamicTest> = readCategories("compile-fail")
        .flatMap(this::readTestCases)
        .map { DynamicTest.dynamicTest("${it.category} > ${it.file.toFile().name}", runner(it)) }
        .stream()

    private fun runner(case: TestCase): () -> Unit = {
        checkSkip(case.source)

        val expectedErrors = parseExpectedErrors(case.source)

        val ex = assertThrows<CompilationFailure> {
            Driver.run(case.source, case.file)
        }

        assertEquals(expectedErrors.size, ex.errors.size)

        expectedErrors.zip(ex.errors).forEach {
            assertEquals(it.first.message, it.second.message)
            assertEquals(it.first.line, it.second.location?.line)
            assertEquals(it.first.column, it.second.location?.column)
        }
    }

    private fun readTestCases(dir: File): List<TestCase> = dir.listFiles()!!
        .filter { it.extension == "rs" }
        .map { it to it.readText() }
        .map { (file, source) -> TestCase(file.toPath(), source, parseExpectedErrors(source), dir.name) }

    private fun parseExpectedErrors(source: String): List<ExpectedError> = source
        .lineSequence()
        .map {
            val simple = errorRe.matchEntire(it)?.groups
            val withLine = errorWithLineRe.matchEntire(it)?.groups

            when {
                simple != null -> {
                    ExpectedError(simple["error"]!!.value)
                }
                withLine != null -> {
                    ExpectedError(
                        withLine["error"]!!.value,
                        withLine["line"]!!.value.toInt(),
                        withLine["col"]!!.value.toInt()
                    )
                }
                else -> {
                    null
                }
            }
        }
        .filterNotNull()
        .toList()
}