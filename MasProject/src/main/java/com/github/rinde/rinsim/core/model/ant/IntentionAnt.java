package com.github.rinde.rinsim.core.model.ant;

import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.pdp.Order;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;
    public Order reservedOrder; // TODO Probably a queue of orders and charging station

    public IntentionAnt(Drone _primaryAgent, Order _reservedOrder) {
        super(_primaryAgent);
        reservationApproved = false;
        reservedOrder = _reservedOrder;
    }
}
