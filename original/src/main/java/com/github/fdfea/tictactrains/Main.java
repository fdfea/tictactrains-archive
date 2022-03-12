package com.github.fdfea.tictactrains;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.github.fdfea.mcts.*;

import java.util.*;
import java.util.stream.IntStream;

public class Main {

    private static final String introMessage = "Welcome to TicTacTrains!\n";

    public static void main(String[] args) {

        Config conf = ConfigFactory.load().getConfig("tictactrains");
        Config rulesConfig = conf.getConfig("rules");
        Config AIConfig = conf.getConfig("AI");

        final List<Boolean> playerList = List.copyOf(rulesConfig.getBooleanList("playerList"));
        final List<IdSet> squareList = List.copyOf(rulesConfig.getEnumList(IdSet.class, "idList"));

        List<MovePolicy> rulesTemp = new ArrayList<>();
        IntStream.range(0,TicTacTrains.ROWS*TicTacTrains.COLUMNS).forEach(i -> rulesTemp.add(new MovePolicy(playerList.get(i), squareList.get(i))));
        List<MovePolicy> rules = Collections.unmodifiableList(rulesTemp);
        
        /*
        String fileName = "ttt-10000-classical-semi_rand-all_areas-d4-no_dupes.csv";
        String filePath = new File("data/" + fileName).getAbsolutePath();

        DataGenerator generator =
                new DataGenerator.DataGeneratorBuilder(filePath, rules)
                        .withNGames(10000)
                        .withPlayoutQuality(DataGenerator.PlayoutQuality.SEMI_RANDOM)
                        .withDataFormat(DataGenerator.DataFormat.ALL_BOARD_AREAS)
                        .withPresetOpening(List.of("d4"))
                        .withNoDuplicates(true)
                        .build();

        generator.generate();
         */

        final boolean computerPlaying = AIConfig.getBoolean("computerPlaying");
        final boolean computerPlayer = AIConfig.getBoolean("computerPlayer");
        final boolean presetOpenings = AIConfig.getBoolean("presetOpenings");
        final long simulationTime = AIConfig.getLong("simulationTime");
        final int nThreads = AIConfig.getInt("threads");

        TicTacTrains game = new TicTacTrains(rules);
        Board board = game.getBoard();
        Scanner moveReader = new Scanner(System.in);

        MonteCarloTreeSearch<Board> AI = new MonteCarloTreeSearch<>(new Board(board), Board.class);
        //ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        //IntStream.range(0, nThreads).forEach(i -> executor.execute(new MonteCarloTreeWorker<>(AI)));

        System.out.println(introMessage);
        System.out.println(board.toString() + "\n");

        while(!board.isFinished()) {
            if(computerPlaying && computerPlayer && presetOpenings && board.getMove() == 0) {
                game.makeMove("d4");
                AI.shiftRoot(new Board(board));
                game.print();
            }
            if(computerPlaying && board.getPlayer() == computerPlayer) {
                try {
                    //Thread.sleep(simulationTime);
                    AI.simulate(simulationTime);
                    //System.out.println("Computer, BEFORE SHIFT");
                    //System.out.print(AI.toString());
                    Board tempBoard = AI.getNextState();
                    game.makeMove(tempBoard.getLastMoveId());
                    AI.shiftRoot(new Board(board));
                    //System.out.println("Computer, AFTER SHIFT");
                    //System.out.print(AI.toString());
                    game.print();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                List<String> nextValidMoves = board.getNextValidMoves(false);
                while (true) {
                    System.out.print("Enter your move: ");
                    try {
                        String input = moveReader.next();
                        if (Board.isIdValid(input) && nextValidMoves.contains(input)) {
                            game.makeMove(input);
                            if(computerPlaying && !board.isFinished()) {
                                //System.out.println("Player, BEFORE SHIFT");
                                //System.out.print(AI.toString());
                                AI.shiftRoot(new Board(board));
                                //System.out.println("Player, AFTER SHIFT");
                                //System.out.print(AI.toString());
                            }
                            game.print();
                            break;
                        }
                    } catch (NoSuchElementException e) {
                        System.exit(0);
                    }
                }
            }
        }

        moveReader.close();
        //executor.shutdownNow();
        int score = board.score();
        System.out.println(score);

    }

}
