package pcd.ass01.model;

import pcd.ass01.common.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;


public class Boid {
    private P2d pos;
    private V2d vel;
    private final Lock lock;

    public Boid(P2d pos, V2d vel) {
        this.pos = pos;
        this.vel = vel;
        this.lock = new ReentrantLock();
    }

    public P2d getPos() {
        lock.lock();
        try {
            return pos;
        } finally {
            lock.unlock();
        }
    }

    public V2d getVel() {
        lock.lock();
        try {
            return vel;
        } finally {
            lock.unlock();
        }
    }

    public void updateState(BoidModel model) {
        lock.lock();
        try {
            List<Boid> nearbyBoids = getNearbyBoids(model);
            V2d separation = calculateSeparation(nearbyBoids, model);
            V2d alignment = calculateAlignment(nearbyBoids, model);
            V2d cohesion = calculateCohesion(nearbyBoids, model);

            vel = vel.sum(alignment.mul(model.getAlignmentWeight()))
                    .sum(separation.mul(model.getSeparationWeight()))
                    .sum(cohesion.mul(model.getCohesionWeight()));

            double speed = vel.abs();
            if (speed > model.getMaxSpeed()) {
                vel = vel.getNormalized().mul(model.getMaxSpeed());
            }
            pos = pos.sum(vel);

            if (pos.x() < model.getMinX()) pos = pos.sum(new V2d(model.getWidth(), 0));
            if (pos.x() >= model.getMaxX()) pos = pos.sum(new V2d(-model.getWidth(), 0));
            if (pos.y() < model.getMinY()) pos = pos.sum(new V2d(0, model.getHeight()));
            if (pos.y() >= model.getMaxY()) pos = pos.sum(new V2d(0, -model.getHeight()));
        } finally {
            lock.unlock();
        }
    }

    private List<Boid> getNearbyBoids(BoidModel model) {
        List<Boid> list = new ArrayList<>();
        for (Boid other : model.getBoids()) {
            if (other != this) {
                P2d otherPos = other.getPos();
                double distance = pos.distance(otherPos);
                if (distance < model.getPerceptionRadius()) {
                    list.add(other);
                }
            }
        }
        return list;
    }

    private V2d calculateAlignment(List<Boid> nearbyBoids, BoidModel model) {
        double avgVx = 0, avgVy = 0;
        if (!nearbyBoids.isEmpty()) {
            for (Boid other : nearbyBoids) {
                V2d otherVel = other.getVel();
                avgVx += otherVel.x();
                avgVy += otherVel.y();
            }
            avgVx /= nearbyBoids.size();
            avgVy /= nearbyBoids.size();
            return new V2d(avgVx - vel.x(), avgVy - vel.y()).getNormalized();
        }
        return new V2d(0, 0);
    }

    private V2d calculateCohesion(List<Boid> nearbyBoids, BoidModel model) {
        double centerX = 0, centerY = 0;
        if (!nearbyBoids.isEmpty()) {
            for (Boid other : nearbyBoids) {
                P2d otherPos = other.getPos();
                centerX += otherPos.x();
                centerY += otherPos.y();
            }
            centerX /= nearbyBoids.size();
            centerY /= nearbyBoids.size();
            return new V2d(centerX - pos.x(), centerY - pos.y()).getNormalized();
        }
        return new V2d(0, 0);
    }

    private V2d calculateSeparation(List<Boid> nearbyBoids, BoidModel model) {
        double dx = 0, dy = 0;
        int count = 0;
        for (Boid other : nearbyBoids) {
            P2d otherPos = other.getPos();
            double distance = pos.distance(otherPos);
            if (distance < model.getAvoidRadius()) {
                dx += pos.x() - otherPos.x();
                dy += pos.y() - otherPos.y();
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
