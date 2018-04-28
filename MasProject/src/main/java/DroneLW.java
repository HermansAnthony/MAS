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
import java.util.List;


public class DroneLW  extends Drone {

    private Optional<Parcel> payload;

    protected DroneLW() {
        super(VehicleDTO.builder()
            .capacity(3500)
            .startPosition(new Point(50,50))
            .speed(200) // TODO find a way to scale linearly
            .build());
//        destination = new Point(500,500);
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
                    payload = Optional.of((Parcel) order);
                    System.out.println("Moving to store...");
                }
            }

        } else if (!hasOrder) {
            // go get the payload at the store
            rm.moveTo(this, payload.get().getPickupLocation(), timeLapse);
            if (rm.getPosition(this) == payload.get().getPickupLocation()) {
                System.out.println("Arrived at store, moving to the customer...");
                hasOrder = true;
            }
        } else if (hasOrder) {
            // Drone has the order, deliver it to the customer
            rm.moveTo(this, payload.get().getDeliveryLocation(), timeLapse);
        }

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (!payload.isPresent()) {
            return;
        }

        RoadModel rm = getRoadModel();
        if (rm.getPosition(this) == payload.get().getDeliveryLocation()) {
            System.out.println("Package delivered.");
            rm.removeObject(findCustomer(rm.getObjects(), payload.get().getDeliveryLocation()));
//            rm.removeObject(rm.getObjects()
//                    .stream()
//                    .filter(obj -> rm.getPosition(obj) == payload.get().getDeliveryLocation())
//                    .findAny());
            rm.removeObject(payload.get());

            payload = Optional.absent();
            hasOrder = false;
        }
    }

    private RoadUser findCustomer(Collection<RoadUser> users, Point location) {
        for (RoadUser user : users) {
            if (user instanceof Customer && getRoadModel().getPosition(user) == location) {
                return user;
            }
        }
        return null;
    }

}
