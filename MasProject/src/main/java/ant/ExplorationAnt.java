package ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExplorationAnt extends Ant {
    public AntDestination destination;
    private List<List<AntUser>> paths;
    private List<AntUser> travelledPath;
    private Map<Class<?>, Double> chargingPointOccupations;
    private int hopCount;

    public ExplorationAnt(AntUser source, AntDestination destination) {
        this(source, destination, 1);
    }

    public ExplorationAnt(AntUser source, AntDestination destination, int hopCount) {
        super(source);
        this.paths = new ArrayList<>();
        this.travelledPath = new ArrayList<>();
        this.destination = destination;
        this.hopCount = hopCount;
        this.chargingPointOccupations = null;
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

    @Override
    public void returnToPrimaryAgent() {
        primaryAgent.receiveExplorationAnt(this);
    }

    public enum AntDestination {
        Order, ChargingPoint
    }
}
