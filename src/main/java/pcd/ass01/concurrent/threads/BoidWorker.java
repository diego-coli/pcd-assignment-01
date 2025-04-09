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
    private final BoidsSimulator simulator;

    public BoidWorker(BoidModel model, List<Boid> assignedBoids, CyclicBarrier barrier, BoidsSimulator simulator) {
        this.model = model;
        this.assignedBoids = assignedBoids;
        this.barrier = barrier;
        this.simulator = simulator;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Se la simulazione è in pausa, attende senza aggiornare i boids
                while (simulator.isPaused()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                long startTime = System.currentTimeMillis();
                // Ora aggiorna i boids
                for (Boid boid : assignedBoids) {
                    int index = model.getBoidIndex(boid); 
                    boid.updateState(model); 
                    model.updateBoid(index, boid); 
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("Worker " + this.getName() + " update time: " + duration + " ms");
                try {
                    barrier.await(); // Attende che tutti i workers abbiano finito di aggiornare i boids
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (BrokenBarrierException e) {
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel worker thread: " + e.getMessage());
        }
    }
}