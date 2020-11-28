fn main() {
    let a = if 2 == 0 {
        0
    } else {};  //! ERROR(4:13): type mismatch: expected int, got ()
}