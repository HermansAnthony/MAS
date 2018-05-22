package experiment;

import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import pdp.Drone;

/**
 * Event indicating the creation of a vehicle.
 * @author Anthony Hermans, Federico Quin
 */
public class AddDroneEvent implements TimedEvent {
    long time;
    Drone drone;
    AddDroneEvent(long _time, Drone _drone) {
        time = _time;
        drone = _drone;
    }

    /**
     * @return Data which describes the drone that should be added.
     */
    public VehicleDTO getVehicleDTO(){return drone.getDTO();}

    /**
     * Creates a new {@link AddDroneEvent}.
     * @param time The time at which the vehicle is added.
     * @param drone The {@link Drone} that describes the drone.
     * @return A new {@link AddDroneEvent} instance.
     */
    public static AddDroneEvent create(long time, Drone drone) {
        return new AddDroneEvent(time, drone);
    }

    @Override
    public long getTime() {
        return 0;
    }
}
