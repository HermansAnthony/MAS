package pdp;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import energy.EnergyDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import util.Range;


public class DroneLW  extends Drone {

    public DroneLW(Range speedRange, int capacity, int batteryLevel, Point chargingLocation) {
        super(VehicleDTO.builder()
            .capacity(capacity)
            .startPosition(chargingLocation)
            .speed(speedRange.getSpeed(1))
            .build(),
            new EnergyDTO(batteryLevel), speedRange);
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {}

    @Override
    public String getDroneString() {
        return "LW_ID" + this.ID;
    }

}
