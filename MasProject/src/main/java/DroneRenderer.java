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
import org.eclipse.swt.SWT;
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

    static DroneRenderer.Builder builder(String map) { return new Builder(map); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {}

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        System.out.println("Rendering dynamic");
        System.out.println(System.getProperty("user.dir"));
        System.out.println("Map " + map);
        Image mapImage = new Image(gc.getDevice(), map);
        gc.drawImage(mapImage, 0, 0); // TODO check these pixels
        System.out.println("DOne Rendering dynamic");
    }

    final static class Builder extends
            ModelBuilder.AbstractModelBuilder<DroneRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;
        private final String map;

        Builder(String m) {
            setDependencies(PlaneRoadModel.class);
            map = m;
        }

        @Override
        public DroneRenderer build(DependencyProvider dependencyProvider) {
            System.out.println("Drone renderer build");
            final PlaneRoadModel pm = dependencyProvider.get(PlaneRoadModel.class);
            return new DroneRenderer(null, map);
        }
    }
}
