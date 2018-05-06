package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.pdp.DroneHW;
import com.github.rinde.rinsim.core.model.pdp.DroneLW;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
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
        droneHW = new ArrayList<>();
        droneLW = new ArrayList<>();
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

    public boolean chargersOccupied(Class droneClass) {
        if (droneClass == DroneLW.class) {
            return droneLW.size() == MAX_CAPACITY_LW;
        } else if (droneClass == DroneHW.class) {
            return droneHW.size() == MAX_CAPACITY_HW;
        }
        return false;
    }

    public boolean dronePresent(Drone drone) {
        return droneHW.contains(drone) || droneLW.contains(drone);
    }


    public void charge(TimeLapse timeLapse) {
        // TODO charge all the drones a certain amount
        for (Drone drone : droneHW) {
            drone.battery.recharge(1);
        }
        for (Drone drone : droneLW) {
            drone.battery.recharge(1);
        }
    }

    public List<Drone> redeployChargedDrones() {
        List<Drone> drones = new ArrayList<>();

        for (int i = 0; i < droneHW.size(); i++) {
            Drone drone = droneHW.get(i);
            if (drone.battery.fullyCharged()) {
                drones.add(drone);
                droneHW.remove(drone);
                i--;
            }
        }
        for (int i = 0; i < droneLW.size(); i++) {
            Drone drone = droneLW.get(i);
            if (drone.battery.fullyCharged()) {
                drones.add(drone);
                droneLW.remove(drone);
                i--;
            }
        }

        return drones;
    }

    public String getStatus() {
        String status = "The current occupation of the charging point is: \n";
        status += droneLW.size() + " lightweight drones are charging";
        status += droneHW.size() + " heavyweight drones are charging";
        return status;
    }
}
