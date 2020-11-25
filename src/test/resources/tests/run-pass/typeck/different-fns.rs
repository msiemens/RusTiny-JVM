fn foo() {
    let a: int = 2;
    a += 4;

    println(a);
}

fn bar() {
    let a: bool = false;
    a |= true;

    if a {
        println(2);
    }
}

fn main() {
    foo();
    bar();
}