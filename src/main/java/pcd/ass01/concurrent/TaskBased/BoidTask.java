package pcd.ass01.concurrent.TaskBased;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;
import java.util.List;
import java.util.ArrayList;

public class BoidTask implements Runnable {
    private final BoidModel model;
    private final List<Boid> assignedBoids;
    private final BoidsSimulator simulator;

    public BoidTask(BoidModel model, List<Boid> assignedBoids, BoidsSimulator simulator) {
        this.model = model;
        // Crea una copia della lista di boid per evitare problemi di concorrenza
        this.assignedBoids = new ArrayList<>(assignedBoids);
        this.simulator = simulator;
    }

    @Override
    public void run() {
        // Verifica se la simulazione è ancora in esecuzione (non in fase di stop/reset)
        if (!simulator.isRunning()) {
            return; // Esci subito se la simulazione è stata fermata
        }
        
        // Attende se la simulazione è in pausa
        while (simulator.isPaused() && simulator.isRunning()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // Esce in caso di interruzione
            }
        }
        
        // Aggiorna tutti i boid assegnati
        for (Boid boid : assignedBoids) {
            // Verifica che la simulazione sia ancora attiva
            if (!simulator.isRunning()) {
                return;
            }
            
            boid.updateState(model);
            int index = model.getBoidIndex(boid);
            
            // Verifica che il boid esista ancora nel modello
            if (index >= 0) {
                model.updateBoid(index, boid);
            }
            // Se index è -1, il boid non esiste più nel modello (è stato rimosso durante il reset)
        }
    }
}
