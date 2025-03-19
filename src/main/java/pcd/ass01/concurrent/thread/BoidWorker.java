package pcd.ass01.concurrent.thread;

import pcd.ass01.model.*;

import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class BoidWorker extends Thread {
    private final BoidModel model;
    private final List<Boid> assignedBoids;
    private final CyclicBarrier barrier;

    public BoidWorker(BoidModel model, List<Boid> assignedBoids, CyclicBarrier barrier) {
        this.model = model;
        this.assignedBoids = assignedBoids;
        this.barrier = barrier;
    }

    public void run() {
        while (true) {
            for (Boid boid : assignedBoids) {
                int index = model.getBoidIndex(boid); // Supponiamo che ci sia un metodo per ottenere l'indice
                boid.updateState(model); // Aggiorna lo stato del boid
                model.updateBoid(index, boid); // Segnala che il boid è stato aggiornato
            }
            try {
                barrier.await(); // Attendi che tutti i thread abbiano finito di aggiornare i boid
                Thread.sleep(5); // Aggiungi un ritardo
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}