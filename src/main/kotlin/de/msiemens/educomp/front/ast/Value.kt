package de.msiemens.educomp.front.ast

sealed class Value(val type: Type)
data class BoolVal(val value: Boolean) : Value(Type.Bool)
data class IntVal(val value: Int) : Value(Type.Int)
data class CharVal(val value: Char) : Value(Type.Char)
data class StringVal(val value: String) : Value(Type.Str)