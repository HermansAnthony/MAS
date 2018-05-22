package experiment;

import java.io.Serializable;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import energy.ChargingPoint;

/**
 * Event indicating that a charging point can be created.
 * @author Anthony Hermans, Federico Quin
 */
public class AddChargingPointEvent implements TimedEvent {
    private static int amountDroneLW = 5;
    private static int amountDroneHW = 5;
    private Point position;
    AddChargingPointEvent(Point _position) {
        position = _position;
    }

    /**
     * @return The position where the charging point is to be added.
     */
    public Point getPosition(){return position;}

    /**
     * Create a new {@link AddChargingPointEvent} instance.
     * @param time The time at which the event is to be dispatched.
     * @param position {@link #getPosition()}
     * @return A new instance.
     */
    public static AddChargingPointEvent create(long time, Point position) {
        return new AddChargingPointEvent(position);
    }

    /**
     * Default {@link TimedEventHandler} that creates a {@link ChargingPoint} for every
     * {@link AddChargingPointEvent} that is received.
     * @return The default handler.
     */
    public static TimedEventHandler<AddChargingPointEvent> defaultHandler() {
        return Handler.INSTANCE;
    }

    /**
     * {@link TimedEventHandler} that creates {@link Depot}s with an overridden
     * toString implementation. The depots are called 'Depot X', where 'X' is a
     * number.
     * <p>
     * <b>Warning:</b> This handler should only be used for debugging purposes and
     * is not thread safe.
     * @return A newly constructed handler.
     */
    public static TimedEventHandler<AddChargingPointEvent> namedHandler() {
        return new ChargingPointCreator();
    }

    @Override
    public long getTime() {
        return 0;
    }

    enum Handler implements TimedEventHandler<AddChargingPointEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(AddChargingPointEvent event, SimulatorAPI sim) {
                sim.register(new ChargingPoint(event.getPosition(), amountDroneLW,amountDroneHW));
            }

            @Override
            public String toString() {
                return AddChargingPointEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };
    }

    static class ChargingPointCreator
            implements TimedEventHandler<AddChargingPointEvent>, Serializable {
        private static final long serialVersionUID = 3888253170041895475L;


        ChargingPointCreator() {}

        @Override
        public void handleTimedEvent(AddChargingPointEvent event, SimulatorAPI simulator) {
            simulator.register(new ChargingPoint(event.getPosition(), amountDroneLW, amountDroneHW));
        }

        @Override
        public String toString() {
            return AddChargingPointEvent.class.getSimpleName() + ".namedHandler()";
        }
    }

}
