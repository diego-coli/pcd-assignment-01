package pcd.ass01.concurrent.thread;

import pcd.ass01.model.*;

import java.util.List;

public class BoidWorker extends Thread {
    private final BoidModel model;
    private final List<Boid> assignedBoids;

    public BoidWorker(BoidModel model, List<Boid> assignedBoids) {
        this.model = model;
        this.assignedBoids = assignedBoids;
    }

    public void run() {
        while (true) {
            for (Boid boid : assignedBoids) {
                int index = model.getBoidIndex(boid); // Supponiamo che ci sia un metodo per ottenere l'indice
                boid.updateState(model);
                try {
                    model.waitForUpdate();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                model.updateBoid(index, boid); // Segnala che il boid è stato aggiornato
            }
        }
    }

}