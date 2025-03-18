package pcd.ass01.concurrent.thread;

import pcd.ass01.model.*;

import java.util.List;

public class ProvWorker extends Thread {
    private final ProvModel model;
    private final List<ProvBoid> assignedBoids;

    public ProvWorker(ProvModel model, List<ProvBoid> assignedBoids) {
        this.model = model;
        this.assignedBoids = assignedBoids;
    }

    public void run() {
        try {
            while (true) {
                for (ProvBoid boid : assignedBoids) {
                    boid.update(model);
                }
                model.waitForUpdate(); // Sincronizza con il monitor
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}