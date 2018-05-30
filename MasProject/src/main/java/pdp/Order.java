package pdp;

import ant.Ant;
import ant.AntUser;
import ant.ExplorationAnt;
import ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import energy.EnergyModel;
import energy.EnergyUser;
import util.BatteryCalculations;

import javax.measure.unit.SI;
import java.util.*;
import java.util.stream.Collectors;

public class Order extends Parcel implements AntUser, TickListener, EnergyUser {

    private static final int MAXIMUM_HOPCOUNT = 3;
    private static final int RADIUS_HOP = 1000;

    private Queue<Ant> temporaryAnts;
    private Map<ExplorationAnt, List<ExplorationAnt>> followupAnts;
    private Map<ExplorationAnt, Boolean> followupAntsPresence;

    private Optional<Vehicle> reserver;
    private int timeoutTimer;
    private static int TIMEOUT_RESERVE = 20; // TODO fine grain this value

    private RoadUser customer;
    private EnergyModel energyModel;
    private RoadModel roadModel;

    public Order(ParcelDTO parcelDto, Customer customer) {
        super(parcelDto);
        temporaryAnts = new ArrayDeque<>();
        followupAnts = new HashMap<>();
        followupAntsPresence = new HashMap<>();
        reserver = Optional.absent();
        timeoutTimer = TIMEOUT_RESERVE;
        this.customer = customer;

        energyModel = null;
        roadModel = null;
    }


    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pPdpModel) {
        this.roadModel = roadModel;
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {
        this.energyModel = energyModel;
    }

    synchronized private void reserve(Vehicle vehicle) {
        reserver = Optional.of(vehicle);
    }

    synchronized public boolean isReserved() {
        return reserver.isPresent();
    }

    private void resetTimeout() {
        timeoutTimer = TIMEOUT_RESERVE;
    }

    public RoadUser getCustomer() {
        return customer;
    }


    /**
     * Checks if all the exploration ants sent out as reaction to a specific exploration ant have returned.
     * If all the ants have returned, the paths they explored are added to the original ant.
     * The original ant is then added to the list of ants that should be returned to their source.
     */
    private void checkReturnExplorationAnts() {
        List<ExplorationAnt> antsToReturn = new ArrayList<>();
        for (Map.Entry<ExplorationAnt, List<ExplorationAnt>> entry : followupAnts.entrySet()) {
            // If all the sent out exploration ants have returned
            if (entry.getValue().stream().allMatch(o -> followupAntsPresence.get(o))) {
                ExplorationAnt originalAnt = entry.getKey();

                originalAnt.addPathEntries(entry.getValue().stream()
                    .map(ExplorationAnt::getPaths)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));

                antsToReturn.add(originalAnt);
            }
        }

        for (ExplorationAnt ant : antsToReturn) {
            followupAnts.get(ant).forEach(o -> followupAntsPresence.remove(o));
            followupAnts.remove(ant);
            temporaryAnts.add(ant);
        }
    }


    @Override
    public void receiveExplorationAnt(ExplorationAnt ant) {
        if (ant.getPrimaryAgent() == this) {
            // The exploration ant was sent out by this order and has returned
            // Set the boolean flag to true for this particular ant (marking it as returned)
            followupAntsPresence.replace(ant, true);
        } else {
            // TODO rewrite this part more clearly
            // The order is the destination for the ant
            ant.setSecondaryAgent(this);
            List<AntUser> travelledPath = ant.addHopTravelledPath(this);

            int hopCount = ant.getHopCount();

            RoadModel rm = getRoadModel();
            Point primaryLocation = rm.getPosition(ant.getPrimaryAgent());
            double distancePickup = rm.getDistanceOfPath(rm.getShortestPathTo(primaryLocation, getPickupLocation())).doubleValue(SI.METER);
            double distanceDeliver = rm.getDistanceOfPath(rm.getShortestPathTo(getPickupLocation(), getDeliveryLocation())).doubleValue(SI.METER);
            double batteryDecrease = BatteryCalculations.calculateNecessaryBatteryLevel(ant.getDroneSpeedRange(),
                ant.getDroneCapacity(), getNeededCapacity(), distancePickup, distanceDeliver);

            // The battery decrease can also be used for the time calculation, since it is based on time
            double remainingBatteryLevel = ant.getRemainingBattery() - batteryDecrease;
            long resultingTime = ant.getResultingTime() + (int) Math.ceil(batteryDecrease*1000) + getDeliveryDuration() + getPickupDuration();

            // Send out more exploration ants if necessary
            if (hopCount == MAXIMUM_HOPCOUNT) {
                // Send an exploration ant to the charging point to finish the explored path
                ExplorationAnt newAnt =
                    new ExplorationAnt(this, remainingBatteryLevel, resultingTime, hopCount+1);
                newAnt.setDrone(ant.getDrone());
                energyModel.getChargingPoint().receiveExplorationAnt(newAnt);

                followupAnts.put(ant, new ArrayList<>(Arrays.asList(newAnt)));
                followupAntsPresence.put(newAnt, false);
            } else {
                // Send exploration ants to all viable hops within a given radius, and one to the charging point
                Collection<Order> ordersWithinDistance =
                    RoadModels.findObjectsWithinRadius(this.getDeliveryLocation(), roadModel, RADIUS_HOP, Order.class);
                List<ExplorationAnt> newAnts = new ArrayList<>();

                for (Order order : ordersWithinDistance) {
                    // Make sure no loops or orders which are already reserved are considered
                    if (travelledPath.contains(order)
                        || order.isReserved()
                        || ant.getDroneCapacity() <= order.getNeededCapacity()) {
                        continue;
                    }

                    ExplorationAnt newAnt =
                        new ExplorationAnt(this, remainingBatteryLevel, resultingTime, hopCount+1);
                    newAnt.setTravelledPath(travelledPath);
                    newAnt.setDrone(ant.getDrone());
                    newAnts.add(newAnt);
                    order.receiveExplorationAnt(newAnt);
                }

                // Charging point exploration ant
                ExplorationAnt newAnt =
                    new ExplorationAnt(this, remainingBatteryLevel, resultingTime, hopCount+1);
                newAnts.add(newAnt);
                newAnt.setDrone(ant.getDrone());
                energyModel.getChargingPoint().receiveExplorationAnt(newAnt);

                followupAnts.put(ant, newAnts);
                newAnts.forEach(o -> followupAntsPresence.put(o, false));
            }
        }
    }

    @Override
    public void receiveIntentionAnt(IntentionAnt ant) {
        if (!this.isReserved()) {
            // If this order has not been reserved yet, reserve it for the drone.
            // NOTE: can do a cast to Drone here since intention ants only originate from drones.
            this.reserve((Drone) ant.getPrimaryAgent());
            ant.reservationApproved = true;
            resetTimeout();
        } else {
            // The order has already been reserved, either deny the reservation for the drone or
            // refresh the timer if it the ant is meant as a reconfirmation of the reservation.
            if (reserver.get().equals(ant.getPrimaryAgent())) {
                resetTimeout();
                ant.reservationApproved = true;
            } else {
                ant.reservationApproved = false;
            }
        }
        synchronized(temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        checkReturnExplorationAnts();

        // Send out all the temporary ants to their respective primary agents.
        synchronized (temporaryAnts) {
            while (!temporaryAnts.isEmpty()) {
                temporaryAnts.remove().returnToPrimaryAgent();
            }
        }

    }


    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (reserver.isPresent()) {
            timeoutTimer--;
        }

        if (timeoutTimer <= 0) {
            reserver = Optional.absent();
        }
    }

    @Override
    public String getDescription() {
        return String.format("<Order (%f grams)>", getNeededCapacity());

//        RoadModel rm = getRoadModel();
//        String result = "";
//        result += "Order - location: ";
//        result += rm.containsObject(this) ? rm.getPosition(this).toString() : "in transit";
//        result += ", payload: " + this.getNeededCapacity() + " grams";
//        return result;
    }

    // TODO remove, here for debugging
    public String toString() {
        return "<Order: " + this.getNeededCapacity() + " grams>";
    }
}
