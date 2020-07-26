package uk.ac.bournemouth.ap.yahtzeebase.logic

import uk.ac.bournemouth.ap.yahtzeebase.testlib.YahtzeeTestBase

import uk.ac.bournemouth.ap.yahtzeebase.lib.Player

internal class StudentYahtzeeGameTest : YahtzeeTestBase<StudentYahtzeeGame>() {
    override fun newGame(players: List<Player>): StudentYahtzeeGame {
        return StudentYahtzeeGame(players)
    }

}