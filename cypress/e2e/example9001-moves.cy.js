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
    cy.get('#test1move1Btn').click()
      .window().then(win => {
        assert.exists(win.test1move)
        assert.isTrue(isPromise(win.test1move))
      })
      .get('#test1move1finished').should('be.visible')
      .window().then(win => {
        assert.exists(win.board1)
        const pos1 = win.board1.position()
        assert.equal(pos1['e4'], 'wP')
      })
  })

  it('test2: multiple moves', () => {
    cy.get('#test2move1Btn').click()
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
})
