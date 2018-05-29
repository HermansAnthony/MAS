package pdp;

import ant.AntUser;
import ant.ChargeIntentionAnt;
import ant.ExplorationAnt;
import ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import energy.ChargingPoint;
import energy.EnergyDTO;
import energy.EnergyModel;
import energy.EnergyUser;
import util.*;

import javax.annotation.Nonnull;
import javax.measure.unit.SI;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class Drone extends Vehicle implements EnergyUser, AntUser {
    private static int nextID = 0;
    private static int RECONSIDERATION_MERIT = 10;


    int ID;
    private final Range SPEED_RANGE;
    private Optional<Parcel> payload;

    // Energy related stuff
    private Optional<EnergyModel> energyModel;
    private ChargingStatus chargingStatus;
    public EnergyDTO battery;

    // Delegate MAS stuff
    private delegateMasState state;
    private Map<ExplorationAnt, Boolean> explorationAnts;
    private Map<IntentionAnt, Boolean> intentionAnt;
    // TODO could maybe move all the intention ants inside the reservation path
    private ReservationPath intendedPath;


    // Logger for the delegate mas actions
    private DroneMonitor monitor;


    protected Drone(VehicleDTO dto, EnergyDTO battery, Range speedRange) {
        super(dto);
        ID = nextID++;
        SPEED_RANGE = speedRange;
        payload = Optional.absent();

        energyModel = Optional.absent();
        chargingStatus = ChargingStatus.Idle;
        this.battery = battery;

        state = delegateMasState.initialState;
        explorationAnts = new HashMap<>();
        intentionAnt = new HashMap<>();
        intendedPath = null;

        monitor = new DroneMonitor(this.getDroneString());
    }

    public int getID() {return this.ID;}


    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    @Override
    public void initEnergyUser(EnergyModel model) {
        energyModel = Optional.of(model);
    }

    private EnergyModel getEnergyModel() {
        return energyModel.get();
    }

    @Override
    public double getSpeed() {
        double currentContentsSize = getPDPModel().getContentsSize(this);

        // If the drone is carrying payload, adjust the ratio at which speed it moves
        double ratio = currentContentsSize == 0 ? 1 : 1 - (currentContentsSize / getCapacity());
        return SPEED_RANGE.getSpeed(ratio);
    }

    public double getSpeed(double capacity) {
        assert capacity <= getCapacity();
        return SPEED_RANGE.getSpeed(1 - (capacity / getCapacity()));
    }

    private void delegateMAS(TimeLapse timeLapse) {
        switch (state){
            case initialState: {
                explorationAnts.clear();
                intentionAnt.clear();
                payload = Optional.absent();
                intendedPath = new ReservationPath();
                chargingStatus = ChargingStatus.Idle;

                // Initially, spawn exploration ants to get information about all possible orders.
                spawnExplorationAnts(true);

                // Only leave this state if exploration ants have been spawned.
                if (!explorationAnts.isEmpty()) {
                    state = delegateMasState.explorationAntsReturned;
                }
                break;
            }
            case explorationAntsReturned: {
                // Check if all exploration ants have returned
                if (!explorationAnts.values().contains(false)) {
                    // Send out an intention ant to the order with the highest merit
                    ReservationPath intention = getBestIntentionPath(timeLapse);
                    if (intention.getMerit() == Double.NEGATIVE_INFINITY) {
                        state = delegateMasState.initialState;
                        break;
                    }
                    intendedPath = intention;

                    String description = " Best merit: " + intention.getMerit() + ".\n";
                    description += "Intention ants are sent to '" + intention +"'.\n";
                    monitor.writeToFile(timeLapse.getStartTime(), description);

                    spawnIntentionAnts(intention.getPath());

                    // Clear all the exploration ants since they are outdated at this point
                    explorationAnts.clear();
                    state = delegateMasState.intentionAntReturned;
                }
                break;
            }
            case intentionAntReturned: {
                if (intentionAnt.values().contains(false)) {
                    // Not all intention ants have returned yet
                    return;
                }

                List<AntUser> path = intendedPath.getPath();
                for (int i = 0; i < path.size(); i++) {
                    // Search for the related ant
                    AntUser antUser = path.get(i);
                    IntentionAnt ant = intentionAnt.keySet().stream()
                        .filter(o -> o.getSecondaryAgent() == antUser)
                        .findFirst().get();

                    if (i == 0) {
                        if (!ant.reservationApproved) {
                            // If the first node of the path was not approved, drop the whole path and resend exploration ants
                            state = delegateMasState.initialState;
                            return;
                        }

                        // Check if the first node of the path is either an order or the chargingPoint
                        // and act accordingly
                        if (antUser instanceof Order) {
                            payload = Optional.of((Order) antUser);
                        } else if (antUser instanceof ChargingPoint) {
                            chargingStatus = ChargingStatus.MoveToCharger;
                        }
                    } else {
                        // For the subsequent nodes, check the approval of the reservations
                        // If a reservation has not been approved, remove all subsequent nodes from the path and
                        // intention ants, as well as calculating the new merit of the remaining path
                        if (!ant.reservationApproved) {
                            removeNodesFromPath(antUser, timeLapse);
                        }
                    }
                }
                state = delegateMasState.continueReservation;
            }
            case continueReservation: {
                if (intentionAnt.values().contains(false)) {
                    // Not all the intention ants have returned yet, move to the next state
                    state = delegateMasState.spawnExplorationAnts;
                    break;
                }

                if (intentionAnt.keySet().stream().anyMatch(o -> !o.reservationApproved)) {
                    // One of the reservations has now been unapproved -> remove this from the path

                    // Find the first occurrence where the reservation was not approved
                    for (AntUser antUser : intendedPath.getPath()) {
                        if (intentionAnt.keySet().stream()
                            .anyMatch(o -> o.getSecondaryAgent() == antUser && !o.reservationApproved)) {
                            removeNodesFromPath(antUser, timeLapse);
                            break;
                        }
                    }
                } else {
                    // Resend the intention ants if no problem with the reservations occurred
                    for (IntentionAnt ant : intentionAnt.keySet()) {
                        ant.reservationApproved = false; // reset reservation approval
                        ant.getSecondaryAgent().receiveIntentionAnt(ant); // send the ant
                        intentionAnt.put(ant, false); // mark the ant as gone
                    }
                }
                state = delegateMasState.spawnExplorationAnts;
                break;
            }
            case spawnExplorationAnts: {
                if (!payload.isPresent() && chargingStatus == ChargingStatus.Idle) {
                    // The order has been delivered or the drone is fully charged, go back to the initial state
                    state = delegateMasState.initialState;
                    break;
                }

                if (explorationAnts.isEmpty()) {
                    // Spawn new exploration ants for possible reconsiderations
                    spawnExplorationAnts(false);
                } else if (!explorationAnts.containsValue(false)) {
                    // Check for reconsiderations
                    ReservationPath intention = getBestIntentionPath(timeLapse);
                    double meritDifference = intention.getMerit() - intendedPath.getMerit();
                    if (meritDifference > RECONSIDERATION_MERIT) {
                        if (reconsiderAction(intention, timeLapse)) {
                            String description = " Reconsideration happened: new best merit = " + intention.getMerit() + ".\n";
                            description += "Intention ants are sent to '" + intention +"'.\n";
                            monitor.writeToFile(timeLapse.getStartTime(), description);

                            state = delegateMasState.intentionAntReturned;
                            break;
                        }
                    }
                    explorationAnts.clear();
                }

                state = delegateMasState.continueReservation;
                break;
            }
        }
    }

    /**
     * Removes a part of the path, starting from the first node given.
     * This method also removes any intention ants associated with the removed nodes.
     * @param firstNode The node from which the path should be shortened.
     * @param timeLapse timelapse.
     */
    private void removeNodesFromPath(AntUser firstNode, TimeLapse timeLapse) {
        List<AntUser> path = intendedPath.getPath();

        for (int i = 0; i < path.size(); i++) {
            if (firstNode == path.get(i)) {
                // Remove all subsequent nodes from the path and intention ants,
                // as well as calculating the new merit of the remaining path
                for (int j = path.size() - 1; j >= i; j--) {
                    AntUser followingAntUser = path.get(j);
                    IntentionAnt antToRemove = intentionAnt.keySet().stream()
                        .filter(o -> o.getSecondaryAgent() == followingAntUser)
                        .findFirst().get();
                    intentionAnt.remove(antToRemove);
                }

                intendedPath.removeAntUserFromPath(firstNode);
                if (intendedPath.getPath().isEmpty()) {
                    state = delegateMasState.initialState;
                    payload = Optional.absent();
                    chargingStatus = ChargingStatus.Idle;
                    return;
                }
                intendedPath.setMerit(
                    determineBenefitsPath(intendedPath.getPath(), intendedPath.getOccupationPercentage(), false, timeLapse));
                break;
            }
        }

    }

    /**
     * Removes the first node of the path, as well as the intention ant related to that node.
     */
    private void removeFirstNodeOfPath() {
        List<AntUser> path = intendedPath.getPath();
        if (path.size() < 1) {
            System.err.println("The path is empty, could not remove first node.");
            return;
        }

        AntUser antUser = path.get(0);
        // Remove the first node of the path.
        intendedPath.removeFirstNode();

        // Find the related intention ant and remove it from the list of intention ants.
        List<IntentionAnt> antsToRemove = new ArrayList<>();
        intentionAnt.keySet().stream()
            .filter(o -> o.getSecondaryAgent() == antUser)
            .forEach(o -> antsToRemove.add(o));
        antsToRemove.forEach(o -> intentionAnt.remove(o));
    }

    private ReservationPath getBestIntentionPath(TimeLapse timeLapse) {
        ReservationPath bestPath = new ReservationPath();

        for (ExplorationAnt ant : explorationAnts.keySet()) {
            for (List<AntUser> path : ant.getPaths()) {
                double chargingPointOccupation = ant.getChargingPointOccupations().get(this.getClass());
                double merit = determineBenefitsPath(path, chargingPointOccupation, true, timeLapse);

                if (merit > bestPath.getMerit()) {
                    bestPath = new ReservationPath(path, merit, chargingPointOccupation);
                }
            }
        }
        return bestPath;
    }

    /**
     * Determines the merit for a path. Paths with higher merit are more beneficial to drones than paths with low merit.
     * Used heuristics (in respective order):
     *  - battery life
     *  - order urgency
     *  - travel distance
     *  - charging point occupation
     * @param path The path to be evaluated.
     * @param occupationPercentage the occupation of the chargers for this specific drone type.
     * @param checkReservation Checks if the order is not reserved yet for merit calculations.
     * @param timeLapse The time at which this is calculated.
     * @return The merit associated with the specific path.
     */
    private double determineBenefitsPath(List<AntUser> path, double occupationPercentage, boolean checkReservation, TimeLapse timeLapse) {
        double merit = 0;
        RoadModel rm = getRoadModel();
        EnergyModel em = getEnergyModel();


        Point startLocation = rm.getPosition(this);
        double startBatteryLevel = this.battery.getBatteryLevel();
        long startTime = timeLapse.getTime();


        for (AntUser destination : path) {
            if (destination instanceof ChargingPoint) {
                merit += determineChargeBenefits(occupationPercentage, startBatteryLevel == this.battery.getMaxCapacity());
                continue;
            }

            Order order = (Order) destination;
            double neededCapacity = order.getNeededCapacity();

            // TODO move this check higher up to not even consider paths which satisfy these conditions (performance improvement)
            if (neededCapacity > this.getDTO().getCapacity()) {
                return Double.NEGATIVE_INFINITY;
            }
            if (checkReservation) {
                if (order.isReserved()) {
                    return Double.NEGATIVE_INFINITY;
                }
            }

            // Determine the total battery capacity that will be used for this specific order if it is chosen.
            // If the usage exceeds the current battery capacity, return -1 to make sure this order is not chosen.
            // Otherwise, choose orders in favour of using as little of the remaining battery capacity as possible.
            double distancePickup = rm.getDistanceOfPath(rm.getShortestPathTo(startLocation, order.getPickupLocation())).doubleValue(SI.METER);
            double distanceDeliver = rm.getDistanceOfPath(rm.getShortestPathTo(order.getPickupLocation(), order.getDeliveryLocation())).doubleValue(SI.METER);
            double distanceCharge = rm.getDistanceOfPath(rm.getShortestPathTo(order.getDeliveryLocation(), em.getChargingPoint().getLocation())).doubleValue(SI.METER);
            double neededBatteryLevelWithCharge = BatteryCalculations.calculateNecessaryBatteryLevel(
                this, distancePickup, distanceDeliver, distanceCharge, neededCapacity);

            if (neededBatteryLevelWithCharge > startBatteryLevel) {
                return Double.NEGATIVE_INFINITY;
            }

            double neededBatteryLevelWithoutCharge =
                BatteryCalculations.calculateNecessaryBatteryLevel(this, distancePickup, distanceDeliver, neededCapacity);
            merit += (1 - (neededBatteryLevelWithoutCharge / startBatteryLevel)) * 100;


            // Favour the orders which are closer to their specific deadline.
            double timeLeft = order.getDeliveryTimeWindow().end() - startTime;
            double totalTimeOrder = order.getDeliveryTimeWindow().end() - order.getOrderAnnounceTime();
            double percentageTimeLeft = timeLeft / totalTimeOrder;
            // Increase the percentage if the order is already over due.
            double meritPercentageTime = order.getDeliveryTimeWindow().end() <= startTime
                ? 1.5 : (1 - percentageTimeLeft);

            merit += meritPercentageTime * 200;

            // Favour the orders which use up least of the drone's current travel capacity.
            double travelDistance = distancePickup + distanceDeliver + distanceCharge;
            merit += (1 - (travelDistance / getTravelCapacity())) * 100;


            startLocation = order.getDeliveryLocation();
            startBatteryLevel -= neededBatteryLevelWithoutCharge;
            startTime -= order.getPickupDuration()
                + order.getDeliveryDuration()
                + (distancePickup / getSpeed(0) * 1000)
                + (distanceDeliver / getSpeed(neededCapacity) * 1000);
        }
        return merit / path.size();
    }

    private double determineChargeBenefits(double chargingPointOccupation, boolean batteryFull) {
        return batteryFull || chargingPointOccupation == 1 ?
            Double.NEGATIVE_INFINITY : 100 * (1 - chargingPointOccupation);
    }

    private boolean reconsiderAction(ReservationPath intention, TimeLapse timeLapse) {
        PDPModel pm = getPDPModel();

        if (chargingStatus == ChargingStatus.Charging) {
            return false;
        } else if (payload.isPresent() && pm.getParcelState(payload.get()) != PDPModel.ParcelState.AVAILABLE) {
            return false;
        }

        intentionAnt.clear();
        payload = Optional.absent();
        chargingStatus = ChargingStatus.Idle;
        intendedPath = new ReservationPath(intention.getPath(), intention.getMerit(), intention.getOccupationPercentage());
        spawnIntentionAnts(intention.getPath());
        explorationAnts.clear();

        return true;
    }

    private void spawnIntentionAnts(List<AntUser> destinations) {
        // Spawn an intention ant for every node on the path
        for (AntUser destination : destinations) {
            IntentionAnt ant;
            if (destination instanceof Order) {
                ant = new IntentionAnt(this, destination);
            } else {
                // TODO adjust time frame with calculated values from exploration ants
                ant = new ChargeIntentionAnt(this, destination, 0, Long.MAX_VALUE);
            }
            intentionAnt.put(ant, false);
            destination.receiveIntentionAnt(ant);
        }
    }

    /**
     * Gets the maximum distance a drone could travel if flying at max speed.
     * @return The calculated distance.
     */
    private double getTravelCapacity() {
        return SPEED_RANGE.getSpeed(1) * battery.getMaxCapacity();
    }

    private void spawnExplorationAnts(boolean sendToChargingPoint) {
        for (Parcel parcel : getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE)) {
            Order order = (Order) parcel;
            ExplorationAnt explorationAnt = new ExplorationAnt(this, ExplorationAnt.AntDestination.Order);
            explorationAnts.put(explorationAnt, false);
            order.receiveExplorationAnt(explorationAnt);
        }

        if (sendToChargingPoint) {
            ExplorationAnt explorationAnt = new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint);
            explorationAnts.put(explorationAnt, false);
            getEnergyModel().getChargingPoint().receiveExplorationAnt(explorationAnt);
        }
    }

    private void handlePickupAndDelivery(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        if (!payload.isPresent()) {
            // Only do pdp if a payload has been specified
            return;
        }

        if (pdp.getContents(this).isEmpty()) {
            moveToStore(rm, pdp, timeLapse);
        } else {
            moveToCustomer(rm, pdp, timeLapse);
        }
    }

    private void moveToStore(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        rm.moveTo(this, payload.get().getPickupLocation(), timeLapse);

        // If the drone has arrived at the store, pickup the secondaryAgent.
        if (rm.getPosition(this) == payload.get().getPickupLocation()) {
            try {
                pdp.pickup(this, payload.get(), timeLapse);
            } catch(IllegalArgumentException e){
                System.err.println(e.toString());
                payload = Optional.absent();
            }
        }
    }

    private void moveToCustomer(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        Order order = (Order) payload.get();
        rm.moveTo(this, order.getDeliveryLocation(), timeLapse);

        // If the drone arrived at the customer, deliver the package.
        if (rm.getPosition(this) == order.getDeliveryLocation()) {
            new Thread(new RemoveCustomer(rm, pdp, order.getCustomer())).start();
            pdp.deliver(this, order, timeLapse);


            // Advance to next node in path
            advanceInPath(timeLapse);
        }
    }

    private void moveToChargingPoint(RoadModel rm, EnergyModel em, TimeLapse timeLapse) {
        final ChargingPoint chargingPoint = em.getChargingPoint();

        if (!rm.getPosition(this).equals(chargingPoint.getLocation())) {
            rm.moveTo(this, chargingPoint.getLocation(), timeLapse);
        } else if (chargingPoint.dronePresent(this, true)) {
            // Only charge if there is a charger free
            try {
                chargingPoint.chargeDrone(this, timeLapse);
                chargingStatus = ChargingStatus.Charging;
            } catch (UnpermittedChargeException e) {
                // TODO keep retrying or just go back to drawing board? (opted for drawing board for now)
                advanceInPath(timeLapse);
            }
        } else {
            // The drone was not reserved in the charging station, advance in the path
            advanceInPath(timeLapse);
        }
    }

    private void advanceInPath(TimeLapse timeLapse) {
        // Remove the last finished node from the reserved path (and its intention ant)
        removeFirstNodeOfPath();

        List<AntUser> path = intendedPath.getPath();
        if (path.isEmpty()) {
            // No more nodes are left in the path, return to the initial state of delegate MAS
            payload = Optional.absent();
            chargingStatus = ChargingStatus.Idle;
            state = delegateMasState.initialState;
        } else {
            intendedPath.setMerit(determineBenefitsPath(path, intendedPath.getOccupationPercentage(), true, timeLapse));
            // 'Move' to the next node in the path
            AntUser nextNode = path.get(0);
            if (nextNode instanceof ChargingPoint) {
                payload = Optional.absent();
                chargingStatus = ChargingStatus.MoveToCharger;
            } else if (nextNode instanceof Order) {
                payload = Optional.of((Order) nextNode);
                chargingStatus = ChargingStatus.Idle;
            }
        }
    }

    public void stopCharging(TimeLapse timeLapse) {
        advanceInPath(timeLapse);
    }

    public void receiveExplorationAnt(ExplorationAnt ant) {
        // Note: It may happen that some ants which have been sent out are not needed anymore by the drone.
        //       In that case, the ant is dropped.
        try {
            explorationAnts.replace(ant, true);
        } catch (Exception e) {}
    }

    public void receiveIntentionAnt(IntentionAnt ant) {
        // Note: It may happen that some ants which have been sent out are not needed anymore by the drone.
        //       In that case, the ant is dropped.
        try {
            intentionAnt.replace(ant, true);
        } catch (Exception e) {}
    }

    public abstract String getDroneString();

    /**
     * Returns the interval of the battery level of the drone.
     *  - 0 indicates the drone is charging
     *  - 1 indicates the drone is 100-80% charged
     *  - 2 indicates the drone is 80-60% charged
     *  - 3 indicates the drone is 60-40% charged
     *  - 4 indicates the drone is 40-20% charged
     *  - 5 indicates the drone is 20-0% charged
     * @return the interval value as mentioned above
     */
    public int getChargingStatus() {
        if (chargingStatus == ChargingStatus.Charging) return 0;
        double chargeStatus = battery.getBatteryLevel() / battery.getMaxCapacity();
        if (chargeStatus >= 0.80) return 1;
        if (chargeStatus >= 0.60) return 2;
        if (chargeStatus >= 0.40) return 3;
        if (chargeStatus >= 0.20) return 4;
        return 5;
    }

    private class RemoveCustomer implements Runnable {
        RoadModel rm;
        PDPModel pdp;
        RoadUser customer;
        Stopwatch stopwatch;

        RemoveCustomer(RoadModel rm, PDPModel pdp, RoadUser customer) {
            this.rm = rm;
            this.pdp = pdp;
            this.customer = customer;
            this.stopwatch = Stopwatch.createStarted();

        }

        @Override
        public void run() {
            while (pdp.getVehicleState(Drone.this) == PDPModel.VehicleState.DELIVERING) {
                // Timeout of 10 seconds in order to kill thread if necessary.
                if (stopwatch.elapsed(TimeUnit.SECONDS) > 10) {
                    return;
                }
            }
            if (rm.containsObject(customer)) {
                rm.removeObject(customer);
            }
        }
    }

    public String toString() {
        return getDroneString();
    }

    @Override
    protected void tickImpl(@Nonnull TimeLapse timeLapse) {
        final RoadModel rm = getRoadModel();
        final PDPModel pdp = getPDPModel();
        final EnergyModel em = getEnergyModel();

        if (!timeLapse.hasTimeLeft()) {
            return;
        }

        // TODO Only do delegate mas every second? Different timing? Maybe remove this?
        // NOTE: the execution of this method is a bit spread out according to the ID of the drone
        if ((timeLapse.getTime() - (250 * (ID%4))) % 1000 == 0) {
            delegateMAS(timeLapse);
        }

        if (chargingStatus == ChargingStatus.MoveToCharger) {
            moveToChargingPoint(rm, em, timeLapse);
        } else {
            handlePickupAndDelivery(rm, pdp, timeLapse);
        }
    }

    @Override
    public void afterTick(TimeLapse time) {}


    // TODO: explanation
    private enum delegateMasState {
        initialState,
        spawnExplorationAnts,
        intentionAntReturned,
        explorationAntsReturned,
        continueReservation
    }

    private enum ChargingStatus {
        Idle,
        MoveToCharger,
        Charging
    }

}
