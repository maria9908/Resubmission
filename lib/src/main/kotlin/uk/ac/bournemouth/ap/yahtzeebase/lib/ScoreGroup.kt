package uk.ac.bournemouth.ap.yahtzeebase.lib

enum class ScoreGroup(val isUpper: Boolean) {
    ONES(true),
    TWOS(true),
    THREES(true),
    FOURS(true),
    FIVES(true),
    SIXES(true),
    THREE_OF_A_KIND(false),
    FOUR_OF_A_KIND(false),
    FULL_HOUSE(false),
    SMALL_STRAIGHT(false),
    LARGE_STRAIGHT(false),
    YAHTZEE(false),
    CHANCE(false);

    val isLower: Boolean get() = ! isUpper
}