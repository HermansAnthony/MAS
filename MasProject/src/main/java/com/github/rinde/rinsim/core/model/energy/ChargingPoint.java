package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.pdp.DroneHW;
import com.github.rinde.rinsim.core.model.pdp.DroneLW;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.ArrayList;
import java.util.List;

public class ChargingPoint implements RoadUser {
    Point location;

    final int MAX_CAPACITY_LW;
    final int MAX_CAPACITY_HW;

    List<DroneLW> droneLW;
    List<DroneHW> droneHW;


    public ChargingPoint(Point loc, int maxCapacityLW, int maxCapacityHW) {
        location = loc;
        MAX_CAPACITY_HW = maxCapacityHW;
        MAX_CAPACITY_LW = maxCapacityLW;
        droneHW = new ArrayList<>(maxCapacityHW);
        droneLW = new ArrayList<>(maxCapacityLW);
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }

    public void chargeDrone(Drone drone) {
        if (drone instanceof DroneLW) {
            assert(droneLW.size() <= MAX_CAPACITY_LW);
            droneLW.add((DroneLW) drone);
        } else if (drone instanceof DroneHW) {
            assert(droneHW.size() <= MAX_CAPACITY_HW);
            droneHW.add((DroneHW) drone);
        }
    }

    public int getOccupation(Class droneClass) {
        if (droneClass == DroneLW.class) {
            return droneLW.size();
        } else if (droneClass == DroneHW.class) {
            return droneHW.size();
        }
        return -1;
    }

    public String getStatus() {
        // TODO fill in
        return "";
    }
}
