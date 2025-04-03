package pcd.ass01.concurrent.virtualThread;

import pcd.ass01.concurrent.Simulator;
import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;
import pcd.ass01.view.BoidsView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    private CyclicBarrier barrier;
    private final List<Thread> virtualThreads = new ArrayList<>();
    private volatile boolean running = false;
    private volatile boolean paused = false;
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
        running = false;
        
        // Interrompi il thread principale
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        
        // Reset della barriera per evitare deadlock
        if (barrier != null) {
            barrier.reset();
        }
        
        // Interrompi tutti i virtual thread
        for (Thread vt : virtualThreads) {
            vt.interrupt();
        }
        
        // Attendi che i thread terminino (opzionale)
        for (Thread vt : virtualThreads) {
            try {
                vt.join(100); // Attende max 100ms per thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Pulisci la lista di virtual thread
        virtualThreads.clear();
    }
    
    @Override
    public void start() {
        // Se già in esecuzione, non fare nulla
        if (running) {
            return;
        }
        
        running = true;
        paused = false;
        
        // Ottieni la lista completa di boid
        List<Boid> boids = model.getBoids();
        
        // Crea una barriera con tanti parti quanti i boid + 1 (thread principale)
        barrier = new CyclicBarrier(boids.size() + 1);
        
        // Crea e avvia un virtual thread per ogni boid
        for (int i = 0; i < boids.size(); i++) {
            final int index = i;
            final Boid boid = boids.get(i);
            
            // Usa Thread.ofVirtual() invece dell'executor
            Thread vThread = Thread.ofVirtual()
                                  .name("VirtualBoid-" + index)
                                  .start(() -> {
                                      new VirtualBoid(model, boid, barrier, this).run();
                                  });
            
            virtualThreads.add(vThread);
        }
        
        // Avvia il thread principale per il rendering
        simulationThread = Thread.ofPlatform()
                                .name("SimulationThread")
                                .start(this::runSimulation);
    }
    
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void runSimulation() {
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
                // Aspetta che tutti i boid completino l'aggiornamento
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
                // La barriera è stata resettata, probabilmente durante un reset
                if (!running) return;
            }
        }
    }
}
