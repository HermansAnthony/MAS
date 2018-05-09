package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.ant.Ant;
import com.github.rinde.rinsim.core.model.ant.ExplorationAnt;
import com.github.rinde.rinsim.core.model.ant.IntentionAnt;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

public class Order extends Parcel implements TickListener {

    private Customer customer;
    private List<Ant> temporaryAnts;
    private Optional<Vehicle> reserver;

    public Order(ParcelDTO parcelDto, Customer _customer) {
        super(parcelDto);
        customer = _customer;
        temporaryAnts = new ArrayList<>();
        reserver = Optional.absent();
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    public void sendAnt(ExplorationAnt explorationAnt) {
        explorationAnt.setParcelInformation(this);
        synchronized(temporaryAnts) {
            temporaryAnts.add(explorationAnt);
        }
    }

    synchronized public void sendAnt(IntentionAnt intentionAnt) {
        if (!this.isReserved()) {
            this.reserve(intentionAnt.getPrimaryAgent());
            intentionAnt.reservationApproved = true;
        } else {
            intentionAnt.reservationApproved = false;
        }

        synchronized(temporaryAnts) {
            temporaryAnts.add(intentionAnt);
        }
    }

    synchronized public void reserve(Vehicle vehicle) {
        reserver = Optional.of(vehicle);
    }

    synchronized public boolean isReserved() {
        return reserver.isPresent();
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        // TODO timeout of reservation
        // Send out all the temporary ants to their respective primary agents.
        synchronized (temporaryAnts) {
            for (Ant ant : temporaryAnts) {
                ant.returnToPrimaryAgent();
            }
            temporaryAnts.clear();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

}