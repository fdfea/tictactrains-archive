package com.github.fdfea.mcts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MonteCarloTreeSearch<T extends MonteCarloTreeSearchable> {

    private MonteCarloTreeNode<T> root;
    private final Class<T> type;
    private static final double c = Math.sqrt(2);

    public MonteCarloTreeSearch(T state, Class<T> type) {
        this.root = new MonteCarloTreeNode<>(state);
        this.type = type;
    }

    MonteCarloTreeNode<T> selectPromisingNode() {
        MonteCarloTreeNode<T> tempNode = root;
        boolean isEmpty;
        synchronized (tempNode.lock) {
            isEmpty = tempNode.getChildren().isEmpty();
        }
        while (!isEmpty) {
            synchronized (tempNode.lock) {
                tempNode = findBestNodeUCT(tempNode);
                isEmpty = tempNode.getChildren().isEmpty();
            }
        }
        return tempNode;
    }

    void expandNode(MonteCarloTreeNode<T> node) {
        List<MonteCarloTreeNode<T>> nextStateNodes = new ArrayList<>();
        synchronized (node.lock) {
            node.getState().getNextStates(type).forEach(nextState -> {
                MonteCarloTreeNode<T> nextNode = new MonteCarloTreeNode<>(nextState);
                nextNode.setParent(node);
                nextStateNodes.add(nextNode);
            });
            node.setChildren(nextStateNodes);
        }
    }

    double simulatePlayout(MonteCarloTreeNode<T> node) {
        boolean player;
        synchronized (root.lock) {
            player = root.getState().getPlayer();
        }
        synchronized (node.lock) {
            return node.getState().simulatePlayout(player);
        }
    }

    void backPropagate(MonteCarloTreeNode<T> node, double score) {
        MonteCarloTreeNode<T> tempNode = node;
        boolean player;
        synchronized (root.lock) {
            player = root.getState().getPlayer();
        }
        while (tempNode != null) {
            synchronized (tempNode.lock) {
                tempNode.incrementVisits();
                tempNode.addScore(tempNode.getState().getPlayer() != player ? score : 1 - score);
                tempNode = tempNode.getParent();
            }
        }
    }

    private MonteCarloTreeNode<T> findBestNodeUCT(MonteCarloTreeNode<T> node) {
        synchronized (node.lock) {
            return Collections.max(node.getChildren(), Comparator.comparing(child -> {
                        synchronized (child.lock) {
                            return UCT(node.getVisits(), child.getScore(), child.getVisits());
                        }
                    }));
        }
    }

    private static double UCT(int totalVisits, double nodeWinScore, int nodeVisits) {
        if(nodeVisits == 0) return Integer.MAX_VALUE;
        else return (nodeWinScore/nodeVisits) + c*Math.sqrt((Math.log(totalVisits))/nodeVisits);
    }

    public T getNextState() {
        synchronized (root.lock) {
            return root.getMostVisitedChild().getState();
        }
    }

    public void shiftRoot(T state) throws IllegalStateException {
        //pause.pause();
        synchronized (root.lock) {
            if (!root.getState().isFinished() &&
                    root.getState().getNextStates(type).stream().anyMatch(s -> s.equals(state))) {
                MonteCarloTreeNode<T> newState = new MonteCarloTreeNode<>(state);
                int index = root.getChildren().indexOf(newState);
                root = (index < 0) ? newState : root.getChildren().get(index);
                root.setParent(null);
            } else {
                throw new IllegalStateException("New state is not reachable from root state");
            }
        }
        //pause.resume();
    }

    @Override
    public String toString() {
        StringBuilder stats = new StringBuilder();
        synchronized (root.lock) {
            for (MonteCarloTreeNode<T> child : root.getChildren()) {
                synchronized (child.lock) {
                    stats.append(child.getState().getStateString()).append(": ")
                            .append(child.getScore()).append("/").append(child.getVisits())
                            .append(" -- ").append(child.size()).append(" Nodes").append(" ** ")
                            .append(UCT(root.getVisits(), child.getScore(), child.getVisits()))
                            .append(" UCT").append("\n");
                }
            }
        }
        return stats.toString();
    }

    public void simulate(long simulationTimeMillis) {
        long end  = System.currentTimeMillis() + simulationTimeMillis;
        while(System.currentTimeMillis() < end) {
            // Phase 1 - Selection
            MonteCarloTreeNode<T> promisingNode = selectPromisingNode();
            // Phase 2 - Expansion
            if (!promisingNode.getState().isFinished()) {
                expandNode(promisingNode);
            }
            // Phase 3 - Simulation
            MonteCarloTreeNode<T> nodeToExplore = promisingNode;
            if (!promisingNode.getChildren().isEmpty()) {
                nodeToExplore = promisingNode.getRandomChild();
            }
            double playoutResult = simulatePlayout(nodeToExplore);
            // Phase 4 - Update
            backPropagate(nodeToExplore, playoutResult);
        }
    }

    public void simulate(int simulations) {
        int count = 0;
        while(count < simulations) {
            // Phase 1 - Selection
            MonteCarloTreeNode<T> promisingNode = selectPromisingNode();
            // Phase 2 - Expansion
            if (!promisingNode.getState().isFinished()) {
                expandNode(promisingNode);
            }
            // Phase 3 - Simulation
            MonteCarloTreeNode<T> nodeToExplore = promisingNode;
            if (!promisingNode.getChildren().isEmpty()) {
                nodeToExplore = promisingNode.getRandomChild();
            }
            double playoutResult = simulatePlayout(nodeToExplore);
            // Phase 4 - Update
            backPropagate(nodeToExplore, playoutResult);
            count++;
        }
    }

}
