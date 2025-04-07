package pcd.ass01.concurrent.threads;

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
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Se la simulazione è in pausa, attendi senza aggiornare i boid.
                while (simulator.isPaused()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Il thread è stato interrotto mentre era in pausa
                        Thread.currentThread().interrupt();
                        return; // Esci dal metodo run() terminando il thread
                    }
                }

                for (Boid boid : assignedBoids) {
                    int index = model.getBoidIndex(boid); 
                    boid.updateState(model); 
                    model.updateBoid(index, boid); 
                }
                
                try {
                    barrier.await(); // Attendi che tutti i thread abbiano finito di aggiornare i boid
                    Thread.sleep(5); // Piccolo ritardo per efficienza 
                } catch (InterruptedException e) {
                    // Il thread è stato interrotto mentre era in attesa o durante lo sleep
                    Thread.currentThread().interrupt();
                    return; // Esci dal metodo run() terminando il thread
                } catch (BrokenBarrierException e) {
                    // La barriera è stata rotta, il reset è stato attivato
                    return; // Esci dal metodo run() terminando il thread senza generare errori
                }
            }
        } catch (Exception e) {
            // Cattura qualsiasi altra eccezione per sicurezza
            System.err.println("Errore nel worker thread: " + e.getMessage());
        }
    }
}