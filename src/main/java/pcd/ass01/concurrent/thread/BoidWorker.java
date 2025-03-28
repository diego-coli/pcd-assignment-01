package pcd.ass01.concurrent.thread;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;


import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class BoidWorker extends Thread {
    private final BoidModel model;
    private final List<Boid> assignedBoids;
    private final CyclicBarrier barrier;
    private final BoidsSimulator simulator; // Passiamo l'istanza del simulatore

    public BoidWorker(BoidModel model, List<Boid> assignedBoids, CyclicBarrier barrier, BoidsSimulator simulator) {
        this.model = model;
        this.assignedBoids = assignedBoids;
        this.barrier = barrier;
        this.simulator = simulator;
    }

    public void run() {
        while (true) {
            // Se la simulazione è in pausa, attendi senza aggiornare i boid.
            while (simulator.isPaused()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            for (Boid boid : assignedBoids) {
                int index = model.getBoidIndex(boid); // Ottieni l'indice del boid
                boid.updateState(model); // Aggiorna lo stato del boid
                model.updateBoid(index, boid); // Segnala che il boid è stato aggiornato
            }
            try {
                barrier.await(); // Attendi che tutti i thread abbiano finito di aggiornare i boid
                //Thread.sleep(5); // Piccolo ritardo per efficenza 
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}