// The purpose of this file is to test that chessboard2 is exported as a CommonJS module

const assert = require('assert')

const cb2 = require('./target/chessboard2.js')

assert(cb2, 'chessboard2 commonjs module does not exist')
assert(cb2.Chessboard2, 'chessboard2 commonjs module does not have .Chessboard2 property')
assert(typeof cb2.Chessboard2 === 'function', 'chessboard2 commonjs module .Chessboard2 property should be a function')
