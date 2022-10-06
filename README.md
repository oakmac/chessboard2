# chessboardjs2 javascript library

An updated version of [chessboard.js]. Written in [ClojureScript], no dependencies, improved API.

## Development Status

**October 2022**: Development in progress. Not recommended using for anything public-facing.

## Naming and Versioning

chessboardjs2 is a completely distinct project from [chessboard.js]. The project
name is "chessboardjs2" and the version of the library will be independent of the
version of original [chessboard.js].

To remove any confusion for users, I will release chessboardjs2 at v2.0.0 for it's
"initial release". There will not be a v1.0.0 major release of chessboardjs2.

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
npx shadow-cljs release chessboard2 && ./scripts/website.js

## run a local web server on port 3232
npm run local-dev
```

## Tests

```sh
## Unit Tests
npx shadow-cljs compile node-tests
## or
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
- [ ] notation should be configurable
- [ ] version the position? increment by 1 every time the position changes
- [ ] review the speed shorthand times. ie: what should "slow" and "superslow" feel like?
- [ ] draggable pieces on the board
- [ ] tap-to-move should work great

## API

- everything that chessboardjs v1 has
- `getItems()` return all items
- "pulse" a piece with some simple animations
- "bounce" a piece?
- animate an arrow
- `isAnimating` boolean
- `arrows()` returns array of the arrows on the board
- `addArrow(src, dest, '#color')`
- `addArrow({src, dest, color})`
- `removeArrow(<arrowId>, <arrowId>, etc)`
- use a `data-chessboard-draggable` property to allow items to be dropped to the board

## HTML / DOM Design

- the board-container has CSS `position: relative` and known `width` and `height` values
- the board contains DOM elements (called "items"), all of which have `position: absolute`
  - squares (usually 64)
  - pieces
  - arrows
  - dots
  - X's
  - your custom element!
- chessboard keeps an internal register of the location of these elements on the board
  and will update their position in response to a change

## Notes

- [duck chess](https://duckchess.com/) should be implementable

## License

[ISC License](LICENSE.md)

[ClojureScript]:https://clojurescript.org/
[chessboard.js]:https://github.com/oakmac/chessboardjs
