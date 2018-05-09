package com.github.rinde.rinsim.core.model.ant;

import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.pdp.Parcel;

public abstract class Ant {

    protected Drone primaryAgent;
    protected Parcel parcel; // TODO advanced exploration ants going to multiple nodes -> sort of planning

    public Ant(Drone _primaryAgent) {
        primaryAgent = _primaryAgent;
    }


    public void setParcelInformation(Parcel _parcel) {
        parcel = _parcel;
    }

    public Parcel getParcel() {
        return parcel;
    }

    public void returnToPrimaryAgent() {
        primaryAgent.receiveAnt(this);
    }

    public Drone getPrimaryAgent() {
        return primaryAgent;
    }
}
