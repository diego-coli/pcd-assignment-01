package pcd.ass01.concurrent.TaskBased;

import pcd.ass01.concurrent.Simulator;
import pcd.ass01.model.BoidModel;
import pcd.ass01.model.Boid;
import pcd.ass01.view.BoidsView;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    
    private static final int FRAMERATE = 30;
    private int framerate;
    // Task-based approach
    private static ExecutorService executor;
    private List<Future<?>> futures;
    
    private static volatile boolean paused = false;
    
    public BoidsSimulator(BoidModel model) {
        this.model = model;
        this.view = Optional.empty();
        futures = new ArrayList<>();
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    @Override
    public boolean isPaused() {
        return paused;
    }
    
    @Override
    public void togglePause() {
        paused = !paused;
    }
    
    @Override
    public void attachView(Object view) {
        if (view instanceof BoidsView) {
            this.view = Optional.of((BoidsView)view);
        }
    }
    public void setPaused(boolean paused) {
        BoidsSimulator.paused = paused;
    }

    @Override
    public void stop() {
        paused = true;
        executor.shutdownNow();
    }

    @Override
    public void start() {
        paused = false;
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        runSimulation();
    }
    
    @Override
    public void runSimulation() {
        while (true) {
            if (paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            
            // Update
            futures.clear();
            List<Boid> boids = model.getBoids();
            int numProcessors = Runtime.getRuntime().availableProcessors();
            // Suddivisione dei boids in batch , Max serve per evitare divisione per 0
            int batchSize = Math.max(1, boids.size() / numProcessors);

            // Creazione di un task per ogni batch. il for serve per creare i task e assegnarli ai thread e non per eseguire il task e aspettare il risultato e poi passare al prossimo.
            for (int i = 0; i < numProcessors; i++) {
                // Calcolo dell'indice di inizio e fine del batch
                int start = i * batchSize;
                // Se è l'ultimo batch, la fine è la fine della lista , se no è la somma dell'indice di inizio e la dimensione del batch, evitando di andare oltre la fine della lista
                int end = (i == numProcessors - 1) ? boids.size() : start + batchSize;
                
                // Creazione di un task per il batch
                if (start < boids.size()) {
                    
                    List<Boid> boidBatch = boids.subList(start, end);

                    // Aggiunta del task alla lista dei task
                    futures.add(executor.submit(() -> {
                        // Esecuzione del task
                        for (Boid boid : boidBatch) {
                            // Aggiornamento dello stato del boid
                            boid.updateState(model);
                            // Aggiornamento del modello con il boid aggiornato
                            int index = model.getBoidIndex(boid);
                            model.updateBoid(index, boid);
                        }
                    }));
                }
            }

            // Attesa di tutti i task
            for (Future<?> future : futures) {
                // Attesa del completamento del task
                try {
                    // Il metodo get() è bloccante, attende il completamento del task e restituisce il risultato
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
           
            // Dopo che il task è completato, procedi direttamente con il rendering
            if (view.isPresent()) {
                long t0 = System.currentTimeMillis();
                view.get().update(framerate);
                long t1 = System.currentTimeMillis();
                long dtElapsed = t1 - t0;
                long framratePeriod = 1000 / FRAMERATE;
                if (dtElapsed < framratePeriod) {
                    try {
                        Thread.sleep(framratePeriod - dtElapsed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000 / dtElapsed);
                }
            }
        }
    }
}
