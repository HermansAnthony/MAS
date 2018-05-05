package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;

public abstract class Drone extends Vehicle {

    protected boolean hasOrder;
    protected Optional<Parcel> payload;
    protected boolean isCharging;
    public EnergyDTO battery;


    protected Drone(VehicleDTO _dto, EnergyDTO _battery) {
        super(_dto);
        battery = _battery;
        hasOrder = false;
        isCharging = false;
        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    /**
     * Is called every tick. This replaces the
     * {@link TickListener#tick(TimeLapse)} for vehicles.
     * @param time The time lapse that can be used.
     * @see TickListener#tick(TimeLapse)
     */
    protected abstract void tickImpl(TimeLapse time);

    @Override
    public void afterTick(TimeLapse time) {
        if (!payload.isPresent()) {
            return;
        }

        final PDPModel pdp = getPDPModel();
        final RoadModel rm = getRoadModel();

        if (rm.getPosition(this) == payload.get().getDeliveryLocation()) {
            System.out.println("Package delivered.");
            pdp.deliver(this, payload.get(), time);

            new Thread(new RemoveCustomer(rm, pdp,
                    rm.getObjects()
                            .stream()
                            .filter(obj -> rm.getPosition(obj) == payload.get().getDeliveryLocation() && obj instanceof Customer)
                            .findFirst().get())
            ).start();
            payload = Optional.absent();
            hasOrder = false;
            isCharging = true;
        }
    }

    public boolean wantsToCharge() {
        return isCharging;
    }

    public void stopCharging() {
        isCharging = false;
    }


    protected class RemoveCustomer implements Runnable {
        RoadModel rm;
        PDPModel pdp;
        RoadUser cust;

        RemoveCustomer(RoadModel _rm, PDPModel _pdp, RoadUser _cust) {
            rm = _rm;
            pdp = _pdp;
            cust = _cust;
        }

        @Override
        public void run() {
            // TODO debug this and find fault
//            while (pdp.getVehicleState(Drone.this) == PDPModel.VehicleState.DELIVERING) {}
//            if (rm.containsObject(cust)) {
//                rm.removeObject(cust);
//            }
        }
    }


}
