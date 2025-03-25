package pcd.ass01.concurrent;

public interface Simulator {
    void runSimulation();
    boolean isPaused();
    void togglePause();
    void attachView(Object view); // oppure, se c'è una classe specifica, ad es. BoidsView
}
