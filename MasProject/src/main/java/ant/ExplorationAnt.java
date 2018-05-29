package ant;

import java.util.*;

public class ExplorationAnt extends Ant {
    private List<List<AntUser>> paths;
    private List<AntUser> travelledPath;
    private Map<Class<?>, Double> chargingPointOccupations;
    private Optional<Double> droneCapacity;
    private int hopCount;

    public ExplorationAnt(AntUser source) {
        this(source, 1);
    }

    public ExplorationAnt(AntUser source, int hopCount) {
        super(source);
        this.paths = new ArrayList<>();
        this.travelledPath = new ArrayList<>();
        this.chargingPointOccupations = null;
        this.droneCapacity = Optional.empty();
        this.hopCount = hopCount;
    }

    public void setChargingPointOccupations(Map<Class<?>, Double> chargingPointOccupation) {
        this.chargingPointOccupations = chargingPointOccupation;
    }

    public Map<Class<?>, Double> getChargingPointOccupations() {
        return chargingPointOccupations;
    }

    public int getHopCount() {
        return hopCount;
    }


    public List<List<AntUser>> getPaths() {
        if (paths.isEmpty()) {
            List<List<AntUser>> path = new ArrayList<>();
            path.add(new ArrayList<>(Arrays.asList(this.getSecondaryAgent())));
            return path;
        } else {
            return paths;
        }
    }


    /**
     * TODO: remove/write proper documentation here
     * source: Drone, destination: Order
     *
     * -> Parcel2....
     * <-
     *
     * [parcel2, parcel3]
     * @param paths
     */
    public void addPathEntries(List<List<AntUser>> paths) {
        for (List<AntUser> path: paths) {
            path.add(0, getSecondaryAgent());
            this.paths.add(path);
        }
    }

    public List<AntUser> addHopTravelledPath(AntUser hop) {
        travelledPath.add(hop);
        return travelledPath;
    }

    public List<AntUser> getTravelledPath() {
        return travelledPath;
    }

    public void setTravelledPath(List<AntUser> travelledPath) {
        this.travelledPath = travelledPath;
    }

    @Override
    public void returnToPrimaryAgent() {
        primaryAgent.receiveExplorationAnt(this);
    }

    public Optional<Double> getDroneCapacity() {
        return droneCapacity;
    }

    public void setDroneCapacity(Double droneCapacity) {
        this.droneCapacity = Optional.of(droneCapacity);
    }
}
