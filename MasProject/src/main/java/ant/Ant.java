package ant;

import pdp.Drone;

public abstract class Ant {

    protected Drone primaryAgent;
    protected AntReceiver secondaryAgent; // TODO advanced exploration ants going to multiple nodes -> sort of planning

    Ant(Drone primaryAgent) {
        this.primaryAgent = primaryAgent;
    }


    public void setSecondaryAgent(AntReceiver parcel) {
        secondaryAgent = parcel;
    }

    public AntReceiver getSecondaryAgent() {
        return secondaryAgent;
    }

    public void returnToPrimaryAgent() {
        primaryAgent.receiveAnt(this);
    }

    public Drone getPrimaryAgent() {
        return primaryAgent;
    }
}
