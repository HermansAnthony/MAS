import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class DroneHW extends Drone {
    public DroneHW() {
        location = new Point(600,800);
        destination = new Point(600,100);
        rm = Optional.absent();
    }

    @Override
    public void setRandomGenerator(RandomProvider randomProvider) {

    }

    @Override
    public double getSpeed() {
        return 100;
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        rm = Optional.of((PlaneRoadModel) roadModel);
        roadModel.addObjectAt(this, location);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (rm.get().getPosition(this) != destination) {
            rm.get().moveTo(this, destination, timeLapse);
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
