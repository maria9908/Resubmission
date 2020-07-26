package uk.ac.bournemouth.ap.yahtzeebase.testlib

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import uk.ac.bournemouth.ap.yahtzeebase.lib.*
import kotlin.random.Random

abstract class YahtzeeTestBase<T : YahtzeeGame<T>> {

    abstract fun newGame(players: List<Player>): T

    fun createGame(
        players: List<Player> = listOf(HumanPlayer("player 1"), HumanPlayer("2")),
        generator: Generator = RandomGenerator
    ): T {
        return newGame(players).also {
            it.generator = generator
        }
    }

    @ParameterizedTest(name = "playerCount= ({0})")
    @ValueSource(ints = [1, 2, 5, 7])
    fun getCurrentPlayer(playerCount: Int) {
        val players = (1..playerCount).map { HumanPlayer("player$it") }
        val game = newGame(players)

        assertTrue(game.currentPlayer in players, "The current player must be one of the players passed in")
    }

    @ParameterizedTest(name = "playerCount= ({0})")
    @ValueSource(ints = [1, 2, 5, 7])
    fun getPlayers(playerCount: Int) {
        val players = (1..playerCount).map { HumanPlayer("player$it") }
        val game = newGame(players)

        assertEquals(players, game.players, "The game should prove the passed in players as the players of the game")

        val otherPlayer = HumanPlayer("player1")
        assertFalse(otherPlayer in game.players, "Player names don't make it the same player")
    }

    @ParameterizedTest(name = "playerCount= ({0})")
    @ValueSource(ints = [1, 2, 5, 7])
    fun modifyPlayerList(playerCount: Int) {
        val players = (1..playerCount).map { HumanPlayer("player$it") }
        val playersCopy = players.toMutableList()
        val game = newGame(playersCopy)
        playersCopy[0] = HumanPlayer("cheater")

        assertNotEquals(playersCopy, game.players) {
            "The players property should not reflect the newly modified list. The constructor parameter should be copied."
        }

        assertEquals(players, game.players, "The game should prove the passed in players as the players of the game")
    }


    @Test
    fun getDice() {
        val game = createGame()

        assertArrayEquals(arrayOfNulls<Int?>(5), game.dice, "Initially there should not be any dice set")
    }

    @Test
    fun getState() {
        val game = createGame()

        assertEquals(0, game.roundInTurn, "When the game starts, the state is before the turn")
    }

    @Test
    fun getGenerator() {
        val generator = object : Generator {
            override fun getNextDieThrow(): Int = throw AssertionError("Just setting the generator should not trigger a die roll")
        }

        val game = createGame(generator = generator)

        assertEquals(generator, game.generator)
    }


    @ParameterizedTest
    @MethodSource("randomSinglePlayerGames")
    fun rollDice(turns: List<Turn>) {
        val game = createGame()
        val generator = TestTurnGenerator(turns)
        game.generator = generator
        game.rollDice()

        assertEquals(generator.currentTurn.rolls[0].expectedDice, game.dice.filterNotNull().sorted()) {
            "The rolled dice should be the expected ones from the generator"
        }
    }

    @ParameterizedTest
    @MethodSource("randomSinglePlayerGames")
    fun testRollDiceKeep(turns: List<Turn>) {
        val game = createGame()
        val generator = TestTurnGenerator(turns)
        generator.currentRollIdx = 0
        game.generator = generator
        game.rollDice()
        game.rollDice(generator.currentRoll.keep.toList())

        assertEquals(generator.currentTurn.rolls[1].expectedDice, game.dice.filterNotNull().sorted())
    }



    @ParameterizedTest
    @MethodSource("singleTurns")
    fun testInvalidKeepsInFirstTurn(turn: Turn) {
        val adjustedTurn = Turn (turn.rolls.sliceArray(1..2), turn.scoreGroup, turn.expectedScore)
        assertThrows<Exception>({
            "It is not valid to keep dice in the first turn. This should throw an exception:\n  Turn $adjustedTurn"
        }) {
            runSingleTurn(adjustedTurn)
        }
    }


