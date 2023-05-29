# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.5.0] - 2023-05-29

### Fixed
- Fixed bug with `position->fen` function ([Issue #47](https://github.com/oakmac/chessboard2/issues/47) - thank you [@gdm3](https://github.com/gdm3)!)

## [0.4.0] - 2023-04-08

### Added
- add `.config()`, `.setConfig()`, `.getConfig()` methods ([Issue #7](https://github.com/oakmac/chessboard2/issues/7))
- added `onMouseenterSquare`, `onMouseleaveSquare` events and examples ([Issue #40](https://github.com/oakmac/chessboard2/issues/40)), ([PR #41](https://github.com/oakmac/chessboard2/pull/41))
- added `onMousedownSquare`, `onMouseupSquare` events and examples ([Commit #88043432c1](https://github.com/oakmac/chessboard2/commit/88043432c15196e3c8a2621f807f6231df78a26a))

## [0.3.0] - 2022-11-23
### Added
- support ES Module format ([Issue #17](https://github.com/oakmac/chessboard2/issues/17))
- add Circles example to website ([Issue #35](https://github.com/oakmac/chessboard2/issues/35))
- add `.destroy()` method
- add `.onChange()` event

### Changed
- convert arrow style to use percent-based values ([Issue #14](https://github.com/oakmac/chessboard2/pull/14)) - thank you @aurmer
- update several examples

## [0.2.0] - 2022-10-24
### Added
- touch-to-move and tap-to-move
- `.resize()` method
- Example 5001: play against random computer

### Changed
- rename project from "chessboardjs2" --> "chessboard2"
- use percent values for Piece and Circle positioning
- improve layout CSS / structure
- gitignore dynamic website files

## [0.1.0] - 2022-10-13
### Added
- pre-release to allow for testing

[Unreleased]: https://github.com/oakmac/chessboard2/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/oakmac/chessboard2/releases/tag/v0.4.0
[0.3.0]: https://github.com/oakmac/chessboard2/releases/tag/v0.3.0
[0.2.0]: https://github.com/oakmac/chessboard2/releases/tag/v0.2.0
[0.1.0]: https://github.com/oakmac/chessboard2/releases/tag/v0.1.0
