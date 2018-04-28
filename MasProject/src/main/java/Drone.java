import com.github.rinde.rinsim.core.model.pdp.ContainerImpl;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public abstract class Drone extends Vehicle {
//    protected Point destination;
    protected boolean hasOrder;


    protected Drone(VehicleDTO vehicleDto) {
        super(vehicleDto);
        hasOrder = false;
    }
}
