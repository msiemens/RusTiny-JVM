fn foo(a: int, b: bool, c: char) -> char {
    return if a == 2 || b {
        c
    } else {
        'd'
    }
}

fn main() {
    let a = foo(1, false, 'a');

    if a == 'd' {
        println(1);
    }
}