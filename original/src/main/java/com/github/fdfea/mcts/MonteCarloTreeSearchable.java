package com.github.fdfea.mcts;

import java.util.List;

public interface MonteCarloTreeSearchable {

    //you also need to implement equals correctly

    //for debugging MCTS with getStats()
    String getStateString();

    //@return game is finished
    boolean isFinished();

    //@return current player (2 players)
    boolean getPlayer();

    //@return current move
    int getMove();

    //@return simulate a semi-random playout of the game from the current state and return the score
    //@param player to score for
    //must return a value between 0 and 1 for the result or evaluation at final position
    double simulatePlayout(boolean player);

    //@return list of next possible state of the game
    //@param type for state cast
    <T extends MonteCarloTreeSearchable> List<T> getNextStates(Class<T> type);

}
