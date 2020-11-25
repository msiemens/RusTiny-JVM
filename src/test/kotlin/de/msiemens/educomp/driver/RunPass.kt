package de.msiemens.educomp.driver

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class RunPass : BaseTest() {
    private data class TestCase(val file: Path, val source: String, val expected: String, val category: String)

    @TestFactory
    fun test(): Stream<DynamicTest> = readCategories("run-pass")
        .flatMap(this::readTestCases)
        .map {
            DynamicTest.dynamicTest("${it.category} > ${it.file.toFile().name}", runner(it))
        }
        .stream()

    private fun runner(case: TestCase): () -> Unit = {
        checkSkip(case.source)

        val standardOut = System.out

        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))

        Driver.run(case.source, case.file)

        assertEquals(case.expected.trim(), out.toString().trim())

        System.setOut(standardOut)
    }

    private fun readTestCases(dir: File) = dir.listFiles()!!
        .filter { it.extension == "rs" }
        .map { source ->
            val path = Path.of(source.toURI())
            val expected = source.name.replace(".rs", ".out")

            TestCase(source.toPath(), source.readText(), path.parent.resolve(expected).toFile().readText(), dir.name)
        }
}