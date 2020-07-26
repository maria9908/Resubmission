package uk.ac.bournemouth.ap.yahtzeebase.lib

import kotlin.random.Random

/**
 * To make the assignment work you will have to implement this interface. The YahtzeeGame interface
 * has a type parameter that is the type of the actual game. For example `StudentYahtzeeGame`, this
 * allows for the listeners to pass along the actual game type.
 */
interface YahtzeeGame<T : YahtzeeGame<T>> {

    /**
     * The list of players in the game
     */
    val players: List<Player>

    /**
     * A property to get the current player
     */
    val currentPlayer: Player

    /**
     * The currently thrown dice. This should/can be an array of nulls when the turn starts.
     */
    val dice: Array<Int?>

    /**
     * Property containing 0, 1, 2 or 3 to determine how many times dice have been thrown in the
     * game.
     */
    val roundInTurn: Int

    /**
     * Property that can be used to determine whether the game has finished (all players have all
     * rounds played).
     */
    val isFinished: Boolean

    /**
     * Because you need to keep score per player, this interface represents the state for an
     * individual player. Note that dice are not in this interface as only one player can have the
     * turn at the same time.
     */
    interface PlayerGameState {

        /**
         * A property for the subtotal of the upper section (the 1, 2, 3, 4, 5, 6 group). This is
         * the raw score.
         */
        val upperSubTotal: Int

        /**
         * In the rules, if you have 63 (1*3 + 2*3 + 3*3 + 4*3 + 5*3 + 6*3) or more points. This
         * property should implement this rule (unlike the upper subtotal).
         */
        val upperTotal: Int

        /**
         * This property represents the score of the bottom group.
         */
        val lowerTotal: Int

        /**
         * The total score for both groups.
         */
        val totalScore: Int

        /** Get the score for an individual group */
        fun getGroupScore(scoreGroup: ScoreGroup): Int?

        /**
         * After the dice have been rolled (at least once), they can be applied to a group (that hasn't
         * been used yet).
         */
        fun applyDiceToGroup(scoreGroup: ScoreGroup)

    }

    /**
     * This is a shortcut property that just retrieves the game state for the current player.
     */
    val currentPlayerGameState: PlayerGameState
        get() = playerGameState[currentPlayer]
            ?: throw UnsupportedOperationException("No info for the current player available")


    val playerGameState: Map<Player, PlayerGameState>

    /**
     * The generator is what you would use to get die throws. This allows for the testing framework
     * to set this and get predictable throws.
     */
    var generator: Generator

    /** Add a listener for update events */
    fun addOnUpdateListener(listener: GameUpdateListener<T>)
    /** Remove a listener for update events */
    fun removeOnUpdateListener(listener: GameUpdateListener<T>)

    /** Add a listener for game finished/over events */
    fun addGameFinishedListener(listener: GameFinishedListener<T>)
    /** Remove a listener for game finished events */
    fun removeGameFinishedListener(listener: GameFinishedListener<T>)

    /**
     * Roll all dice, normally for the first turn. This function should use the generator to
     * actually determine the dice. By default just forwards to the keeping function with an empty list.
     */
    fun rollDice() = rollDice(emptyList())

    /**
     * Roll some of the dice, but keep the ones in the parameter. This function should check that
     * the dice to keep are actually present in the original dice. This function should use the generator to
     * actually determine the dice. It is valid to pass in all dice and not do another turn.
     */
    fun rollDice(keep: Iterable<Int>)

    /**
     * Function that triggers the playing of computer turns.
     */
    fun playComputerTurns()

    companion object {
        const val MAX_TURNS=3
    }

}

/**
 * For testing purposes it is necessary to control the generating of dice. This interface is
 * implemented by the code that generates a die throw.
 */
interface Generator {
    /** Get an actual die throw */
    fun getNextDieThrow(): Int
}

/**
 * The default generator of die throws
 */
object RandomGenerator : Generator {
    /** The random generator to use */
    private val random: Random = Random.Default

    /** Get the die from the random generator. Note that dice start with the value 1 until the value
     *  6, not with 0. */
    override fun getNextDieThrow(): Int {
        return random.nextInt(6) + 1
    }

}

/** Listener interface for generic game update events */
interface GameUpdateListener<in T> {
    fun onGameUpdated(game: T)
}

/** Listener interface for the game finished event */
interface GameFinishedListener<in T> {
    fun onGameFinished(game: T)
}

