describe('Example 3001', () => {
  beforeEach(() => {
    cy.visit('http://localhost:3232/examples/3001-set-position.html')
      .get('#myBoard')
      .should('have.length', 1)
      .get('#myBoard .piece-349f8')
      .should('have.length', 0)
  })

  it('Empty Board', () => {
    cy.get('#myBoard .piece-349f8').should('have.length', 0)
  })

  it('Start Position', () => {
    cy.get('#setStartBtn')
      .click()
      .get('#myBoard .piece-349f8')
      .should('have.length', 32)
  })

  // FIXME: this test does not always work due to the speed of animations
  // -- 5 Oct 2022, C. Oakman
  // it('Start Position --> Rook Checkmate --> Start Position', () => {
  //   cy.get('#setStartBtn')
  //     .click()
  //     .get('#myBoard .piece-349f8').should('have.length', 32)
  //     .get('#setRookCheckmateBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 3)
  //     .get('#setStartBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 32)
  // })

  // FIXME: this test does not always work due to the speed of animations
  // -- 5 Oct 2022, C. Oakman
  // it('Start Position --> Pawn Attack', () => {
  //   cy.get('#setStartBtn')
  //     .click()
  //     .get('#myBoard .piece-349f8').should('have.length', 32)
  //     .get('#pawnAttackBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 64)
  //     .get('#pawnAttackBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 64)
  //     .get('#pawnAttackBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 64)
  //     .get('#pawnAttackBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 64)
  //     .get('#setStartBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 32)
  //     .get('#clearBoardBtn').click()
  //     .get('#myBoard .piece-349f8').should('have.length', 0)
  // })
})
