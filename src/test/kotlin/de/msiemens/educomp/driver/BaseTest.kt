package de.msiemens.educomp.driver

import org.junit.jupiter.api.Assumptions
import java.io.File

open class BaseTest {
    protected fun readCategories(type: String): Array<File> {
        val resource = javaClass.getResource("/tests/$type")

        return File(resource.toURI()).listFiles()!!
    }

    protected fun checkSkip(source: String) {
        if (source.lineSequence().first() == ANNOTATION_SKIP) {
            Assumptions.assumeTrue(false)
        }
    }

    companion object {
        const val ANNOTATION_SKIP = "//! SKIP"
    }
}
