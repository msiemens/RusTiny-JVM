fn main() {
    let a = false;

    a += 2;  //! ERROR(4:5): type mismatch: expected int, got bool
}