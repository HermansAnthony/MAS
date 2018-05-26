package energy;

import util.ChargerReservation;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Charger<DroneType> {
    private DroneType currentDrone;
    TreeSet<ChargerReservation<DroneType>> reservations;

    public Charger() {
        currentDrone = null;
        reservations = new TreeSet<>(new Comparator<ChargerReservation<DroneType>>() {
            @Override
            public int compare(ChargerReservation<DroneType> reservation1, ChargerReservation<DroneType> reservation2) {
                return 0;
            }
        });
    }

    public void setDrone(DroneType drone) {
        currentDrone = drone;
    }

    public void releaseDrone() {
        currentDrone = null;
    }

    public void addReservation(ChargerReservation<DroneType> reservation) {
        reservations.add(reservation);
    }

    public boolean reservationPossible(long timeBegin, long timeEnd) {
        return reservations.stream().noneMatch(o -> o.conflicts(timeBegin, timeEnd));
    }

    public void removeReservationForDrone(DroneType drone) {
        reservations.removeAll(reservations.stream()
            .filter(o -> o.getOwner() == drone)
            .collect(Collectors.toList()));
    }
}