    @ParameterizedTest
    @MethodSource("invalidKeepTurns")
    fun testInvalidKeeps(turn: Turn) {
        assertThrows<Exception>({
            "Turn $turn should not have a valid keep"
        }) {
            runSingleTurn(turn)
        }
    }

    @ParameterizedTest
    @MethodSource("singleTurns")
    fun testDisallowPlayTurnTwice(turn: Turn) {
        val game = createGame(listOf(HumanPlayer("Single human")))
        val generator = TestTurnGenerator(listOf(turn))
        game.generator = generator

        runSingleTurn(turn, game)

        game.rollDice() // should just give random numbers

        assertThrows<Exception>("It should not be possible to apply a group that has already been applied") {
            game.currentPlayerGameState.applyDiceToGroup(turn.scoreGroup)
        }
    }



    @ParameterizedTest
    @MethodSource("randomSinglePlayerGames")
    fun testRollFullTurn(turns: List<Turn>) {
        runSingleTurn(turns.first())
    }

    private fun runSingleTurn(turn: Turn, game: T = createGame(listOf(HumanPlayer("Single human")))) {
        val generator = TestTurnGenerator(listOf(turn))
        game.generator = generator

        assertEquals(0, game.roundInTurn, "Initially no dice should have been thrown")

        run {
            val roll = turn.rolls[0]
            if (roll.keep.isNotEmpty()) { // allow for this to be se
                game.rollDice(roll.keep.toList())
            } else {
                game.rollDice()
            }
        }
        assertEquals(1, game.roundInTurn, "After the first throw dice should have been thrown once")
        for (rollIdx in 1..2) {
            val roll = turn.rolls[rollIdx]
            if (roll.roll.isNotEmpty()) {
                game.rollDice(roll.keep.toList())
                assertEquals(1 + rollIdx, game.roundInTurn, "The dice are expected to have been thrown ${1 + rollIdx} times")

                assertEquals(roll.expectedDice, game.dice.requireNoNulls().sorted()) {
                    "The result after rolling a turn should be as expected"
                }
            } else {
                break
            }
        }
        assertNull(game.currentPlayerGameState.getGroupScore(turn.scoreGroup), "The score should not be set yet")
        game.currentPlayerGameState.applyDiceToGroup(turn.scoreGroup)

        assertEquals(turn.expectedScore, game.currentPlayerGameState.getGroupScore(turn.scoreGroup)) {
            "The score for the group should be as expected"
        }
    }

