# chessboard2 JavaScript Library [![npm](https://img.shields.io/npm/v/@chrisoakman/chessboard2)](https://www.npmjs.com/package/@chrisoakman/chessboard2) [![ISC License](https://img.shields.io/npm/l/@chrisoakman/chessboard2)](https://github.com/oakmac/chessboard2/blob/master/LICENSE.md)

An updated version of [chessboard.js].

- better mobile support
- no dependencies
- written in [ClojureScript]
- improved API

## Development Status

**April 2023**: Development in progress. Getting close to an initial v2 release. Pretty usable for most projects in it's current state.

In-progress documentation available at [https://chessboardjs.com/v2/examples](https://chessboardjs.com/v2/examples)

## Download and Install

Instructions are available [here](https://chessboardjs.com/v2/download).

[@chrisoakman/chessboard2 on npm](https://www.npmjs.com/package/@chrisoakman/chessboard2)

Or use via [CDN](https://en.wikipedia.org/wiki/Content_delivery_network):

```html
<!-- add stylesheet via CDN: -->
<link rel="stylesheet"
      href="https://unpkg.com/@chrisoakman/chessboard2@0.4.0/dist/chessboard2.min.css"
      integrity="sha384-MZONbGYADvdl4hLalNF4d+E/6BVdYIty2eSgtkCbjG7iQJAe35a7ujTk1roZIdJ+"
      crossorigin="anonymous">

<!-- add JS via CDN: -->
<script src="https://unpkg.com/@chrisoakman/chessboard2@0.4.0/dist/chessboard2.min.js"
        integrity="sha384-zl6zz0W4cEX3M2j9+bQ2hv9af6SF5pTFrnm/blYYjBmqSS3tdJChVrY9nenhLyNg"
        crossorigin="anonymous"></script>
```

## Naming and Versioning

chessboard2 is a distinct project from [chessboard.js]. The project name is
"chessboard2" and the version of the library will be independent of the version
of original [chessboard.js].

To remove any confusion for users, I will release chessboard2 at v2.0.0 for it's
"initial release". There will not be a v1.0.0 major release of chessboard2.

It is possible (although unlikely) that chessboard.js will have a v2.0.0 branch.

## Development Setup

Make sure that [node.js], [yarn], and a modern version of the JVM are installed (for [shadow-cljs]), then:

[node.js]:https://nodejs.org
[yarn]:https://yarnpkg.com/
[shadow-cljs]:https://github.com/thheller/shadow-cljs

```sh
## initial setup: install node_modules/ folder
yarn install

## produce website/chessboard2.js and build the local website
npm run build

## run a local web server on port 3232
npm run local-dev
```

In order to create and publish a release:

```sh
## 1) update CHANGELOG
## 2) make sure flags/runtime-checks? is set to false
## 3) update package.json version
## 4) create a git commit
## 5) create git tag: "git tag -a v1.4 -m "my version 1.4" "
## 6) push git tag to GitHub: git push origin <tagname>

## 7) create fresh build
npm run release

## 8) sanity-check the result files
npm publish --dry-run

## 9) publish
npm publish --access=public
```

## Tests

```sh
## Unit Tests
npm run test

## Cypress
npm run cypress
```

## TODO before go-live

- [ ] variadic `removeArrow`, `removeCircle`, `removePiece` functions
- [ ] add "Rings"
  - are these separate from Circles or just an added config value?
- [ ] custom Items
  - add Duck to board, add toaster SVG
- [ ] version the position? increment by 1 every time the position changes
- [ ] review the speed shorthand times. ie: what should "slow" and "superslow" feel like?

## API

- `getItems()` return all items
- "pulse" a piece with some simple animations
- "bounce" a piece?
- animate an arrow
- `removeArrow(<arrowId>, <arrowId>, etc)`

## License

[ISC License](LICENSE.md)

[ClojureScript]:https://clojurescript.org/
[chessboard.js]:https://github.com/oakmac/chessboardjs
