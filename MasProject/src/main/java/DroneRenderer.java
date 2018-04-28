import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import java.util.Map;


public class DroneRenderer extends CanvasRenderer.AbstractCanvasRenderer {
    private final PlaneRoadModel rm;
    private final PDPModel pdpModel;
    private final int offsetY = 15;
    private final int offsetX = -5;

    public DroneRenderer(PlaneRoadModel r, PDPModel p) {
        rm = r;
        pdpModel = p;
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
                    // TODO put this code in a separate function to avoid code duplication
                    Drone d = (Drone) entry.getKey();
                    final PDPModel.VehicleState vs = pdpModel.getVehicleState(d);
                    String text = determineStatus(d, vs);
                    gc.fillOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));

                    gc.drawOval(
                            xpx - vp.scale(r), ypx - vp.scale(r),
                            2 * vp.scale(r), 2 * vp.scale(r));
                    if (text != null) {
                        final org.eclipse.swt.graphics.Point extent = gc.textExtent(text);
                        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
                        gc.fillRoundRectangle(xpx - extent.x / 2, ypx + offsetY - extent.y / 2,
                                extent.x + 2, extent.y + 2, 5,
                                5);
                        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

                        gc.drawText(text, xpx - extent.x / 2 + 1, ypx + offsetY - extent.y / 2 + 1,
                                true);
                    }
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
            setDependencies(PlaneRoadModel.class, PDPModel.class);
        }

        @Override
        public DroneRenderer build(DependencyProvider dependencyProvider) {
            System.out.println("Drone renderer build");
            final PlaneRoadModel rm = dependencyProvider.get(PlaneRoadModel.class);
            final PDPModel pdpModel = dependencyProvider.get(PDPModel.class);
            return new DroneRenderer(rm, pdpModel);
        }
    }

    // Determine the status of the drone (pickup, delivering and carrying a load)
    private String determineStatus(Drone d, PDPModel.VehicleState vs){
        String text = null;
        final int size = (int) pdpModel.getContentsSize(d);
        if (vs == PDPModel.VehicleState.DELIVERING) {
            text = "Delivering order";
        } else if (vs == PDPModel.VehicleState.PICKING_UP) {
            text = "Pickup order";
        } else if (size > 0) {
            text = "Load: " + Integer.toString(size) + " grams";
        }
        return text;
    }
}
