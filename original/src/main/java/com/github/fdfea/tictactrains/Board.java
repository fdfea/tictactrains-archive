package com.github.fdfea.tictactrains;

import com.github.fdfea.mcts.MonteCarloTreeSearch;
import com.github.fdfea.mcts.MonteCarloTreeSearchable;

import java.util.*;

public class Board implements MonteCarloTreeSearchable {

    private static final int ROWS = 7;
    private static final int COLUMNS = 7;
    private static final double WIN = 1.0;
    private static final double DRAW = 0.5;
    private static final double LOSS = 0.0;
    private static final char EMPTY = ' ';
    private static final char X = 'X';
    private static final char O = 'O';
    private static final boolean onlyAdjacent = true;

    private char[] data;
    private int move;
    private int lastMoveIndex;
    private List<MovePolicy> rules;

    Board(List<MovePolicy> rules) {
        this.data = new char[ROWS*COLUMNS];
        this.move = 0;
        this.lastMoveIndex = -1;
        this.rules = rules;
        Arrays.fill(data, EMPTY);
    }

    Board(Board board) {
        this.data = board.data.clone();
        this.move = board.move;
        this.lastMoveIndex = board.lastMoveIndex;
        this.rules = board.rules;
    }

    @Override
    public boolean getPlayer() {
        return !isFinished() && rules.get(move).getPlayer();
    }

    @Override
    public boolean isFinished() {
        return move >= ROWS*COLUMNS;
    }

    @Override
    public int getMove() {
        return move;
    }

    @Override
    public double simulatePlayout(boolean player) {
        Board boardCopy = new Board(this);
        Random rand = new Random();
        while (!boardCopy.isFinished()) {
            boardCopy.randomMove(rand, onlyAdjacent);
        }
        int score = boardCopy.score();
        return (score == 0) ? DRAW : ((score > 0 && player) || (score < 0 && !player) ? WIN : LOSS);
    }

    char[] getData() {
        return data;
    }

    //for data generation
    public double simulatePlayout(boolean player, boolean onlyAdjacent) {
        Random rand = new Random();
        while (!isFinished()) {
            randomMove(rand, onlyAdjacent);
        }
        int score = score();
        return (score == 0) ? DRAW : ((score > 0 && player) || (score < 0 && !player) ? WIN : LOSS);
    }

    void randomPlayout(Random rand, boolean onlyAdjacent) {
        while (!isFinished()) {
            randomMove(rand, onlyAdjacent);
        }
    }

    void simulatedPlayout(int simulations) {
        MonteCarloTreeSearch<Board> mctsX = new MonteCarloTreeSearch<>(new Board(this), Board.class);
        MonteCarloTreeSearch<Board> mctsO = new MonteCarloTreeSearch<>(new Board(this), Board.class);
        while (!isFinished()) {
            Board nextState;
            if (getPlayer()) {
                mctsX.simulate(simulations);
                nextState = mctsX.getNextState();
            } else {
                mctsO.simulate(simulations);
                nextState = mctsO.getNextState();
            }
            makeMove(nextState.getLastMoveId());
            mctsX.shiftRoot(new Board(this));
            mctsO.shiftRoot(new Board(this));
        }
        System.out.print("\n");
    }

    void playOpening(List<String> ids) throws IllegalStateException {
        if (move != 0) {
            throw new IllegalStateException();
        }
        for (String id : ids) {
            if (!isIdValid(id)) {
                throw new IllegalArgumentException();
            }
            makeMove(id);
        }
    }

    @Override
    public <T extends MonteCarloTreeSearchable> List<T> getNextStates(Class<T> type) {
        List<T> nextStateNodes = new ArrayList<>();
        getNextValidMoves(onlyAdjacent).forEach(id -> {
            Board boardCopy = new Board(this);
            boardCopy.makeMove(id);
            T state = type.cast(boardCopy);
            nextStateNodes.add(state);
        });
        return nextStateNodes;
    }

    private void randomMove(Random rand, boolean onlyAdjacent) {
        List<String> nextValidMoves = getNextValidMoves(onlyAdjacent);
        String randomId = nextValidMoves.get(rand.nextInt(nextValidMoves.size()));
        makeMove(randomId);
    }

    List<String> getNextValidMoves(boolean onlyAdjacent) {
        List<String> validMoves = new ArrayList<>();
        List<String> validAdjacentMoves = new ArrayList<>();
        rules.get(move).getSquareIds().forEach(id -> {
            int index = idToIndex(id);
            if(isEmpty(index)) {
                validMoves.add(id);
                if(onlyAdjacent && hasAdjacentNotEmpty(index)) {
                    validAdjacentMoves.add(id);
                }
            }
        });
        if(onlyAdjacent && !validAdjacentMoves.isEmpty()) return validAdjacentMoves;
        else return validMoves;
    }

