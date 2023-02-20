function isPromise (p) {
  return !!p && typeof p.then === 'function'
}

describe('Example 9001: moves', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/9001.html')
      .get('#myBoard .piece-349f8').should('have.length', 32)
      .window().then((win) => {
        assert.exists(win.board1)
        // test that a few of the API methods exist
        assert.isFunction(win.board1.position)
        assert.isFunction(win.board1.move)
        assert.isFunction(win.board1.addArrow)

        // we should be in the start position
        assert.equal(win.board1.position('map').size, 32)
        assert.equal(win.board1.fen(), 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR')
      })
  })

  it('test1: e2 - e4', () => {
    cy.get('#test1StartBtn').click()
      .get('#test1Container').should('be.visible')
      .get('#test1move1Btn').should('be.visible')
      .get('#test1move1Btn').click()
      .window().then(win => {
        assert.exists(win.test1move)
        assert.isTrue(isPromise(win.test1move))
      })
      .get('#test1results').should('be.visible')
      .get('#test1move1finished').should('be.visible')
      .window().then(win => {
        assert.exists(win.board1)
        const pos1 = win.board1.position()
        assert.equal(pos1.e4, 'wP')
      })
  })

  it('test2: multiple moves', () => {
    cy.get('#test2StartBtn').click()
      .get('#test2move1Btn').click()
      .window().then(win => {
        assert.exists(win.test2move1)
        assert.exists(win.test2move2)
        assert.isTrue(isPromise(win.test2move1))
        assert.isTrue(isPromise(win.test2move2))
      })
      .get('#test2move1finished').should('be.visible')
      .get('#test2move2finished').should('not.exist')
      // wait for animation to finish ...
      .get('#test2move2finished').should('be.visible')
  })

  it('test3: set position callback', () => {
    cy.get('#test3StartBtn').click()
      .get('#test3step1Btn').click()
      .window().then(win => {
        assert.exists(win.test3step1)
        assert.isTrue(isPromise(win.test3step1))
        assert.equal(win.board1.fen(), '8/8/rnbqkbnr/pppppppp/PPPPPPPP/1NBQKBNR/8/8')
      })
      .get('#test3step1finished').should('not.exist')
      // wait for animation to finish ...
      .get('#test3step1finished').should('be.visible')
      .get('#myBoard .piece-349f8').should('have.length', 31)
  })

  it('test4: set position Promise', () => {
    cy.get('#test4StartBtn').click()
      .get('#test4step1Btn').click()
      .get('#test4step1finished').should('not.exist')
      // wait for animation to finish ...
      .get('#test4step1finished').should('be.visible')
      // ensure that we can parse the JSON inside the result div
      .get('#test4step1finished').then($div => {
        const result = JSON.parse($div.text())
        assert.exists(result)
        assert.isObject(result.beforePosition)
        assert.isObject(result.afterPosition)
      })
  })

  it('test5: orientation', () => {
    cy.get('#test5StartBtn').click()

    // step1: add Arrows
      .get('#test5step1Btn').click()
      .get('#myBoard .arrow-bc3c7').should('have.length', 3)
      .get('#myBoard .circle-a0266').should('have.length', 0)
      .get('#myBoard .item-18a5b').should('have.length', 3)
      .window().then(win => {
        const arrows1 = win.board1.arrows('map')
        assert.exists(arrows1)
        assert.equal(arrows1.size, 3, 'there should be 3 Arrows after step1')

        const circles1 = win.board1.circles('map')
        assert.exists(circles1)
        assert.equal(circles1.size, 0, 'there should be 0 Circles after step1')

        assert.equal(win.board1.orientation(), 'white', 'Orientation should be white after step1')
      })

      // step2: add Circles
      .get('#test5step2Btn').click()
      .get('#myBoard .arrow-bc3c7').should('have.length', 3)
      .get('#myBoard .circle-a0266').should('have.length', 3)
      .get('#myBoard .item-18a5b').should('have.length', 6)
      .window().then(win => {
        const arrows1 = win.board1.getArrows()
        assert.exists(arrows1)
        assert.equal(arrows1.length, 3, 'there should be 3 Arrows after step2')

        const circles1 = win.board1.getCircles()
        assert.exists(circles1)
        assert.equal(circles1.length, 3, 'there should be 3 Circles after step2')

        const items1 = win.board1.items()
        assert.exists(items1)
        assert.equal(items1.length, 6, 'there should be 6 total Items after step2')

        assert.equal(win.board1.orientation(), 'white', 'Orientation should be white after step2')
      })

      // step3: flip the board
      .get('#test5step3Btn').click()
      .get('#myBoard .arrow-bc3c7').should('have.length', 3)
      .get('#myBoard .circle-a0266').should('have.length', 3)
      .get('#myBoard .item-18a5b').should('have.length', 6)
      .window().then(win => {
        const arrows1 = win.board1.getArrows()
        assert.exists(arrows1)
        assert.equal(arrows1.length, 3, 'there should be 3 Arrows after step3')

        const circles1 = win.board1.getCircles()
        assert.exists(circles1)
        assert.equal(circles1.length, 3, 'there should be 3 Circles after step3')

        const items1 = win.board1.items()
        assert.exists(items1)
        assert.equal(items1.length, 6, 'there should be 6 total Items after step3')

        assert.equal(win.board1.orientation(), 'black', 'Orientation should be black after step3')
      })

      // step4: remove some items
      .get('#test5step4Btn').click()
      .get('#myBoard .arrow-bc3c7').should('have.length', 2)
      .get('#myBoard .circle-a0266').should('have.length', 2)
      .get('#myBoard .item-18a5b').should('have.length', 4)
      .window().then(win => {
        const arrows1 = win.board1.getArrows()
        assert.exists(arrows1)
        assert.equal(arrows1.length, 2, 'there should be 2 Arrows after step4')

        const circles1 = win.board1.getCircles()
        assert.exists(circles1)
        assert.equal(circles1.length, 2, 'there should be 2 Circles after step4')

        const items1 = win.board1.items()
        assert.exists(items1)
        assert.equal(items1.length, 4, 'there should be 4 Items after step4')

        assert.equal(win.board1.orientation(), 'black', 'Orientation should be black after step4')
      })

      // step5: flip again
      .get('#test5step3Btn').click()
      .get('#myBoard .arrow-bc3c7').should('have.length', 2)
      .get('#myBoard .circle-a0266').should('have.length', 2)
      .get('#myBoard .item-18a5b').should('have.length', 4)
      .window().then(win => {
        const arrows1 = win.board1.getArrows()
        assert.exists(arrows1)
        assert.equal(arrows1.length, 2, 'there should be 2 Arrows after step5')

        const circles1 = win.board1.getCircles()
        assert.exists(circles1)
        assert.equal(circles1.length, 2, 'there should be 2 Circles after step5')

        const items1 = win.board1.items()
        assert.exists(items1)
        assert.equal(items1.length, 4, 'there should be 4 Items after step5')

        assert.equal(win.board1.orientation(), 'white', 'Orientation should be white after step5')
      })
  })
})
