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
import java.util.concurrent.TimeUnit;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    
    private static final int FRAMERATE = 30;
    private int framerate;
    // Task-based approach
    private ExecutorService executor;  // Rimuovi static per evitare problemi con più istanze
    private List<Future<?>> futures;
    
    private volatile boolean paused = false;  // Rimuovi static
    private volatile boolean running = false; // Aggiungi per controllare il loop
    private Thread simulationThread;          // Aggiungi per tenere traccia del thread principale
    
    public BoidsSimulator(BoidModel model) {
        this.model = model;
        this.view = Optional.empty();
        futures = new ArrayList<>();
        // Non creare l'executor qui, lo faremo in start()
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
    
    @Override
    public void stop() {
        // Prima imposta running a false per far terminare il loop
        running = false;
        paused = true;
        
        // Interrompi il thread principale
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        
        // Shutdown dell'executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                // Aspetta fino a 3 secondi per la terminazione
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Cancella le future
        futures.clear();
    }

    @Override
    public void start() {
        // Se già in esecuzione, non fare nulla
        if (running) {
            return;
        }
        
        // Imposta lo stato
        paused = false;
        running = true;
        
        // Crea un nuovo executor
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        futures = new ArrayList<>();
        
        // Avvia il thread di simulazione
        simulationThread = new Thread(this::runSimulation);
        simulationThread.start();
    }

    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void runSimulation() {
        // Cambiato da while(true) a while(running) per permettere la terminazione
        while (running) {
            if (paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!running) return; // Esci se non più in esecuzione
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            
            // Update
            futures.clear();
            List<Boid> boids = model.getBoids();
            int numProcessors = Runtime.getRuntime().availableProcessors();
            int batchSize = Math.max(1, boids.size() / numProcessors);

            // Creazione e sottomissione dei task
            for (int i = 0; i < numProcessors && running; i++) {
                int start = i * batchSize;
                int end = (i == numProcessors - 1) ? boids.size() : start + batchSize;
                
                if (start < boids.size()) {
                    // Crea una copia della sottolista per evitare problemi di concorrenza
                    final List<Boid> boidBatch = new ArrayList<>(boids.subList(start, end));
                    
                    futures.add(executor.submit(() -> {
                        for (Boid boid : boidBatch) {
                            if (!running) return; // Esci se non più in esecuzione
                            boid.updateState(model);
                            int index = model.getBoidIndex(boid);
                            if (index >= 0) {
                                model.updateBoid(index, boid);
                            }
                        }
                    }));
                }
            }

            // Attesa di tutti i task
            for (Future<?> future : futures) {
                try {
                    if (!running) break; // Esci se non più in esecuzione
                    future.get();
                } catch (Exception e) {
                    if (!running) break; // Ignora le eccezioni se stiamo terminando
                }
            }
            
            // Rendering
            if (running && view.isPresent()) {
                long t0 = System.currentTimeMillis();
                view.get().update(framerate);
                long t1 = System.currentTimeMillis();
                long dtElapsed = t1 - t0;
                long framratePeriod = 1000 / FRAMERATE;
                if (dtElapsed < framratePeriod) {
                    try {
                        Thread.sleep(framratePeriod - dtElapsed);
                    } catch (InterruptedException e) {
                        if (!running) return; // Esci se non più in esecuzione
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
