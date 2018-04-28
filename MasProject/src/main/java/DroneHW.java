import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class DroneHW extends Drone {
    protected DroneHW() {
        super(VehicleDTO.builder()
                .capacity(9000)
                .startPosition(new Point(600,800))
                .speed(100) // TODO find a way to scale linearly
                .build());
    }

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
//        RoadModel rm = getRoadModel();
//        if (rm.getPosition(this) != destination) {
//            rm.moveTo(this, destination, timeLapse);
//        }
//
//        final PDPModel pm = getPDPModel();
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
