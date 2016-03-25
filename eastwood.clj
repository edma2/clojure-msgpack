(disable-warning
 {:linter :constant-test
  :if-inside-macroexpansion-of #{'msgpack.macros/extend-msgpack}
  :within-depth 4
  :reason "The `extend-msgpack` macro verifies the extension type is within bounds after macroexpansion."})
