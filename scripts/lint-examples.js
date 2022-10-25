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

fs.removeSync('tmp-linting')
fs.makeTreeSync('tmp-linting')

// grab the examples
const examplesArr = kidif('examples/*.example',{camelCaseTitles: false})

// sanity check the examples
assert(examplesArr, 'Could not load the Example files')
assert(examplesArr.length > 1, 'Zero examples loaded')

// create temporary js files for linting
examplesArr.forEach((example) => {
  const tmpPath = path.join('tmp-linting', example.id + '.js')
  fs.writeFileSync(tmpPath, example.JS)
})

// run linter on temporary folder
// standard throws errors on non-fixable issues, so log the error
try{
  const output = childProcess.execSync(`npx standard --global attachEvent --global Chess --global Chessboard --global Chessboard2 --fix tmp-linting/`, {
    cwd: path.join(__dirname, '..'),
    encoding: 'utf8'
  })
  console.log(output.stdOut)
} catch (err) {
  console.error(err)
}

// loop over all examples, match up lintedJS for each, and re-write the example
fs.readdirSync('./examples/').forEach( (file) => {
  const originalExamplesArr = kidif(`examples/${file}`,{camelCaseTitles: false})
  const originalExample = originalExamplesArr[0]
  
  if(originalExamplesArr.length === 1 && originalExample.id && originalExample.JS) {
    const lintedJS = fs.readFileSync(`tmp-linting/${originalExample.id}.js`).toString()
    assert(lintedJS.length > 0)

    let outputString = ''

    const lastKeyIndex = Object.keys(originalExample).length - 1
    Object.entries(originalExample).forEach(([key, originalValue],idx) => {
      outputString += `===== ${key}\n` +
                      `${key === "JS" ? lintedJS : originalValue}`
      if(idx < lastKeyIndex) {
        outputString += '\n\n'
      }
    })

    fs.writeFileSync(`./examples/${file}`,outputString)
  }
})

// delete temporary linting directory
fs.removeSync('tmp-linting')
