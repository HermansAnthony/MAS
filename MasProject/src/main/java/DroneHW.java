import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.Collection;

public class DroneHW extends Drone {
    protected DroneHW() {
        super(VehicleDTO.builder()
                .capacity(9000)
                .startPosition(new Point(600,800))
                .speed(600) // TODO find a way to scale linearly
                .build());
        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
        RoadModel rm = getRoadModel();

        final PDPModel pm = getPDPModel();

        Collection<RoadUser> roadUsers = RoadModels.findObjectsWithinRadius(rm.getPosition(this), rm, 10000);

        if (!payload.isPresent()) {
            // Has no payload yet
            for (RoadUser user : roadUsers) {
                if (user instanceof Order) {
                    Order order = (Order) user;
                    // HW drone can only take a payload of 9000 grams or less
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
//                pm.pickup(this, payload.get(), timeLapse);
                hasOrder = true;
            }
        } else if (hasOrder) {
            // Drone has the order, deliver it to the customer
            rm.moveTo(this, payload.get().getDeliveryLocation(), timeLapse);
        }

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        final PDPModel pm = getPDPModel();
        if (!payload.isPresent()) {
            return;
        }

        RoadModel rm = getRoadModel();
        if (rm.getPosition(this) == payload.get().getDeliveryLocation()) {
            System.out.println("Package delivered.");
            pm.deliver(this, payload.get(), timeLapse);
            rm.removeObject(rm.getObjects()
                    .stream()
                    .filter(obj -> rm.getPosition(obj) == payload.get().getDeliveryLocation() && obj instanceof Customer)
                    .findFirst().get());
            payload = Optional.absent();
            hasOrder = false;
        }
    }

}
