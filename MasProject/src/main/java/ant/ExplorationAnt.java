package ant;

import pdp.Drone;
import util.ChargerReservation;
import util.ExplorationPath;
import util.Range;

import java.util.ArrayList;
import java.util.List;

public class ExplorationAnt extends Ant {
    private List<ExplorationPath> paths;
    private List<AntUser> travelledPath;
    private Drone drone;
    private int hopCount;

    private double remainingBattery;
    private long resultingTime;

    public ExplorationAnt(AntUser source, double remainingBattery, long resultingTime) {
        this(source, remainingBattery, resultingTime, 1);
    }

    public ExplorationAnt(AntUser source, double remainingBattery, long resultingTime, int hopCount) {
        super(source);
        this.paths = new ArrayList<>();
        this.travelledPath = new ArrayList<>();
        this.drone = null;
        this.hopCount = hopCount;

        this.remainingBattery = remainingBattery;
        this.resultingTime = resultingTime;
    }

    public int getHopCount() {
        return hopCount;
    }


    public List<ExplorationPath> getPaths() {
        return paths;
    }


    /**
     * This method processed all the given paths, which are explored by succeeding exploration ants.
     * For each path given, the exploration ant adds its own destination in front of the path.
     * For example:
     *   As a reaction to this exploration ant, 2 exploration ants have been sent out to OrderX and OrderY respectively
     *   OrderX and OrderY return the following paths:
     *     - [OrderX]
     *     - [OrderZ, OrderY]
     *     - [OrderA, OrderY]
     *   OrderX is in this case an end destination, while the paths explored by the second exploration ant have 2 hops.
     *
     *   This exploration ant will take all the respective explored paths, and add its own destination to them as follows:
     *     - [OwnDestination, OrderX]
     *     - [OwnDestination, OrderZ, OrderY]
     *     - [OwnDestination, OrderA, OrderY]
     *
     * These paths are propagated to the first originating node, which will contain all the paths explored.
     * @param paths The paths which have been explored by additional exploration ants.
     */
    public void addPathEntries(List<ExplorationPath> paths) {
        for (ExplorationPath path: paths) {
            path.addNodeToPath(getSecondaryAgent(), 0);
            this.paths.add(path);
        }
    }

    public List<AntUser> addHopTravelledPath(AntUser hop) {
        travelledPath.add(hop);
        return travelledPath;
    }

    public void setTravelledPath(List<AntUser> travelledPath) {
        this.travelledPath = travelledPath;
    }

    @Override
    public void returnToPrimaryAgent() {
        primaryAgent.receiveExplorationAnt(this);
    }

    public Double getDroneCapacity() {
        return drone.getCapacity();
    }


    public double getRemainingBattery() {
        return remainingBattery;
    }

    public long getResultingTime() {
        return resultingTime;
    }

    public void setRemainingBattery(double remainingBattery) {
        this.remainingBattery = remainingBattery;
    }

    public void setResultingTime(long resultingTime) {
        this.resultingTime = resultingTime;
    }

    public Range getDroneSpeedRange() {
        return drone.getSpeedRange();
    }

    public void buildInitialExplorationPath(ChargerReservation bestReservation) {
        if (!paths.isEmpty()) {
            System.err.println("This method should only be called if the path is still empty.");
            return;
        }
        paths.add(new ExplorationPath(this.remainingBattery, this.resultingTime, this.getSecondaryAgent()));
        paths.get(0).setBestReservation(bestReservation);
        paths.get(0).setResultingTime(resultingTime);
        paths.get(0).setRemainingBattery(remainingBattery);
    }

    public void setDrone(Drone drone) {
        this.drone = drone;
    }

    public Drone getDrone() {
        return this.drone;
    }
}
