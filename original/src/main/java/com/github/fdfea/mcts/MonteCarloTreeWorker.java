package com.github.fdfea.mcts;

public class MonteCarloTreeWorker<T extends MonteCarloTreeSearchable> implements Runnable {

    private final MonteCarloTreeSearch<T> tree;
    //private final ReadWriteLock pauseLock;

    public MonteCarloTreeWorker(MonteCarloTreeSearch<T> tree) {
        this.tree = tree;
        //this.pauseLock = tree.getPauseLock();
    }

    public void run() {
        while(!Thread.interrupted()) {
            MonteCarloTreeNode<T> promisingNode = tree.selectPromisingNode();
            if (!promisingNode.getState().isFinished() && promisingNode.getChildren().isEmpty()) {
                tree.expandNode(promisingNode);
            }
            MonteCarloTreeNode<T> nodeToExplore = promisingNode;
            if (!promisingNode.getChildren().isEmpty()) {
                nodeToExplore = promisingNode.getRandomChild();
            }
            double playoutResult = tree.simulatePlayout(nodeToExplore);
            tree.backPropagate(nodeToExplore, playoutResult);
        }
    }

    /*
    public void run() {
        while(!Thread.interrupted()) {
            pauseLock.readLock().lock();
            try {
                MonteCarloTreeNode<T> promisingNode = tree.selectPromisingNode();
                if (!promisingNode.getState().isFinished() && promisingNode.getChildren().isEmpty()) {
                    tree.expandNode(promisingNode);
                }
                MonteCarloTreeNode<T> nodeToExplore = promisingNode;
                if (!promisingNode.getChildren().isEmpty()) {
                    nodeToExplore = promisingNode.getRandomChild();
                }
                double playoutResult = tree.simulatePlayout(nodeToExplore);
                tree.backPropagate(nodeToExplore, playoutResult);
            } finally {
                pauseLock.readLock().unlock();
            }
        }
    }
     */

}
