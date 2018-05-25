package ant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExplorationAnt extends Ant {
    public AntDestination destination;
    private List<List<AntReceiver>> paths;
    private List<AntReceiver> travelledPath;
    private Map<Class<?>, Double> chargingPointOccupations;
    private int hopCount;

    public ExplorationAnt(AntReceiver source, AntDestination destination) {
        super(source);
        this.paths = new ArrayList<>();
        this.travelledPath = new ArrayList<>();
        this.destination = destination;
        this.hopCount = 1;
        this.chargingPointOccupations = null;
    }

    public ExplorationAnt(AntReceiver source, AntDestination destination, int hopCount) {
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


    public List<List<AntReceiver>> getPaths() {
        if (paths.isEmpty()) {
            List<List<AntReceiver>> path = new ArrayList<>();
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
    public void addPathEntries(List<List<AntReceiver>> paths) {
        for (List<AntReceiver> path: paths) {
            path.add(0, getSecondaryAgent());
            this.paths.add(path);
        }
    }

    public List<AntReceiver> addHopTravelledPath(AntReceiver hop) {
        travelledPath.add(hop);
        return travelledPath;
    }

    public enum AntDestination {
        Order, ChargingPoint
    }
}
