#! /usr/bin/env node

// -----------------------------------------------------------------------------
// This file builds the contents of the website/ folder.
// -----------------------------------------------------------------------------

// libraries
const assert = require('assert')
const commonmark = require('commonmark')
const docs = require('../data/docs.json')
const fs = require('fs-plus')
const kidif = require('kidif')
const mustache = require('mustache')
const path = require('path')

const encoding = { encoding: 'utf8' }

const cmReader = new commonmark.Parser()
const cmWriter = new commonmark.HtmlRenderer()

// toggle development version
const useLocalDevFiles = true
const jsCDNUrl = 'https://unpkg.com/@chrisoakman/chessboard2@0.5.0/dist/chessboard2.min.js'
const jsCDNIntegrity = 'sha384-/KwQCjA1GWovZNV3QDVtvSMDzO4reGgarF/RqHipr7hIUElH3r5zNl9WEPPOBRIF'
const jsCDNScript = '<script src="' + jsCDNUrl + '" integrity="' + jsCDNIntegrity + '" crossorigin="anonymous"></script>'

// const esmCDNScript = '<script src="https://unpkg.com/@chrisoakman/chessboard2@0.3.0/dist/chessboard2.min.mjs" integrity="sha384-1yHocjOlRFtt1hT94ytsOQ/8eylPRk9Gj/DLNca1faolxec6F7k4c+f3S3FS60Rf" crossorigin="anonymous"></script>'

const cssCDNUrl = 'https://unpkg.com/@chrisoakman/chessboard2@0.5.0/dist/chessboard2.min.css'
const cssCDNIntegrity = 'sha384-47VeTDpmy4yT21gKPXQcLQYQZwlmz27gEH5NTrOmTk3G/SGvMyltclOW/Q8uE+sL'
const cssCDNLink = '<link rel="stylesheet" href="' + cssCDNUrl + '" integrity="' + cssCDNIntegrity + '" crossorigin="anonymous">'

let jsScript = jsCDNScript
let cssLink = cssCDNLink
if (useLocalDevFiles) {
  jsScript = '<script src="js/chessboard2.js"></script>'
  cssLink = '<link rel="stylesheet" href="css/chessboard2.css">'
}

// grab some mustache templates
const docsTemplate = fs.readFileSync('templates/docs.mustache', encoding)
const downloadTemplate = fs.readFileSync('templates/download.mustache', encoding)
const examplesTemplate = fs.readFileSync('templates/examples.mustache', encoding)
const homepageTemplate = fs.readFileSync('templates/homepage.mustache', encoding)
const singleExampleTemplate = fs.readFileSync('templates/single-example.mustache', encoding)
const licensePageTemplate = fs.readFileSync('templates/license.mustache', encoding)
const headTemplate = fs.readFileSync('templates/_head.mustache', encoding)
const headerTemplate = fs.readFileSync('templates/_header.mustache', encoding)
const footerTemplate = fs.readFileSync('templates/_footer.mustache', encoding)

const latestChessboardJS = fs.readFileSync(path.join(__dirname, '../target/chessboard2.js'), encoding)
const latestChessboardESM = fs.readFileSync(path.join(__dirname, '../target/chessboard2-esm.js'), encoding)
const latestChessboardCSS = fs.readFileSync(path.join(__dirname, '../src-css/chessboard2.css'), encoding)

assert(typeof latestChessboardJS === 'string' && latestChessboardJS !== '')
assert(typeof latestChessboardESM === 'string' && latestChessboardESM !== '')
assert(typeof latestChessboardCSS === 'string' && latestChessboardCSS !== '')

// grab the examples
let examplesArr = kidif('examples/*.example')

// sanity check the examples
assert(examplesArr, 'Could not load the Example files')
assert(examplesArr.length > 1, 'Zero examples loaded')

infoLog('Loaded ' + examplesArr.length + ' examples')

// convert Descriptions in Markdown to HTML
examplesArr = examplesArr.map(ex => {
  if (ex.descriptionmd) {
    const tweakedMarkdown = tweakExampleMarkdown(ex.descriptionmd)
    const parsed = cmReader.parse(tweakedMarkdown)
    if (!parsed) assert('Example ' + ex.id + ' has bad Markdown.')
    ex.description = cmWriter.render(parsed)
  }
  return ex
})

