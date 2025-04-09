/*
    COMPONENTI DEL GRUPPO:
    Arcese Gabriele
    Colì Diego
    Costantini Marco
    Meco Daniel
 */

package pcd.ass01;

import pcd.ass01.model.BoidModel;
import pcd.ass01.concurrent.tasks.BoidsSimulator;
import pcd.ass01.view.BoidsView;
import javax.swing.JOptionPane;

public class TaskBoidsSimulation {

    final static double SEPARATION_WEIGHT = 1.0;
    final static double ALIGNMENT_WEIGHT = 1.0;
    final static double COHESION_WEIGHT = 1.0;

    final static int ENVIRONMENT_WIDTH = 1000; 
    final static int ENVIRONMENT_HEIGHT = 1000;
    static final double MAX_SPEED = 4.0;
    static final double PERCEPTION_RADIUS = 50.0;
    static final double AVOID_RADIUS = 20.0;

    final static int SCREEN_WIDTH = 1000; 
    final static int SCREEN_HEIGHT = 800; 

    public static void main(String[] args) {      
        // Chiede all'utente di inserire il numero di boids
        String input = JOptionPane.showInputDialog("Inserisci il numero di boids:");
        int nBoids = 1500; // valore default
        try {
            nBoids = Integer.parseInt(input);
        } catch(NumberFormatException e) {
            System.out.println("Numero non valido, utilizzo valore di default 1500.");
        }
        
        var model = new BoidModel(
                        nBoids, 
                        SEPARATION_WEIGHT, ALIGNMENT_WEIGHT, COHESION_WEIGHT, 
                        ENVIRONMENT_WIDTH, ENVIRONMENT_HEIGHT,
                        MAX_SPEED,
                        PERCEPTION_RADIUS,
                        AVOID_RADIUS); 
        
        var sim = new BoidsSimulator(model);
        var view = new BoidsView(model, sim, SCREEN_WIDTH, SCREEN_HEIGHT);
        sim.attachView(view);
        sim.start();
    }
}
