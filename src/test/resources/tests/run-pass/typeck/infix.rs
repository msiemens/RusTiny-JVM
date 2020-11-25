fn main() {
    let a: int = 2 + 2;
    let b: bool = true && false;
    let c: int = 2 | 1;
    let d: bool = true | true;
    let e: bool = 2 <= 5;

    if b {
        println(a);
    }

    if d {
        println(c);
    }

    if e {
        println(a);
        println(c);
    }
}