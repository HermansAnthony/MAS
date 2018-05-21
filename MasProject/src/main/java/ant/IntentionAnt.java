package ant;

import pdp.Drone;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;
    public double merit;
    public AntReceiver destination;

    public IntentionAnt(Drone primaryAgent, AntReceiver destination, double merit) {
        super(primaryAgent);
        this.reservationApproved = false;
        this.destination = destination;
        this.merit = merit;
    }
}
