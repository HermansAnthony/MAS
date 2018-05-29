package energy;

import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import pdp.Drone;
import util.ChargerReservation;
import util.UnpermittedChargeException;

import java.util.TreeSet;
import java.util.stream.Collectors;

public class Charger implements TickListener {
    private Drone currentDrone;
    private TreeSet<ChargerReservation> reservations;
    private long currentTime;

    Charger() {
        currentDrone = null;

        reservations = new TreeSet<>((ChargerReservation res1, ChargerReservation res2) -> {
            // Intentionally left out equal values since these should not occur (due to collision check).
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

    public void releaseDrone() {
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
    }

    public boolean hasReservation(Drone drone, long startTime) {
        return reservations.stream().anyMatch(o -> o.getOwner() == drone
            && o.getTimeWindow().first <= startTime
            && o.getTimeWindow().second >= startTime);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (reservations.first().getTimeWindow().second <= timeLapse.getStartTime()) {
            // Remove the reservation since it passed its time window.
            reservations.remove(reservations.first());
        }
        currentTime = timeLapse.getStartTime();
        // TODO also release ants from this point?
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

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
}
