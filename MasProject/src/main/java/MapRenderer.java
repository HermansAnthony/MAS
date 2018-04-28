import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

public class MapRenderer extends CanvasRenderer.AbstractCanvasRenderer {

    final PlaneRoadModel pdpModel;
    final String map;

    public MapRenderer(PlaneRoadModel p, String m) {
        pdpModel = p;
        map = m;
    }

    static MapRenderer.Builder builder(String map) { return new MapRenderer.Builder(map); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
        Image mapImage = new Image(gc.getDevice(), map);
        gc.drawImage(mapImage, 0, 0);
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
    }


    final static class Builder extends
            ModelBuilder.AbstractModelBuilder<MapRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;
        private final String map;

        Builder(String m) {
            setDependencies(PlaneRoadModel.class);
            map = m;
        }

        @Override
        public MapRenderer build(DependencyProvider dependencyProvider) {
            System.out.println("Drone renderer build");
            final PlaneRoadModel pm = dependencyProvider.get(PlaneRoadModel.class);
            return new MapRenderer(null, map);
        }
    }

}
