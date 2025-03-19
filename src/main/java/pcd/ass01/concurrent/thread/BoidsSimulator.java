package pcd.ass01.concurrent.thread;

import java.util.Optional;

import pcd.ass01.model.*;
import pcd.ass01.view.BoidsView;

import java.util.ArrayList;
import java.util.List;

public class BoidsSimulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    private static final int FRAMERATE = 25;
    private int framerate;
    private final List<BoidWorker> workers;

    public BoidsSimulator(BoidModel model, int numWorkers) {
        this.model = model;
        this.view = Optional.empty();
        this.workers = new ArrayList<>();

        // Suddividere i boids tra i worker
        List<Boid> boids = model.getBoids();
        int batchSize = boids.size() / numWorkers;
        for (int i = 0; i < numWorkers; i++) {
            int start = i * batchSize;
            int end = (i == numWorkers - 1) ? boids.size() : start + batchSize;
            List<Boid> assignedBoids = boids.subList(start, end);
            BoidWorker worker = new BoidWorker(model, assignedBoids);
            workers.add(worker);
        }
    }

    public void attachView(BoidsView view) {
        this.view = Optional.of(view);
    }

    public void runSimulation() {
        // Avviare i worker
        for (BoidWorker worker : workers) {
            worker.start();
        }

        while (true) {
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
