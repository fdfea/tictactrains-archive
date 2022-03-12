package com.github.fdfea.mcts;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MonteCarloTreeNode<T extends MonteCarloTreeSearchable> {

    private final T state;
    final Object lock = new Object();
    private MonteCarloTreeNode<T> parent;
    private List<MonteCarloTreeNode<T>> children;
    private int visits;
    private double score;

    MonteCarloTreeNode(T state) {
        this.state = state;
        this.parent = null;
        this.children = new ArrayList<>();
        this.visits = 0;
        this.score = 0.0;
    }

    T getState() {
        return state;
    }

    MonteCarloTreeNode<T> getParent() {
        return parent;
    }

    List<MonteCarloTreeNode<T>> getChildren() {
        return children;
    }

    int getVisits() {
        return visits;
    }

    double getScore() {
        return score;
    }

    void setParent(MonteCarloTreeNode<T> parent) {
        this.parent = parent;
    }

    void setChildren(List<MonteCarloTreeNode<T>> children) {
        this.children = children;
    }

    void incrementVisits() {
        visits++;
    }

    void addScore(double score) {
        this.score += score;
    }

    MonteCarloTreeNode<T> getRandomChild() {
        return children.get(new Random().nextInt(children.size()));
    }

    MonteCarloTreeNode<T> getMostVisitedChild() {
        return Collections.max(children, Comparator.comparing(MonteCarloTreeNode::getVisits));
    }

    int size() {
        return 1 + children.stream().mapToInt(MonteCarloTreeNode::size).sum();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MonteCarloTreeNode<?>) {
            MonteCarloTreeNode<?> node = (MonteCarloTreeNode<?>) obj;
            return state.equals(node.getState());
        }
        return false;
    }

}
