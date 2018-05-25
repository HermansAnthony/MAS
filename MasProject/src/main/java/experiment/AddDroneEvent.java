package experiment;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import pdp.Drone;

/**
 * Event indicating the creation of a vehicle.
 * @author Anthony Hermans, Federico Quin
 */
public class AddDroneEvent implements TimedEvent {
    private Drone drone;

    private AddDroneEvent(Drone drone) {
        this.drone = drone;
    }

    public Drone getDrone() {
        return drone;
    }

    /**
     * Creates a new {@link AddDroneEvent}.
     * @param drone The {@link Drone} that describes the drone.
     * @return A new {@link AddDroneEvent} instance.
     */
    public static AddDroneEvent create(Drone drone) {
        return new AddDroneEvent(drone);
    }

    @Override
    public long getTime() {
        return 0;
    }

    /**
     * Default {@link TimedEventHandler} that creates a {@link pdp.Drone} for every
     * {@link AddDroneEvent} that is received.
     * @return The default handler.
     */
    public static TimedEventHandler<AddDroneEvent> defaultHandler() {
        return AddDroneEvent.Handler.INSTANCE;
    }


    enum Handler implements TimedEventHandler<AddDroneEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(AddDroneEvent event, SimulatorAPI sim) {
                sim.register(event.getDrone());
            }

            @Override
            public String toString() {
                return AddDroneEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
