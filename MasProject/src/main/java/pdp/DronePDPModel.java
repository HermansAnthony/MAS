package pdp;

import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;

public class DronePDPModel extends PDPModel {

    @Override
    protected void continuePreviousActions(Vehicle vehicle, TimeLapse time) {

    }

    @Override
    protected boolean doRegister(PDPObject pdpObject) {
        return false;
    }

    @Override
    public ImmutableSet<Parcel> getContents(Container container) {
        return null;
    }

    @Override
    public double getContentsSize(Container container) {
        return 0;
    }

    @Override
    public double getContainerCapacity(Container container) {
        return 0;
    }


    public void pickup(Vehicle vehicle, Parcel parcel, TimeLapse time) {

    }

    @Override
    public void drop(Vehicle vehicle, Parcel parcel, TimeLapse time) {

    }

    @Override
    public void deliver(Vehicle vehicle, Parcel parcel, TimeLapse time) {

    }

    @Override
    public void addParcelIn(Container container, Parcel parcel) {

    }

    @Override
    public Collection<Parcel> getParcels(ParcelState parcelState) {
        return null;
    }

    @Override
    public Collection<Parcel> getParcels(ParcelState... states) {
        return null;
    }

    @Override
    public Set<Vehicle> getVehicles() {
        return null;
    }

    @Override
    public ParcelState getParcelState(Parcel parcel) {
        return null;
    }

    @Override
    public VehicleState getVehicleState(Vehicle vehicle) {
        return null;
    }

    @Override
    public VehicleParcelActionInfo getVehicleActionInfo(Vehicle vehicle) {
        return null;
    }

    @Override
    public EventAPI getEventAPI() {
        return null;
    }

    @Override
    public boolean containerContains(Container container, Parcel parcel) {
        return false;
    }

    @Override
    public TimeWindowPolicy getTimeWindowPolicy() {
        return null;
    }

    @Override
    public void service(Vehicle vehicle, Parcel parcel, TimeLapse time) {

    }

    @Override
    public void tick(TimeLapse timeLapse) {

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    @Override
    public boolean unregister(com.github.rinde.rinsim.core.model.pdp.PDPObject pdpObject) {
        return false;
    }
}