    private boolean hasAdjacentNotEmpty(int index) {
        return ((index % COLUMNS > 0 && !isEmpty(index-1)) ||
                (index % COLUMNS < COLUMNS-1 && !isEmpty(index+1)) ||
                (index >= COLUMNS && !isEmpty(index-COLUMNS)) ||
                (index < ROWS*(COLUMNS-1) && !isEmpty(index+COLUMNS)) ||
                (index % COLUMNS > 0 && index >= COLUMNS && !isEmpty(index-1-COLUMNS)) ||
                (index % COLUMNS > 0 && index < ROWS*(COLUMNS-1) && !isEmpty(index-1+COLUMNS)) ||
                (index % COLUMNS < COLUMNS-1 && index >= COLUMNS && !isEmpty(index+1-COLUMNS)) ||
                (index % COLUMNS < COLUMNS-1 && index < ROWS*(COLUMNS-1) && !isEmpty(index+1+COLUMNS))
        );
    }

    @Override
    public String getStateString() {
        return indexToId(lastMoveIndex);
    }

    String getLastMoveId()  {
        return indexToId(lastMoveIndex);
    }

    private static String indexToId(int index) {
        return String.valueOf(new char[]{(char)((index % ROWS)+'a'),(char)((ROWS-(index/ROWS))+'0')});
    }

    private static int idToIndex(String id) {
        return ROWS*(ROWS-(id.charAt(1)-'0'))+(id.charAt(0)-'a');
    }

    static boolean isIdValid(String id) {
        if(id.length() == 2) {
            int index = (id.charAt(0) - 'a') + ROWS * (id.charAt(1) - '0' - 1);
            return index >= 0 && index < ROWS*COLUMNS;
        }
        return false;
    }

    void makeMove(String id) {
        int index = idToIndex(id);
        data[index] = getPlayer() ? X : O;
        lastMoveIndex = index;
        move++;
    }

    private boolean isEmpty(int index) {
        return data[index] == EMPTY;
    }

    int score() {
        int xScore = 0, oScore = 0;
        for(int i = 0; i < data.length; i++) {
            if(!isEmpty(i)) {
                int sqScore = longestPath(i, new boolean[ROWS*COLUMNS]);
                if (data[i] == X && sqScore > xScore) xScore = sqScore;
                else if (data[i] == O && sqScore > oScore) oScore = sqScore;
            }
        }
        return xScore - oScore;
    }

    private int longestPath(int index, boolean[] checked) {
        int leftLen = 1, rightLen = 1, topLen = 1, bottomLen = 1;
        char piece = data[index];
        checked[index] = true;
        if(index % COLUMNS > 0 && data[index-1] == piece && !checked[index-1])
            leftLen += longestPath(index-1, checked);
        if(index % COLUMNS < COLUMNS-1 && data[index+1] == piece && !checked[index+1])
            rightLen += longestPath(index+1, checked);
        if(index >= COLUMNS && data[index-COLUMNS] == piece && !checked[index-COLUMNS])
            topLen += longestPath(index-COLUMNS, checked);
        if(index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece && !checked[index+COLUMNS])
            bottomLen += longestPath(index+COLUMNS, checked);
        checked[index] = false;
        return Math.max(Math.max(Math.max(leftLen, rightLen), topLen), bottomLen);
    }

    private long toLong() {
        long board = 0L;
        if (!isFinished()) throw new IllegalStateException("Board is not finished");
        for (int i = 0; i < ROWS * COLUMNS; i++) {
            if (data[i] == 'X') {
                board |= 1L << i;
            }
        }
        return board;
    }

