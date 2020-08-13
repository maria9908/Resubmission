package uk.ac.bournemouth.ap.yahtzeebase.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.bournemouth.ap.yahtzeebase.lib.*
import uk.ac.bournemouth.ap.yahtzeebase.logic.StudentYahtzeeGame

class MainActivity : AppCompatActivity(), GameUpdateListener<StudentYahtzeeGame> {

    lateinit var scoreViews: Array<TextView>
    lateinit var scoreButtons: Array<Button>
    lateinit var dieViews: Array<ImageView>

    // TODO you want to have some sort of game property
    var game: StudentYahtzeeGame= StudentYahtzeeGame(players = listOf(HumanPlayer("Player 1"),
        HumanPlayer("Player 2")
    ))

    /** Simple accessor for the button associated with a scoregroup. */
    private val ScoreGroup.button: Button get() = scoreButtons[this.ordinal]
    /** Simple accessor for the value label associated with a scoregroup. */
    private val ScoreGroup.valueLabel: TextView get() = scoreViews[this.ordinal]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRollDice.setOnClickListener { doRollDice() }

        // TODO Listen to game update events (to update the views)
        // TODO Listen to game finish events (to be able to show a win dialog/lock the UI)

        // Just some convenient ways to get at the ui controls. As an array is so much easier than
        // as individual views.
        dieViews = arrayOf(die0, die1, die2, die3, die4)

        // Same for score buttons. They all work the same, so treat them the same and use an
        // array and loops for them.
        scoreButtons = Array(ScoreGroup.values().size) { groupNo ->
            when(ScoreGroup.values()[groupNo]) {
                ScoreGroup.ONES -> lblOnes
                ScoreGroup.TWOS -> lblTwos
                ScoreGroup.THREES -> lblThrees
                ScoreGroup.FOURS -> lblFours
                ScoreGroup.FIVES -> lblFives
                ScoreGroup.SIXES -> lblSixes
                ScoreGroup.THREE_OF_A_KIND -> lblThreeOfAKind
                ScoreGroup.FOUR_OF_A_KIND -> lblFourOfAKind
                ScoreGroup.FULL_HOUSE -> lblFullHouse
                ScoreGroup.SMALL_STRAIGHT -> lblSmallStraight
                ScoreGroup.LARGE_STRAIGHT -> lblLargeStraight
                ScoreGroup.YAHTZEE -> lblYahtzee
                ScoreGroup.CHANCE -> lblChance
            }
        }

        // Like the buttons the views that display the scores for specific groups need to be captured.
        scoreViews = Array(ScoreGroup.values().size) { groupNo ->
            when(ScoreGroup.values()[groupNo]) {
                ScoreGroup.ONES -> lblValOnes
                ScoreGroup.TWOS -> lblValTwos
                ScoreGroup.THREES -> lblValThrees
                ScoreGroup.FOURS -> lblValFours
                ScoreGroup.FIVES -> lblValFives
                ScoreGroup.SIXES -> lblValSixes
                ScoreGroup.THREE_OF_A_KIND -> lblValThreeOfAKind
                ScoreGroup.FOUR_OF_A_KIND -> lblValFourOfAKind
                ScoreGroup.FULL_HOUSE -> lblValFullHouse
                ScoreGroup.SMALL_STRAIGHT -> lblValSmallStraight
                ScoreGroup.LARGE_STRAIGHT -> lblValLargeStraight
                ScoreGroup.YAHTZEE -> lblValYahtzee
                ScoreGroup.CHANCE -> lblValChance
            }
        }

        // Make sure to listen to each button. The lambda allows us to pass along the group
        for(group in ScoreGroup.values()) {
            group.button.setOnClickListener { doChooseGroup(group) }
        }

        for(dieView in dieViews) {
            dieView.setOnClickListener {

                if (game.roundInTurn== 1 || game.roundInTurn == 2) {
                    it.isSelected = !it.isSelected
                }

            }
        }


        // TODO: Trigger the update once to init the view.
    }

    private fun doChooseGroup(scoreGroup: ScoreGroup) {
        TODO("Handle the choosing of the scoring group by calling the right function on the game")
    }

    private fun doRollDice() {
        TODO("Handle rolling dice, including determining which dice to keep if it is not round 0")
    }

    override fun onGameUpdated(game: StudentYahtzeeGame) {
        TODO("Update all the game display when the game is updated")
    }

    companion object {
         /*In onGameUpdated you may want to display the dice, using this array (and appropriate image
           resources makes that quite easy to do.

        val DIE_IMAGES = intArrayOf(R.drawable.ic_unknowndie, R.drawable.ic_die1, R.drawable.ic_die2, R.drawable.ic_die3,
                R.drawable.ic_die4, R.drawable.ic_die5, R.drawable.ic_die6)
        */

    }
}