fn main() {
    let a = 2 + false;  //! ERROR(2:17): type mismatch: expected int, got bool
    let b = false && 2;  //! ERROR(3:22): type mismatch: expected bool, got int
    let c = 2 <= 'a';  //! ERROR(4:18): type mismatch: expected int, got char
    let d = 2 ^ false;  //! ERROR(5:17): type mismatch: expected int, got bool
    let e = false ^ 2;  //! ERROR(6:21): type mismatch: expected bool, got int
    let f = 'a' ^ 'b';  //! ERROR(7:13): binary operation `^` cannot be applied to char
}