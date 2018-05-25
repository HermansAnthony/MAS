package pdp;

import ant.Ant;
import ant.AntReceiver;
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
import com.google.common.base.Optional;
import energy.EnergyModel;
import energy.EnergyUser;

import java.util.*;
import java.util.stream.Collectors;

public class Order extends Parcel implements AntReceiver, TickListener, EnergyUser {

    private static final int MAXIMUM_HOPCOUNT = 3;
    private Queue<Ant> temporaryAnts;
    // TODO split up Map for returning exploration ants
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

    synchronized private void reserve(Vehicle vehicle) {
        reserver = Optional.of(vehicle);
    }

    synchronized public boolean isReserved() {
        return reserver.isPresent();
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

    private String getOrderDescription(){
        // TODO move this to the description down below
        String result = "location: ";
        try {
            result += this.getRoadModel().getPosition(this);
        } catch (Exception e) {
            result += "in transit";
        }
        result += ", payload: " + this.getNeededCapacity() + " grams";
        return result;
    }

    private void resetTimeout() {
        timeoutTimer = TIMEOUT_RESERVE;
    }

    @Override
    public void receiveAnt(Ant ant) {
        if (ant instanceof ExplorationAnt) {
            ExplorationAnt explorationAnt = (ExplorationAnt) ant;

            if (explorationAnt.getPrimaryAgent() == this) {
                // The exploration ant was sent out by this order and has returned
                // Set the boolean flag to true for this particular ant
                followupAntsPresence.replace(explorationAnt, true);
            } else {
                explorationAnt.setSecondaryAgent(this);
                List<AntReceiver> travelledPath = explorationAnt.addHopTravelledPath(this);

                // The order is the destination for the ant
                // Send out more exploration ants if necessary
                int hopCount = explorationAnt.getHopCount();
                Collection<Order> ordersWithinDistance =
                    RoadModels.findObjectsWithinRadius(this.getDeliveryLocation(), roadModel, 1000, Order.class);

                if (hopCount == MAXIMUM_HOPCOUNT) {
                    // Send an exploration ant to the charging point
                    ExplorationAnt newAnt =
                        new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint, hopCount+1);
                    energyModel.getChargingPoint().receiveAnt(newAnt);

                    followupAnts.put(explorationAnt, new ArrayList<>(Arrays.asList(newAnt)));
                    followupAntsPresence.put(newAnt, false);
                } else {
                    List<ExplorationAnt> newAnts = new ArrayList<>();

                    for (Order order : ordersWithinDistance) {
                        // Make sure no loops or orders which are already reserved are considered
                        if (travelledPath.contains(order) || order.isReserved()) {
                            continue;
                        }

                        ExplorationAnt newAnt =
                            new ExplorationAnt(this, ExplorationAnt.AntDestination.Order, hopCount+1);
                        newAnts.add(newAnt);
                        order.receiveAnt(newAnt);
                    }
                    ExplorationAnt newAnt =
                        new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint, hopCount+1);
                    newAnts.add(newAnt);
                    energyModel.getChargingPoint().receiveAnt(newAnt);

                    followupAnts.put(explorationAnt, newAnts);
                    newAnts.forEach(o -> followupAntsPresence.put(o, false));
                }
            }
        }
        else if (ant instanceof IntentionAnt) {
            IntentionAnt intentionAnt = (IntentionAnt) ant;
            if (!this.isReserved()) {
                // Can do a cast to Drone here since intention ants only originate from drones.
                this.reserve((Drone) intentionAnt.getPrimaryAgent());
                intentionAnt.reservationApproved = true;
                resetTimeout();
            } else {
                if (reserver.get().equals(intentionAnt.getPrimaryAgent())) {
                    resetTimeout();
                    intentionAnt.reservationApproved = true;
                } else {
                    intentionAnt.reservationApproved = false;
                }
            }
            synchronized(temporaryAnts) {
                temporaryAnts.add(ant);
            }
        }
    }


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

                // TODO add way to get most recent occupation percentage of charging point (i.e. biggest hop count of path)
                for (ExplorationAnt ant : entry.getValue()) {
                    if (ant.getChargingPointOccupations() != null) {
                        originalAnt.setChargingPointOccupations(ant.getChargingPointOccupations());
                    }
                }
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
    public String getDescription() {
        return "Order - " + getOrderDescription();
    }

    public RoadUser getCustomer() {
        return customer;
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {
        this.energyModel = energyModel;
    }

    public String toString() {
        return "<Order: " + this.getNeededCapacity() + " grams>";
    }

}
