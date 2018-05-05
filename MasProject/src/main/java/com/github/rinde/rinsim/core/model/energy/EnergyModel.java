package com.github.rinde.rinsim.core.model.energy;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;

public abstract class EnergyModel extends Model.AbstractModel<RoadUser>
        implements TickListener {
}