    @ParameterizedTest
    @MethodSource("randomSinglePlayerGames")
    fun testFullGame(turns: List<Turn>) {
        val game = createGame(listOf(HumanPlayer("SinglePlayer")))
        val generator = TestTurnGenerator(turns)
        game.generator = generator
        val testListener = TestGameListener(game)

        game.addGameFinishedListener(testListener)
        game.addOnUpdateListener(testListener)

        while (!generator.isFinished()/*! game.isFinished*/) {
            assertEquals(0, generator.currentRollIdx) {
                "When starting a turn the generator should not have rolled more than needed"
            }

            assertEquals(0, generator.currentDieIdx) {
                "When starting a turn the generator should not have rolled more than needed"
            }

            assertFalse(game.isFinished, "The game should not be finished at this point")
            assertFalse(testListener.onGameOverCalled, "Game over should not have been called")

            assertArrayEquals(arrayOfNulls(5), game.dice, "At the start of a turn the dice should have the null value / be reset")

            run {
                val changeCalledCount = testListener.onGameUpdateCalled
                game.rollDice()
                assertEquals(changeCalledCount + 1, testListener.onGameUpdateCalled) {
                    "Rolling the dice should trigger a change signal"
                }

            }
            val turn = generator.currentTurn

            for (rollIdx in 1..2) {
                val roll = turn.rolls[rollIdx]
                if (roll.roll.isNotEmpty()) {
                    run {
                        val changeCalledCount = testListener.onGameUpdateCalled
                        game.rollDice(roll.keep.toList())
                        assertEquals(changeCalledCount + 1, testListener.onGameUpdateCalled) {
                            "Rolling the dice should trigger a change signal"
                        }
                    }

                    assertEquals(roll.expectedDice, game.dice.requireNoNulls().sorted()) {
                        "The result after rolling a turn should be as expected"
                    }

                } else {
                    break
                }
            }

            val scoreGroup = turn.scoreGroup
            assertNull(game.currentPlayerGameState.getGroupScore(scoreGroup), "Before the turns finished")

            val lastDice = game.dice

            run {
                val changeCalledCount = testListener.onGameUpdateCalled
                game.currentPlayerGameState.applyDiceToGroup(scoreGroup)

                assertEquals(changeCalledCount + 1, testListener.onGameUpdateCalled) {
                    "Applying the dice to a group should trigger a change signal"
                }

            }

            assertEquals(turn.expectedScore, game.currentPlayerGameState.getGroupScore(turn.scoreGroup)) {
                """|The score for the group ${turn.scoreGroup} should be as expected
                   |    ${lastDice.joinToString(" ")} <-> ${turn.rolls.last()}""".trimMargin()
            }

        }

        assertTrue(testListener.onGameOverCalled, "Game over should have been called")
        assertTrue(game.isFinished)

        val upperSubScore = turns.filter { it.scoreGroup.isUpper }.sumBy { it.expectedScore }
        assertEquals(upperSubScore, game.currentPlayerGameState.upperSubTotal, "The upper subtotal should be correct")

        val upperScore = if (upperSubScore < 63) upperSubScore else upperSubScore + 25
        assertEquals(upperScore, game.currentPlayerGameState.upperTotal, "The upper total should be correct")

        val lowerScore = turns.filter { it.scoreGroup.isLower }.sumBy { it.expectedScore }
        assertEquals(lowerScore, game.currentPlayerGameState.lowerTotal, "The upper total should be correct")

        assertEquals(lowerScore + upperScore, game.currentPlayerGameState.totalScore) {
            "The total score should be as expected (the sum of upper and lower totals"
        }

        assertThrows<Exception>("It should not be possible to roll the dice on a finished game") {
            game.rollDice()
        }

    }

    @Test
    fun testInitialGameState() {
        val game = createGame()
        assertEquals(0, game.roundInTurn, "The game should start in the beforeturn state")

        for (playerInfo in game.playerGameState.values) {
            for (scoreGroup in ScoreGroup.values()) {
                assertNull(playerInfo.getGroupScore(scoreGroup), "Initially there should not be any score")
            }

            assertEquals(0, playerInfo.upperTotal, "The initial total for the upper group should be 0")

            assertEquals(0, playerInfo.upperSubTotal, "The initial subtotal (before bonus) for the upper group should be 0")

            assertEquals(0, playerInfo.lowerTotal, "The initial total for the lower group should be 0")

            assertEquals(0, playerInfo.totalScore, "The initial total should be 0")
        }

        assertArrayEquals(arrayOfNulls<Int?>(5), game.dice, "Initially there should not be any dice set")

    }


