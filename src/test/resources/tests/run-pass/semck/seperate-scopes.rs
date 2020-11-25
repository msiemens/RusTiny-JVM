fn main() {
    let foo: int = 3;
    foo += 2;

    println(foo);
    println(foo());
}

fn foo() -> int {
    12
}