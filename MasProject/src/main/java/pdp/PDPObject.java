package pdp;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;

/**
 * TODO ranzige copy paste
 */
public interface PDPObject extends RoadUser {

    /**
     * @return The type of the PDPObject.
     */
    DronePDPType getType();

    /**
     * Is called when object is registered in {@link PDPModel}.
     * @param model A reference to the model.
     */
    void initPDPObject(PDPModel model);

}