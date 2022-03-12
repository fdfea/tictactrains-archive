package com.github.fdfea.tictactrains;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

class DataGenerator {

    enum PlayoutQuality {
        RANDOM, SEMI_RANDOM, SIMULATED
    }

    enum DataFormat {
        LONGEST_AREAS, ALL_BOARD_AREAS, ONE_BOARD_DATA, AREA_NDATAS
    }

    private String fileName;
    private List<MovePolicy> rules;
    private int nGames;
    private PlayoutQuality playoutQuality;
    private DataFormat dataFormat;
    private int simulations;
    private boolean presetOpening;
    private List<String> openingIds;
    private Random rand;
    private boolean noDuplicates;

    private DataGenerator() {

    }

    static class DataGeneratorBuilder {

        private String fileName;
        private final List<MovePolicy> rules;
        private int nGames;
        private PlayoutQuality playoutQuality;
        private DataFormat dataFormat;
        private int simulations;
        private boolean presetOpening;
        private List<String> openingIds;
        private Random rand;
        private boolean noDuplicates;

        DataGeneratorBuilder(String fileName, List<MovePolicy> rules) {
            this.fileName = fileName;
            this.rules = rules;
            this.nGames = 100;
            this.playoutQuality = PlayoutQuality.RANDOM;
            this.dataFormat = DataFormat.LONGEST_AREAS;
            this.simulations = 100;
            this.presetOpening = false;
            this.openingIds = null;
            this.rand = new Random();
            this.noDuplicates = false;
        }

        DataGeneratorBuilder withNGames(int nGames) {
            this.nGames = nGames;
            return this;
        }

        DataGeneratorBuilder withPlayoutQuality(PlayoutQuality playoutQuality) {
            this.playoutQuality = playoutQuality;
            return this;
        }

        DataGeneratorBuilder withDataFormat(DataFormat dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        DataGeneratorBuilder withSimulations(int simulations) {
            this.simulations = simulations;
            return this;
        }

        DataGeneratorBuilder withPresetOpening(List<String> ids) {
            this.presetOpening = true;
            this.openingIds = ids;
            return this;
        }

        DataGeneratorBuilder withNoDuplicates(boolean noDuplicates) {
            this.noDuplicates = noDuplicates;
            return this;
        }

        DataGenerator build() {
            DataGenerator dataGenerator = new DataGenerator();
            dataGenerator.fileName = this.fileName;
            dataGenerator.nGames = this.nGames;
            dataGenerator.rules = this.rules;
            dataGenerator.playoutQuality = this.playoutQuality;
            dataGenerator.dataFormat = this.dataFormat;
            dataGenerator.simulations = this.simulations;
            dataGenerator.presetOpening = this.presetOpening;
            dataGenerator.openingIds = this.openingIds;
            dataGenerator.rand = this.rand;
            dataGenerator.noDuplicates = this.noDuplicates;
            return dataGenerator;
        }

    }

    void generate() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(fileName), CSVFormat.DEFAULT)) {
            switch(dataFormat) {
                case LONGEST_AREAS:
                case ONE_BOARD_DATA:
                    printer.printRecord(Board.FinishedBoardData.COLUMN_TITLES);
                    break;
                case ALL_BOARD_AREAS:
                    printer.printRecord(Board.FinishedBoardArea.COLUMN_TITLES);
                    break;
                case AREA_NDATAS:
                    printer.printRecord(Board.BoardAreaNData.COLUMN_TITLES);
                    break;
            }
            HashSet<Integer> uniqueRecords = new HashSet<>();
            IntStream.range(0, nGames).forEach(i -> {
                Board board = new Board(rules);
                if (presetOpening) {
                    board.playOpening(openingIds);
                }
                switch(playoutQuality) {
                    case RANDOM:
                        board.randomPlayout(rand, false);
                        break;
                    case SEMI_RANDOM:
                        board.randomPlayout(rand, true);
                        break;
                    case SIMULATED:
                        board.simulatedPlayout(simulations);
                        break;
                }
                try {
                    switch (dataFormat) {
                        case LONGEST_AREAS: {
                            Board.FinishedBoardData boardData = board.getFinishedBoardData();
                            List<Number> record = boardData.toList();
                            if (!noDuplicates || uniqueRecords.add(record.hashCode())) {
                                printer.printRecord(record);
                            }
                            break;
                        }
                        case ALL_BOARD_AREAS: {
                            List<Board.FinishedBoardArea> boardAreas = board.getFinishedBoardAreas();
                            for (Board.FinishedBoardArea boardArea : boardAreas) {
                                List<Byte> record = boardArea.toListAll();
                                if (!noDuplicates || uniqueRecords.add(record.hashCode())) {
                                    printer.printRecord(record);
                                }
                            }
                            break;
                        }
                        case ONE_BOARD_DATA: {
                            Board.FinishedBoardData boardData = board.getOneFinishedBoardData();
                            List<Number> record = boardData.toList();
                            if (!noDuplicates || uniqueRecords.add(record.hashCode())) {
                                printer.printRecord(record);
                            }
                            break;
                        }
                        case AREA_NDATAS: {
                            List<Board.BoardAreaNData> nDatas = board.getBoardNDatas();
                            for (Board.BoardAreaNData nData : nDatas) {
                                List<Byte> record = nData.toList();
                                if (!noDuplicates || uniqueRecords.add(record.hashCode())) {
                                    printer.printRecord(record);
                                }
                            }
                            break;
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
