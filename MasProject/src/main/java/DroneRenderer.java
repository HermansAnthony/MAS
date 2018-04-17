import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.examples.core.taxi.TaxiRenderer;

public class DroneRenderer {
    public DroneRenderer() {}

    static DroneRenderer.Builder builder() {
        return null;
    }
    // This builder is using Google's AutoValue for creating a value object, see
    // https://github.com/google/auto/tree/master/value for more information on
    // how to make it work in your project. You can also manually implement the
    // equivalent code by making the class concrete and giving it a 'language'
    // field and a constructor parameter to set it. Don't forget to implement
    // equals() and hashCode().
    abstract static class Builder extends
            ModelBuilder.AbstractModelBuilder<TaxiRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;

        Builder() {
            setDependencies(RoadModel.class, PDPModel.class);
        }

        public DroneRenderer build() {
            return new DroneRenderer();
        }
    }
}
