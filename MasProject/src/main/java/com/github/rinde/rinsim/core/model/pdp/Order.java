package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.model.ant.Ant;
import com.github.rinde.rinsim.core.model.ant.ExplorationAnt;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import java.util.ArrayList;
import java.util.List;

public class Order extends Parcel implements TickListener {

    private Customer customer;
    private List<Ant> temporaryAnts;

    public Order(ParcelDTO parcelDto, Customer cust) {
        super(parcelDto);
        customer = cust;
        temporaryAnts = new ArrayList<>();
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
    public void afterTick(TimeLapse timeLapse) {}
}