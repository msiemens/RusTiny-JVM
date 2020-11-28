fn main() {
    let a = 2;

    if 2 == 0 {
        a = false;  //! ERROR(5:13): type mismatch: expected int, got bool
    };
}