package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.road.MoveEvent;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;

import javax.annotation.Nonnull;
import javax.measure.unit.SI;
import java.util.ArrayList;
import java.util.List;

public class DefaultEnergyModel extends EnergyModel {

    List<Drone> drones;
    ChargingPoint chargingPoint;
    RoadModel roadModel;


    private DefaultEnergyModel(RoadModel rm) {
        roadModel = rm;
        drones = new ArrayList<>();
        chargingPoint = null;


        rm.getEventAPI().addListener((Event e) -> {
            if (!(e instanceof MoveEvent)) {
                return;
            }
            final MoveEvent event = (MoveEvent) e;
            Drone drone = (Drone) event.roadUser;

            double tickLength = event.pathProgress.time().doubleValue(SI.MILLI(SI.SECOND));
            // TODO decrease maybe more dynamically.
            drone.battery.decreaseBatteryLevel(tickLength / 1000);
        }, PlaneRoadModel.RoadEventType.MOVE);
    }

    public String getStatus() {
        return chargingPoint.getStatus();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean register(EnergyUser energyUser) {
        if (energyUser instanceof Drone) {
            drones.add((Drone) energyUser);
        } else if (energyUser instanceof ChargingPoint) {
            chargingPoint = (ChargingPoint) energyUser;
        }

        energyUser.initEnergyUser(this);
        return true;
    }

    @Override
    public boolean unregister(EnergyUser energyUser) {
        if (energyUser instanceof Drone) {
            drones.remove(energyUser);
        } else if (energyUser instanceof ChargingPoint) {
            chargingPoint = null;
        }
        return true;
    }

    @Override
    @Nonnull
    public <U> U get(Class<U> type) {
        synchronized (this) {
            return type.cast(this);
        }
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        chargingPoint.charge(timeLapse);
        for (Drone drone : chargingPoint.redeployChargedDrones()) {
            drone.stopCharging();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public ChargingPoint getChargingPoint() {
        return chargingPoint;
    }


    public static class Builder extends
            AbstractModelBuilder<DefaultEnergyModel, EnergyUser> {

        private Builder() {
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
