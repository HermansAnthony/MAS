package com.github.rinde.rinsim.core.model.ant;

import com.github.rinde.rinsim.core.model.pdp.Parcel;

public abstract class Ant {

    protected AntReceiver primaryAgent;
    protected Parcel parcel; // TODO advanced exploration ants going to multiple nodes -> sort of planning

    public Ant(AntReceiver _primaryAgent) {
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
}
