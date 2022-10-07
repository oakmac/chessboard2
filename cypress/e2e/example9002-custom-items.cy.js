function isPromise (p) {
  return !!p && typeof p.then === 'function'
}

describe('Example 9002: custom items', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/9002.html')
      .get('#myBoard .piece-349f8').should('have.length', 3)
      .window().then((win) => {
        assert.exists(win.board1)
        // test that a few of the API methods exist
        assert.isFunction(win.board1.position)
        assert.isFunction(win.board1.move)
        assert.isFunction(win.board1.addArrow)

        // we should be in the start position
        assert.equal(win.board1.position('map').size, 3)
        assert.equal(win.board1.fen(), '8/8/8/3pk3/8/4K3/8/8')
      })
  })

  it('test1: custom items', () => {
    // step1: add a custom item to the board
    cy.get('#test1step1Btn').click()
      .window().then(win => {
        assert.exists(win.itm1Id)
        assert.isTrue(typeof win.itm1Id === 'string')

        const items1 = win.board1.getItems('map')
        assert.equal(items1.size, 3)
        assert.equal(items1.get(win.itm1Id).id, win.itm1Id)

        cy.get('#' + win.itm1Id).should('be.visible')
      })
      .get('#myBoard .item-18a5b').should('have.length', 3)

      // step2: move the item
      .get('#test1step2Btn').click()
      .get('#test1step2finished').should('not.exist')
      // wait for animation to finish ...
      .get('#test1step2finished').should('be.visible')

      // step3: remove the item
      .get('#test1step3Btn').click()
      .get('#myBoard .item-18a5b').should('have.length', 2)
      .window().then(win => {
        assert.exists(win.itm1Id)
        assert.isTrue(typeof win.itm1Id === 'string')

        const items1 = win.board1.getItems('map')
        assert.equal(items1.size, 2)
        assert.notExists(items1.get(win.itm1Id))

        cy.get('#' + win.itm1Id).should('not.exist')
      })
  })
})
