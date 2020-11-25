package de.msiemens.educomp.front.parser.parselet

enum class Associativity(val precedence: Int) {
    Left(0), Right(1)
}

enum class Precedence(val value: Int) {
    Call(13),

    //Prefix     (12),
    Exponent(11),
    Product(10),
    Sum(9),
    Shift(8),
    BitAnd(7),
    BitXor(6),
    BitOr(5),
    Compare(4),
    And(3),
    Or(2),
    Assignment(1),
}