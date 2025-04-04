package pcd.ass01;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;
import pcd.ass01.concurrent.thread.BoidWorker;
import pcd.ass01.concurrent.thread.BoidsSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

/**
 * Test per verificare la presenza di deadlock nell'approccio thread-based
 */
public class ThreadBoidsJPFTest {
    
    // Versione ridotta per testing
    private static final int NUM_BOIDS = 10;
    private static final int NUM_WORKERS = 2;
    private static final int MAX_ITERATIONS = 3;
    
    public static void main(String[] args) {
        testForDeadlocks();
        //testForRaceConditions();  // Aggiungi un secondo test
    }
    
    public static void testForDeadlocks() {
        // Crea un modello semplificato
        BoidModel model = new BoidModel(
            NUM_BOIDS, 
            1.0, 1.0, 1.0,
            100, 100,
            1.0,
            10.0,
            5.0
        );
        
        // Crea una barriera
        CyclicBarrier barrier = new CyclicBarrier(NUM_WORKERS + 1);
        
        // Prepara le liste di boid per i worker
        List<Boid> allBoids = model.getBoids();
        List<Thread> threads = new ArrayList<>();
        
        // Crea un MockSimulator che estende BoidsSimulator
        MockSimulator simulator = new MockSimulator(model);
        
        int batchSize = NUM_BOIDS / NUM_WORKERS;
        
        // Crea i worker thread
        for (int i = 0; i < NUM_WORKERS; i++) {
            int start = i * batchSize;
            int end = (i == NUM_WORKERS - 1) ? NUM_BOIDS : start + batchSize;
            
            List<Boid> workerBoids = new ArrayList<>(allBoids.subList(start, end));
            
            // BoidWorker estende Thread, quindi lo creiamo direttamente
            BoidWorker worker = new BoidWorker(model, workerBoids, barrier, simulator);
            threads.add(worker);
        }
        
        // Avvia i thread
        for (Thread t : threads) {
            t.start();
        }
        
        // Simula alcune iterazioni
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            try {
                // Sincronizzazione con tutti i worker
                barrier.await();
                System.out.println("Main thread completato step " + i);
                Thread.sleep(5); // Piccolo ritardo come nel codice originale
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Interrompi i thread
        for (Thread t : threads) {
            t.interrupt();
        }
        
        // Attendi la terminazione
        for (Thread t : threads) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("Test completato senza deadlock");
    }
    
    // MockSimulator che estende BoidsSimulator per compatibilità
    static class MockSimulator extends BoidsSimulator {
        private volatile boolean forcedPause = false;
        
        public MockSimulator(BoidModel model) {
            super(model,NUM_WORKERS);
        }
        
        @Override
        public boolean isPaused() { 
            return forcedPause;
        }
        
        public void setForcedPause(boolean paused) {
            this.forcedPause = paused;
        }
    }
}