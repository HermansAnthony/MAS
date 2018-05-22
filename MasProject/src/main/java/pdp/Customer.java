package pdp;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

public class Customer implements RoadUser {
    private Point location;

    public Customer(Point location) {
        this.location = location;
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }
}
