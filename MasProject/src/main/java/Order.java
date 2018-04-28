import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;

public class Order extends Parcel {

    private Customer customer;

    public Order(ParcelDTO parcelDto, Customer cust) {
        super(parcelDto);
        customer = cust;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
}