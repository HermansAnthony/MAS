package util;

import ant.AntUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationPath {
    private List<AntUser> path;
    private double merit;
    private ChargerReservation chargerReservation;

    public ReservationPath() {
        this(new ArrayList<>(), Double.NEGATIVE_INFINITY, null);
    }

    public ReservationPath(List<AntUser> path, double merit, ChargerReservation chargerReservation) {
        this.path = path;
        this.merit = merit;
        this.chargerReservation = chargerReservation;
    }

    public List<AntUser> getPath() {
        return path;
    }

    public void setMerit(double merit) {
        this.merit = merit;
    }

    public double getMerit() {
        return merit;
    }

    public ChargerReservation getChargerReservation() {
        return chargerReservation;
    }

    /**
     * Removes the ant user *AND* all the following nodes from the path.
     * @param antUser The ant user and its subsequent nodes that should be removed from the path.
     */
    public void removeAntUserFromPath(AntUser antUser) {
        if (!path.contains(antUser)) {
            System.err.println("AntUser " + antUser.getDescription() + " is not part of the path.");
            System.err.println("\t" + path);
            return;
        }

        int antUserLocation = path.indexOf(antUser);
        for (int i = path.size()-1; i >= antUserLocation; i--) {
            path.remove(i);
        }
    }

    public void removeFirstNode() {
        if (path.size() < 1) {
            System.err.println("Could not remove the first node of the path since the path is empty.");
            return;
        }

        this.path.remove(0);
    }

    public String toString() {
        return "["
            + String.join(", ", path.stream().map(AntUser::getDescription).collect(Collectors.toList()))
            + "]";
    }
}
