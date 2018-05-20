package ant;

import pdp.Drone;

public abstract class Ant {

    protected Drone primaryAgent;
    protected AntReceiver secondaryAgent; // TODO advanced exploration ants going to multiple nodes -> sort of planning

    Ant(Drone _primaryAgent) {
        primaryAgent = _primaryAgent;
    }


    public void setParcelInformation(AntReceiver _parcel) {
        secondaryAgent = _parcel;
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
