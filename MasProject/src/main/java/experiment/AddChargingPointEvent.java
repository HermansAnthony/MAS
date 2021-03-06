package experiment;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import energy.Charger;
import energy.ChargingPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Event indicating that a charging point can be created.
 * @author Anthony Hermans, Federico Quin
 */
public class AddChargingPointEvent implements TimedEvent {
    private final int amountDroneLW;
    private final int amountDroneHW;
    private Point position;

    private AddChargingPointEvent(Point position, int amtDroneLW, int amtDroneHW) {
        this.position = position;
        this.amountDroneLW = amtDroneLW;
        this.amountDroneHW = amtDroneHW;
    }

    /**
     * @return The position where the charging point is to be added.
     */
    public Point getPosition(){return position;}

    public int getAmtChargersLW() {
        return amountDroneLW;
    }

    public int getAmtChargersHW() {
        return amountDroneHW;
    }


    /**
     * Create a new {@link AddChargingPointEvent} instance.
     * @param position {@link #getPosition()}
     * @param amtDroneLW the amount of chargers for the {@link pdp.DroneLW} drones.
     * @param amtDroneHW the amount of chargers for the {@link pdp.DroneHW} drones.
     * @return A new instance.
     */
    public static AddChargingPointEvent create(Point position, int amtDroneLW, int amtDroneHW) {
        return new AddChargingPointEvent(position, amtDroneLW, amtDroneHW);
    }

    /**
     * Default {@link TimedEventHandler} that creates a {@link ChargingPoint} for every
     * {@link AddChargingPointEvent} that is received.
     * @return The default handler.
     */
    public static TimedEventHandler<AddChargingPointEvent> defaultHandler() {
        return Handler.INSTANCE;
    }

    @Override
    public long getTime() {
        return 0;
    }

    enum Handler implements TimedEventHandler<AddChargingPointEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(AddChargingPointEvent event, SimulatorAPI sim) {
                List<Charger> LWChargers = new ArrayList<>();
                List<Charger> HWChargers = new ArrayList<>();

                for (int i = 0; i < event.amountDroneLW; i++) {
                    LWChargers.add(new Charger());
                    sim.register(LWChargers.get(i));
                }

                for (int i = 0; i < event.amountDroneHW; i++) {
                    HWChargers.add(new Charger());
                    sim.register(HWChargers.get(i));
                }

                sim.register(new ChargingPoint(event.getPosition(), LWChargers, HWChargers));
            }

            @Override
            public String toString() {
                return AddChargingPointEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };
    }

}
