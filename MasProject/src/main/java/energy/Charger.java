package energy;

import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import pdp.Drone;
import util.ChargerReservation;
import util.UnpermittedChargeException;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Charger implements EnergyUser, TickListener {
    private Drone currentDrone;
    private TreeSet<ChargerReservation> reservations;
    private long currentTime;

    public Charger() {
        currentDrone = null;

        reservations = new TreeSet<>((ChargerReservation res1, ChargerReservation res2) -> {
            // Intentionally left out equal values since these should not occur (due to collision check).
            if (res1.equals(res2)) return 0;
            return res1.getTimeWindow().first > res2.getTimeWindow().first ? 1 : -1;
        });

        currentTime = 0;
    }

    public void setDrone(Drone drone, TimeLapse timeLapse) throws UnpermittedChargeException {
        ChargerReservation reservation = reservations.first();

        if (reservation.getTimeWindow().first <= timeLapse.getStartTime() && reservation.getOwner() == drone) {
            currentDrone = drone;
        } else {
            throw new UnpermittedChargeException();
        }

    }

    private void releaseDrone() {
        currentDrone.stopCharging(currentTime);
        currentDrone = null;
    }

    public void addReservation(ChargerReservation reservation) {
        reservations.add(reservation);
    }

    public boolean reservationPossible(long timeBegin, long timeEnd) {
        return reservations.stream().noneMatch(o -> o.conflicts(timeBegin, timeEnd));
    }

    public void removeReservationForDrone(Drone drone) {
        reservations.removeAll(reservations.stream()
            .filter(o -> o.getOwner() == drone)
            .collect(Collectors.toList()));

        if (currentDrone == drone) {
            releaseDrone();
        }
    }

    public boolean hasReservation(Drone drone) {
        return reservations.stream().anyMatch(o -> o.getOwner() == drone);
    }

    public boolean hasReservation(Drone drone, long startTime) {
        return reservations.stream().anyMatch(o -> o.getOwner() == drone
            && o.getTimeWindow().first <= startTime
            && o.getTimeWindow().second >= startTime);
    }

    public boolean hasReservation(Drone drone, long startTime, long endTime) {
        return reservations.stream().anyMatch(o -> o.getOwner() == drone
            && o.getTimeWindow().first == startTime
            && o.getTimeWindow().second == endTime);
    }

    @Override
    public void tick(@Nonnull TimeLapse timeLapse) {
        currentTime = timeLapse.getStartTime();

        if (currentDrone != null) {
            this.chargeDrone((double) timeLapse.getTickLength() / 1000);
        }

        if (!reservations.isEmpty()) {
            if (currentTime > reservations.first().getTimeWindow().second) {
                if (currentDrone != null) {
                    releaseDrone();
                }
                // Remove the reservation since it passed its time window.
                reservations.pollFirst();
            }
        }
    }

    @Override
    public void afterTick(@Nonnull TimeLapse timeLapse) {

    }

    public boolean hasReservationCurrently() {
        return !reservations.isEmpty() && reservations.first().getTimeWindow().first <= currentTime;
    }

    public boolean hasReservationCurrently(Drone drone) {
        return !reservations.isEmpty()
            && reservations.first().getTimeWindow().first <= currentTime
            && reservations.first().getOwner() == drone;
    }

    public boolean isDronePresent() {
        return currentDrone != null;
    }

    public Drone getCurrentDrone() {
        return currentDrone;
    }

    public ChargerReservation getBestReservation(Drone drone, long beginTime, long endTime) {
        // Loop over all the reservations and find the best one possible in the given time frame
        long reservationDuration = endTime - beginTime;

        if (reservations.isEmpty()) {
            return new ChargerReservation(drone, beginTime, endTime);
        } else if (reservations.first().getTimeWindow().first >= endTime) {
            return new ChargerReservation(drone, beginTime, endTime);
        }

        ChargerReservation previousReservation = null;
        // Try to fit the reservation in between current reservations
        Iterator<ChargerReservation> it = reservations.iterator();
        while (it.hasNext()) {
            ChargerReservation reservation = it.next();

            // Base case
            if (previousReservation != null) {
                long durationBetween = previousReservation.getTimeWindow().second -
                    reservation.getTimeWindow().first;
                // Check if the time between the 2 reservations is greater than the time needed
                // if so, get a reservation in between.
                if (durationBetween > reservationDuration) {
                    long newBeginTime = previousReservation.getTimeWindow().second;
                    return new ChargerReservation(drone, newBeginTime, newBeginTime + reservationDuration);
                }
            }
            previousReservation = reservation;
        }

        // At this point, the previous reservation is the last reservation
        long newBeginTime = Math.max(previousReservation.getTimeWindow().second, beginTime);
        return new ChargerReservation(drone, newBeginTime, newBeginTime + reservationDuration);
    }

    private void chargeDrone(double amount) {
        currentDrone.battery.recharge(amount);

        if (currentDrone.battery.fullyCharged()) {
            releaseDrone();
            // Remove the reservation since it passed its time window.
            reservations.pollFirst();
        }
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {

    }
}
