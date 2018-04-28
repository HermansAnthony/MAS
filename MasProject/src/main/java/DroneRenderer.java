import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.examples.core.taxi.TaxiRenderer;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;


public class DroneRenderer extends CanvasRenderer.AbstractCanvasRenderer {
//    final PDPModel pdpModel; TODO change this model later on
    final PlaneRoadModel pdpModel;

    public DroneRenderer(PlaneRoadModel p) {
        pdpModel = p;
    }

    static DroneRenderer.Builder builder() { return new Builder(); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        // TODO render drones here
    }

    @Override
    public Optional<ViewRect> getViewRect() {
        return Optional.of(new ViewRect(new Point(0, 0), new Point(1120, 956)));
    }

    final static class Builder extends
            ModelBuilder.AbstractModelBuilder<DroneRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;

        Builder() {
            setDependencies(PlaneRoadModel.class);
        }

        @Override
        public DroneRenderer build(DependencyProvider dependencyProvider) {
            System.out.println("Drone renderer build");
            final PlaneRoadModel pm = dependencyProvider.get(PlaneRoadModel.class);
            return new DroneRenderer(null);
        }
    }
}