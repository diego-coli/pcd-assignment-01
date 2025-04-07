package pcd.ass01.concurrent.virtualThreads;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class VirtualBoid implements Runnable {
    private final BoidModel model;
    private final Boid boid;
    private final CyclicBarrier barrier;
    private final BoidsSimulator simulator;

    public VirtualBoid(BoidModel model, Boid boid, CyclicBarrier barrier, BoidsSimulator simulator) {
        this.model = model;
        this.boid = boid;
        this.barrier = barrier;
        this.simulator = simulator;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Gestione della pausa
                while (simulator.isPaused() && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(50);
                }
                
                if (Thread.currentThread().isInterrupted() || !simulator.isRunning()) {
                    break;
                }
                
                // Aggiorna il boid
                boid.updateState(model);
                int index = model.getBoidIndex(boid);
                if (index >= 0) {
                    model.updateBoid(index, boid);
                }
                
                try {
                    // Sincronizzazione con gli altri thread
                    barrier.await();
                } catch (BrokenBarrierException e) {
                    // Barriera rotta (probabilmente durante reset)
                    break;
                }
            }
        } catch (InterruptedException e) {
            // Thread interrotto, esci silenziosamente
            Thread.currentThread().interrupt();
        }
    }
}
