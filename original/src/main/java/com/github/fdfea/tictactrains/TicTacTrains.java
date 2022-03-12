package com.github.fdfea.tictactrains;

import java.util.List;
import java.util.ArrayList;

class TicTacTrains {

    static final int ROWS = 7;
    static final int COLUMNS = 7;

    private Board board;
    private List<String> moves;
    private List<MovePolicy> rules;

    TicTacTrains(List<MovePolicy> rules) {
        this.rules = rules;
        this.board = new Board(rules);
        this.moves = new ArrayList<>();
    }

    void makeMove(String id) {
        board.makeMove(id);
        moves.add(id);
    }

    int getMove() {
        return moves.size();
    }

    Board getBoard() {
        return board;
    }

    List<MovePolicy> getRules() {
        return rules;
    }

    boolean hasStarted() {
        return moves.size() == 0;
    }

    String getLastMove() {
        return moves.get(moves.size()-1);
    }

    public String toString() {
        int move = 1;
        StringBuilder moveString = new StringBuilder();
        boolean startedMove = false;
        for(int i = 0; i < moves.size(); i++) {
            if(rules.get(i).getPlayer() && !startedMove) {
                moveString.append(move).append(". ").append(moves.get(i)).append(" ");
                move++;
                startedMove = true;
            } else {
                moveString.append(moves.get(i)).append(" ");
                if(!rules.get(i).getPlayer() && i+1 < moves.size() && rules.get(i+1).getPlayer()) {
                    startedMove = false;
                    if((move - 1) % TicTacTrains.ROWS == 0) moveString.append("\n");
                }
            }
        }
        return moveString.toString();
    }

    void print() {
        System.out.println();
        System.out.println(toString());
        System.out.println(board.toString());
        System.out.println();
    }

}
