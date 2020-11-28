package de.msiemens.educomp.front.ast

/**
 * A node in the AST
 *
 * Will eventually contain additional information about the node's source location
 * (span) and an unique node id.
 */
data class Node<T>(val value: T, val span: Span) {
    val id = nextId()

    data class Span(val pos: Int, val len: Int) {
        operator fun plus(rhs: Span): Span = Span(pos, rhs.pos + rhs.len - pos)

        companion object {
            val empty = Span(pos = -1, len = 0)
        }
    }

    data class Id(val id: Int) {
        companion object {
            val EMPTY = Id(Int.MAX_VALUE)
        }
    }


    companion object {
        private var nextId = 0

        fun nextId(): Id = Id(nextId++)

        fun <T> dummy(value: T): Node<T> = Node(value, Span.empty)
    }
}