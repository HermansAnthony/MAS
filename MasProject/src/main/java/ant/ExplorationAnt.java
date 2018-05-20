package ant;

import pdp.Drone;

public class ExplorationAnt extends Ant {
    public AntDestination destination;
    private double chargingPointOccupation;

    public ExplorationAnt(Drone drone, AntDestination destination) {
        super(drone);
        this.destination = destination;
    }

    public void setChargingPointOccupation(double chargingPointOccupation) {
        this.chargingPointOccupation = chargingPointOccupation;
    }

    public double getChargingPointOccupation() {
        return chargingPointOccupation;
    }

    public enum AntDestination {
        Order, ChargingPoint;
    }
}
