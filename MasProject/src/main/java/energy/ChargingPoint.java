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
import pdp.Order;
import util.ChargerReservation;
import util.ChargingPointMeasurement;
import util.ChargingPointMonitor;
import util.UnpermittedChargeException;

import javax.annotation.Nonnull;
import javax.measure.unit.SI;
import java.util.*;
import java.util.stream.Stream;


public class ChargingPoint implements AntUser, RoadUser, EnergyUser, TickListener {
    private static final Integer TIMEOUT_RESERVATION = 20;
    private static final long MAXIMUM_RESERVATION_LENIENCY = 5*60*1000;

    private Optional<RoadModel> roadModel;
    private Point location;
    private Map<Class<? extends Drone>, List<Charger>> chargers; // Keeps a list per drone type of chargers
    private Map<Drone, Integer> timeoutReservations;

    private Queue<Ant> temporaryAnts;
    private ChargingPointMonitor monitor;

    public ChargingPoint(Point loc, List<Charger> LWChargers, List<Charger> HWChargers) {
        roadModel = Optional.empty();
        location = loc;
        chargers = new HashMap<>();
        chargers.put(DroneLW.class, LWChargers);
        chargers.put(DroneHW.class, HWChargers);
        timeoutReservations = new HashMap<>();
        temporaryAnts = new ArrayDeque<>();
        monitor = new ChargingPointMonitor();
    }

    @Override
    public void initRoadUser(@Nonnull RoadModel roadModel) {
        this.roadModel = Optional.of(roadModel);
        roadModel.addObjectAt(this, location);
    }

    private ChargerReservation reserveCharger(Drone drone, long timeBegin, long timeEnd) {
        assert(!this.chargersOccupied(drone.getClass()));

        ChargerReservation bestReservation = null;
        Charger bestCharger = null;
        List<Charger> chargers = this.chargers.get(drone.getClass());

        for (Charger charger : chargers) {
            ChargerReservation reservation = charger.getBestReservation(drone, timeBegin, timeEnd);
            // Make sure that the reservation is not too far off the desired reservation
            if (bestReservation == null && timeBegin + MAXIMUM_RESERVATION_LENIENCY > reservation.getTimeWindow().first) {
                bestReservation = reservation;
                bestCharger = charger;
            } else if (bestReservation != null && bestReservation.getTimeWindow().first > reservation.getTimeWindow().first) {
                bestReservation = reservation;
                bestCharger = charger;
            }
        }

        if (bestReservation != null) {
            timeoutReservations.put(drone, TIMEOUT_RESERVATION);
            bestCharger.addReservation(bestReservation);
        }
        return bestReservation;
    }

    private void cancelReservation(Drone drone) {
        assert(this.dronePresent(drone, true));
        List<Charger> chargers = this.chargers.get(drone.getClass());

        // Remove the drone from the charger
        chargers.forEach(o -> o.removeReservationForDrone(drone));
        // Cancel the timer on the specific reservation
        timeoutReservations.remove(drone);
    }

    public boolean chargeDrone(Drone drone, TimeLapse timeLapse) throws UnpermittedChargeException {
        if (!dronePresent(drone, true)) {
            System.err.println("Drone not reserved yet, unable to charge.");
        } else {
            for (Charger charger : chargers.get(drone.getClass())) {
                if (charger.hasReservation(drone, timeLapse.getStartTime())) {
                    charger.setDrone(drone, timeLapse);
                    return true;
                }
            }
        }
        return false;
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

    public List<Charger> getChargeStations(Class Drone) {
        return chargers.get(Drone);
    }

    public boolean dronePresent(Drone drone, boolean countReservation) {
        return countReservation ?
            chargers.get(drone.getClass()).stream().anyMatch(o -> o.hasReservation(drone)) :
            chargers.get(drone.getClass()).stream().anyMatch(o -> o.getCurrentDrone() == drone);
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
        RoadModel rm = getRoadModel();
        Point primaryLocation;
        if (ant.getPrimaryAgent() instanceof Drone) {
            primaryLocation = rm.getPosition(ant.getPrimaryAgent());
        } else {
            primaryLocation = ((Order) ant.getPrimaryAgent()).getDeliveryLocation();
        }
        double distance = rm.getDistanceOfPath(rm.getShortestPathTo(primaryLocation, this.location)).doubleValue(SI.METER);
        double batteryDecrease = distance / ant.getDroneSpeedRange().getSpeed(1);

        // The battery decrease can also be used for the time calculation, since it is based on time
        double remainingBatteryLevel = ant.getRemainingBattery() - batteryDecrease;
        long resultingTime = ant.getResultingTime() + (int) Math.ceil(batteryDecrease*1000);

        // Since this is the last destination of the exploration ant,
        // set the remaining battery level and resulting time in the ant.
        // These values will be used to build the exploration path
        ant.setRemainingBattery(remainingBatteryLevel);
        ant.setResultingTime(resultingTime);

        // Get the best reservation possible for the given drone, and include it in the path
        ant.setSecondaryAgent(this);
        ant.buildInitialExplorationPath(this.getBestReservation(remainingBatteryLevel, resultingTime, ant.getDrone()));


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
            acquireReservationForAnt(chargeAnt, drone);
        } else if (timeoutReservations.containsKey(ant.getPrimaryAgent())) {
            if (checkReservation(drone, chargeAnt.getBeginTime(), chargeAnt.getEndTime())) {
                timeoutReservations.replace(drone, TIMEOUT_RESERVATION);
                ant.reservationApproved = true;
            } else {
                cancelReservation(drone);
                acquireReservationForAnt(chargeAnt, drone);
            }
        }

        synchronized (temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }


    private ChargerReservation getBestReservation(double remainingBatteryLevel, long resultingTime, Drone drone) {
        ChargerReservation bestReservation = null;

        // TODO 2000 -> 2 sec buffer -> necessary?
        int chargeTime = (int) Math.ceil((drone.battery.getMaxCapacity() - remainingBatteryLevel) * 1000) + 2000;

        for (Charger charger : chargers.get(drone.getClass())) {
            ChargerReservation reservation = charger.getBestReservation(drone, resultingTime, resultingTime + chargeTime);

            if (bestReservation == null) {
                bestReservation = reservation;
            } else if (bestReservation.getTimeWindow().first > reservation.getTimeWindow().first) {
                bestReservation = reservation;
            }
        }

        return bestReservation;
    }

    private void acquireReservationForAnt(ChargeIntentionAnt ant, Drone drone) {
        ChargerReservation reservation = this.reserveCharger(drone, ant.getBeginTime(), ant.getEndTime());
        if (reservation != null) {
            ant.setBeginTime(reservation.getTimeWindow().first);
            ant.setEndTime(reservation.getTimeWindow().second);
        }
        ant.reservationApproved = !Objects.isNull(reservation);
    }

    private boolean checkReservation(Drone drone, long beginTime, long endTime) {
        for (Charger charger : chargers.get(drone.getClass())) {
            if (charger.hasReservation(drone, beginTime, endTime)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "<ChargingPoint>";
//        return "ChargingPoint - location: " + location + ", occupation: "
//            + getOccupationPercentage(DroneLW.class, false) * 100 + "% lightweight & "
//            + getOccupationPercentage(DroneHW.class, false) * 100 + "% heavyweight";
    }

    @Override
    public void tick(@Nonnull TimeLapse timeLapse) {
        if (!timeLapse.hasTimeLeft())
            return;

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

    private RoadModel getRoadModel() {
        return roadModel.get();
    }

    public String toString() {
        return "<ChargingPoint: " + this.getLocation() + ">";
    }
}
