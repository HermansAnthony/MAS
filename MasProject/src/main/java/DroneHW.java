import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

public class DroneHW extends Drone {
    public DroneHW() {}

    @Override
    public void setRandomGenerator(RandomProvider randomProvider) {

    }

    @Override
    public double getSpeed() {
        return 0;
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {

    }

    @Override
    public void tick(TimeLapse timeLapse) {

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
