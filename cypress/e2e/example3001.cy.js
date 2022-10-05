describe('Example 3001', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/3001.html')
  })

  it('Empty Board', () => {
    cy.get('#myBoard .piece-349f8').should('have.length', 0)
  })

  it('Start Position', () => {
    cy.get('#setStartBtn').click()
    cy.get('#myBoard .piece-349f8').should('have.length', 32)
  })

  it('Start Position --> Rook Checkmate', () => {
    cy.get('#setStartBtn').click()
    cy.get('#myBoard .piece-349f8').should('have.length', 32)

    cy.get('#setRookCheckmateBtn').click()
    cy.get('#myBoard .piece-349f8').should('have.length', 3)
  })
})
