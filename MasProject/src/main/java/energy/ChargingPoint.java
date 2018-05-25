package energy;

import ant.Ant;
import ant.AntReceiver;
import ant.ExplorationAnt;
import ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import org.jetbrains.annotations.NotNull;
import pdp.Drone;
import pdp.DroneHW;
import pdp.DroneLW;
import util.ChargingPointMeasurement;
import util.ChargingPointMonitor;
import util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChargingPoint implements AntReceiver, RoadUser, EnergyUser, TickListener {
    private static final Integer TIMEOUT_RESERVATION = 20;
    private Point location;
    private Map<Class<?>, List<Tuple<Drone, Boolean>>> chargers; // Keeps a list per drone type of chargers
                                                                 // The boolean indicates if the drone is present currently
    private Map<Drone, Integer> timeoutReservations;

    private Queue<Ant> temporaryAnts;
    private ChargingPointMonitor monitor;

    public ChargingPoint(Point loc, int maxCapacityLW, int maxCapacityHW) {
        location = loc;
        chargers = new HashMap<>();
        chargers.put(DroneLW.class, Arrays.asList(new Tuple[maxCapacityLW]));
        chargers.put(DroneHW.class, Arrays.asList(new Tuple[maxCapacityHW]));
        timeoutReservations = new HashMap<>();
        temporaryAnts = new ArrayDeque<>();
        monitor = new ChargingPointMonitor();
    }

    @Override
    public void initRoadUser(@NotNull RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }

    private void reserveCharger(Drone drone) {
        assert(!this.chargersOccupied(drone.getClass()));

        List<Tuple<Drone, Boolean>> drones = chargers.get(drone.getClass());
        drones.set(drones.indexOf(null), new Tuple<>(drone, false));
        timeoutReservations.put(drone, TIMEOUT_RESERVATION);
    }

    private void cancelReservation(Drone drone) {
        assert(this.dronePresent(drone));
        List<Tuple<Drone, Boolean>> drones = chargers.get(drone.getClass());
        // Remove the drone from the charger
        drones.set(drones.indexOf(drones.stream()
            .filter(Objects::nonNull)
            .filter(o -> o.first == drone)
            .collect(Collectors.toList())
            .iterator().next()), null);
        // Cancel the timer on the specific reservation
        timeoutReservations.remove(drone);
    }

    public void chargeDrone(Drone drone) {
        boolean dronePresent = dronePresent(drone);
        if (!dronePresent) {
            System.err.println("Drone not reserved yet, unable to charge.");
        } else {
            // Set the drone in the charger to active
            chargers.get(drone.getClass()).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.first == drone)
                .findFirst().get()
                .second = true;
        }

    }

    public boolean chargersOccupied(Drone drone) {
        return chargersOccupied(drone.getClass());
    }

    private boolean chargersOccupied(Class droneClass) {
        return !chargers.get(droneClass).contains(null);
    }

    public double getOccupationPercentage(Class droneClass, boolean includeReservations) {
        Stream<Tuple<Drone, Boolean>> stream = chargers.get(droneClass).stream().filter(Objects::nonNull);
        if (!includeReservations) {
            stream = stream.filter(o -> o.second);
        }
        return ((double) stream.count()) / chargers.get(droneClass).size();
    }

    public List<Tuple<Drone, Boolean>> getChargeStations(Class Drone){
        return chargers.get(Drone);
    }

    public boolean dronePresent(Drone drone) {
        return chargers.get(drone.getClass()).stream()
            .filter(Objects::nonNull)
            .anyMatch(o -> o.first == drone);
    }


    /**
     * Charges all the drones present in the ChargingPoint.
     * TODO This method also keeps track of the occupation of the charging station, since it is called every tick.
     * @param timeLapse timelapse.
     */
    private void charge(TimeLapse timeLapse) {
        double tickLength = timeLapse.getTickLength();

        for (List<Tuple<Drone, Boolean>> drones : chargers.values()) {
            drones.stream()
                .filter(Objects::nonNull)
                .filter(o -> o.second)
                .forEach(o -> o.first.battery.recharge(tickLength / 1000));
        }
    }

    private List<Drone> redeployChargedDrones() {
        List<Drone> redeployableDrones = new ArrayList<>();

        for (List<Tuple<Drone, Boolean>> droneTuples : chargers.values()) {
            for (int i = 0; i < droneTuples.size(); i++) {
                Tuple<Drone, Boolean> droneTuple = droneTuples.get(i);
                if (droneTuple != null && droneTuple.first.battery.fullyCharged() && droneTuple.second) {
                    redeployableDrones.add(droneTuple.first);
                    droneTuples.set(i, null);
                    timeoutReservations.remove(droneTuple.first);
                }
            }
        }
        return redeployableDrones;
    }

    String getStatus() {
        String status = "The current occupation of the charging point is: \n";
        status += chargers.get(DroneLW.class).stream().filter(Objects::nonNull).filter(o -> o.second).count() + " lightweight drones are charging\n";
        status += chargers.get(DroneHW.class).stream().filter(Objects::nonNull).filter(o -> o.second).count() + " heavyweight drones are charging";
        return status;
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {}

    public final Point getLocation() {
        return location;
    }

    @Override
    public void receiveAnt(Ant ant) {
        if (ant instanceof ExplorationAnt) {
            ExplorationAnt explorationAnt = (ExplorationAnt) ant;
            explorationAnt.setChargingPointOccupations(this.getOccupations(true));
            explorationAnt.setSecondaryAgent(this);
        } else if (ant instanceof IntentionAnt) {
            IntentionAnt intentionAnt = (IntentionAnt) ant;
            // Can do a cast to Drone here since intention ants only originate from drones.
            Drone drone = (Drone) intentionAnt.getPrimaryAgent();
            if (!dronePresent(drone)) {
                // The drone wishes to reserve a spot in the charger
                if (!chargersOccupied(drone)) {
                    this.reserveCharger(drone);
                    intentionAnt.reservationApproved = true;
                } else {
                    intentionAnt.reservationApproved = false;
                }
            } else if (timeoutReservations.containsKey(intentionAnt.getPrimaryAgent())) {
                timeoutReservations.replace(drone, TIMEOUT_RESERVATION);
            }
        }

        synchronized (temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }

    private Map<Class<?>,Double> getOccupations(boolean includeReservations) {
        Map<Class<?>, Double> occupations = new HashMap<>();

        for (Class<?> classType : chargers.keySet()) {
            occupations.put(classType, getOccupationPercentage(classType, includeReservations));
        }

        return occupations;
    }

    @Override
    public String getDescription() {
        return "ChargingPoint - location: " + location + ", occupation: "
            + getOccupationPercentage(DroneLW.class, true) * 100 + "% lightweight & "
            + getOccupationPercentage(DroneHW.class, true) * 100 + "% heavyweight";
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        if (!timeLapse.hasTimeLeft())
            return;

        this.charge(timeLapse);
        for (Drone drone : this.redeployChargedDrones()) {
            drone.stopCharging();
        }

        synchronized (temporaryAnts) {
            while (!temporaryAnts.isEmpty()) {
                temporaryAnts.remove().returnToPrimaryAgent();
            }
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
        List<Drone> timeoutDrones = new ArrayList<>();
        for (Map.Entry<Drone, Integer> reservation : timeoutReservations.entrySet()) {
            int newValue = reservation.getValue() - 1;
            reservation.setValue(newValue);

            if (newValue <= 0) {
                timeoutDrones.add(reservation.getKey());
            }
        }

        for (Drone drone: timeoutDrones) {
            cancelReservation(drone);
        }

        monitor.addMeasurement(new ChargingPointMeasurement(
            this.getOccupationPercentage(DroneLW.class, true),
            this.getOccupationPercentage(DroneHW.class, true)));
    }

    public String toString() {
        return "<ChargingPoint: " + this.getLocation() + ">";
    }


    public ChargingPointMeasurement getAverageOccupation() {
        return monitor.getAverageOccupation();
    }
}
