fn foo(a: int) {
    let a = false;  //! ERROR(2:9): cannot redeclare `a`
}

fn main() {}