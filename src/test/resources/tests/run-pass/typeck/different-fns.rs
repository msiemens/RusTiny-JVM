fn foo() {
    let a = 2;
    a += 4;

    println(a);
}

fn bar() {
    let a = false;
    a |= true;

    if a {
        println(2);
    }
}

fn main() {
    foo();
    bar();
}