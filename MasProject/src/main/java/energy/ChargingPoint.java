package energy;

import ant.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import pdp.Drone;
import pdp.DroneHW;
import pdp.DroneLW;
import util.ChargerReservation;
import util.ChargingPointMeasurement;
import util.ChargingPointMonitor;
import util.UnpermittedChargeException;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;


public class ChargingPoint implements AntUser, RoadUser, EnergyUser, TickListener {
    private static final Integer TIMEOUT_RESERVATION = 20;
    private Point location;
    private Map<Class<? extends Drone>, List<Charger>> chargers; // Keeps a list per drone type of chargers
    private Map<Drone, Integer> timeoutReservations;

    private Queue<Ant> temporaryAnts;
    private ChargingPointMonitor monitor;

    public ChargingPoint(Point loc, int maxCapacityLW, int maxCapacityHW) {
        location = loc;
        chargers = new HashMap<>();
        chargers.put(DroneLW.class, Arrays.asList(new Charger[maxCapacityLW]));
        chargers.put(DroneHW.class, Arrays.asList(new Charger[maxCapacityHW]));
        for (int i = 0; i < maxCapacityLW; i++) {
            chargers.get(DroneLW.class).set(i, new Charger());
        }
        for (int i = 0; i < maxCapacityHW; i++) {
            chargers.get(DroneHW.class).set(i, new Charger());
        }
        timeoutReservations = new HashMap<>();
        temporaryAnts = new ArrayDeque<>();
        monitor = new ChargingPointMonitor();
    }

    @Override
    public void initRoadUser(@Nonnull RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }

    private void reserveCharger(Drone drone, long timeBegin, long timeEnd) {
        assert(!this.chargersOccupied(drone.getClass()));

        List<Charger> chargers = this.chargers.get(drone.getClass());
        for (Charger charger : chargers) {
            if (charger.reservationPossible(timeBegin, timeEnd)) {
                charger.addReservation(new ChargerReservation(drone, timeBegin, timeEnd));
                break;
            }
        }
        timeoutReservations.put(drone, TIMEOUT_RESERVATION);
    }

    private void cancelReservation(Drone drone) {
        assert(this.dronePresent(drone, true));
        List<Charger> chargers = this.chargers.get(drone.getClass());

        // Remove the drone from the charger
        chargers.forEach(o -> o.removeReservationForDrone(drone));
        // Cancel the timer on the specific reservation
        timeoutReservations.remove(drone);
    }

    public void chargeDrone(Drone drone, TimeLapse timeLapse) throws UnpermittedChargeException {
        if (!dronePresent(drone, true)) {
            System.err.println("Drone not reserved yet, unable to charge.");
        } else {
            for (Charger charger : chargers.get(drone.getClass())) {
                if (charger.hasReservation(drone, timeLapse.getStartTime())) {
                    charger.setDrone(drone, timeLapse);
                }
            }
        }
    }

    private boolean chargersOccupied(Drone drone) {
        return chargersOccupied(drone.getClass());
    }

    private boolean chargersOccupied(Class droneClass) {
        // TODO fix/change this -> maybe look at reservations instead?
        return chargers.get(droneClass).stream().allMatch(Charger::hasReservationCurrently);
    }

    public double getOccupationPercentage(Class droneClass, boolean includeReservations) {
        Stream<Charger> stream = chargers.get(droneClass).stream()
            .filter(Charger::hasReservationCurrently);
        if (!includeReservations) {
            stream = stream.filter(Charger::isDronePresent);
        }
        return ((double) stream.count()) / chargers.get(droneClass).size();
    }

    public List<Charger> getChargeStations(Class Drone){
        return chargers.get(Drone);
    }

    public boolean dronePresent(Drone drone, boolean countReservation) {
        return countReservation ?
            chargers.get(drone.getClass()).stream().anyMatch(o -> o.hasReservationCurrently(drone)) :
            chargers.get(drone.getClass()).stream().anyMatch(o -> o.getCurrentDrone() == drone);
    }


    /**
     * Charges all the drones present in the ChargingPoint.
     * @param timeLapse timelapse.
     */
    private void charge(TimeLapse timeLapse) {
        double tickLength = timeLapse.getTickLength();

        for (List<Charger> chargers : chargers.values()) {
            chargers.stream()
                .filter(Charger::isDronePresent)
                .forEach(o -> o.getCurrentDrone().battery.recharge(tickLength / 1000));
        }
    }