    @Suppress("unused")
    companion object {
        /**
         * Some turns that have invalid keeps
         */
        @JvmStatic
        fun invalidKeepTurns(): List<Turn> = listOf(
            Turn("2 3 3 6 6, (2 2 3 6) 4,  (2 3 4 6) 3", ScoreGroup.CHANCE, 18),
            Turn("1 3 4 4 6, (1 3 5) 1 4, (1 3 4) 3 6", ScoreGroup.THREES, 6),
            Turn("1 1 3 3 5, (1 1 1) 3 4, (1 3 4 5) 2", ScoreGroup.FIVES, 5),
            Turn("2 2 4 5 6, (2 3 5 6) 3, (2 3 4 5 6)", ScoreGroup.LARGE_STRAIGHT, 40),
            Turn("1 2 3 3 6, (1 2 3 4) 5, (3 3 3) 5 6", ScoreGroup.FOUR_OF_A_KIND, 0),
            Turn("2 4 5 5 6, (3) 1 6 5 6, (6 6) 2 4 5", ScoreGroup.SIXES, 12),
            Turn("1 3 5 5 6, (3 5 6) 2 4, (1 2 3 4) 5", ScoreGroup.FOURS, 4),
            Turn("1 1 2 3 6, (1 2 3) 2 5, (1 2 4) 3 2", ScoreGroup.TWOS, 2)
        )

        /**
         * This function will return a list of "games". Each game is a list of turns. Each turn has 3 die rolls (the kept dice between
         * parentheses), a ScoreGroup and the expected group for that.
         *
         * Note that these were generated with a rather crappy AI (not enough time to do it better, time is better used for code you will
         * see).
         */
        @JvmStatic
        fun randomSinglePlayerGames(): List<List<Turn>> {
            return listOf(
                listOf(
                    Turn("5 4 6 6 1, (6 6) 3 5 6, (6 6 6) 2 3", ScoreGroup.SIXES, 18),
                    Turn("2 1 3 6 6, (2 1 3) 1 2, (1 1 2 2) 2", ScoreGroup.FULL_HOUSE, 25),
                    Turn("1 2 1 3 2, (2 2) 4 3 5, (2 3 4 5) 3", ScoreGroup.SMALL_STRAIGHT, 30),
                    Turn("2 1 4 1 4, (4 4) 3 1 5, (4 4) 1 6 3", ScoreGroup.FOURS, 8),
                    Turn("5 1 5 6 5, (5 5 5) 2 4, (5 5 5) 5 4", ScoreGroup.FIVES, 20),
                    Turn("4 6 3 5 1, (3 4 5 6) 2, (2 3 4 5 6)", ScoreGroup.LARGE_STRAIGHT, 40),
                    Turn("3 6 3 3 3, (3 3 3 3) 4, (3 3 3 3) 4", ScoreGroup.THREES, 12),
                    Turn("1 1 2 3 4, (1 2 3 4) 4, (1 2 3 4) 1", ScoreGroup.CHANCE, 11),
                    Turn("1 5 6 6 6, (6 6 6) 2 6, (6 6 6 6) 5", ScoreGroup.ONES, 0),
                    Turn("1 3 5 5 5, (5 5 5) 1 2, (2) 2 2 3 5", ScoreGroup.TWOS, 6),
                    Turn("1 2 4 5 5, (1 2 4) 1 4, (1 2 4) 1 5", ScoreGroup.THREE_OF_A_KIND, 0),
                    Turn("2 3 4 5 5, (2 3 4 5) 4, (2 3 4 5) 1", ScoreGroup.FOUR_OF_A_KIND, 0),
                    Turn("4 5 5 6 6, (6 6) 1 2 5, (6 6) 2 3 5", ScoreGroup.YAHTZEE, 0)
                ),
                listOf(
                    Turn("1 4 6 6 6, (6 6 6) 1 5, (6 6 6) 2 6", ScoreGroup.SIXES, 24),
                    Turn("1 3 4 5 6, (1 3 4 5) 1, (1 3 4 5) 3", ScoreGroup.CHANCE, 16),
                    Turn("2 2 3 5 6, (2 3 5 6) 6, (2 3 5 6) 6", ScoreGroup.FIVES, 5),
                    Turn("1 2 3 4 6, (1 2 3 4) 4, (1 2 3 4) 5", ScoreGroup.FOURS, 4),
                    Turn("1 1 3 4 6, (1 3 4) 3 5, (1 3 4 5) 6", ScoreGroup.THREES, 3),
                    Turn("3 3 4 5 6, (3 4 5 6) 4, (3 4 5 6) 3", ScoreGroup.ONES, 0),
                    Turn("1 5 5 6 6, (5 5 6 6) 6, (5 5 6 6 6)", ScoreGroup.FULL_HOUSE, 25),
                    Turn("2 4 4 5 5, (2 4 5) 4 5, (2 4 5) 3 3", ScoreGroup.TWOS, 2),
                    Turn("2 4 4 5 5, (2 4 5) 2 3, (2 3 4 5) 4", ScoreGroup.THREE_OF_A_KIND, 0),
                    Turn("1 1 4 4 6, (4 4) 2 2 6, (2 4 6) 4 4", ScoreGroup.FOUR_OF_A_KIND, 0),
                    Turn("4 5 5 6 6, (4 5 6) 1 1, (1 4 5) 2 5", ScoreGroup.SMALL_STRAIGHT, 0),
                    Turn("5 5 5 6 6, (5 5 5) 2 5, (5 5 5 5) 2", ScoreGroup.LARGE_STRAIGHT, 0),
                    Turn("1 2 3 5 6, (6) 1 3 4 4, (4 4) 4 5 6", ScoreGroup.YAHTZEE, 0)

                ),
                listOf(
                    Turn("1 2 2 3 5, (1 2 3 5) 6, (1 2 3 5) 1", ScoreGroup.CHANCE, 12),
                    Turn("2 4 4 5 5, (2 4 5) 2 4, (2 4 5) 4 6", ScoreGroup.FOURS, 8),
                    Turn("2 3 4 6 6, (2 3 4 6) 3, (2 3 4 6) 5", ScoreGroup.SIXES, 6),
                    Turn("1 1 2 4 4, (1 2 4) 2 6, (1 2 4) 1 2", ScoreGroup.TWOS, 4),
                    Turn("1 2 5 5 6, (1 2 5) 5 5, (1 2 5) 4 5", ScoreGroup.FIVES, 10),
                    Turn("2 5 5 6 6, (2 5 6) 1 4, (1 2 4 5) 6", ScoreGroup.ONES, 1),
                    Turn("1 1 3 4 6, (1 3 4) 1 5, (1 3 4 5) 4", ScoreGroup.THREES, 3),
                    Turn("2 4 4 6 6, (2 4 6) 3 4, (2 3 4 6) 3", ScoreGroup.THREE_OF_A_KIND, 0),
                    Turn("1 1 2 3 4, (1 2 3 4) 2, (1 2 3 4) 6", ScoreGroup.FOUR_OF_A_KIND, 0),
                    Turn("2 3 3 4 5, (2 3 4 5) 4, (2 3 4 5) 1", ScoreGroup.FULL_HOUSE, 0),
                    Turn("2 5 5 6 6, (2 5 6) 2 4, (2 4 5 6) 6", ScoreGroup.SMALL_STRAIGHT, 0),
                    Turn("2 2 3 3 4, (2 3 4) 5 5, (2 3 4 5) 6", ScoreGroup.LARGE_STRAIGHT, 40),
                    Turn("2 2 2 5 6, (2 2 2) 2 2, (2 2 2 2 2)", ScoreGroup.YAHTZEE, 50)
                )
            )
        }

        @JvmStatic
        fun singleTurns(): List<Turn> = randomSinglePlayerGames().flatten()
    }
}

