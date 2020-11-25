# RusTiny-JVM

This is an educational compiler for a Rust-like language that runs
on the JVM. It's based [RusTiny](https://github.com/msiemens/rustiny),
a project that attempts to compile a language like this to x86-64
assembly.

The syntax is based on Rust, but in order to keep the scope managable
there are a log of limitations:

- Only `bool`, `char` and `int` data types are supported (although
  `strings` may be implemented later).
- No structs/classes, no modules, only functions.
- No `mut`, no borrow checker.