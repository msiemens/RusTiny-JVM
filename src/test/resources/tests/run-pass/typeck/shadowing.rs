fn main() {
    let a: bool = true;
    if a {
        let a: int = 3;
        a += 2;

        println(a);
    }

    a = false;

    if a {
        println(1);
    }
}