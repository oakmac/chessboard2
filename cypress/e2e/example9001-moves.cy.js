const assert = require('assert')

describe('Example 9001: moves', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/9001.html')

    cy.get('#myBoard .piece-349f8').should(($pieces) => {
      expect($pieces).to.have.length(32)
    })

    cy.window().then((win) => {
      expect(win.board1).to.exist
      expect(win.board1.position('map').size === 32).to.be.true
    })
  })

  it('Start Position', () => {
    cy.get('#myBoard .piece-349f8').should('have.length', 32)
  })

  it('e2 - e4', () => {
    cy.get('#move1Btn').click()
    cy.get('#myBoard .piece-349f8').should('have.length', 32)

    cy.window().then(win => {
      expect(win.board1).to.exist
      const pos1 = win.board1.position()
      expect(pos1['e4'] === 'wP').to.be.true
    })

    // expect(3 === 3).to.be.true

    // cy.get('#setRookCheckmateBtn').click()
    // cy.get('#myBoard .piece-349f8').should('have.length', 3)
  })
})
