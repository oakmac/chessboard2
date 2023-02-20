describe('Example 9005: config', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/9005.html')
      .get('#myBoard .piece-349f8').should('have.length', 32)
      .window().then((win) => {
        assert.exists(win.board1)
        // test that a few of the API methods exist
        assert.isFunction(win.board1.position)
        assert.isFunction(win.board1.move)
        assert.isFunction(win.board1.addArrow)

        // we should be in the start position
        assert.equal(win.board1.position('map').size, 32)
        assert.equal(win.board1.fen(), "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")
      })
  })

  it('change config values at runtime', () => {
    cy.get('#setConfig1Btn').click()
      .window().then(win => {
        assert.equal(win.board1.orientation(), 'black')
      })
      .get('#setKingPawnEndgameBtn').click()
      .get('#myBoard .piece-349f8').should('have.length', 3)
      .get('#onChangeTarget').then($div => {
        const innerText = $div.text()
        assert.equal(innerText, 'change1')
      })
      .get('#setConfig2Btn').click()
      .get('#setRuyLopezBtn').click()
      .get('#myBoard .piece-349f8').should('have.length', 32)
      .get('#onChangeTarget').then($div => {
        const innerText = $div.text()
        assert.equal(innerText, 'change2')
      })
  })
})
