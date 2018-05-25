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
import util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class Order extends Parcel implements AntReceiver, TickListener, EnergyUser {

    private Queue<Ant> temporaryAnts;
    // TODO split up Map for returning exploration ants
    private Map<ExplorationAnt, List<Tuple<ExplorationAnt, Boolean>>> followupAnts;

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

    public String getOrderDescription(){
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
                followupAnts.values().stream()
                    .flatMap(List::stream)
                    .filter(o -> o.first == explorationAnt)
                    .forEach(o -> o.second = true);
            } else {
                explorationAnt.setSecondaryAgent(this);
                // The order is the destination for the ant
                // Send out more exploration ants if necessary
                int hopCount = explorationAnt.getHopCount();
                Collection<Order> ordersWithinDistance =
                    RoadModels.findObjectsWithinRadius(this.getDeliveryLocation(), roadModel, 1000, Order.class);
                // TODO keep track of current path of exploration ant -> filter parcels which have already been visited/considered

                // 1 because this order itself is still counted
//                if (hopCount == 3 || ordersWithinDistance.size() <= 1) {
                if (hopCount == 3 ||
                    ordersWithinDistance.isEmpty() ||
                    (ordersWithinDistance.size() == 1 && ordersWithinDistance.contains(this))) {
                    // Send an exploration ant to the charging point
                    ExplorationAnt newAnt =
                        new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint, hopCount+1);
                    energyModel.getChargingPoint().receiveAnt(newAnt);
                    followupAnts.put(explorationAnt, new ArrayList<>(Arrays.asList(new Tuple<>(newAnt, false))));
                } else {
                    List<Tuple<ExplorationAnt, Boolean>> newAnts = new ArrayList<>();

                    for (Order order : ordersWithinDistance) {
                        if (order == this) {
                            continue;
                        }

                        ExplorationAnt newAnt =
                            new ExplorationAnt(this, ExplorationAnt.AntDestination.Order, hopCount+1);
                        newAnts.add(new Tuple<>(newAnt, false));
                        order.receiveAnt(newAnt);
                    }
                    ExplorationAnt newAnt =
                        new ExplorationAnt(this, ExplorationAnt.AntDestination.ChargingPoint, hopCount+1);
                    newAnts.add(new Tuple<>(newAnt, false));
                    energyModel.getChargingPoint().receiveAnt(newAnt);

                    followupAnts.put(explorationAnt, newAnts);
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
        for (Map.Entry<ExplorationAnt,List<Tuple<ExplorationAnt, Boolean>>> entry : followupAnts.entrySet()) {
            // If all the sent out exploration ants have returned
            if (entry.getValue().stream().allMatch(o -> o.second)) {
                ExplorationAnt originalAnt = entry.getKey();

                originalAnt.addPathEntries(entry.getValue().stream()
                    .map(o -> o.first.getPaths())
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));

                // TODO add way to get most recent occupation percentage of charging point (i.e. biggest hop count of path)
                for (Tuple<ExplorationAnt, Boolean> tuple : entry.getValue()) {
                    if (tuple.first.getChargingPointOccupations() != null) {
                        originalAnt.setChargingPointOccupations(tuple.first.getChargingPointOccupations());
                    }
                }
                antsToReturn.add(originalAnt);
            }
        }

        for (ExplorationAnt ant : antsToReturn) {
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


}
