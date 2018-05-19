package ant;

import pdp.Drone;
import pdp.Order;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;
    public Order reservedOrder; // TODO Probably a queue of orders and charging station

    public IntentionAnt(Drone _primaryAgent, Order _reservedOrder) {
        super(_primaryAgent);
        reservationApproved = false;
        reservedOrder = _reservedOrder;
    }
}