    private List<Drone> redeployChargedDrones() {
        List<Drone> redeployableDrones = new ArrayList<>();

        for (List<Charger> chargers : chargers.values()) {
            for (Charger charger : chargers) {
                Drone currentDrone = charger.getCurrentDrone();

                // If the currently present drone is fully charged, remove it from the charger and its reservation
                if (currentDrone != null && currentDrone.battery.fullyCharged()) {
                    redeployableDrones.add(currentDrone);
                    charger.releaseDrone();
                    charger.removeReservationForDrone(currentDrone);
                    timeoutReservations.remove(currentDrone);
                }
            }
        }
        return redeployableDrones;
    }

    String getStatus() {
        String status = "The current occupation of the charging point is: \n";
        status += chargers.get(DroneLW.class).stream().filter(o -> o.getCurrentDrone() != null).count() + " lightweight drones are charging\n";
        status += chargers.get(DroneHW.class).stream().filter(o -> o.getCurrentDrone() != null).count() + " heavyweight drones are charging";
        return status;
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {}

    public final Point getLocation() {
        return location;
    }

    public ChargingPointMeasurement getAverageOccupation() {
        return monitor.getAverageOccupation();
    }

    private Map<Class<?>,Double> getOccupations(boolean includeReservations) {
        Map<Class<?>, Double> occupations = new HashMap<>();

        for (Class<?> classType : chargers.keySet()) {
            occupations.put(classType, getOccupationPercentage(classType, includeReservations));
        }

        return occupations;
    }

    @Override
    public void receiveExplorationAnt(ExplorationAnt ant) {
        ant.setChargingPointOccupations(this.getOccupations(true));
        ant.setSecondaryAgent(this);

        synchronized (temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }

    @Override
    public void receiveIntentionAnt(IntentionAnt ant) {
        if (!(ant instanceof ChargeIntentionAnt)) {
            System.err.println("Only ChargeIntentionAnts should be sent to the charging point.");
            return;
        }
        ChargeIntentionAnt chargeAnt = (ChargeIntentionAnt) ant;

        // Can do a cast to Drone here since intention ants only originate from drones.
        Drone drone = (Drone) ant.getPrimaryAgent();
        if (!dronePresent(drone, true)) {
            // The drone wishes to reserve a spot in the charger
            if (!chargersOccupied(drone)) {
                // TODO temporary for now until intention ant also contains time window of reservation
                this.reserveCharger(drone, chargeAnt.getBeginTime(), chargeAnt.getEndTime());
                ant.reservationApproved = true;
            } else {
                ant.reservationApproved = false;
            }
        } else if (timeoutReservations.containsKey(ant.getPrimaryAgent())) {
            // TODO make sure the reservation window in the intention ant is the same as the one currently present -> otherwise try to get new reservation
            timeoutReservations.replace(drone, TIMEOUT_RESERVATION);
            ant.reservationApproved = true;
        }

        synchronized (temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }

    @Override
    public String getDescription() {
        return "ChargingPoint - location: " + location + ", occupation: "
            + getOccupationPercentage(DroneLW.class, false) * 100 + "% lightweight & "
            + getOccupationPercentage(DroneHW.class, false) * 100 + "% heavyweight";
    }

    @Override
    public void tick(@Nonnull TimeLapse timeLapse) {
        if (!timeLapse.hasTimeLeft())
            return;

        this.charge(timeLapse);
        for (Drone drone : this.redeployChargedDrones()) {
            drone.stopCharging(timeLapse);
        }

        synchronized (temporaryAnts) {
            while (!temporaryAnts.isEmpty()) {
                temporaryAnts.remove().returnToPrimaryAgent();
            }
        }
    }

    @Override
    public void afterTick(@Nonnull TimeLapse timeLapse) {
        List<Drone> timeoutDrones = new ArrayList<>();
        for (Map.Entry<Drone, Integer> reservationTimeouts : timeoutReservations.entrySet()) {
            int newValue = reservationTimeouts.getValue() - 1;
            reservationTimeouts.setValue(newValue);

            if (newValue <= 0) {
                timeoutDrones.add(reservationTimeouts.getKey());
            }
        }

        for (Drone drone: timeoutDrones) {
            cancelReservation(drone);
        }

        monitor.addMeasurement(new ChargingPointMeasurement(
            this.getOccupationPercentage(DroneLW.class, false),
            this.getOccupationPercentage(DroneHW.class, false)));
    }

    public String toString() {
        return "<ChargingPoint: " + this.getLocation() + ">";
    }
}
