fn main() {
    let a: int = 4;

    {
        let a: int = 2;

        a -= 2;

        println(a);
    }

    println(a);
}