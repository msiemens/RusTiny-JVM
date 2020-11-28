fn main() {
    let a = false;
    let b = 5;

    a |= true;
    b &= 1;

    if a {
        println(b);
    }
}