// make some slight adjustments to the Examples Markdown before it is parsed
// NOTE: I am pretty sure this can be done using the AST from CommonMark instead
// of string replacement
function tweakExampleMarkdown (md) {
  return md
    .replaceAll('`true`', '<code class="js keyword">true</code>')
    .replaceAll(/`("|')[a-zA-Z]+("|')`/g, (str) => {
      return str.replace('`', '<code class="js string">')
        .replace('`', '</code>')
    })
}

const examplesMap = new Map()
examplesArr.forEach((example) => {
  examplesMap.set(example.id, example)
})

// NOTE: this needs to stay in sync with the ids of the example files
const examplesGroups = [
  {
    name: 'Basic Usage',
    examples: [
      '1000-empty-board',
      '1001-start-position',
      '1002-fen-string',
      '1003-position-object',
      '1004-multiple-boards'
    ]
  },
  {
    name: 'Config',
    examples: [
      '2000-config-position',
      '2044',
      '2063',
      '2001-config-orientation',
      '2002-config-notation',
      '2003-draggable-pieces',
      '2004',
      '2030',
      '2005',
      '2006'
    ]
  },
  {
    name: 'Methods',
    examples: [
      '3000-get-position',
      '3001-set-position',
      '3002-set-position-instant',
      '3023-set-random-position',
      '3003-clear',
      '3004-move-pieces',
      '3008-analysis-arrows',
      '3009-circles',
      '3005-orientation',
      '3006-destroy',
      '3007-resize'
    ]
  },
  {
    name: 'Events',
    examples: [
      '4000-onchange',
      '4001-ondragstart',
      '4002-ondragstart-prevent-drag',
      '4003',
      '4004',
      '4005',
      '4006',
      '4011',
      '4012',
      '4015-mouse-enter-leave-squares',
      '4016-mouse-down-up-squares'
    ]
  },
  {
    name: 'Integration',
    examples: [
      '5000-allow-legal-moves',
      '5001-play-random-computer',
      '5002-random-vs-random',
      '5003',
      '5004',
      '5005',
      '5006-click-to-create-arrows',
      '5007-click-to-create-arrows-preview'
    ]
  }
]

// sanity-check that all of the example ids exist
examplesGroups.forEach((group) => {
  group.examples.forEach((exampleId) => {
    assert(examplesMap.has(exampleId), 'examplesGroups has invalid exampleId: ' + exampleId)
  })
})

const homepageExample2 = `
const game = new Chess()
const board = Chessboard2('board2', 'start')

window.setTimeout(makeRandomMove, 500)

function makeRandomMove () {
  if (game.game_over()) return

  const legalMoves = game.moves()
  const randomIdx = Math.floor(Math.random() * legalMoves.length)
  game.move(legalMoves[randomIdx])
  board.position(game.fen())

  window.setTimeout(makeRandomMove, 500)
}`.trim()

function writeSrcFiles () {
  fs.writeFileSync('website/js/chessboard2.js', latestChessboardJS, encoding)
  fs.writeFileSync('website/js/chessboard2.mjs', latestChessboardESM, encoding)
  fs.writeFileSync('website/css/chessboard2.css', latestChessboardCSS, encoding)
}

