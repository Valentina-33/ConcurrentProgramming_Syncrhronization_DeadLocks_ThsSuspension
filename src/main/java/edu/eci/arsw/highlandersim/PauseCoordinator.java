package edu.eci.arsw.highlandersim;

public class PauseCoordinator {

    private int pausedCount = 0;
    private final int total;

    public PauseCoordinator(int total) {
        this.total = total;
    }

    public synchronized void threadPaused() {
        pausedCount++;
        notifyAll();
    }

    public synchronized void threadResumed() {
        pausedCount--;
    }

    public synchronized void awaitAllPaused() throws InterruptedException {
        while (pausedCount < total) {
            wait();
        }
    }
}
