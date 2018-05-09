package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.ant.Ant;
import com.github.rinde.rinsim.core.model.ant.AntReceiver;
import com.github.rinde.rinsim.core.model.ant.ExplorationAnt;
import com.github.rinde.rinsim.core.model.energy.ChargingPoint;
import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.energy.EnergyModel;
import com.github.rinde.rinsim.core.model.energy.EnergyUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import util.Range;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class Drone extends Vehicle implements EnergyUser, AntReceiver {

    private final Range SPEED_RANGE;
    private Optional<Parcel> payload;
    private Optional<EnergyModel> energyModel;
    private boolean wantsToCharge;
    private Map<Ant, Boolean> explorationAnts;
    public EnergyDTO battery;


    protected Drone(VehicleDTO _dto, EnergyDTO _battery, Range speedRange) {
        super(_dto);
        SPEED_RANGE = speedRange;
        battery = _battery;
        wantsToCharge = false;
        payload = Optional.absent();
        energyModel = Optional.absent();
        explorationAnts = new HashMap<>();
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

        delegateMAS();

        if (wantsToCharge) {
            moveToChargingPoint(rm, em, timeLapse);
        } else {
            handlePickupAndDelivery(rm, pdp, timeLapse);
        }
    }

    private void delegateMAS() {
        if (explorationAnts.isEmpty()) {
            sendOutExplorationAnts();
        } else {
            // Check if all the exploration ants have returned yet
            if (!explorationAnts.values().contains(Boolean.FALSE)) {
                // TODO Do something with the ants
                explorePossibleOptions();
            }
        }
    }

    private void explorePossibleOptions() {
        // Send out intention ants dependent on desired.

    }

    private void sendOutExplorationAnts() {
        for (Order order : getRoadModel().getObjectsOfType(Order.class)) {
            order.sendAnt(new ExplorationAnt(this));
        }
    }

    private void handlePickupAndDelivery(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        if (!payload.isPresent()) {
            getParcel(pdp);
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
        rm.moveTo(this, payload.get().getDeliveryLocation(), timeLapse);

        // If the drone arrived at the customer, deliver the package.
        if (rm.getPosition(this) == payload.get().getDeliveryLocation()) {
            System.out.println("At destination.");

            // TODO fix this again
            new Thread(new RemoveCustomer(rm, pdp, payload.get())).start();
            pdp.deliver(this, payload.get(), timeLapse);

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
            explorationAnts.replace(ant, Boolean.TRUE);
        }
    }


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


}
