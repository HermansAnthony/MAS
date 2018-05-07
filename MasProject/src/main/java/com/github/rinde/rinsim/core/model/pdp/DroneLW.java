package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;


public class DroneLW  extends Drone {
    public DroneLW() {
        super(VehicleDTO.builder()
            .capacity(3500)
            .startPosition(new Point(50,50))
            .speed(22) // TODO find a way to scale linearly
            .build(),
            new EnergyDTO(2400)); // TODO adjust later to better value);

        payload = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}



}
