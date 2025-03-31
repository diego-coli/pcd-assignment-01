package pcd.ass01.concurrent;

public interface Simulator {
    void runSimulation();
    boolean isPaused();
    void togglePause();
    void attachView(Object view); // oppure, se c'è una classe specifica, ad es. BoidsView
    void stop(); // Aggiunto per fermare la simulazione
    void start(); // Aggiunto per avviare la simulazione
}
