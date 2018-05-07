package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class DroneHW extends Drone {
    public DroneHW() {
        super(VehicleDTO.builder()
                .capacity(9000)
                .startPosition(new Point(600,800))
                .speed(17) // TODO find a way to scale linearly
                .build(),
                new EnergyDTO(1500)); // TODO adjust later to better value
        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}

}
