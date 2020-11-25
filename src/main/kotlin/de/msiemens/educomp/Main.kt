package de.msiemens.educomp

import de.msiemens.educomp.driver.Driver
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val file = Path.of(args[0])
    val source = Files.readString(file)

    Driver.compile(source, file)
}