/**
 * Helper class to generate dice throws from the passed in turns. This allows or predictable testing.
 */
class TestTurnGenerator(val turns: List<Turn>) : Generator {
    var currentTurnIdx = 0
        private set
    val currentTurn: Turn get() = turns[currentTurnIdx]
    var currentRollIdx = 0
    val currentRoll get() = currentTurn.rolls[currentRollIdx]
    var currentDieIdx = 0

    override fun getNextDieThrow(): Int {
        if (currentDieIdx < 0 || currentRollIdx < 0) return Random.nextInt(6)
        return currentRoll.roll[currentDieIdx].also {
            if (++currentDieIdx >= currentRoll.roll.size) {
                currentDieIdx = 0
                do {
                    ++currentRollIdx
                } while (currentRollIdx < currentTurn.rolls.size && currentRoll.roll.isEmpty())
                if (currentRollIdx >= currentTurn.rolls.size) {
                    currentDieIdx = -1// Invalid index should trigger exception on next throw
                    currentRollIdx = -1
                    currentTurnIdx++
                    if (currentTurnIdx < turns.size) {
                        while ((currentTurnIdx + 1 < turns.size) && currentTurn.rolls.isEmpty()) {
                            ++currentTurnIdx
                        }

                    }
                    if (currentTurnIdx < turns.size) {
                        currentRollIdx = 0
                        currentDieIdx = 0
                    }
                }
            }
        }
    }

