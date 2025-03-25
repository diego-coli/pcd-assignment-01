package pcd.ass01.concurrent.thread;

import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

import pcd.ass01.model.*;
import pcd.ass01.view.BoidsView;

import java.util.ArrayList;
import java.util.List;
import pcd.ass01.concurrent.Simulator;;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    private final CyclicBarrier barrier;
    private static final int FRAMERATE = 30;
    private int framerate;
    private final List<BoidWorker> workers;
    private static volatile boolean paused = false;

    public BoidsSimulator(BoidModel model, int numWorkers) {
        this.model = model;
        this.view = Optional.empty();
        this.workers = new ArrayList<>();

        //Inizializza la barriera
        barrier = new CyclicBarrier (numWorkers);

        // Suddividere i boids tra i worker
        List<Boid> boids = model.getBoids();
        int batchSize = boids.size() / numWorkers;
        assignBoidsToWorkers(model, numWorkers, batchSize, boids);
    }

    private void assignBoidsToWorkers(BoidModel model, int numWorkers, int batchSize, List<Boid> boids) {
        for (int i = 0; i < numWorkers; i++) {
            int start = i * batchSize;
            int end = (i == numWorkers - 1) ? boids.size() : start + batchSize;
            List<Boid> assignedBoids = boids.subList(start, end);
            BoidWorker worker = new BoidWorker(model, assignedBoids, barrier, this);
            workers.add(worker);
        }
    }

    @Override
    public  boolean isPaused() {
        return paused;
    }
    @Override
    public void togglePause() {
        paused = !paused;
    }
    @Override
    public void attachView(Object view) {
        if (view instanceof BoidsView) {
            this.view = Optional.of((BoidsView)view);
        }
    }

    public void runSimulation() {
        // Avviare i worker
        for (BoidWorker worker : workers) {
            worker.start();
        }

        while (true) {

            if(paused){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            try {
                model.waitForUpdate();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Rendering
            if (view.isPresent()) {
                var t0 = System.currentTimeMillis();
                view.get().update(framerate);
                var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var framratePeriod = 1000 / FRAMERATE;

                if (dtElapsed < framratePeriod) {
                    try {
                        Thread.sleep(framratePeriod - dtElapsed);
                    } catch (Exception ignored) {}
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000 / dtElapsed);
                }
            }
        }
    }
}
