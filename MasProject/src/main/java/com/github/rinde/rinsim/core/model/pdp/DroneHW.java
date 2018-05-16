package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import util.Range;

public class DroneHW extends Drone {

    public DroneHW(Range speedRange, int capacity, int batteryLevel) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(new Point(600,800))
                .speed(speedRange.getSpeed(1))
                .build(),
                new EnergyDTO(batteryLevel), speedRange);
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}

    @Override
    public String getDroneString() {
        // TODO fill in
        return null;
    }

}
