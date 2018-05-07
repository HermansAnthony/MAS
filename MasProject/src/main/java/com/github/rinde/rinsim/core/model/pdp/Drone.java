package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public abstract class Drone extends Vehicle {

    protected Optional<Parcel> payload;
    protected boolean wantsToCharge;
    public EnergyDTO battery;


    protected Drone(VehicleDTO _dto, EnergyDTO _battery) {
        super(_dto);
        battery = _battery;
        wantsToCharge = false;
        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
//        List<Integer> levels = new ArrayList<>();
//        levels.add(500);
//        levels.add(1000);
//        levels.add(1500);
//        levels.add(2000);
//        levels.add(2399);
//        if (levels.contains(battery.getBatteryLevel())) {
//            System.out.println("Battery level: " + battery.getBatteryLevel());
//        }

        final RoadModel rm = getRoadModel();
        final PDPModel pdp = getPDPModel();

        if (!timeLapse.hasTimeLeft()) {
            return;
        }

        if (wantsToCharge) {
            moveToChargingPoint(rm, timeLapse);
        } else {
            handlePickupAndDelivery(rm, pdp, timeLapse);
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

            new Thread(new RemoveCustomer(rm, pdp, payload.get())).start();
            pdp.deliver(this, payload.get(), timeLapse);

            payload = Optional.absent();
            wantsToCharge = true;
        }
    }

    private void moveToChargingPoint(RoadModel rm, TimeLapse timeLapse) {
        // TODO experimental implementation for now, hardcoded
        // TODO subclass vehicle in order to provide some way of accessing the energy model
        Point chargingPointLocation = new Point(560,478);
        if (!rm.getPosition(this).equals(chargingPointLocation)) {
            rm.moveTo(this, chargingPointLocation, timeLapse);
        }
    }


    @Override
    public void afterTick(TimeLapse time) {}

    public boolean wantsToCharge() {
        return wantsToCharge;
    }

    public void stopCharging() {
        wantsToCharge = false;
    }


    protected class RemoveCustomer implements Runnable {
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
