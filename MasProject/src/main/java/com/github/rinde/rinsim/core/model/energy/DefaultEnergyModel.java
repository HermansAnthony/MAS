package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.road.MoveEvent;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class DefaultEnergyModel extends EnergyModel {

    List<RoadUser> drones;
    ChargingPoint chargingPoint;
    RoadModel roadModel;


    public DefaultEnergyModel(RoadModel rm) {
        roadModel = rm;
        drones = new ArrayList<>();
        chargingPoint = null;

        rm.getEventAPI().addListener(new Listener() {
            @Override
            public void handleEvent(Event e) {
                @SuppressWarnings("unchecked")
                final MoveEvent event = (MoveEvent) e;
                Drone drone = (Drone) event.roadUser;
                drone.battery.decreaseBatteryLevel(1);
                // TODO do something here.
            }
        }, PlaneRoadModel.RoadEventType.MOVE);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean register(RoadUser roadUser) {
        if (roadUser instanceof Drone) {
            Drone drone = (Drone) roadUser;
            drones.add(drone);
        } else if (roadUser instanceof ChargingPoint) {
            chargingPoint = (ChargingPoint) roadUser;
        }
        return true;
    }

    @Override
    public boolean unregister(RoadUser roadUser) {
        if (roadUser instanceof Drone) {
            drones.remove(roadUser);
        } else if (roadUser instanceof ChargingPoint) {
            chargingPoint = null;
        }
        return true;
    }

    @Override
    public <U> U get(Class<U> aClass) {
        return null;
    }

    @Override
    public void tick(TimeLapse timeLapse) {

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }


    public static class Builder extends ModelBuilder.AbstractModelBuilder<DefaultEnergyModel, RoadUser> {
        public Builder() {
            setProvidingTypes(EnergyModel.class);
            setDependencies(RoadModel.class);
        }

        @Override
        public DefaultEnergyModel build(DependencyProvider dependencyProvider) {
            final RoadModel rm = dependencyProvider.get(RoadModel.class);
            return new DefaultEnergyModel(rm);
        }
    }
}
