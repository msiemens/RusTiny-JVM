fn main() {
    let a = true;
    if a {
        let a = 3;
        a += 2;

        println(a);
    }

    a = false;

    if a {
        println(1);
    }
}