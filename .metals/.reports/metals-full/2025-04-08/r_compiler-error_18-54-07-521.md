file://<WORKSPACE>/src/main/java/pcd/ass01/concurrent/thread/BoidsSimulator.java
### java.util.NoSuchElementException: next on empty iterator

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file://<WORKSPACE>/src/main/java/pcd/ass01/concurrent/thread/BoidsSimulator.java
text:
```scala
package pcd.ass01.concurrent.thread;

import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

import pcd.ass01.model.*;
import pcd.ass01.view.BoidsView;

import java.util.ArrayList;
import java.util.List;
import pcd.ass01.concurrent.Simulator;;

public class BoidsSimulator implements Simulator {
    private final BoidModel model;
    private Optional<BoidsView> view;
    private CyclicBarrier barrier;
    private static final int FRAMERATE = 30;
    private int framerate;
    private final List<BoidWorker> workers;
    private static volatile boolean paused = false;
    
    private Thread simulationThread;
    private volatile boolean running = false;

    public BoidsSimulator(BoidModel model, int numWorkers) {
        this.model = model;
        this.view = Optional.empty();
        this.workers = new ArrayList<>();
        
        // Rimuovi completamente la creazione dei worker dal costruttore
        // I worker verranno creati solo quando viene chiamato start()
    }

    private void assignBoidsToWorkers(BoidModel model, int numWorkers, int batchSize, List<Boid> boids) {
        for (int i = 0; i < numWorkers; i++) {
            int start = i * batchSize;
            int end = (i == numWorkers - 1) ? boids.size() : start + batchSize;
            
            // Usa una copia dei boid invece di una vista
            List<Boid> assignedBoids = new ArrayList<>(boids.subList(start, end));
            
            BoidWorker worker = new BoidWorker(model, assignedBoids, barrier, this);
            workers.add(worker);
        }
    }

    @Override
    public  boolean isPaused() {
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
        // Interrompi il thread principale della simulazione
        running = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        
        // Resetta la barriera prima di interrompere i worker
        if (barrier != null) {
            barrier.reset();
        }
        
        // Interrompi tutti i worker
        for (BoidWorker worker : workers) {
            worker.interrupt();
        }
        
        // Attendi che i worker terminino
        for (BoidWorker worker : workers) {
            try {
                if (worker.isAlive()) {
                    worker.join(1000); // Attendi al massimo 1 secondo
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        workers.clear();
    }

    @Override
    public void start() {
        int numWorkers = Runtime.getRuntime().availableProcessors();
        
        // Crea la barriera prima di creare i worker
        this.barrier = new CyclicBarrier(numWorkers);
        
        // Ora crea i worker (la lista dovrebbe essere vuota se stop() è stato chiamato correttamente)
        List<Boid> boids = model.getBoids();
        int batchSize = boids.size() / numWorkers;
        assignBoidsToWorkers(model, numWorkers, batchSize, boids);
        
        // Avvia tutti i worker thread
        for (BoidWorker worker : workers) {
            worker.start();
        }
        
        // Avvia un nuovo thread per la simulazione
        running = true;
        simulationThread = new Thread(this::runSimulation);
        simulationThread.start();
    }

    public void runSimulation() {
        // Rimuovi l'avvio dei worker thread dato che ora è gestito da start()
        
        while (running) {
            if(paused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
                continue;
            }

            try {
                model.waitForUpdate();
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            }
            
            // Rendering
            if (view.isPresent()) {
                var t0 = System.currentTimeMillis();
                view.get().update(framerate);
                var t1 = System.currentTimeMillis();
                var dtElapsed = t1 - t0;
                var framratePeriod = 1000 / FRAMERATE;

                if (dtElapsed < framratePeriod) {
                    try {
                        Thread.sleep(framratePeriod - dtElapsed);
                    } catch (Exception ignored) {}
                    framerate = FRAMERATE;
                } else {
                    framerate = (int) (1000 / dtElapsed);
                }
            }
        }
    }
}

```



#### Error stacktrace:

```
scala.collection.Iterator$$anon$19.next(Iterator.scala:973)
	scala.collection.Iterator$$anon$19.next(Iterator.scala:971)
	scala.collection.mutable.MutationTracker$CheckedIterator.next(MutationTracker.scala:76)
	scala.collection.IterableOps.head(Iterable.scala:222)
	scala.collection.IterableOps.head$(Iterable.scala:222)
	scala.collection.AbstractIterable.head(Iterable.scala:935)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:164)
	dotty.tools.pc.CachingDriver.run(CachingDriver.scala:45)
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.SimpleCollector.<init>(PcCollector.scala:351)
	dotty.tools.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector$lzyINIT1(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:63)
	dotty.tools.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:88)
	dotty.tools.pc.ScalaPresentationCompiler.semanticTokens$$anonfun$1(ScalaPresentationCompiler.scala:111)
```
#### Short summary: 

java.util.NoSuchElementException: next on empty iterator