    fun isFinished(): Boolean {
        return currentTurnIdx >= turns.size
    }
}

/**
 * Class representing a single die roll.It has a toString that outputs
 * the roll in the format used by the [Turn] string constructor.
 */
data class DieRoll(val keep: Array<Int>, val roll: Array<Int>) {
    val expectedDice: List<Int> get() = (keep.asSequence() + roll.asSequence()).sorted().toList()

    override fun toString(): String {
        return when {
            keep.size == 0 -> roll.joinToString(" ")
            roll.size == 0 -> keep.joinToString(" ", prefix = "(", postfix = ")")
            else -> buildString {
                keep.joinTo(this, " ", "(", ") ")
                roll.joinTo(this, " ")
            }
        }
    }
}

/**
 * The turn class represents an entire turn. While there is a constructor that takes an array of rolls, there is a secondary constructor
 * that does allow for initializing the rolls using a (much more compact) string.
 */
data class Turn(val rolls: Array<DieRoll>, val scoreGroup: ScoreGroup, val expectedScore: Int) {
    constructor(rolls: String, category: ScoreGroup, expectedScore: Int) : this(
        rolls.toRolls(),
        category,
        expectedScore
    )


    override fun toString(): String {
        return rolls.joinToString(
            prefix = "Turn(\"",
            postfix = "\", ScoreGroup.$scoreGroup, $expectedScore)"
        )
    }

    companion object {

        fun String.toRolls(): Array<DieRoll> {
            return split(",").map { rollStr ->
                val parenOpenIdx = rollStr.indexOf('(')
                val keepStr: String
                val newStr: String
                if (parenOpenIdx >= 0) {
                    val parenCloseIdx = rollStr.indexOf(')')
                    keepStr = rollStr.substring(parenOpenIdx + 1, parenCloseIdx)
                    newStr =
                        rollStr.substring(0, parenOpenIdx) + rollStr.substring(parenCloseIdx + 1)
                } else {
                    keepStr = ""
                    newStr = rollStr
                }
                val keep = keepStr.split(" ")
                    .filter(String::isNotEmpty)
                    .map(String::toInt)
                    .toTypedArray()

                val new = newStr.split(" ")
                    .filter(String::isNotEmpty)
                    .map(String::toInt)
                    .toTypedArray()

                DieRoll(keep, new)
            }.toTypedArray()
        }
    }
}


/**
 * Game listener implementation that counts the invocation count (for game change) and makes sure
 * that game over is only called once.
 */
class TestGameListener<G : YahtzeeGame<G>>(val expectedGame: G) : GameUpdateListener<G>,
    GameFinishedListener<G> {

    var onGameUpdateCalled = 0
    var onGameOverCalled = false

    override fun onGameUpdated(game: G) {
        assertEquals(
            expectedGame,
            game,
            "The game passed to the listener should be the game created for the test"
        )

        assertEquals(
            onGameOverCalled,
            game.isFinished,
            "When the game is finished, either the isFinished property should be set after the game update is called, or the update should be called after onGameFinished"
        )

        onGameUpdateCalled++
    }

    override fun onGameFinished(game: G) {
        assertEquals(
            expectedGame,
            game,
            "The game passed to the listener should be the game that was created for the test"
        )
        assertEquals(false, onGameOverCalled, "A game can not be over twice")
        assertTrue(game.isFinished, "When onGameFinished is called, the isFinished property should return true")
        onGameOverCalled = true
    }
}