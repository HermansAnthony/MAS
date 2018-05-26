package ant;

import pdp.Drone;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;

    public IntentionAnt(Drone primaryAgent, AntUser destination) {
        super(primaryAgent);
        this.reservationApproved = false;
        setSecondaryAgent(destination);
    }

    @Override
    public void returnToPrimaryAgent() {
        primaryAgent.receiveIntentionAnt(this);
    }
}
