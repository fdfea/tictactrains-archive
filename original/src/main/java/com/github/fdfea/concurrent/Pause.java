package com.github.fdfea.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Pause {

    private final int nThreads;
    private int nPaused = 0;
    private boolean paused = false;
    private final Lock pauseLock = new ReentrantLock();
    private final Condition allPaused = pauseLock.newCondition();
    private final Condition unPaused = pauseLock.newCondition();

    public Pause(int nThreads) {
        this.nThreads = nThreads;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        pauseLock.lock();
        try {
            paused = true;
            while (nPaused < nThreads) {
                allPaused.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    public void resume() {
        pauseLock.lock();
        try {
            paused = false;
            nPaused = 0;
            allPaused.signalAll();
            unPaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }

    public void check() {
        if (paused) {
            pauseLock.lock();
            try {
                nPaused++;
                if (nPaused == nThreads) {
                    allPaused.signalAll();
                }
                while (paused) {
                    unPaused.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pauseLock.unlock();
            }
        }
    }

}