    String toBinaryString() {
        if (!isFinished()) throw new IllegalStateException("Board is not finished");
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : getData()) {
            stringBuilder.append(c == 'X' ? 1 : 0);
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Board) {
            Board b = (Board) obj;
            for(int i = 0; i < ROWS*COLUMNS; i++) {
                if(data[i] != b.data[i]) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder boardString = new StringBuilder();
        for (int i = 0; i < ROWS*COLUMNS; i++) {
            if(i%ROWS == 0) boardString.append(ROWS-i/ROWS).append(" ");
            if((i+1)%COLUMNS == 0) boardString.append("[").append(data[i]).append("]\n");
            else boardString.append("[").append(data[i]).append("]");
        }
        boardString.append("& ");
        for(int col = 0; col < COLUMNS; col++) boardString.append(" ").append((char)('a'+col)).append(" ");
        return boardString.toString();
    }

    FinishedBoardData getOneFinishedBoardData() throws IllegalStateException {
        if (!isFinished()) {
            //only collect data on finished boards
            throw new IllegalStateException();
        }
        long boardLong = toLong();
        FinishedBoardData boardDataX = new FinishedBoardData(boardLong);
        FinishedBoardData boardDataO = new FinishedBoardData(boardLong);
        boolean[] checked = new boolean[ROWS*COLUMNS];
        int xScore = 0, oScore = 0;
        for(int i = 0; i < data.length; i++) {
            if(!isEmpty(i)) {
                int sqScore = longestPath(i, new boolean[ROWS*COLUMNS]);
                if (data[i] == X) {
                    if (!checked[i]) {
                        extractFinishedBoardData(boardDataX, i, checked);
                    }
                    if (sqScore > xScore) {
                        xScore = sqScore;
                    }
                }
                else if (data[i] == O) {
                    if (!checked[i]) {
                        extractFinishedBoardData(boardDataO, i, checked);
                    }
                    if (sqScore > oScore) {
                        oScore = sqScore;
                    }
                }
            }
        }
        boardDataX.setLongestPath(true, xScore);
        boardDataO.setLongestPath(false, oScore);
        boardDataX.merge(boardDataO, false);
        return boardDataX;
    }

    List<FinishedBoardArea> getFinishedBoardAreas() throws IllegalStateException {
        if (!isFinished()) {
            //only collect data on finished boards
            throw new IllegalStateException();
        }
        List<FinishedBoardArea> boardAreas = new ArrayList<>();
        boolean[] checked = new boolean[ROWS*COLUMNS];
        for(int i = 0; i < data.length; i++) {
            if(!isEmpty(i) && !checked[i]) {
                FinishedBoardArea boardArea = new FinishedBoardArea();
                boolean[] areaIndices = new boolean[ROWS*COLUMNS];
                int longestPath = extractFinishedBoardArea(boardArea, i, areaIndices);
                for (int j = 0; j < areaIndices.length; j++)
                {
                    if (areaIndices[j]) {
                        checked[j] = true;
                        int tmpPath = longestPath(j, new boolean[ROWS * COLUMNS]);
                        if (tmpPath > longestPath) {
                            longestPath = tmpPath;
                        }
                    }
                }
                boardArea.setLongestPath(longestPath);
                boardAreas.add(boardArea);
            }
        }
        return boardAreas;
    }

    List<BoardAreaNData> getBoardNDatas() throws IllegalStateException {
        if (!isFinished()) {
            throw new IllegalStateException();
        }
        List<BoardAreaNData> nDatas = new ArrayList<>();
        boolean[] checked = new boolean[ROWS*COLUMNS];
        for(int i = 0; i < data.length; i++) {
            if(!isEmpty(i) && !checked[i]) {
                BoardAreaNData nData = new BoardAreaNData();
                boolean[] areaIndices = new boolean[ROWS*COLUMNS];
                //this only gets the longest path from given index!!!???
                int longestPath = extractBoardAreaNData(nData, i, areaIndices);
                for (int j = 0; j < areaIndices.length; j++)
                {
                    if (areaIndices[j]) {
                        int tmpPath = longestPath(j, new boolean[ROWS * COLUMNS]);
                        if (tmpPath > longestPath) {
                            longestPath = tmpPath;
                        }
                        checked[j] = true;
                    }
                }
                nData.longestPath = (byte) longestPath;
                nDatas.add(nData);
            }
        }
        return nDatas;
    }

    FinishedBoardData getFinishedBoardData() throws IllegalStateException {
        if (!isFinished()) {
            //only collect data on finished boards
            throw new IllegalStateException();
        }
        int xScore = 0, oScore = 0, xIndex = 0, oIndex = 0;
        for(int i = 0; i < data.length; i++) {
            if(!isEmpty(i)) {
                int sqScore = longestPath(i, new boolean[ROWS*COLUMNS]);
                if (data[i] == X && sqScore > xScore) {
                    xScore = sqScore;
                    xIndex = i; //index of longest X path
                }
                else if (data[i] == O && sqScore > oScore) {
                    oScore = sqScore;
                    oIndex = i; //index of longest O path
                }
            }
        }
        long boardLong = toLong();
        //harvest data for longest path of both X and O
        FinishedBoardData boardDataX = new FinishedBoardData(boardLong);
        boardDataX.setLongestPath(true, xScore);
        extractFinishedBoardData(boardDataX, xIndex, new boolean[ROWS*COLUMNS]);
        FinishedBoardData boardDataO = new FinishedBoardData(boardLong);
        boardDataO.setLongestPath(false, oScore);
        extractFinishedBoardData(boardDataO, oIndex, new boolean[ROWS*COLUMNS]);
        boardDataX.merge(boardDataO, false);
        return boardDataX;
    }

    private void extractFinishedBoardData(FinishedBoardData boardData, int index, boolean[] checked) {
        char piece = data[index];
        checked[index] = true;
        boardData.incrementData(piece == 'X', getIndexRank(index));
        if (index % COLUMNS > 0 && data[index-1] == piece && !checked[index-1])
            extractFinishedBoardData(boardData, index - 1, checked);
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece && !checked[index+1])
            extractFinishedBoardData(boardData, index + 1, checked);
        if (index >= COLUMNS && data[index-COLUMNS] == piece && !checked[index-COLUMNS])
            extractFinishedBoardData(boardData, index - COLUMNS, checked);
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece && !checked[index+COLUMNS])
            extractFinishedBoardData(boardData, index + COLUMNS, checked);
    }

    private int extractFinishedBoardArea(FinishedBoardArea boardArea, int index, boolean[] checked) {
        char piece = data[index];
        int longestPath = longestPath(index, new boolean[ROWS*COLUMNS]);
        checked[index] = true;
        boardArea.incrementData(getIndexRank(index));
        //boardArea.incrementNData(getIndexNRank(index));
        if (index % COLUMNS > 0 && data[index-1] == piece && !checked[index-1])
            longestPath = Math.max(longestPath, extractFinishedBoardArea(boardArea, index - 1, checked));
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece && !checked[index+1])
            longestPath = Math.max(longestPath, extractFinishedBoardArea(boardArea, index + 1, checked));
        if (index >= COLUMNS && data[index-COLUMNS] == piece && !checked[index-COLUMNS])
            longestPath = Math.max(longestPath, extractFinishedBoardArea(boardArea, index - COLUMNS, checked));
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece && !checked[index+COLUMNS])
            longestPath = Math.max(longestPath, extractFinishedBoardArea(boardArea, index + COLUMNS, checked));
        return longestPath;
    }

    private int extractBoardAreaNData(BoardAreaNData area, int index, boolean[] checked) {
        char piece = data[index];
        int longestPath = longestPath(index, new boolean[ROWS*COLUMNS]);
        checked[index] = true;
        area.incrementData(determineSquareNType(index));
        if (index % COLUMNS > 0 && data[index-1] == piece && !checked[index-1])
            longestPath = Math.max(longestPath, extractBoardAreaNData(area, index - 1, checked));
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece && !checked[index+1])
            longestPath = Math.max(longestPath, extractBoardAreaNData(area, index + 1, checked));
        if (index >= COLUMNS && data[index-COLUMNS] == piece && !checked[index-COLUMNS])
            longestPath = Math.max(longestPath, extractBoardAreaNData(area, index - COLUMNS, checked));
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece && !checked[index+COLUMNS])
            longestPath = Math.max(longestPath, extractBoardAreaNData(area, index + COLUMNS, checked));
        return longestPath;
    }

    //determine n-type (0-31), checking for flips and rotations
    private int determineSquareNType(int index) {
        //determine neighbor count
        char piece = data[index];
        int neighborCount = 0;
        long valid = 0L;
        valid = setbit64(valid, index);
        short nArea = 0;
        nArea = setbit16(nArea, 4);
        //check left
        if (index % COLUMNS > 0 && data[index-1] == piece && !testbit64(valid, index-1)) {
            valid = setbit64(valid, index-1);
            nArea = setbit16(nArea, 3);
            neighborCount++;
            //check left-top
            if (index >= COLUMNS && data[index-1-COLUMNS] == piece && !testbit64(valid, index-1-COLUMNS)) {
                valid = setbit64(valid, index-1-COLUMNS);
                nArea = setbit16(nArea, 0);
                neighborCount++;
            }
            //check left-bottom
            if (index < ROWS*(COLUMNS-1) && data[index-1+COLUMNS] == piece && !testbit64(valid, index-1+COLUMNS)) {
                valid = setbit64(valid, index-1+COLUMNS);
                nArea = setbit16(nArea, 6);
                neighborCount++;
            }
        }
        //check right
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece && !testbit64(valid, index+1)) {
            valid = setbit64(valid, index+1);
            nArea = setbit16(nArea, 5);
            neighborCount++;
            //check right-top
            if (index >= COLUMNS && data[index+1-COLUMNS] == piece && !testbit64(valid, index+1-COLUMNS)) {
                valid = setbit64(valid, index+1-COLUMNS);
                nArea = setbit16(nArea, 2);
                neighborCount++;
            }
            //check right-bottom
            if (index < ROWS*(COLUMNS-1) && data[index+1+COLUMNS] == piece && !testbit64(valid, index+1+COLUMNS)) {
                valid = setbit64(valid, index+1+COLUMNS);
                nArea = setbit16(nArea, 8);
                neighborCount++;
            }
        }
        //check top
        if (index >= COLUMNS && data[index-COLUMNS] == piece && !testbit64(valid, index-COLUMNS)) {
            valid = setbit64(valid, index-COLUMNS);
            nArea = setbit16(nArea, 1);
            neighborCount++;
            //check top-left
            if (index % COLUMNS > 0 && data[index-COLUMNS-1] == piece && !testbit64(valid, index-COLUMNS-1)) {
                valid = setbit64(valid, index-COLUMNS-1);
                nArea = setbit16(nArea, 0);
                neighborCount++;
            }
            //check top-right
            if (index % COLUMNS < COLUMNS-1 && data[index-COLUMNS+1] == piece && !testbit64(valid, index-COLUMNS+1)) {
                valid = setbit64(valid, index-COLUMNS+1);
                nArea = setbit16(nArea, 2);
                neighborCount++;
            }
        }
        //check bottom
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece && !testbit64(valid, index+COLUMNS)) {
            valid = setbit64(valid, index+COLUMNS);
            nArea = setbit16(nArea, 7);
            neighborCount++;
            //check bottom-left
            if (index % COLUMNS > 0 && data[index+COLUMNS-1] == piece && !testbit64(valid, index+COLUMNS-1)) {
                valid = setbit64(valid, index+COLUMNS-1);
                nArea = setbit16(nArea, 6);
                neighborCount++;
            }
            //check bottom-right
            if (index % COLUMNS < COLUMNS-1 && data[index+COLUMNS+1] == piece && !testbit64(valid, index+COLUMNS+1)) {
                valid = setbit64(valid, index+COLUMNS+1);
                nArea = setbit16(nArea, 8);
                neighborCount++;
            }
        }
        //classify n-type
        int nType = -1;
        switch (neighborCount) {
            case 0: {
                if (BoardAreaNData.NTypes.eN0_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN0_1.type;
                } else {
                    System.out.println("Unknown N0 type: " + nArea);
                }
                break;
            }
            case 1: {
                if (BoardAreaNData.NTypes.eN1_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN1_1.type;
                } else {
                    System.out.println("Unknown N1 type: " + nArea);
                }
                break;
            }
            case 2: {
                if (BoardAreaNData.NTypes.eN2_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN2_1.type;
                } else if (BoardAreaNData.NTypes.eN2_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN2_2.type;
                } else if (BoardAreaNData.NTypes.eN2_3.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN2_3.type;
                } else {
                    System.out.println("Unknown N2 type: " + nArea);
                }
                break;
            }
            case 3: {
                if (BoardAreaNData.NTypes.eN3_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN3_1.type;
                } else if (BoardAreaNData.NTypes.eN3_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN3_2.type;
                } else if (BoardAreaNData.NTypes.eN3_3.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN3_3.type;
                } else if (BoardAreaNData.NTypes.eN3_4.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN3_4.type;
                } else if (BoardAreaNData.NTypes.eN3_5.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN3_5.type;
                } else {
                    System.out.println("Unknown N3 type: " + nArea);
                }
                break;
            }
            case 4: {
                if (BoardAreaNData.NTypes.eN4_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_1.type;
                } else if (BoardAreaNData.NTypes.eN4_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_2.type;
                } else if (BoardAreaNData.NTypes.eN4_3.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_3.type;
                } else if (BoardAreaNData.NTypes.eN4_4.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_4.type;
                } else if (BoardAreaNData.NTypes.eN4_5.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_5.type;
                } else if (BoardAreaNData.NTypes.eN4_6.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_6.type;
                } else if (BoardAreaNData.NTypes.eN4_7.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_7.type;
                } else if (BoardAreaNData.NTypes.eN4_8.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN4_8.type;
                } else {
                    System.out.println("Unknown N4 type: " + nArea);
                }
                break;
            }
            case 5: {
                if (BoardAreaNData.NTypes.eN5_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_1.type;
                } else if (BoardAreaNData.NTypes.eN5_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_2.type;
                } else if (BoardAreaNData.NTypes.eN5_3.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_3.type;
                } else if (BoardAreaNData.NTypes.eN5_4.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_4.type;
                } else if (BoardAreaNData.NTypes.eN5_5.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_5.type;
                } else if (BoardAreaNData.NTypes.eN5_6.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_6.type;
                } else if (BoardAreaNData.NTypes.eN5_7.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN5_7.type;
                } else {
                    System.out.println("Unknown N5 type: " + nArea);
                }
                break;
            }
            case 6: {
                if (BoardAreaNData.NTypes.eN6_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN6_1.type;
                } else if (BoardAreaNData.NTypes.eN6_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN6_2.type;
                } else if (BoardAreaNData.NTypes.eN6_3.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN6_3.type;
                } else if (BoardAreaNData.NTypes.eN6_4.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN6_4.type;
                } else if (BoardAreaNData.NTypes.eN6_5.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN6_5.type;
                } else {
                    System.out.println("Unknown N6 type: " + nArea);
                }
                break;
            }
            case 7: {
                if (BoardAreaNData.NTypes.eN7_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN7_1.type;
                } else if (BoardAreaNData.NTypes.eN7_2.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN7_2.type;
                } else {
                    System.out.println("Unknown N7 type: " + nArea);
                }
                break;
            }
            case 8: {
                if (BoardAreaNData.NTypes.eN8_1.anyMatch(nArea)) {
                    nType = BoardAreaNData.NTypes.eN8_1.type;
                } else {
                    System.out.println("Unknown N8 type: " + nArea);
                }
                break;
            }
            default: System.out.println("Unknown neighbor count: " + neighborCount); break;
        }
        return nType;
    }

    private int getIndexRank(int index) {
        int rank = 0;
        char piece = data[index];
        if (index % COLUMNS > 0 && data[index-1] == piece) {
            rank++;
        }
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece) {
            rank++;
        }
        if (index >= COLUMNS && data[index-COLUMNS] == piece) {
            rank++;
        }
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece) {
            rank++;
        }
        return rank;
    }

    private int getIndexNRank(int index) {
        int rank = 0;
        char piece = data[index];
        if (index % COLUMNS > 0 && data[index-1] == piece) {
            rank++;
        }
        if (index % COLUMNS < COLUMNS-1 && data[index+1] == piece) {
            rank++;
        }
        if (index >= COLUMNS && data[index-COLUMNS] == piece) {
            rank++;
        }
        if (index < ROWS*(COLUMNS-1) && data[index+COLUMNS] == piece) {
            rank++;
        }
        if (index % COLUMNS > 0 && index >= COLUMNS && data[index-1-COLUMNS] == piece) {
            rank++;
        }
        if (index % COLUMNS > 0 && index < ROWS*(COLUMNS-1) && data[index-1+COLUMNS] == piece) {
            rank++;
        }
        if (index % COLUMNS < COLUMNS-1 && index >= COLUMNS && data[index+1-COLUMNS] == piece) {
            rank++;
        }
        if (index % COLUMNS < COLUMNS-1 && index < ROWS*(COLUMNS-1) && data[index+1+COLUMNS] == piece) {
            rank++;
        }
        return rank;
    }

    private static long setbit64(long s, int index) {
        return (s | (1L << index));
    }

    private static short setbit16(short s, int index) {
        return (short) (s | (1 << index));
    }

    private static short resetbit16(short s, int index) {
        return (short) (s & ~(1 << index));
    }

    private static boolean testbit64(long s, int index) {
        return (s & (1L << index)) != 0;
    }

    private static boolean testbit16(short s, int index) {
        return (s & (1 << index)) != 0;
    }

    private static short forcebit16(short s, int index, boolean force) {
        short temp = s;
        if (force) {
            temp = setbit16(temp, index);
        } else {
            temp = resetbit16(temp, index);
        }
        return temp;
    }

    private static short flipCenterVertical9(short s) {
        short temp = s;
        temp = forcebit16(temp, 0, testbit16(s, 2));
        temp = forcebit16(temp, 2, testbit16(s, 0));
        temp = forcebit16(temp, 3, testbit16(s, 5));
        temp = forcebit16(temp, 5, testbit16(s, 3));
        temp = forcebit16(temp, 6, testbit16(s, 8));
        temp = forcebit16(temp, 8, testbit16(s, 6));
        return temp;
    }

    private static short rotate90Clockwise9(short s) {
        short temp = s;
        temp = forcebit16(temp, 0, testbit16(s, 6));
        temp = forcebit16(temp, 1, testbit16(s, 3));
        temp = forcebit16(temp, 2, testbit16(s, 0));
        temp = forcebit16(temp, 3, testbit16(s, 7));
        temp = forcebit16(temp, 5, testbit16(s, 1));
        temp = forcebit16(temp, 6, testbit16(s, 8));
        temp = forcebit16(temp, 7, testbit16(s, 5));
        temp = forcebit16(temp, 8, testbit16(s, 2));
        return temp;
    }

    static class FinishedBoardData {

        static final List<String> COLUMN_TITLES = List.of(
                "FINAL_BOARD", "RESULT", "X_LONGEST_PATH", "O_LONGEST_PATH",
                /*"X_LONGEST_N",*/ "X_LONGEST_N1", "X_LONGEST_N2", "X_LONGEST_N3", "X_LONGEST_N4",
                /*"O_LONGEST_N",*/ "O_LONGEST_N1", "O_LONGEST_N2", "O_LONGEST_N3", "O_LONGEST_N4"
        );

        final long finalBoard;
        float result = 0; //computed after merge
        byte xLongestPath = 0;
        byte oLongestPath = 0;

        byte xLongestN = 0; //area of x longest path
        byte xLongestN1 = 0;
        byte xLongestN2 = 0;
        byte xLongestN3 = 0;
        byte xLongestN4 = 0;

        byte oLongestN = 0; //area of o longest path
        byte oLongestN1 = 0;
        byte oLongestN2 = 0;
        byte oLongestN3 = 0;
        byte oLongestN4 = 0;

        FinishedBoardData(long finalBoard) {
            this.finalBoard = finalBoard;
        }

        List<Number> toList() {
            return List.of(finalBoard, result, xLongestPath, oLongestPath,
                    /*xLongestN,*/ xLongestN1, xLongestN2, xLongestN3, xLongestN4,
                    /*oLongestN,*/ oLongestN1, oLongestN2, oLongestN3, oLongestN4
            );
        }

        void incrementData(boolean player, int rank) {
            if (player) {
                xLongestN++;
                switch (rank) {
                    case 1: xLongestN1++; break;
                    case 2: xLongestN2++; break;
                    case 3: xLongestN3++; break;
                    case 4: xLongestN4++; break;
                }
            } else {
                oLongestN++;
                switch (rank) {
                    case 1: oLongestN1++; break;
                    case 2: oLongestN2++; break;
                    case 3: oLongestN3++; break;
                    case 4: oLongestN4++; break;
                }
            }
        }

        void setLongestPath(boolean player, int longestPath) {
            if (player) {
                this.xLongestPath = (byte) longestPath;
            } else {
                this.oLongestPath = (byte) longestPath;
            }
        }

        void merge(FinishedBoardData boardData, boolean player) {
            if (player) {
                this.xLongestPath = boardData.xLongestPath;
                this.xLongestN = boardData.xLongestN;
                this.xLongestN1 = boardData.xLongestN1;
                this.xLongestN2 = boardData.xLongestN2;
                this.xLongestN3 = boardData.xLongestN3;
                this.xLongestN4 = boardData.xLongestN4;
            } else {
                this.oLongestPath = boardData.oLongestPath;
                this.oLongestN = boardData.oLongestN;
                this.oLongestN1 = boardData.oLongestN1;
                this.oLongestN2 = boardData.oLongestN2;
                this.oLongestN3 = boardData.oLongestN3;
                this.oLongestN4 = boardData.oLongestN4;
            }
            int score = xLongestPath - oLongestPath;
            this.result = (score != 0) ? (score > 0) ? 1.0f : 0.0f : 0.5f;
        }

    }

    static class FinishedBoardArea {

        static final List<String> COLUMN_TITLES = List.of(
                "LONGEST_PATH", /*"N",*/ "N1", "N2", "N3", "N4"//,
                //"NN1", "NN2", "NN3", "NN4", "NN5", "NN6", "NN7", "NN8"
        );

        byte longestPath = 0;
        byte n = 0;
        byte n1 = 0;
        byte n2 = 0;
        byte n3 = 0;
        byte n4 = 0;

        float result = 0.0f;

        byte nn1 = 0;
        byte nn2 = 0;
        byte nn3 = 0;
        byte nn4 = 0;
        byte nn5 = 0;
        byte nn6 = 0;
        byte nn7 = 0;
        byte nn8 = 0;

        List<Byte> toListAll() {
            return List.of(longestPath, /*n,*/ n1, n2, n3, n4);//, nn1, nn2, nn3, nn4, nn5, nn6, nn7, nn8);
        }

        void setLongestPath(int longestPath) {
            this.longestPath = (byte) longestPath;
        }

        void setResult(float result) {
            this.result = result;
        }

        void incrementData(int rank) {
            n++;
            switch (rank) {
                case 1: n1++; break;
                case 2: n2++; break;
                case 3: n3++; break;
                case 4: n4++; break;
            }
        }

        void incrementNData(int rank) {
            switch (rank) {
                case 1: nn1++; break;
                case 2: nn2++; break;
                case 3: nn3++; break;
                case 4: nn4++; break;
                case 5: nn5++; break;
                case 6: nn6++; break;
                case 7: nn7++; break;
                case 8: nn8++; break;
            }
        }

    }

    static class BoardAreaNData {

        static final List<String> COLUMN_TITLES = List.of(
                "LONGEST_PATH", /*"RESULT",*/
                "N0_1",
                "N1_1",
                "N2_1", "N2_2", "N2_3",
                "N3_1", "N3_2", "N3_3", "N3_4", "N3_5",
                "N4_1", "N4_2", "N4_3", "N4_4", "N4_5", "N4_6", "N4_7", "N4_8",
                "N5_1", "N5_2", "N5_3", "N5_4", "N5_5", "N5_6", "N5_7",
                "N6_1", "N6_2", "N6_3", "N6_4", "N6_5",
                "N7_1", "N7_2",
                "N8_1"
        );

        byte longestPath = 0;
        float result = 0.0f;
        byte    N0_1 = 0,
                N1_1 = 0,
                N2_1 = 0, N2_2 = 0, N2_3 = 0,
                N3_1 = 0, N3_2 = 0, N3_3 = 0, N3_4 = 0, N3_5 = 0,
                N4_1 = 0, N4_2 = 0, N4_3 = 0, N4_4 = 0, N4_5 = 0, N4_6 = 0, N4_7 = 0, N4_8 = 0,
                N5_1 = 0, N5_2 = 0, N5_3 = 0, N5_4 = 0, N5_5 = 0, N5_6 = 0, N5_7 = 0,
                N6_1 = 0, N6_2 = 0, N6_3 = 0, N6_4 = 0, N6_5 = 0,
                N7_1 = 0, N7_2 = 0,
                N8_1 = 0;

        enum NTypes {

            eN0_1((short) 0b000010000, 0),
            eN1_1((short) 0b000110000, 1),
            eN2_1((short) 0b000111000, 2),
            eN2_2((short) 0b000110010, 3),
            eN2_3((short) 0b000110100, 4),
            eN3_1((short) 0b010010110, 5),
            eN3_2((short) 0b000010111, 6),
            eN3_3((short) 0b000110110, 7),
            eN3_4((short) 0b000011110, 8),
            eN3_5((short) 0b010110010, 9),
            eN4_1((short) 0b010111010, 10),
            eN4_2((short) 0b010010111, 11),
            eN4_3((short) 0b011010110, 12),
            eN4_4((short) 0b010110011, 13),
            eN4_5((short) 0b110010110, 14),
            eN4_6((short) 0b011011010, 15),
            eN4_7((short) 0b000011111, 16),
            eN4_8((short) 0b011110100, 17),
            eN5_1((short) 0b010111110, 18),
            eN5_2((short) 0b010110111, 19),
            eN5_3((short) 0b011110110, 20),
            eN5_4((short) 0b011110011, 21),
            eN5_5((short) 0b111010110, 22),
            eN5_6((short) 0b011011011, 23),
            eN5_7((short) 0b001011111, 24),
            eN6_1((short) 0b110111011, 25),
            eN6_2((short) 0b101111101, 26),
            eN6_3((short) 0b111011011, 27),
            eN6_4((short) 0b010111111, 28),
            eN6_5((short) 0b111110011, 29),
            eN7_1((short) 0b111111011, 30),
            eN7_2((short) 0b111011111, 31),
            eN8_1((short) 0b111111111, 32);

            final int type;
            final List<Short> flipsRotations;

            NTypes(short code, int type) {
                this.type = type;
                flipsRotations = new ArrayList<>();
                short rotator = code;
                for (int degrees = 0; degrees < 360; degrees += 90) {
                    flipsRotations.add(rotator);
                    flipsRotations.add(flipCenterVertical9(rotator));
                    rotator = rotate90Clockwise9(rotator);
                }
            }

            boolean anyMatch(short code) {
                boolean match = false;
                for (Short s : flipsRotations) {
                    if (s.equals(code)) {
                        match = true;
                        break;
                    }
                }
                return match;
            }

        }

        List<Byte> toList() {
            return List.of(longestPath,
                    N0_1,
                    N1_1,
                    N2_1, N2_2, N2_3,
                    N3_1, N3_2, N3_3, N3_4, N3_5,
                    N4_1, N4_2, N4_3, N4_4, N4_5, N4_6, N4_7, N4_8,
                    N5_1, N5_2, N5_3, N5_4, N5_5, N5_6, N5_7,
                    N6_1, N6_2, N6_3, N6_4, N6_5,
                    N7_1, N7_2,
                    N8_1
            );
        }

        void incrementData(int rank) {
            switch(rank) {
                case 0: N0_1++; break;
                case 1: N1_1++; break;
                case 2: N2_1++; break;
                case 3: N2_2++; break;
                case 4: N2_3++; break;
                case 5: N3_1++; break;
                case 6: N3_2++; break;
                case 7: N3_3++; break;
                case 8: N3_4++; break;
                case 9: N3_5++; break;
                case 10: N4_1++; break;
                case 11: N4_2++; break;
                case 12: N4_3++; break;
                case 13: N4_4++; break;
                case 14: N4_5++; break;
                case 15: N4_6++; break;
                case 16: N4_7++; break;
                case 17: N4_8++; break;
                case 18: N5_1++; break;
                case 19: N5_2++; break;
                case 20: N5_3++; break;
                case 21: N5_4++; break;
                case 22: N5_5++; break;
                case 23: N5_6++; break;
                case 24: N5_7++; break;
                case 25: N6_1++; break;
                case 26: N6_2++; break;
                case 27: N6_3++; break;
                case 28: N6_4++; break;
                case 29: N6_5++; break;
                case 30: N7_1++; break;
                case 31: N7_2++; break;
                case 32: N8_1++; break;
                default: System.out.println("Unknown rank encountered: " + rank); break;
            }
        }

        static void printLookupTable() {
            byte[] lookupTable = new byte[512];
            Arrays.fill(lookupTable, (byte) 255);
            for (short i = 0; i < lookupTable.length; i++) {

                for (NTypes type : NTypes.values()) {
                    if (type.anyMatch(i)) {
                        lookupTable[i] = (byte) type.type;
                        break;
                    }
                }
            }
            for (Byte b : lookupTable) {
                System.out.print(String.format("0x%02X,", b));
            }
        }

    }

}
