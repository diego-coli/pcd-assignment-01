package pcd.ass01.concurrent.tasks;

import pcd.ass01.concurrent.Simulator;
import pcd.ass01.model.BoidModel;
import pcd.ass01.model.Boid;
import pcd.ass01.view.BoidsView;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    private ExecutorService executor;
    private CyclicBarrier barrier;
    private volatile boolean paused = false;
    private volatile boolean running = false;
    private Thread simulationThread;
    private static final int FRAMERATE = 30;
    private int framerate;
    
    public BoidsSimulator(BoidModel model) {
        this.model = model;
        this.view = Optional.empty();
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
        // Setta variabili a false per far terminare il loop
        running = false;
        paused = false;
        
        // Reset della barriera per evitare deadlock
        if (barrier != null) {
            barrier.reset();
        }
        
        // Interrompe il master thread
        if (simulationThread != null) {
            simulationThread.interrupt();
            try {
                // Aspetta fino a 500ms per la terminazione
                simulationThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Shutdown dell'executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                // Aspetta fino a 3s per la terminazione
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void start() {
        // Se già in esecuzione, non fa nulla
        if (running) {
            return;
        }
        
        running = true;
        paused = false;
        
        List<Boid> boids = model.getBoids();
        int numProcessors = Runtime.getRuntime().availableProcessors();
        int batchSize = Math.max(1, boids.size() / numProcessors);
        
        // Crea l'executor e la barriera
        executor = Executors.newFixedThreadPool(numProcessors);
        barrier = new CyclicBarrier(numProcessors + 1); // + 1 per il master thread
        
        // Crea i task persistenti una sola volta
        for (int i = 0; i < numProcessors; i++) {
            int start = i * batchSize;
            int end = (i == numProcessors - 1) ? boids.size() : start + batchSize;
            List<Boid> boidBatch = new ArrayList<>(boids.subList(start, end));
            executor.submit(new BoidTask(model, boidBatch, this, barrier)); // Un task per ogni batch
        }
        
        // Avvia il master thread
        simulationThread = new Thread(() -> runSimulation(barrier));
        simulationThread.setName("SimulationThread");
        simulationThread.start();
    }

    public boolean isRunning() {
        return running;
    }

    // Solo per compatibilità con interfaccia Simulator
    @Override
    public void runSimulation() {
        // Se non si ha una barriera, non si può sincronizzare correttamente
        if (barrier != null) {
            runSimulation(barrier);
        } else {
            System.err.println("Warning: runSimulation() chiamato senza barriera!");
        }
    }

    public void runSimulation(CyclicBarrier barrier) {
        while (running) {
            if (paused) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    if (!running) return;
                    Thread.currentThread().interrupt();
                }
            }
            
            try {
                // Sincronizzazione con tutti i tasks
                barrier.await();
                
                // Rendering
                if (view.isPresent()) {
                    long t0 = System.currentTimeMillis();
                    view.get().update(framerate);
                    long t1 = System.currentTimeMillis();
                    long dtElapsed = t1 - t0;
                    long frameRatePeriod = 1000 / FRAMERATE;
                    
                    if (dtElapsed < frameRatePeriod) {
                        Thread.sleep(frameRatePeriod - dtElapsed);
                        framerate = FRAMERATE;
                    } else {
                        framerate = (int) (1000 / dtElapsed);
                    }
                }
            } catch (InterruptedException e) {
                if (!running) return;
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                if (!running) return;
            }
        }
    }
}
