package ant;

import pdp.Drone;
import pdp.Order;

public class IntentionAnt extends Ant {

    public boolean reservationApproved;
    public double merit;
    public Order reservedOrder; // TODO Probably a queue of orders and charging station

    public IntentionAnt(Drone _primaryAgent, Order _reservedOrder, double _merit) {
        super(_primaryAgent);
        reservationApproved = false;
        reservedOrder = _reservedOrder;
        merit = _merit;
    }
}
