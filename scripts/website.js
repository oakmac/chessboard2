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
const jsCDNScript = '<script src="https://unpkg.com/@chrisoakman/chessboardjs2@0.1.0/dist/chessboard2.min.js" integrity="sha384-ljFKlPNmPU0eDTYaCKv+07alkNG+MQFDJWCekcZv9C8Eptr0rVLJZZ03J5vOWlak" crossorigin="anonymous"></script>'
const cssCDNLink = '<link rel="stylesheet" href="https://unpkg.com/@chrisoakman/chessboardjs2@0.1.0/dist/chessboard2.min.css" integrity="sha384-fMMbbVRSzK7dM0LfiAtcxjQpQrr6jiiAXTWhwapOds+a8Bu/NUMxiHh+TKAY3LPk" crossorigin="anonymous">'

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
const latestChessboardCSS = fs.readFileSync(path.join(__dirname, '../src-css/chessboard2.css'), encoding)

assert(typeof latestChessboardJS === 'string' && latestChessboardJS !== '')
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
    const parsed = cmReader.parse(ex.descriptionmd)
    if (!parsed) assert('Example ' + ex.id + ' has bad Markdown.')
    ex.description = cmWriter.render(parsed)
  }
  return ex
})

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
      '2003',
      '2082',
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
      '3003',
      '3004-move-pieces',
      '3005',
      '3006',
      '3007'
    ]
  },
  {
    name: 'Events',
    examples: [
      '4000',
      '4001',
      '4002',
      '4003',
      '4004',
      '4005',
      '4006',
      '4011',
      '4012'
    ]
  },
  {
    name: 'Integration',
    examples: [
      '5000',
      '5001',
      '5002',
      '5003',
      '5004',
      '5005'
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
const board2 = Chessboard2('board2', {
  draggable: true,
  dropOffBoard: 'trash',
  sparePieces: true
})

$('#startBtn').on('click', board2.start)
$('#clearBtn').on('click', board2.clear)`.trim()

function writeSrcFiles () {
  fs.writeFileSync('website/js/chessboard2.js', latestChessboardJS, encoding)
  fs.writeFileSync('website/css/chessboard2.css', latestChessboardCSS, encoding)
}

function writeHomepage () {
  const headHTML = mustache.render(headTemplate, { pageTitle: 'Homepage' })

  const html = mustache.render(homepageTemplate, {
    jsScript,
    example2: homepageExample2,
    footer: footerTemplate,
    head: headHTML
  })
  const filename = 'website/index.html'
  fs.writeFileSync(filename, html, encoding)
  infoLog('Wrote ' + filename)
}

function writeExamplesPage () {
  const headHTML = mustache.render(headTemplate, {
    chessboard2CSSLink: cssLink,
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
  const headHTML = mustache.render(headTemplate, { pageTitle: 'Download' })
  const headerHTML = mustache.render(headerTemplate, { downloadActive: true })

  const html = mustache.render(downloadTemplate, {
    footer: footerTemplate,
    head: headHTML,
    header: headerHTML
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

function writeWebsite () {
  writeSrcFiles()
  writeHomepage()
  writeExamplesPage()
  writeSingleExamplesPages()
  writeDocsPage()
  writeDownloadPage()
  writeLicensePage()
}

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
      '  html: ' + JSON.stringify(ex.html) + ',\n' +
      '  name: ' + JSON.stringify(ex.name) + ',\n' +
      '  jsStr: ' + JSON.stringify(ex.js) + ',\n' +
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
