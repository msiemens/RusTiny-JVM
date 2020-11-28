fn main() {
    let a = 4;
    let b = true;

    {
        let a = 2;
        let b = false;

        a -= 2;

        if !b {
            println(a);
        }
    }

    if b {
        println(a);
    }
}