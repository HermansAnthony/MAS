package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

public class Customer implements RoadUser {
    private Point location;

    public Customer(Point loc) {
        location = loc;
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }
}