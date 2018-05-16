package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.ant.Ant;
import com.github.rinde.rinsim.core.model.ant.AntReceiver;
import com.github.rinde.rinsim.core.model.ant.ExplorationAnt;
import com.github.rinde.rinsim.core.model.ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.energy.ChargingPoint;
import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.energy.EnergyModel;
import com.github.rinde.rinsim.core.model.energy.EnergyUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import util.BatteryCalculations;
import util.Range;

import javax.measure.unit.SI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class Drone extends Vehicle implements EnergyUser, AntReceiver {
    public static int nextID = 0;
    private int ID;
    private final Range SPEED_RANGE;
    private Optional<Parcel> payload;

    // Energy related stuff
    private Optional<EnergyModel> energyModel;
    private boolean wantsToCharge;
    public EnergyDTO battery;

    // Delegate MAS stuff
    private delegateMasState state;
    private Map<ExplorationAnt, Boolean> explorationAnts;
    private Map<IntentionAnt, Boolean> intentionAnt; // Just one intention ant

    // TODO monitor class that writes delegate states mas states to file



    protected Drone(VehicleDTO _dto, EnergyDTO _battery, Range speedRange) {
        super(_dto);
        ID = nextID++;
        SPEED_RANGE = speedRange;
        battery = _battery;
        wantsToCharge = false;
        payload = Optional.absent();
        energyModel = Optional.absent();
        explorationAnts = new HashMap<>();
        intentionAnt = new HashMap<>();
        state = delegateMasState.initialState;
    }

    @Override
    public void initEnergyUser(EnergyModel model) {
        energyModel = Optional.of(model);
    }

    @Override
    public double getSpeed() {
        double currentContentsSize = getPDPModel().getContentsSize(this);

        // If the drone is carrying payload, adjust the ratio at which speed it moves
        double ratio = currentContentsSize == 0 ? 1 : 1 - (currentContentsSize / getCapacity());
        return SPEED_RANGE.getSpeed(ratio);
    }

    public double getSpeed(double capacity) {
        assert capacity <= getCapacity();
        return SPEED_RANGE.getSpeed(capacity / getCapacity());
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
        final RoadModel rm = getRoadModel();
        final PDPModel pdp = getPDPModel();
        final EnergyModel em = getEnergyModel();

        if (!timeLapse.hasTimeLeft()) {
            return;
        }

        delegateMAS(timeLapse);

        if (wantsToCharge) {
            moveToChargingPoint(rm, em, timeLapse);
        } else {
            handlePickupAndDelivery(rm, pdp, timeLapse);
        }
    }

    private void delegateMAS(TimeLapse timeLapse) {
        switch (state){
            case initialState:
                spawnExplorationAnts();
                state = delegateMasState.explorationAntsReturned;
                break;
            case explorationAntsReturned:
                if (!explorationAnts.values().contains(false)) {
                    spawnIntentionAnt(timeLapse);
                    state = delegateMasState.intentionAntReturned;
                }
                break;
            case intentionAntReturned:
                if (intentionAnt.isEmpty()){
                    state = delegateMasState.initialState;
                    break;
                }
                Map.Entry<IntentionAnt, Boolean> entry = intentionAnt.entrySet().iterator().next();
                // Ant has returned and has an approved reservation
                if (entry.getValue() && entry.getKey().reservationApproved){
                    if(!payload.isPresent()){
                        System.out.println("Payload is filled by the intention ant");
                        payload = Optional.of(entry.getKey().reservedOrder);
                    }
                    entry.setValue(false); // Intention ant needs to go away again
                    entry.getKey().reservationApproved = false;
                    entry.getKey().reservedOrder.receiveAnt(entry.getKey()); // TODO For now no reconsideration whatsoever
                }
            case spawnExplorationAnts:
                // TODO when reconsideration is needed continuously resend exploration ants
                break;

        }
    }

    private void spawnIntentionAnt(TimeLapse timeLapse) {
        /**
         * Send out intention ants dependent on desired belief.
         *
         * Used heuristics (in respective order):
         *  - battery life
         *  - order urgency
         *  - charging point occupation
         *  - travel distance
         */

        Order bestOrder = null;
        double bestMerit = -1;

        for (ExplorationAnt ant : explorationAnts.keySet()) {
            Order order = (Order) ant.getParcel();
            if (order.isReserved())
                continue;

            double merit = determineBenefits(order, timeLapse);

            if (merit > bestMerit) {
                bestMerit = merit;
                bestOrder = order;
            }
        }

        if (bestOrder == null) {
            return;
        }

        IntentionAnt ant = new IntentionAnt(this, bestOrder);
        intentionAnt.put(ant, false);
        bestOrder.receiveAnt(ant);

    }

    /**
     * Returns the merit for a certain order. Orders with higher merit are more beneficial to drones than order with low merit.
     * @param order The order for which the merit is calculated.
     * @param timeLapse The time at which this is calculated.
     * @return The merit associated with the specific order.
     */
    private double determineBenefits(Order order, TimeLapse timeLapse) {
        double merit = 0;

        RoadModel rm = getRoadModel();
        EnergyModel em = getEnergyModel();


        // Determine the total battery capacity that will be used for this specific order if it is chosen.
        // If the usage exceeds the current battery capacity, return -1 to make sure this order is not chosen.
        // Otherwise, choose orders in favour of using as much of the remaining battery capacity as possible.
        double distancePickup = rm.getDistanceOfPath(rm.getShortestPathTo(rm.getPosition(this), order.getPickupLocation())).doubleValue(SI.METER);
        double distanceDeliver = rm.getDistanceOfPath(rm.getShortestPathTo(order.getPickupLocation(), order.getDeliveryLocation())).doubleValue(SI.METER);
        double distanceCharge = rm.getDistanceOfPath(rm.getShortestPathTo(order.getDeliveryLocation(), em.getChargingPoint().getLocation())).doubleValue(SI.METER);
        double neededBatteryLevel = BatteryCalculations.calculateNecessaryBatteryLevel(
            this, distancePickup, distanceDeliver, distanceCharge, order.getNeededCapacity());
        if (neededBatteryLevel > this.battery.getBatteryLevel()) {
            return -1;
        }

        merit += (neededBatteryLevel / this.battery.getBatteryLevel()) * 100;


        // Favour the orders which are closer to their specific deadline.
        double timeLeft = order.getDeliveryTimeWindow().end() - timeLapse.getTime();
        double totalTimeOrder = order.getDeliveryTimeWindow().end() - order.getOrderAnnounceTime();
        double percentageTimeLeft = timeLeft / totalTimeOrder;
        merit += (1 - percentageTimeLeft) * 100;

        // TODO same as above, for chargingPoint calculations
        merit += em.getChargingPoint().chargersOccupied(this) ? 50 : 0;


        // Favour the orders which use up most of the drone's current travel capacity.
        double travelDistance = distancePickup + distanceDeliver + distanceCharge;
        merit += (travelDistance / getTravelCapacity()) * 100;

        return merit;
    }

    /**
     * Gets the maximum distance a drone could travel if flying at max speed.
     * @return The calculated distance.
     */
    protected double getTravelCapacity() {
        return SPEED_RANGE.getSpeed(1) * battery.getMaxCapacity();
    }

    private void spawnExplorationAnts() {
        for (Order order : getRoadModel().getObjectsOfType(Order.class)) {
            ExplorationAnt explorationAnt = new ExplorationAnt(this);
            explorationAnts.put(explorationAnt, Boolean.FALSE);
            order.receiveAnt(explorationAnt);
        }
    }

    private void handlePickupAndDelivery(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        if (!payload.isPresent()) {
            return;
//            getParcel(pdp);
        } else if (pdp.getContents(this).isEmpty()) {
            moveToStore(rm, pdp, timeLapse);
        } else {
            moveToCustomer(rm, pdp, timeLapse);
        }
    }

    private void getParcel(PDPModel pdp) {
        for (Parcel parcel : pdp.getParcels(PDPModel.ParcelState.AVAILABLE)) {
            if (parcel.getNeededCapacity() <= this.getCapacity()) {
                payload = Optional.of(parcel);
                System.out.println("Moving to store...");
            }
        }
    }

    private void moveToStore(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        rm.moveTo(this, payload.get().getPickupLocation(), timeLapse);

        // If the drone has arrived at the store, pickup the parcel.
        if (rm.getPosition(this) == payload.get().getPickupLocation()) {
            try {
                System.out.println("Arrived at store, moving to the customer...");
                pdp.pickup(this, payload.get(), timeLapse);
                System.out.println("Carrying parcel.");
            } catch(IllegalArgumentException e){
                System.out.println("Parcel is already in transport with another drone.");
                payload = Optional.absent();
            }
        }
    }

    private void moveToCustomer(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        Order order = (Order) payload.get();
        rm.moveTo(this, order.getDeliveryLocation(), timeLapse);

        // If the drone arrived at the customer, deliver the package.
        if (rm.getPosition(this) == order.getDeliveryLocation()) {
            intentionAnt.clear(); // No more intention ants
            System.out.println("At destination.");

            new Thread(new RemoveCustomer(rm, pdp, order.getCustomer())).start();
            pdp.deliver(this, order, timeLapse);

            payload = Optional.absent();
            wantsToCharge = true;
        }
    }

    private void moveToChargingPoint(RoadModel rm, EnergyModel em, TimeLapse timeLapse) {
        final ChargingPoint chargingPoint = em.getChargingPoint();

        if (!rm.getPosition(this).equals(chargingPoint.getLocation())) {
            rm.moveTo(this, chargingPoint.getLocation(), timeLapse);
        } else if (!chargingPoint.dronePresent(this)) {
            // Only charge if there is a charger free
            if (!chargingPoint.chargersOccupied(this)) {
                chargingPoint.chargeDrone(this);
            }
        }
    }


    @Override
    public void afterTick(TimeLapse time) {}


    public void stopCharging() {
        wantsToCharge = false;
    }

    public EnergyModel getEnergyModel() {
        return energyModel.get();
    }

    public void receiveAnt(Ant ant) {
        if (ant instanceof ExplorationAnt) {
            explorationAnts.replace((ExplorationAnt) ant, true);
        } else if (ant instanceof IntentionAnt) {
            IntentionAnt intAnt = (IntentionAnt) ant;
            intentionAnt.replace(intAnt, true);
        }
    }

    public abstract String getDroneString();


    // TODO find better way of dealing with removal of customers than using threads.
    private class RemoveCustomer implements Runnable {
        RoadModel rm;
        PDPModel pdp;
        RoadUser customer;
        Stopwatch stopwatch;

        RemoveCustomer(RoadModel _rm, PDPModel _pdp, RoadUser _customer) {
            rm = _rm;
            pdp = _pdp;
            customer = _customer;
            stopwatch = Stopwatch.createStarted();

        }

        @Override
        public void run() {
            while (pdp.getVehicleState(Drone.this) == PDPModel.VehicleState.DELIVERING) {
                // Timeout of 30 seconds in order to kill thread if necessary.
                if (stopwatch.elapsed(TimeUnit.SECONDS) > 30) {
                    return;
                }
            }
            System.out.println("Package delivered.");
            if (rm.containsObject(customer)) {
                rm.removeObject(customer);
            }
        }
    }

    private enum delegateMasState {
//        TODO explanation
        initialState,
        spawnExplorationAnts,
        explorationAntsReturned,
        intentionAntReturned;

    }

}
