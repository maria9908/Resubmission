package uk.ac.bournemouth.ap.yahtzeebase.lib

sealed class Player(val name: String) {

}

open class HumanPlayer(name: String): Player(name) {
    override fun toString(): String {
        return "HumanPlayer(\"$name\")"
    }
}
abstract class ComputerPlayer(name: String): Player(name) {
    override fun toString(): String {
        return "ComputerPlayer(\"$name\")"
    }


    abstract fun playFullTurn(yahtzeeGame: YahtzeeGame<*>)
}
