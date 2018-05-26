package ant;

import pdp.Drone;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;
    public double merit;
    public AntUser destination;

    public IntentionAnt(Drone primaryAgent, AntUser destination, double merit) {
        super(primaryAgent);
        this.reservationApproved = false;
        this.destination = destination;
        this.merit = merit;
    }

    @Override
    public void returnToPrimaryAgent() {
        primaryAgent.receiveIntentionAnt(this);
    }
}
