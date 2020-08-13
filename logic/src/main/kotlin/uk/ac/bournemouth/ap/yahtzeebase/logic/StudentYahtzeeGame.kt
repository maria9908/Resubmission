package uk.ac.bournemouth.ap.yahtzeebase.logic

import uk.ac.bournemouth.ap.yahtzeebase.lib.*
import uk.ac.bournemouth.ap.yahtzeebase.lib.YahtzeeGame.Companion.MAX_TURNS
import java.lang.AssertionError


class StudentYahtzeeGame(
    players: List<Player> = listOf(
        HumanPlayer("Player 1"),
        HumanPlayer("Player 2")
    )
) : YahtzeeGame<StudentYahtzeeGame> {

    /** You want to store the list of players here. It is passed in as a constuctor argument. */
    override val players: List<Player> = players

    /** This stores the list of game finished listeners. */
    private val gameFinishedListeners = mutableListOf<GameFinishedListener<StudentYahtzeeGame>>()

    /** This stores the list of game update listeners. */
    private val gameUpdateListeners = mutableListOf<GameUpdateListener<StudentYahtzeeGame>>()

    /**
     * You want to record when the game is finished. While it is possible to calculate this, it is
     * probably better to just store it.
     */
    override val isFinished: Boolean get() = TODO("Make this a var that stores whether the game is finished")

    /**
     * Get access to the current player. Note that it may be convenient to store the index of
     * the current player in the array and just look up the current player from that index in a
     * getter.
     */
    private var currentPlayerIndex: Int = 0
    override val currentPlayer: Player get() = players[currentPlayerIndex ]

    /**
     * This property provides access to the state of individual players. You access it most times
     * through [currentPlayerGameState] defined on the interface. [associateWith] just creates a map
     * from a given set of keys.
     */
    override val playerGameState: Map<Player, YahtzeeGame.PlayerGameState> =
        players.associateWith { StudentPlayerGameState() }

    /**
     * The generator that is used to create dice rolls.
     */
    override var generator: Generator = RandomGenerator

    /**
     * The current set of thrown dice (or null when the turn just started). Note that the die values
     * are between 1 and 6, not between 0 and 5.
     *
     * @see [arrayOfNulls]
     */
    override val dice: Array<Int?> = arrayOfNulls(5)

    /**
     * This property holds an integer that determines in which turn the game is:
     *
     * 0: Before any die has been thrown
     * 1: Dice have been thrown once
     * 2: Dice have been thrown twice
     * 3: Dice have been thrown three times. No more rolls are allowed.
     *
     * Note that there is no state 4, as applying the dice to a score group will finish the turn
     * and immediately start the turn for the next player.
     */
    override var roundInTurn: Int = 0


    /**
     * Implement this function to roll the dice. This function will update the [dice] property. All
     * die values in keep should be retained and new values added using
     * [generator]`.`[ getNextDieThrow](Generator.getNextDieThrow).
     *
     * There are some validity requirements for this function:
     *   - If no dice have been thrown, the list of kept dice must be empty
     *   - The function should check that the kept dice are actually in the original list (create a
     *     mutable copy of the list of dice and one by one try to remove the dice in keep from it,
     *     when removing fails that die can't be kept).
     *   - You probably want to sort the dice (using [List<Int>.sorted] and [List.toTypedArray])
     *   - You are only allowed 3 throws of the dice
     *
     * @param keep The list of the dice to keep.
     */
    override fun rollDice(keep: Iterable<Int>) {

        for (i in 0 until dice.size){
            dice[i]= generator.getNextDieThrow()
        }

        //making a copy list of the dice that have been thrown
        var copiedListOfDice =dice.toMutableList()

    do {
            if (copiedListOfDice.removeAll(keep))
            {
                var newDice=keep.toMutableList()
                if (newDice.size < 5)
                {
                    newDice.add(generator.getNextDieThrow())
                }
            } else throw Exception("That die can't be kept")
        }while (roundInTurn < 4)




        // Remember to use generator.getNextDieThrow() to get an individual new die throw.
    }

    /**
     * This function is used to trigger computer turns. This can be called on initial
     * game creation as well as after assigning the dice to a new group.
     */
    override fun playComputerTurns() {
        var current = currentPlayer
        while (current is ComputerPlayer && !isFinished) {
            current.playFullTurn(this)
            current = currentPlayer
        }
    }


    /**
     * Add a listener for update events
     */
    override fun addOnUpdateListener(listener: GameUpdateListener<StudentYahtzeeGame>) {
        if (listener !in gameUpdateListeners) gameUpdateListeners.add(listener)
    }

    /**
     * Remove a listener for update events
     */
    override fun removeOnUpdateListener(listener: GameUpdateListener<StudentYahtzeeGame>) {
        gameUpdateListeners.remove(listener)
    }

    /**
     * Add a listener for game finish events
     */
    override fun addGameFinishedListener(listener: GameFinishedListener<StudentYahtzeeGame>) {
        if (listener !in gameFinishedListeners) gameFinishedListeners.add(listener)
    }

    /**
     * Remove a listener for game finish events
     */
    override fun removeGameFinishedListener(listener: GameFinishedListener<StudentYahtzeeGame>) {
        gameFinishedListeners.remove(listener)
    }

    /**
     * Helper function to fire update events. Use this in [rollDice] and
     * [StudentPlayerGameState.applyDiceToGroup].
     */
    fun fireGameUpdate() {
        for (listener in gameUpdateListeners) {
            listener.onGameUpdated(this)
        }
    }

    /**
     * Helper function to fire the game finish event. Use this in and
     * [StudentPlayerGameState.applyDiceToGroup] once the game has finished.
     */
    fun fireGameFinished() {
        for (listener in gameFinishedListeners) {
            listener.onGameFinished(this)
        }
    }

    /**
     * This class contains the state for individual players. You probably want to create a private
     * property "scores" that is either an enum map or an array. Look at [getGroupScore] for the
     * details.
     */
    inner class StudentPlayerGameState : YahtzeeGame.PlayerGameState {



        /* TODO you probably want to have a property recording the scores for this player. Remember
           that a score of 0 is valid and different from a non-existing score (maybe you record that
           as `-1` or `null`
         */


        /**
         * This property calculates the upper subtotal of the scores. This is probably best a
         * calculated property (not with backing field). You may want to consider using
         * [ScoreGroup.isUpper], [Iterable.filter], [Iterable.mapNotNull] and [Iterable<Int>.sum]
         */
        override val upperSubTotal: Int
        get()
        {
            var values= ScoreGroup.values().filter { it.isUpper }



            /**for (value in ScoreGroup.values().filter { it.isUpper })
            {


            }*/

            return upperSubTotal

        }


        /**
         * The upper total is an adjustment of [upperSubTotal] where if the subtotal is 63 or more
         * an additional 25 point bonus is applied. This is probably best a calculated property, and
         * if [upperSubTotal] is calculated, this one should too.
         */
        override val upperTotal: Int
            get() = TODO("Calculate the full total of the upper table by applying the bonus if earned")

        /**
         * The total of the lower groups (you can use [ScoreGroup.isLower] for this). Again,
         * it is probably easiest to calculate this on demand.
         */
        override val lowerTotal: Int
            get() = TODO("Calculate the total of the lower table")

        /**
         * The total of the score of both upper and lower groups. Or just the overall total. Again
         * this would probably be a calculated property.
         */
        override val totalScore: Int
            get() = TODO("Calculate the overall total score")

        /**
         * This is a key function of the game. It takes the current [dice] property, checks that
         * the values are not null [Array<Any?>.requireNotNull], and updates the score for the
         * passed in score group.
         *
         * Note:
         *   - The dice use the values *1 to 6*, not 0 to 5
         *   - to keep this clean and do allow for code reuse you probably want to implement the
         *     part of this function that calculates the score as a big `when` statement that
         *     delegates to individual functions depending on the group (You may want to treat all
         *     the upper group ones the same, just pass along the target die value as parameter).
         *   - It is probably worth it to also count the amount of occurrences of specific die
         *     values. (This makes three/four of a kind and full house easier).
         *   - You want to check that the dice have been rolled at least once (it is not required to
         *     use all 3 possible throws).
         *   - Once a group has a score it is no longer possible to score the group again (you
         *     should check this, probably in common code before you actually handle getting the
         *     score.
         *   - This function should reset the [dice] as well as the [roundInTurn] variables.
         *   - You need to store the score in some sort of property of scores.
         *   - After you update the state you should:
         *     - call [fireGameUpdate] to inform listeners that the game state changed
         *     - check whether the game was finished, if so, update [isFinished] and then call
         *       [fireGameFinished].
         *
         * @param scoreGroup The group under which to score the current dice.
         */
        override fun applyDiceToGroup(scoreGroup: ScoreGroup) {
            TODO("Implement the actual code that applies the dice to a group and moves to the next turn")
        }

        /**
         * This function retrieves the score this particular user has for a group. Please note that
         * there is a difference between not set and a zero score. For this reason you want to
         * initialize the scores as null, and set them to a score once that group has been "played".
         */
        override fun getGroupScore(scoreGroup: ScoreGroup): Int? {
            return TODO("Return the score for the given group")
        }

    }

}
