# chessboard.js version 2

chessboard2 library

## Development Setup

```sh
yarn install

npx shadow-cljs release chessboard2
```

## Tests

```sh
npx shadow-cljs compile node-tests
```

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

- maybe use the "squares as background image" technique like Wikipedia
  - would reduce the number of DOM elements
  - but less customizable for consumers
