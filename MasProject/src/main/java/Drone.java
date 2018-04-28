import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public abstract class Drone implements MovingRoadUser, TickListener, RandomUser {
    protected Point location;
    protected Optional<PlaneRoadModel> rm;
    protected Point destination;
}
