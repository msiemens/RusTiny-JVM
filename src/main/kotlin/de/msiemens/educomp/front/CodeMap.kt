package de.msiemens.educomp.front

class CodeMap {
    private val lines = mutableListOf(0)

    data class Location(val line: Int, val column: Int)

    fun newLine(pos: Int) {
        assert(lines.size == 0 || lines.last() < pos)

        lines.add(pos)
    }

    fun resolve(pos: Int): Location {
        var pos = pos
        var lower = 0
        var upper = lines.size

        while (upper - lower > 1) {
            val mid = (lower + upper) / 2
            val offset = lines[mid]
            if (offset > pos) {
                upper = mid
            } else {
                lower = mid
            }
        }

        val line = lower
        val offset = lines[line]

        if (line == 0) {
            pos++
        }

        assert(pos >= offset)

        return Location(line + 1, pos - offset)
    }
}