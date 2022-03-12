package com.github.fdfea.tictactrains;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

class TicTacTrainsFactory {

    private static final int ROWS = 7;
    private static final int COLUMNS = 7;
    private static final int THREAD_COUNT_MAX = 16;
    private static final int THREAD_COUNT_MIN = 1;
    private static final int PLAYER_DIFF_MAX = 6;
    private static final long SIMULATION_TIME_MIN = 1;
    private static final long SIMULATION_TIME_MAX = 3600000;

    static TicTacTrains load() throws IllegalStateException {

        Config conf = ConfigFactory.load().getConfig("tictactrains");

        final List<Boolean> playerList = List.copyOf(conf.getBooleanList("rules.playerList"));
        if(!validatePlayerList(playerList)) throw new IllegalStateException("Invalid playerList");

        final List<IdSet> idList = List.copyOf(conf.getEnumList(IdSet.class, "rules.idList"));
        if(!validateIdList(idList)) throw new IllegalStateException("Invalid idList");

        List<MovePolicy> rules = generateRules(playerList, idList);

        final boolean computerPlaying = conf.getBoolean("AI.computerPlaying");
        final boolean computerPlayer = conf.getBoolean("AI.computerPlayer");

        final int nThreads = conf.getInt("AI.threads");
        if(!validateThreadCount(nThreads)) throw new IllegalStateException("Invalid nThreads");

        final long simulationTime = conf.getLong("AI.simulationTime");
        if(!validateSimulationTime(simulationTime)) throw new IllegalStateException("Invalid simulationTime");

        TicTacTrains game = new TicTacTrains(rules);
        //TicTacTrains game = new TicTacTrains(rules, computerPlaying, computerPlayer, nThreads, simulationTime);

        final String positionToLoad = conf.getString("rules.positionToLoad");
        if(!positionToLoad.equals("")) loadPosition(game, positionToLoad);

        return game;

    }

    private static boolean validatePlayerList(List<Boolean> playerList) {
        return playerList.size() == ROWS*COLUMNS
                && IntStream.range(0, playerList.size()).reduce(0, (sum, i) -> playerList.get(i) ? sum++ : sum--) < PLAYER_DIFF_MAX;
    }

    private static boolean validateIdList(List<IdSet> idSetList) {
        return idSetList.size() == ROWS*COLUMNS
                && IntStream.range(0, idSetList.size()).allMatch(i -> idSetList.get(i).getIds().size() < i);
    }

    private static List<MovePolicy> generateRules(List<Boolean> playerList, List<IdSet> idList) {
        List<MovePolicy> rulesTemp = new ArrayList<>();
        IntStream.range(0,TicTacTrains.ROWS*TicTacTrains.COLUMNS).forEach(i -> rulesTemp.add(new MovePolicy(playerList.get(i), idList.get(i))));
        return Collections.unmodifiableList(rulesTemp);
    }

    private static boolean validateThreadCount(int nThreads) {
        return nThreads > THREAD_COUNT_MIN && nThreads < THREAD_COUNT_MAX;
    }

    private static boolean validateSimulationTime(long simulationTime) {
        return simulationTime > SIMULATION_TIME_MIN && simulationTime < SIMULATION_TIME_MAX;
    }

    private static void loadPosition(TicTacTrains game, String positionString) throws IllegalStateException {
        String[] ids = positionString.split("\\s+");
        for (String s : ids) {
            String id = s.replace(".*.", "");
            List<String> nextValidMoves = game.getBoard().getNextValidMoves(false);
            if (Board.isIdValid(id) && nextValidMoves.contains(id)) {
                game.makeMove(id);
            } else {
                throw new IllegalStateException("Invalid positionToLoad");
            }
        }
    }

}
