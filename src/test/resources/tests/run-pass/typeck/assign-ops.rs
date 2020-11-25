fn main() {
    let a: bool = false;
    let b: int = 5;

    a |= true;
    b &= 1;

    if a {
        println(b);
    }
}