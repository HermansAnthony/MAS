package experiment;

import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import pdp.DroneLW;

public class DroneFollowingVehicle extends RouteFollowingVehicle {
    public DroneFollowingVehicle(VehicleDTO dto, boolean allowDelayedRouteChanging) {
        super(dto, allowDelayedRouteChanging);
//        this.ID
    }

    @Override
    public String toString() {
        return "Drone";
    }

}
