package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private volatile int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());

    private volatile boolean suspended = false;
    private final Object pauseLock = new Object();
    private final PauseCoordinator coordinator;

    private volatile boolean alive = true;
    private volatile boolean stopped = false;

    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb, PauseCoordinator coordinator) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
        this.coordinator = coordinator;
    }

    public void run() {

        while (true) {
            checkSuspension();

            if (stopped) {
                return;
            }

            if (!alive) {
                coordinator.threadPaused();
                return;
            }

            Immortal im;

            int myIndex = immortalsPopulation.indexOf(this);

            int nextFighterIndex = r.nextInt(immortalsPopulation.size());

            //avoid self-fight
            if (nextFighterIndex == myIndex) {
                nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
            }

            im = immortalsPopulation.get(nextFighterIndex);

            this.fight(im);

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public void fight(Immortal i2) {

        Immortal first, second;

        if (System.identityHashCode(this) < System.identityHashCode(i2)) {
            first = this;
            second = i2;
        } else {
            first = i2;
            second = this;
        }

        synchronized (first) {
            synchronized (second) {
                if (i2.getHealth() > 0) {
                    i2.changeHealth(i2.getHealth() - defaultDamageValue);
                    this.health += defaultDamageValue;
                    if (i2.getHealth() <= 0) {
                        i2.eliminate();
                        immortalsPopulation.remove(i2);
                    }
                    updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
                } else {
                    updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
                }
            }
        }
    }

    private void checkSuspension() {
        if (suspended) {
            coordinator.threadPaused();
            synchronized (pauseLock) {
                while (suspended && !stopped) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            coordinator.threadResumed();
        }
    }

    public void pauseImmortal() {
        suspended = true;
    }

    public void resumeImmortal() {
        synchronized (pauseLock) {
            suspended = false;
            pauseLock.notifyAll();
        }
    }

    public void stopImmortal() {
        stopped = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public void changeHealth(int v) {
        health = v;
    }

    public void eliminate() {
        alive = false;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
