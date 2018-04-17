import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.examples.core.taxi.TaxiRenderer;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;


public class DroneRenderer extends CanvasRenderer.AbstractCanvasRenderer {
//    final PDPModel pdpModel; TODO change this model later on
    final PlaneRoadModel pdpModel;
    final String map;

    public DroneRenderer(PlaneRoadModel p, String m) {
        pdpModel = p;
        map = m;
    }

    static DroneRenderer.Builder builder() { return new Builder(); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {}

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        Image mapImage = new Image(gc.getDevice(), map);
        gc.drawImage(mapImage, 0, 0); // TODO check these pixels
    }

    @Override
    public Optional<ViewRect> getViewRect() {
        return null;
    }

    final static class Builder extends
            ModelBuilder.AbstractModelBuilder<DroneRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;

        Builder() {
            setDependencies(PlaneRoadModel.class);
        }

        @Override
        public DroneRenderer build(DependencyProvider dependencyProvider) {
            final PlaneRoadModel pm = dependencyProvider.get(PlaneRoadModel.class);
            return new DroneRenderer(null, "/resources/leuven.png");
        }
    }
}
