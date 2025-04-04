package pcd.ass01.model;

import pcd.ass01.common.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicReference;

public class Boid {
    // Usa AtomicReference per letture atomiche
    private final AtomicReference<P2d> pos;
    private final AtomicReference<V2d> vel;
    private final Lock lock = new ReentrantLock();

    public Boid(P2d pos, V2d vel) {
        this.pos = new AtomicReference<>(pos);
        this.vel = new AtomicReference<>(vel);
    }

    public P2d getPos() {
        // Lettura atomica, thread-safe
        P2d currentPos = pos.get();
        // Copia difensiva per evitare modifiche esterne
        return new P2d(currentPos.x(), currentPos.y());
    }

    public V2d getVel() {
        // Lettura atomica, thread-safe
        V2d currentVel = vel.get();
        // Copia difensiva per evitare modifiche esterne
        return new V2d(currentVel.x(), currentVel.y());
    }

    public void updateState(BoidModel model) {
        // Ottieni copie sicure per i calcoli
        P2d currentPos = getPos();
        V2d currentVel = getVel();
        
        // Calcoli fuori dal lock
        List<Boid> nearbyBoids = getNearbyBoids(model, currentPos);
        V2d separation = calculateSeparation(nearbyBoids, model, currentPos);
        V2d alignment = calculateAlignment(nearbyBoids, model, currentVel);
        V2d cohesion = calculateCohesion(nearbyBoids, model, currentPos);
        
        // Proteggiamo solo l'aggiornamento dello stato
        lock.lock();
        try {
            // Aggiornamento della velocità
            V2d newVel = currentVel.sum(alignment.mul(model.getAlignmentWeight()))
                    .sum(separation.mul(model.getSeparationWeight()))
                    .sum(cohesion.mul(model.getCohesionWeight()));

            double speed = newVel.abs();
            if (speed > model.getMaxSpeed()) {
                newVel = newVel.getNormalized().mul(model.getMaxSpeed());
            }
            
            // Aggiornamento atomico della velocità
            vel.set(newVel);
            
            // Aggiornamento della posizione
            P2d newPos = currentPos.sum(newVel);

            // Controllo dei bordi
            if (newPos.x() < model.getMinX()) newPos = newPos.sum(new V2d(model.getWidth(), 0));
            if (newPos.x() >= model.getMaxX()) newPos = newPos.sum(new V2d(-model.getWidth(), 0));
            if (newPos.y() < model.getMinY()) newPos = newPos.sum(new V2d(0, model.getHeight()));
            if (newPos.y() >= model.getMaxY()) newPos = newPos.sum(new V2d(0, -model.getHeight()));
            
            // Aggiornamento atomico della posizione
            pos.set(newPos);
        } finally {
            lock.unlock();
        }
    }

    private List<Boid> getNearbyBoids(BoidModel model, P2d myPos) {
        List<Boid> list = new ArrayList<>();
        
        for (Boid other : model.getBoids()) {
            if (other != this) {
                P2d otherPos = other.getPos();
                double distance = myPos.distance(otherPos);
                if (distance < model.getPerceptionRadius()) {
                    list.add(other);
                }
            }
        }
        return list;
    }

    private V2d calculateAlignment(List<Boid> nearbyBoids, BoidModel model, V2d currentVel) {
        double avgVx = 0, avgVy = 0;
        if (!nearbyBoids.isEmpty()) {
            for (Boid other : nearbyBoids) {
                V2d otherVel = other.getVel();
                avgVx += otherVel.x();
                avgVy += otherVel.y();
            }
            avgVx /= nearbyBoids.size();
            avgVy /= nearbyBoids.size();
            return new V2d(avgVx - currentVel.x(), avgVy - currentVel.y()).getNormalized();
        }
        return new V2d(0, 0);
    }

    private V2d calculateCohesion(List<Boid> nearbyBoids, BoidModel model, P2d currentPos) {
        double centerX = 0, centerY = 0;
        if (!nearbyBoids.isEmpty()) {
            for (Boid other : nearbyBoids) {
                P2d otherPos = other.getPos();
                centerX += otherPos.x();
                centerY += otherPos.y();
            }
            centerX /= nearbyBoids.size();
            centerY /= nearbyBoids.size();
            return new V2d(centerX - currentPos.x(), centerY - currentPos.y()).getNormalized();
        }
        return new V2d(0, 0);
    }

    private V2d calculateSeparation(List<Boid> nearbyBoids, BoidModel model, P2d currentPos) {
        double dx = 0, dy = 0;
        int count = 0;
        for (Boid other : nearbyBoids) {
            P2d otherPos = other.getPos();
            double distance = currentPos.distance(otherPos);
            if (distance < model.getAvoidRadius()) {
                dx += currentPos.x() - otherPos.x();
                dy += currentPos.y() - otherPos.y();
                count++;
            }
        }
        if (count > 0) {
            dx /= count;
            dy /= count;
            return new V2d(dx, dy).getNormalized();
        }
        return new V2d(0, 0);
    }
}
