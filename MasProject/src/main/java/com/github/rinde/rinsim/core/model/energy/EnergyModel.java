package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;

public abstract class EnergyModel extends AbstractModel<RoadUser>
        implements TickListener {
}