function writeHomepage () {
  const headHTML = mustache.render(headTemplate, {
    chessboard2CSSLink: cssLink,
    includeHighlightjsCSS: true,
    pageTitle: 'Homepage'
  })

  const html = mustache.render(homepageTemplate, {
    example2JS: homepageExample2,
    footer: footerTemplate,
    head: headHTML,
    jsScript
  })
  const filename = 'website/index.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

function writeExamplesPage () {
  const headHTML = mustache.render(headTemplate, {
    chessboard2CSSLink: cssLink,
    includeHighlightjsCSS: true,
    pageTitle: 'Examples'
  })
  const headerHTML = mustache.render(headerTemplate, { examplesActive: true })

  const html = mustache.render(examplesTemplate, {
    jsScript,
    examplesJavaScript: buildExamplesJS(),
    footer: footerTemplate,
    head: headHTML,
    header: headerHTML,
    nav: buildExamplesNavHTML()
  })
  const filename = 'website/examples.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

const configTableRowsHTML = docs.config.reduce(function (html, itm) {
  if (isString(itm)) return html
  return html + buildConfigDocsTableRowHTML('config', itm)
}, '')

const methodTableRowsHTML = docs.methods.reduce(function (html, itm) {
  if (isString(itm)) return html
  return html + buildMethodRowHTML(itm)
}, '')

const errorRowsHTML = docs.errors.reduce(function (html, itm) {
  if (isString(itm)) return html
  return html + buildErrorRowHTML(itm)
}, '')

function isIntegrationExample (example) {
  return (example.id + '').startsWith('5')
}

function writeSingleExamplePage (example) {
  if (isIntegrationExample(example)) {
    example.includeChessJS = true
  }
  example.jsScript = jsScript
  example.cssLink = cssLink
  const html = mustache.render(singleExampleTemplate, example)
  const filename = 'website/examples/' + example.id + '.html'
  fs.writeFileSync(filename, html, encoding)
}

function writeSingleExamplesPages () {
  examplesArr.forEach(writeSingleExamplePage)
  infoLog('Wrote ' + examplesArr.length + ' single example pages')
}

function writeDocsPage () {
  const headHTML = mustache.render(headTemplate, { pageTitle: 'Documentation' })
  const headerHTML = mustache.render(headerTemplate, { docsActive: true })

  const html = mustache.render(docsTemplate, {
    configTableRows: configTableRowsHTML,
    errorRows: errorRowsHTML,
    footer: footerTemplate,
    head: headHTML,
    header: headerHTML,
    methodTableRows: methodTableRowsHTML
  })
  const filename = 'website/docs.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

function writeDownloadPage () {
  const headHTML = mustache.render(headTemplate, {
    includeHighlightjsCSS: true,
    pageTitle: 'Download'
  })
  const headerHTML = mustache.render(headerTemplate, { downloadActive: true })

  const html = mustache.render(downloadTemplate, {
    cssCDNIntegrity,
    cssCDNUrl,
    footer: footerTemplate,
    head: headHTML,
    header: headerHTML,
    jsCDNIntegrity,
    jsCDNScript,
    jsCDNUrl
  })
  const filename = 'website/download.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

function writeLicensePage () {
  const html = mustache.render(licensePageTemplate)
  const filename = 'website/license.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

// remove dynamically generated files
function removeOldFiles () {
  fs.removeSync('website/css/chessboard2.css')
  fs.removeSync('website/docs.html')
  fs.removeSync('website/download.html')
  fs.removeSync('website/examples.html')
  fs.removeSync('website/examples')
  fs.removeSync('website/index.html')
  fs.removeSync('website/js/chessboard2.js')
  fs.removeSync('website/license.html')
}

function writeWebsite () {
  writeSrcFiles()
  writeHomepage()
  writeExamplesPage()
  writeSingleExamplesPages()
  writeDocsPage()
  writeDownloadPage()
  writeLicensePage()
}

removeOldFiles()
writeWebsite()
infoLog('Successfully wrote the website/ folder 👍')

// -----------------------------------------------------------------------------
// HTML
// -----------------------------------------------------------------------------

function buildExampleGroupHTML (idx, groupName, examplesInGroup) {
  const groupNum = idx + 1
  let html = '<h4 id="groupHeader-' + groupNum + '">' + groupName + '</h4>' +
    '<ul id="groupContainer-' + groupNum + '" style="display:none">'

  examplesInGroup.forEach(function (exampleId) {
    const example = examplesMap.get(exampleId)
    html += '<li id="exampleLink-' + exampleId + '">' + example.name + '</id>'
  })

  html += '</ul>'

  return html
}

function buildExamplesNavHTML () {
  let html = ''
  examplesGroups.forEach(function (group, idx) {
    html += buildExampleGroupHTML(idx, group.name, group.examples)
  })
  return html
}

function buildExamplesJS () {
  let txt = 'window.CHESSBOARD2_EXAMPLES = {}\n\n'

  examplesArr.forEach(function (ex) {
    txt += 'CHESSBOARD2_EXAMPLES["' + ex.id + '"] = {\n' +
      '  description: ' + JSON.stringify(ex.description) + ',\n' +
      '  html64: ' + JSON.stringify(btoa(ex.html)) + ',\n' +
      '  name: ' + JSON.stringify(ex.name) + ',\n' +
      '  jsStr64: ' + JSON.stringify(btoa(ex.js)) + ',\n' +
      '  jsFn: function () {\n' + ex.js + '\n  }\n' +
      '};\n\n'
  })

  return txt
}

function buildConfigDocsTableRowHTML (propType, prop) {
  let html = ''

  // table row
  html += '<tr id="' + propType + ':' + prop.name + '">'

  // property and type
  html += '<td>' + buildPropertyAndTypeHTML(propType, prop.name, prop.type) + '</td>'

  // default
  html += '<td class="center"><p>' + buildDefaultHTML(prop.default) + '</p></td>'

  // description
  html += '<td>' + buildDescriptionHTML(prop.desc) + '</td>'

  // examples
  html += '<td>' + buildExamplesCellHTML(prop.examples) + '</td>'

  html += '</tr>'

  return html
}

function buildMethodRowHTML (method) {
  const nameNoParens = method.name.replace(/\(.+$/, '')

  let html = ''

  // table row
  if (method.noId) {
    html += '<tr>'
  } else {
    html += '<tr id="methods:' + nameNoParens + '">'
  }

  // name
  html += '<td><p><a href="docs.html#methods:' + nameNoParens + '">' +
    '<code class="js plain">' + method.name + '</code></a></p></td>'

  // args
  if (method.args) {
    html += '<td>'
    method.args.forEach(function (arg) {
      html += '<p>' + arg[1] + '</p>'
    })
    html += '</td>'
  } else {
    html += '<td><small>none</small></td>'
  }

  // description
  html += '<td>' + buildDescriptionHTML(method.desc) + '</td>'

  // examples
  html += '<td>' + buildExamplesCellHTML(method.examples) + '</td>'

  html += '</tr>'

  return html
}

function buildPropertyAndTypeHTML (section, name, type) {
  const html = '<p><a href="docs.html#' + section + ':' + name + '">' +
    '<code class="js plain">' + name + '</code></a></p>' +
    '<p class=property-type-7ae66>' + buildTypeHTML(type) + '</p>'
  return html
}

function buildTypeHTML (type) {
  if (!Array.isArray(type)) {
    type = [type]
  }

  let html = ''
  for (let i = 0; i < type.length; i++) {
    if (i !== 0) {
      html += ' <small>or</small><br />'
    }
    html += type[i]
  }

  return html
}

function buildDescriptionHTML (desc) {
  if (!Array.isArray(desc)) {
    desc = [desc]
  }

  let html = ''
  desc.forEach(function (d) {
    html += '<p>' + d + '</p>'
  })

  return html
}

function buildDefaultHTML (defaultValue) {
  if (!defaultValue) {
    return '<small>n/a</small>'
  }
  return defaultValue
}

function buildExamplesCellHTML (examplesIds) {
  if (!Array.isArray(examplesIds)) {
    examplesIds = [examplesIds]
  }

  let html = ''
  examplesIds.forEach(function (exampleId) {
    const example = examplesMap.get(exampleId)
    if (!example) return
    html += '<p><a href="examples.html#' + exampleId + '">' + example.name + '</a></p>'
  })

  return html
}

function buildErrorRowHTML (error) {
  let html = ''

  // table row
  html += '<tr id="errors:' + error.id + '">'

  // id
  html += '<td class="center">' +
    '<p><a href="docs.html#errors:' + error.id + '">' + error.id + '</a></p></td>'

  // desc
  html += '<td><p>' + error.desc + '</p></td>'

  // more information
  if (error.fix) {
    if (!Array.isArray(error.fix)) {
      error.fix = [error.fix]
    }

    html += '<td>'
    error.fix.forEach(function (p) {
      html += '<p>' + p + '</p>'
    })
    html += '</td>'
  } else {
    html += '<td><small>n/a</small></td>'
  }

  html += '</tr>'

  return html
}

function isString (s) {
  return typeof s === 'string'
}

function infoLog (msg) {
  console.log('[scripts/website.js] ' + msg)
}
