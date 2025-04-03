package pcd.ass01;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Test semplificato per JPF che evita classi problematiche
 */
public class ThreadBoidsJPFTest {
    
    // Versione ridotta per testing
    private static final int NUM_BOIDS = 4;
    private static final int NUM_WORKERS = 2;
    private static final int MAX_ITERATIONS = 2;
    
    public static void main(String[] args) {
        testForDeadlocks();
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
        
        List<Boid> allBoids = model.getBoids();
        List<SimpleWorker> workers = new ArrayList<>();
        
        // Oggetti per sincronizzazione semplice
        Object[] locks = new Object[NUM_WORKERS + 1];
        for (int i = 0; i <= NUM_WORKERS; i++) {
            locks[i] = new Object();
        }
        
        // Crea i worker con sincronizzazione semplice
        for (int i = 0; i < NUM_WORKERS; i++) {
            int start = i * (NUM_BOIDS / NUM_WORKERS);
            int end = (i == NUM_WORKERS - 1) ? NUM_BOIDS : start + (NUM_BOIDS / NUM_WORKERS);
            
            List<Boid> workerBoids = new ArrayList<>(allBoids.subList(start, end));
            SimpleWorker worker = new SimpleWorker(model, workerBoids, i, locks);
            workers.add(worker);
            worker.start();
        }
        
        // Thread principale
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // Segnala ai worker di procedere
            for (int i = 0; i < NUM_WORKERS; i++) {
                synchronized(locks[i]) {
                    locks[i].notify();
                }
            }
            
            // Attendi che tutti i worker finiscano
            synchronized(locks[NUM_WORKERS]) {
                try {
                    locks[NUM_WORKERS].wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            System.out.println("Main thread completato step " + iter);
        }
        
        // Termina i thread
        for (SimpleWorker worker : workers) {
            worker.terminate();
        }
    }
    
    static class SimpleWorker extends Thread {
        private final BoidModel model;
        private final List<Boid> assignedBoids;
        private final int id;
        private final Object[] locks;
        private boolean running = true;
        
        public SimpleWorker(BoidModel model, List<Boid> boids, int id, Object[] locks) {
            this.model = model;
            this.assignedBoids = boids;
            this.id = id;
            this.locks = locks;
        }
        
        public void terminate() {
            running = false;
            interrupt();
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    // Attendi segnale per procedere
                    synchronized(locks[id]) {
                        locks[id].wait();
                    }
                    
                    if (!running) break;
                    
                    // Aggiorna i boid
                    for (Boid boid : assignedBoids) {
                        boid.updateState(model);
                        int index = model.getBoidIndex(boid);
                        if (index >= 0) {
                            model.updateBoid(index, boid);
                        }
                    }
                    
                    // Segnala completamento
                    synchronized(locks[locks.length - 1]) {
                        locks[locks.length - 1].notify();
                    }
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
        }
    }
}