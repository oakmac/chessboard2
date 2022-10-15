#! /usr/bin/env node

// -----------------------------------------------------------------------------
// This script runs standardjs on the JS in the .example files
// -----------------------------------------------------------------------------

// libraries
const assert = require('assert')
const childProcess = require('child_process')
const fs = require('fs-plus')
const kidif = require('kidif')
const path = require('path')

fs.removeSync('tmp')
fs.makeTreeSync('tmp')

// grab the examples
const examplesArr = kidif('examples/*.example')

// sanity check the examples
assert(examplesArr, 'Could not load the Example files')
assert(examplesArr.length > 1, 'Zero examples loaded')

examplesArr.forEach((example) => {
  fs.writeFileSync(path.join('tmp', example.id + '.js'), example.js)
})

const output = childProcess.execSync('npx standard --fix tmp/', {
  cwd: path.join(__dirname, '..'),
  encoding: 'utf8'
})

console.log(output.stdout)

fs.removeSync('tmp')
