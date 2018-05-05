package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.Collection;


public class DroneLW  extends Drone {
    public DroneLW() {
        super(VehicleDTO.builder()
            .capacity(3500)
            .startPosition(new Point(50,50))
            .speed(22) // TODO find a way to scale linearly
            .build(),
            new EnergyDTO(2400)); // TODO adjust later to better value);

        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
        System.out.println("Battery level: " + battery.getBatteryLevel());

        if (isCharging) {

        } else {
            handlePickupAndDelivery(timeLapse);
        }
    }

    protected void handlePickupAndDelivery(TimeLapse timeLapse) {
        final RoadModel rm = getRoadModel();
        final PDPModel pm = getPDPModel();

        Collection<RoadUser> roadUsers = RoadModels.findObjectsWithinRadius(rm.getPosition(this), rm, 10000);

        if (!payload.isPresent()) {
            // Has no payload yet
            for (RoadUser user : roadUsers) {
                if (user instanceof Order) {
                    Order order = (Order) user;
                    // HW drone can only take a payload of 3500 grams or less
                    if (order.getNeededCapacity() <= this.getCapacity()) {
                        payload = Optional.of((Parcel) order);
                        System.out.println("Moving to store...");
                    }
                }
            }

        } else if (!hasOrder) {
            // go get the payload at the store
            rm.moveTo(this, payload.get().getPickupLocation(), timeLapse);
            if (rm.getPosition(this) == payload.get().getPickupLocation()) {
                System.out.println("Arrived at store, moving to the customer...");
                try {
                    pm.pickup(this, payload.get(), timeLapse);
                } catch(IllegalArgumentException e){
                    System.out.println("Parcel is already in transport with another drone");
                    payload = Optional.absent();
                    return;
                }
                hasOrder = true;
            }
        } else if (hasOrder) {
            // Drone has the order, deliver it to the customer
            rm.moveTo(this, payload.get().getDeliveryLocation(), timeLapse);
        }
    }

}
