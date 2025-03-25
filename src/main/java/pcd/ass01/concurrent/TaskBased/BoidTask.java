package pcd.ass01.concurrent.TaskBased;

import pcd.ass01.model.Boid;
import pcd.ass01.model.BoidModel;
import java.util.List;

public class BoidTask implements Runnable {
    private final BoidModel model;
    private final List<Boid> assignedBoids;
    private final BoidsSimulator simulator; // Utilizzato per controllare lo stato di pausa

    public BoidTask(BoidModel model, List<Boid> assignedBoids, BoidsSimulator simulator) {
        this.model = model;
        this.assignedBoids = assignedBoids;
        this.simulator = simulator;
    }

    @Override
    public void run() {
        // Attende se la simulazione è in pausa, evitando esecuzioni non necessarie
        while (simulator.isPaused()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // Esce in caso di interruzione
            }
        }
        
        // Aggiorna tutti i boid assegnati
        for (Boid boid : assignedBoids) {
            boid.updateState(model);
            int index = model.getBoidIndex(boid);
            model.updateBoid(index, boid);
        }
    }
}
