import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import org.eclipse.swt.graphics.GC;

import java.util.Map;


public class DroneRenderer extends CanvasRenderer.AbstractCanvasRenderer {
    private final PlaneRoadModel rm;



    public DroneRenderer(PlaneRoadModel r) {
        rm = r;
    }

    static DroneRenderer.Builder builder() { return new Builder(); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        final int r = 1;
        final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
        synchronized (objects) {
            for (final Map.Entry<RoadUser, Point> entry : objects.entrySet()) {
                final Point p = entry.getValue();
                final int xpx = vp.toCoordX(p.x);
                final int ypx = vp.toCoordY(p.y);

                if (entry.getKey() instanceof DroneLW) {
                    gc.fillOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));

                    gc.drawOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));
                } else if (entry.getKey() instanceof DroneHW) {
                    gc.fillOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));

                    gc.drawOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));
                }


            }
        }
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
            final PlaneRoadModel rm = dependencyProvider.get(PlaneRoadModel.class);
            return new DroneRenderer(rm);
        }
    }
}
