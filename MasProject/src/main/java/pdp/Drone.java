package pdp;

import ant.Ant;
import ant.AntReceiver;
import ant.ExplorationAnt;
import ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import energy.ChargingPoint;
import energy.EnergyDTO;
import energy.EnergyModel;
import energy.EnergyUser;
import util.BatteryCalculations;
import util.Monitor;
import util.Range;
import util.Tuple;

import javax.measure.unit.SI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class Drone extends Vehicle implements EnergyUser, AntReceiver {
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
    /* TODO: timeout for ant receiving -> imagine ant being sent out just at the end of the lifespan of a certain order
       TODO:                           -> the order gets delivered and the intention ant never returns
       TODO:                           -> this may break the system since it keeps waiting for the end to return.

       NOTE: not entirely sure about the above remark, maybe the order lives long enough for that situation to possibly happen.

       TODO: (possible) alternative    -> filter out orders which are currently in transit
     */

    private delegateMasState state;
    private Map<ExplorationAnt, Boolean> explorationAnts;
    private Map<IntentionAnt, Boolean> intentionAnt; // Just one intention ant

    // Logger for the delegate mas actions
    private Monitor monitor;


    protected Drone(VehicleDTO _dto, EnergyDTO _battery, Range speedRange) {
        super(_dto);
        ID = nextID++;
        SPEED_RANGE = speedRange;
        payload = Optional.absent();

        energyModel = Optional.absent();
        chargingStatus = ChargingStatus.Idle;
        battery = _battery;

        state = delegateMasState.initialState;
        explorationAnts = new HashMap<>();
        intentionAnt = new HashMap<>();

        monitor = new Monitor(this.getDroneString());
    }

    public int getID(){return this.ID;}


    @Override
    public void initEnergyUser(EnergyModel model) {
        energyModel = Optional.of(model);
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
        return SPEED_RANGE.getSpeed(capacity / getCapacity());
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    @Override
    protected void tickImpl(TimeLapse timeLapse) {
        final RoadModel rm = getRoadModel();
        final PDPModel pdp = getPDPModel();
        final EnergyModel em = getEnergyModel();

        if (!timeLapse.hasTimeLeft()) {
            return;
        }

        delegateMAS(timeLapse);

        if (chargingStatus == ChargingStatus.MoveToCharger) {
            moveToChargingPoint(rm, em, timeLapse);
        } else {
            handlePickupAndDelivery(rm, pdp, timeLapse);
        }
    }

    private void delegateMAS(TimeLapse timeLapse) {
        switch (state){
            case initialState: {
                // Initially, spawn exploration ants to get information about all possible orders.
                explorationAnts.clear();
                intentionAnt.clear();
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
                    Tuple<Order, Double> intention = getBestIntention(timeLapse);
                    if (intention.first == null) {
                        state = delegateMasState.initialState;
                        break;
                    }
                    spawnIntentionAnt(intention.first, intention.second);
                    // Clear all the exploration ants since they are outdated at this point
                    explorationAnts.clear();
                    state = delegateMasState.intentionAntReturned;
                }
                break;
            }
            case intentionAntReturned: {
                // If the intentionAnt has been cleared, the order has been delivered,
                // and we need to start looking for a new one.
                if (intentionAnt.isEmpty()) {
                    state = delegateMasState.initialState;
                    break;
                }

                Map.Entry<IntentionAnt, Boolean> entry = intentionAnt.entrySet().iterator().next();
                // Ant has returned and has an approved reservation
                if (entry.getValue() && entry.getKey().reservationApproved) {
                    payload = Optional.of(entry.getKey().reservedOrder);

                    // Intention ant needs to be sent out again to hold the reservation
                    state = delegateMasState.continueReservation;
                } else if (entry.getValue() && !entry.getKey().reservationApproved) {
                    // The order has been reserved for another package, resend exploration ants and find a new order.
                    state = delegateMasState.initialState;
                }
                break;
            }
            case continueReservation: {
                Map.Entry<IntentionAnt, Boolean> entry = intentionAnt.entrySet().iterator().next();

                if (!payload.isPresent()) {
                    // The order has been delivered, go back to the initial state
                    state = delegateMasState.initialState;
                    break;
                }

                if (entry.getValue()) {
                    // If the intention ant has returned, resend it
                    entry.setValue(false); // Mark the ant as gone
                    entry.getKey().reservationApproved = false;
                    entry.getKey().reservedOrder.receiveAnt(entry.getKey());
                }

                state = delegateMasState.spawnExplorationAnts;
                break;
            }
            case spawnExplorationAnts: {
                if (!payload.isPresent()) {
                    // The order has been delivered, go back to the initial state
                    state = delegateMasState.initialState;
                    break;
                }

                if (explorationAnts.isEmpty()) {
                    // Spawn new exploration ants for possible reconsiderations
                    spawnExplorationAnts(false);
                } else if (!explorationAnts.containsValue(false)) {
                    // Check for reconsiderations
                    Tuple<Order, Double> intention = getBestIntention(timeLapse);
                    double meritDifference = intention.second - intentionAnt.entrySet().iterator().next().getKey().merit;
                    if ((meritDifference > RECONSIDERATION_MERIT) && (intention.first != null)) {
                        if (reconsiderOrder(intention, timeLapse)) {
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

    private boolean reconsiderOrder(Tuple<Order,Double> intention, TimeLapse timeLapse) {
        PDPModel pm = getPDPModel();

        if (pm.getParcelState(payload.get()) != PDPModel.ParcelState.AVAILABLE) {
            return false;
        }

        intentionAnt.clear();
        payload = Optional.absent();
        spawnIntentionAnt(intention.first, intention.second);
        explorationAnts.clear();

        String description = " Reconsideration happened: New best merit: " + intention.second + ".\n";
        description += "Intention ant is sent to order (" + intention.first.getOrderDescription() +").\n";
        monitor.writeToFile(timeLapse.getStartTime(), description);
        return true;
    }

    private void spawnIntentionAnt(Order order, double merit) {
        IntentionAnt ant = new IntentionAnt(this, order, merit);
        intentionAnt.put(ant, false);
        order.receiveAnt(ant);
    }

    private util.Tuple<Order,Double> getBestIntention(TimeLapse timeLapse) {
        Order bestOrder = null;
        double bestMerit = Double.NEGATIVE_INFINITY;

        for (ExplorationAnt ant : explorationAnts.keySet().stream()
            .filter(o -> o.destination == ExplorationAnt.AntDestination.Order).collect(Collectors.toList())) {
            Order order = (Order) ant.getSecondaryAgent();
            if (order.isReserved() || order.getNeededCapacity() > this.getCapacity())
                continue;


            double merit = determineBenefits(order, timeLapse);
            if (merit < 0) {
                // Order is impossible to be completed by the drone
                continue;
            }

            if (merit > bestMerit) {
                bestMerit = merit;
                bestOrder = order;
            }
        }

        // Log the intention ant and merit calculation in the log file
        if (bestOrder != null) {
            String description = " Best merit: " + bestMerit + ".\n";
            description += "Intention ant is sent to order (" + bestOrder.getOrderDescription() +").\n";
            monitor.writeToFile(timeLapse.getStartTime(), description);
        }

        return new Tuple(bestOrder, bestMerit);
    }

    private double determineChargeBenefits(double chargingPointOccupation) {
        return 400 * (1 - chargingPointOccupation);
    }

    /**
     * Returns the merit for a certain order. Orders with higher merit are more beneficial to drones than order with low merit.
     * Used heuristics (in respective order):
     *  - battery life
     *  - order urgency
     *  - charging point occupation
     *  - travel distance
     * @param order The order for which the merit is calculated.
     * @param timeLapse The time at which this is calculated.
     * @return The merit associated with the specific order.
     */
    private double determineBenefits(Order order, TimeLapse timeLapse) {
        double merit = 0;

        RoadModel rm = getRoadModel();
        EnergyModel em = getEnergyModel();


        // Determine the total battery capacity that will be used for this specific order if it is chosen.
        // If the usage exceeds the current battery capacity, return -1 to make sure this order is not chosen.
        // Otherwise, choose orders in favour of using as much of the remaining battery capacity as possible.
        double distancePickup = rm.getDistanceOfPath(rm.getShortestPathTo(rm.getPosition(this), order.getPickupLocation())).doubleValue(SI.METER);
        double distanceDeliver = rm.getDistanceOfPath(rm.getShortestPathTo(order.getPickupLocation(), order.getDeliveryLocation())).doubleValue(SI.METER);
        double distanceCharge = rm.getDistanceOfPath(rm.getShortestPathTo(order.getDeliveryLocation(), em.getChargingPoint().getLocation())).doubleValue(SI.METER);
        double neededBatteryLevel = BatteryCalculations.calculateNecessaryBatteryLevel(
            this, distancePickup, distanceDeliver, distanceCharge, order.getNeededCapacity());
        if (neededBatteryLevel > this.battery.getBatteryLevel()) {
            return -1;
        }

        merit += (neededBatteryLevel / this.battery.getBatteryLevel()) * 100;


        // Favour the orders which are closer to their specific deadline.
        double timeLeft = order.getDeliveryTimeWindow().end() - timeLapse.getTime();
        double totalTimeOrder = order.getDeliveryTimeWindow().end() - order.getOrderAnnounceTime();
        double percentageTimeLeft = timeLeft / totalTimeOrder;
        double meritPercentageTime = order.getDeliveryTimeWindow().end() <= timeLapse.getTime()
                ? 1.5 : (1 - percentageTimeLeft);

        merit += meritPercentageTime * 200;

        // TODO same as above, for chargingPoint calculations
        merit += em.getChargingPoint().chargersOccupied(this) ? 50 : 0;


        // Favour the orders which use up most of the drone's current travel capacity.
        double travelDistance = distancePickup + distanceDeliver + distanceCharge;
        merit += ( 1 - (travelDistance / getTravelCapacity())) * 100;

        return merit;
    }

    /**
     * Gets the maximum distance a drone could travel if flying at max speed.
     * @return The calculated distance.
     */
    protected double getTravelCapacity() {
        return SPEED_RANGE.getSpeed(1) * battery.getMaxCapacity();
    }

    private void spawnExplorationAnts(boolean sendToChargingPoint) {
        for (Parcel parcel : getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE)) {
            Order order = (Order) parcel;
            ExplorationAnt explorationAnt = new ExplorationAnt(this, ExplorationAnt.AntDestination.Order);
            explorationAnts.put(explorationAnt, false);
            order.receiveAnt(explorationAnt);
        }

        if (sendToChargingPoint) {
            ExplorationAnt explorationAnt = new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint);
            explorationAnts.put(explorationAnt, false);
            getEnergyModel().getChargingPoint().receiveAnt(explorationAnt);
        }
    }

    private void handlePickupAndDelivery(RoadModel rm, PDPModel pdp, TimeLapse timeLapse) {
        if (!payload.isPresent()) {
            return;
        } else if (pdp.getContents(this).isEmpty()) {
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

            payload = Optional.absent();
            chargingStatus = ChargingStatus.MoveToCharger;
        }
    }

    private void moveToChargingPoint(RoadModel rm, EnergyModel em, TimeLapse timeLapse) {
        final ChargingPoint chargingPoint = em.getChargingPoint();

        if (!rm.getPosition(this).equals(chargingPoint.getLocation())) {
            rm.moveTo(this, chargingPoint.getLocation(), timeLapse);
        } else if (!chargingPoint.dronePresent(this)) {
            // Only charge if there is a charger free
            if (!chargingPoint.chargersOccupied(this)) {
                chargingPoint.reserveCharger(this);
                chargingPoint.chargeDrone(this);
                chargingStatus = ChargingStatus.Charging;
            }
        }
    }


    @Override
    public void afterTick(TimeLapse time) {}


    public void stopCharging() {
        chargingStatus = ChargingStatus.Idle;
    }

    public EnergyModel getEnergyModel() {
        return energyModel.get();
    }

    public void receiveAnt(Ant ant) {
        if (ant instanceof ExplorationAnt) {

            // Note: It may happen that some ants which have been sent out are not needed anymore by the drone.
            //       In that case, the ant is dropped.
            try {
                explorationAnts.replace((ExplorationAnt) ant, true);
            } catch (Exception e) {
                System.out.println("Exploration and dropped.");
            }
        } else if (ant instanceof IntentionAnt) {
            try {
                intentionAnt.replace((IntentionAnt) ant, true);
            } catch (Exception e) {
                System.out.println("Intention ant dropped.");
            }
        }
    }

    public abstract String getDroneString();


    // TODO find better way of dealing with removal of customers than using threads.
    private class RemoveCustomer implements Runnable {
        RoadModel rm;
        PDPModel pdp;
        RoadUser customer;
        Stopwatch stopwatch;

        RemoveCustomer(RoadModel _rm, PDPModel _pdp, RoadUser _customer) {
            rm = _rm;
            pdp = _pdp;
            customer = _customer;
            stopwatch = Stopwatch.createStarted();

        }

        @Override
        public void run() {
            while (pdp.getVehicleState(Drone.this) == PDPModel.VehicleState.DELIVERING) {
                // Timeout of 30 seconds in order to kill thread if necessary.
                if (stopwatch.elapsed(TimeUnit.SECONDS) > 10) {
                    return;
                }
            }
            if (rm.containsObject(customer)) {
                rm.removeObject(customer);
            }
        }
    }

    private enum delegateMasState {
//        TODO explanation
        initialState,
        spawnExplorationAnts,
        intentionAntReturned,
        explorationAntsReturned,
        continueReservation;

    }

    private enum ChargingStatus {
        Idle,
        MoveToCharger,
        Charging;
    }

}
