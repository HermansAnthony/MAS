package util;

import ant.AntUser;

import java.util.ArrayList;
import java.util.List;

public class ExplorationPath {
    private List<AntUser> path;
    private double remainingBattery;
    private long resultingTime;
    private ChargerReservation bestReservation;

    public ExplorationPath(double remainingBattery, long resultingTime) {
        path = new ArrayList<>();
        this.remainingBattery = remainingBattery;
        this.resultingTime = resultingTime;
        bestReservation = null;
    }

    public ExplorationPath(double remainingBattery, long resultingTime, AntUser firstNode) {
        this(remainingBattery, resultingTime);
        path.add(firstNode);
    }

    public void addNodeToPath(AntUser node) {
        addNodeToPath(node, path.size());
    }

    public void addNodeToPath(AntUser node, int index) {
        path.add(index, node);
    }


    public List<AntUser> getPath() {
        return path;
    }

    public void setBestReservation(ChargerReservation bestReservation) {
        this.bestReservation = bestReservation;
    }

    public void setResultingTime(long resultingTime) {
        this.resultingTime = resultingTime;
    }

    public void setRemainingBattery(double remainingBattery) {
        this.remainingBattery = remainingBattery;
    }

    public ChargerReservation getBestReservation() {
        return bestReservation;
    }

    public long getResultingTime() {
        return resultingTime;
    }

    public double getRemainingBattery() {
        return remainingBattery;
    }
}
