#! /usr/bin/env node

// -----------------------------------------------------------------------------
// This file creates a versioned release in the dist/ folder
// -----------------------------------------------------------------------------

// libraries
const assert = require('assert')
const csso = require('csso')
const fs = require('fs-plus')

const encoding = { encoding: 'utf8' }

const copyrightYear = '2021'
const packageJSON = JSON.parse(fs.readFileSync('package.json', encoding))
const version = packageJSON.version
const cssSrc = fs.readFileSync('src-css/chessboard2.css', encoding)
  .trim()
  .replace('@VERSION', version)
const jsSrc = fs.readFileSync('target/chessboard2.js', encoding)
  .trim()
  .replace('@VERSION', version)
  .replace('var shadow$provide = {};\n', '')
  .replace('\n/*\n\n Copyright The Closure Library Authors.\n SPDX-License-Identifier: Apache-2.0\n*/\n', '')
const esmSrc = fs.readFileSync('target/chessboard2-esm.js', encoding)
  .trim()
  .replace('@VERSION', version)
  .replace('var shadow$provide = {};\n', '')
  .replace('\n/*\n\n Copyright The Closure Library Authors.\n SPDX-License-Identifier: Apache-2.0\n*/\n', '')

fs.removeSync('dist')
fs.makeTreeSync('dist')
infoLog('Creating dist/ folder for version ' + version)

// sanity checks
assert(cssSrc && typeof cssSrc === 'string' && cssSrc !== '', 'Failed to release: chessboard2.css does not exist')
assert(jsSrc && typeof jsSrc === 'string' && jsSrc !== '', 'Failed to release: chessboard2.js does not exist')
assert(esmSrc && typeof esmSrc === 'string' && esmSrc !== '', 'Failed to release: chessboard2-min.js does not exist')

// minify CSS
const minifiedCSS = csso.minify(cssSrc).css
assert(minifiedCSS && typeof minifiedCSS === 'string' && minifiedCSS !== '', 'Failed to release: chessboard2.min.css does not exist')

// add license to the top of minified files
const minifiedJSWithBanner = banner() + jsSrc
const minifiedESMWithBanner = banner() + esmSrc

// copy lib files to dist/
const distCssFile = 'dist/chessboard2.css'
fs.writeFileSync(distCssFile, cssSrc, encoding)
infoLog('Wrote ' + distCssFile)

const distCssMinFile = 'dist/chessboard2.min.css'
fs.writeFileSync(distCssMinFile, minifiedCSS, encoding)
infoLog('Wrote ' + distCssMinFile)

const distJsFile = 'dist/chessboard2.min.js'
fs.writeFileSync(distJsFile, minifiedJSWithBanner, encoding)
infoLog('Wrote ' + distJsFile)

const distEsmFile = 'dist/chessboard2.min.mjs'
fs.writeFileSync(distEsmFile, minifiedESMWithBanner, encoding)
infoLog('Wrote ' + distEsmFile)

infoLog('Success 👍')
process.exit(0)

function banner () {
  return '/*! chessboard2 v' + version + ' | (c) ' + copyrightYear + ' Chris Oakman | ISC License | https://chessboardjs.com/v2 */\n'
}

function infoLog (msg) {
  console.log('[scripts/release.js] ' + msg)
}
