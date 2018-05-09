package com.github.rinde.rinsim.core.model.ant;

import com.github.rinde.rinsim.core.model.pdp.Drone;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;

    public IntentionAnt(Drone _primaryAgent) {
        super(_primaryAgent);
        reservationApproved = false;
    }
}
