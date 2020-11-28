fn foo() -> int {
    let a = 2;

    let b = if a + 3 == 7 {
        a
    } else {
        2
    };

    return b;
}


fn main() {
    let a = foo();

    println(a);
}