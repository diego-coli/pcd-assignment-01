package pcd.ass01.concurrent.threads;

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
    private CyclicBarrier barrier;
    private final List<BoidWorker> workers;
    private Thread simulationThread;
    private volatile boolean running = false;
    private static volatile boolean paused = false;
    private static final int FRAMERATE = 30;
    private int framerate;

    public BoidsSimulator(BoidModel model) {
        this.model = model;
        this.view = Optional.empty();
        this.workers = new ArrayList<>();
    }

    private void assignBoidsToWorkers(BoidModel model, int numWorkers, int batchSize, List<Boid> boids) {
        for (int i = 0; i < numWorkers; i++) {
            int start = i * batchSize;
            int end = (i == numWorkers - 1) ? boids.size() : start + batchSize;
            // Usa una copia dei boids
            List<Boid> assignedBoids = new ArrayList<>(boids.subList(start, end));
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

    @Override
    public void stop() {
        // Interrompe il master thread
        running = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        
        // Resetta la barriera prima di interrompere i workers
        if (barrier != null) {
            barrier.reset();
        }
        
        // Interrompe tutti i workers
        for (BoidWorker worker : workers) {
            worker.interrupt();
        }
        
        // Attende che i workers terminino
        for (BoidWorker worker : workers) {
            try {
                if (worker.isAlive()) {
                    // Attende al max 1s
                    worker.join(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        workers.clear();
    }

    @Override
    public void start() {
        int numWorkers = Runtime.getRuntime().availableProcessors();
        // Crea la barriera prima di creare i workers
        this.barrier = new CyclicBarrier(numWorkers);
        // Crea i workers
        List<Boid> boids = model.getBoids();
        int batchSize = boids.size() / numWorkers;
        assignBoidsToWorkers(model, numWorkers, batchSize, boids);
        // Avvia tutti i workers
        for (BoidWorker worker : workers) {
            worker.start();
        }
        // Avvia un nuovo master thread
        running = true;
        simulationThread = new Thread(this::runSimulation);
        simulationThread.start();
    }

    @Override
    public void runSimulation() {
        while (running) {
            if(paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
                continue;
            }
            try {
                model.waitForUpdate();
            } catch (InterruptedException e) {
                if (!running) break;
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
