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
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;
import util.TimeMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Order extends Parcel implements AntReceiver, TickListener {

    private List<Ant> temporaryAnts;
    private Optional<Vehicle> reserver;
    private int timeoutTimer;

    private static int TIMEOUT_RESERVE = 20; // TODO fine grain this value
    private RoadUser customer;

    // Indicates if order has been delivered
    private TimeMonitor timeMonitor;
    private boolean delivered;

    public Order(ParcelDTO parcelDto, Customer customer) {
        super(parcelDto);
        temporaryAnts = new ArrayList<>();
        reserver = Optional.absent();
        timeoutTimer = TIMEOUT_RESERVE;
        this.customer = customer;
        this.delivered = false;
        this.timeMonitor = TimeMonitor.getInstance();
    }


    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    synchronized private void reserve(Vehicle vehicle) {
        reserver = Optional.of(vehicle);
    }

    synchronized public boolean isReserved() {
        return reserver.isPresent();
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        // Send out all the temporary ants to their respective primary agents.
        synchronized (temporaryAnts) {
            for (Ant ant : temporaryAnts) {
                ant.returnToPrimaryAgent();
            }
            temporaryAnts.clear();
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
        // Fetch all the parcels which are delivered and verify if this order is in that list
        // If so write the announcement time and delivery time to a file
        // which can be aggregated later on by the experiment postprocessor
        Collection<Parcel> parcels = getPDPModel().getParcels(PDPModel.ParcelState.DELIVERED);
        if (!delivered) {
            if (parcels.stream().anyMatch(o -> o.getDto().equals(this.getDto()))) {
                delivered = true;
                long announcementTime = this.getDto().getOrderAnnounceTime();
                long deliveryTime = timeLapse.getTime();
                // The order was delivered too late
                if (deliveryTime > this.getDto().getDeliveryTimeWindow().end()) {
                    timeMonitor.writeToFile(this.getDto().getDeliveryTimeWindow().end(), deliveryTime, false);
                    return;
                }
                timeMonitor.writeToFile(announcementTime, deliveryTime, true);
            }
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
            explorationAnt.setSecondaryAgent(this);
            synchronized(temporaryAnts) {
                temporaryAnts.add(explorationAnt);
            }
        }
        else if (ant instanceof IntentionAnt) {
            IntentionAnt intentionAnt = (IntentionAnt) ant;
            if (!this.isReserved()) {
                this.reserve(intentionAnt.getPrimaryAgent());
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
        }

        synchronized(temporaryAnts) {
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
}