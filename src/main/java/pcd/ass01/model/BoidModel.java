package pcd.ass01.model;

import pcd.ass01.common.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoidModel {
    private final List<Boid> boids;
    private double separationWeight;
    private double alignmentWeight;
    private double cohesionWeight;
    private final double width;
    private final double height;
    private final double maxSpeed;
    private final double perceptionRadius;
    private final double avoidRadius;
    private final Lock lock;
    private final Condition isUpdated;

    public BoidModel(int nboids, double initialSeparationWeight, double initialAlignmentWeight, double initialCohesionWeight,
                     double width, double height, double maxSpeed, double perceptionRadius, double avoidRadius) {
        separationWeight = initialSeparationWeight;
        alignmentWeight = initialAlignmentWeight;
        cohesionWeight = initialCohesionWeight;
        this.width = width;
        this.height = height;
        this.maxSpeed = maxSpeed;
        this.perceptionRadius = perceptionRadius;
        this.avoidRadius = avoidRadius;
        boids = new ArrayList<>();
        generateBoids(boids, nboids, width, height, maxSpeed);
        lock = new ReentrantLock();
        isUpdated = lock.newCondition();
    }

    private void generateBoids(List<Boid> boids, int nboids, double width, double height, double maxSpeed) {
        for (int i = 0; i < nboids; i++) {
            P2d pos = new P2d(-width / 2 + Math.random() * width, -height / 2 + Math.random() * height);
            V2d vel = new V2d(Math.random() * maxSpeed / 2 - maxSpeed / 4, Math.random() * maxSpeed / 2 - maxSpeed / 4);
            boids.add(new Boid(pos, vel));
        }
    }

    public List<Boid> getBoids() {
        lock.lock();
        try {
            return new ArrayList<>(boids);
        } finally {
            lock.unlock();
        }
    }

    public int getBoidIndex(Boid boid) {
        lock.lock();
        try {
            return boids.indexOf(boid);
        } finally {
            lock.unlock();
        }
    }

    public void updateBoid(int index, Boid newBoid) {
        lock.lock();
        try {
            boids.set(index, newBoid);
            // Notifica tutti i threads in attesa che lo stato è cambiato
            isUpdated.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Metodo che fa attendere un thread finché non viene notificato un aggiornamento
    public void waitForUpdate() throws InterruptedException {
        lock.lock();
        try {
            // Thread si blocca finché non viene segnalato
            isUpdated.await();
        } finally {
            lock.unlock();
        }
    }

    public double getMinX() { return -width / 2; }
    public double getMaxX() { return width / 2; }
    public double getMinY() { return -height / 2; }
    public double getMaxY() { return height / 2; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getAvoidRadius() { return avoidRadius; }
    public double getPerceptionRadius() { return perceptionRadius; }

    public void setSeparationWeight(double value) {
        lock.lock();
        try {
            separationWeight = value;
        } finally {
            lock.unlock();
        }
    }

    public void setAlignmentWeight(double value) {
        lock.lock();
        try {
            alignmentWeight = value;
        } finally {
            lock.unlock();
        }
    }

    public void setCohesionWeight(double value) {
        lock.lock();
        try {
            cohesionWeight = value;
        } finally {
            lock.unlock();
        }
    }

    public double getSeparationWeight() {
        lock.lock();
        try {
            return separationWeight;
        } finally {
            lock.unlock();
        }
    }

    public double getCohesionWeight() {
        lock.lock();
        try {
            return cohesionWeight;
        } finally {
            lock.unlock();
        }
    }

    public double getAlignmentWeight() {
        lock.lock();
        try {
            return alignmentWeight;
        } finally {
            lock.unlock();
        }
    }

    // Metodo che resetta la lista di boids e ne crea una nuova
    public void resetWithNewBoids(int nboids) {
        lock.lock();
        try {
            boids.clear();
            generateBoids(boids, nboids, width, height, maxSpeed);
            // Notifica tutti i threads in attesa che lo stato è cambiato
            isUpdated.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

