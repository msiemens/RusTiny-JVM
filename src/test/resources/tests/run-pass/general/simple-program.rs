const CONST: int = 0;
static STATIC: int = 1;

fn mul(a: int, b: int) -> int {
    let i: int = 0;

    while i < 10 {
        b += if b > 0 { b * 4 } else { -8 };
        a -= i * (2 + 2);

        i += STATIC;
    }

    return i;
}

fn main() {
    println(mul(3, 5));
}
