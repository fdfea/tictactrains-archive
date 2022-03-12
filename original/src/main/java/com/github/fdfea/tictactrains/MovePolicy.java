package com.github.fdfea.tictactrains;

import java.util.Set;

class MovePolicy {

    private final boolean player;
    private final IdSet squareRule;

    MovePolicy(boolean player, IdSet squareRule) {
        this.player = player;
        this.squareRule = squareRule;
    }

    synchronized boolean getPlayer() {
        return player;
    }

    synchronized Set<String> getSquareIds() {
        return squareRule.getIds();
    }

}
