import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.energy.EnergyModel;
import com.github.rinde.rinsim.core.model.pdp.Drone;
import com.github.rinde.rinsim.core.model.pdp.DroneHW;
import com.github.rinde.rinsim.core.model.pdp.DroneLW;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.Map;


public class DroneRenderer extends CanvasRenderer.AbstractCanvasRenderer {
    private final PlaneRoadModel rm;
    private final PDPModel pdpModel;
    private final EnergyModel energyModel;
    private final int offsetY = 15;
    private final int offsetX = -5;

    public DroneRenderer(PlaneRoadModel r, PDPModel p, EnergyModel d) {
        rm = r;
        pdpModel = p;
        energyModel = d;
    }

    static DroneRenderer.Builder builder() { return new Builder(); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }


    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        final int r = 1;
        final Map<RoadUser, Point> objects = rm.getObjectsAndPositions();
//        System.out.println(energyModel); // TODO fix this dependency mess 
//        String status = energyModel.getStatus();
//        showChargingInfo(status);
        synchronized (objects) {
            for (final Map.Entry<RoadUser, Point> entry : objects.entrySet()) {
                final Point p = entry.getValue();
                final int xpx = vp.toCoordX(p.x);
                final int ypx = vp.toCoordY(p.y);

                if (entry.getKey() instanceof DroneLW || entry.getKey() instanceof DroneHW) {
                    renderDrone(entry.getKey(), gc, vp, xpx, ypx, r);
                }
            }
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

    // Show the information of the charging station in a separate window
    private void showChargingInfo(String status) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setBounds(10, 10, 500, 1000);
        shell.setText(status);
    }

    private void renderDrone(RoadUser user, GC gc, ViewPort vp, int xpx, int ypx, int r){
        // TODO render the drone ID on the UI!
        Drone d = (Drone) user;
        final PDPModel.VehicleState vs = pdpModel.getVehicleState(d);
        String text = determineStatus(d, vs);
        gc.fillOval(
                xpx - vp.scale(r), ypx - vp.scale(r),
                2 * vp.scale(r), 2 * vp.scale(r));

        gc.drawOval(
                xpx - vp.scale(r), ypx - vp.scale(r),
                2 * vp.scale(r), 2 * vp.scale(r));
        String droneInfo = Integer.toString(d.getID());
        final org.eclipse.swt.graphics.Point nameExtent = gc.textExtent(droneInfo);
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
        gc.fillRoundRectangle(xpx + offsetX - nameExtent.x / 2, ypx - 20 - nameExtent.y / 2,
                nameExtent.x + 2, nameExtent.y + 2, 5,
                5);
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        gc.drawText(droneInfo, xpx + offsetX - nameExtent.x / 2 + 1, ypx - 20 - nameExtent.y / 2 + 1,
                true);
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
    }

    final static class Builder extends
            AbstractModelBuilder<DroneRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;

        Builder() {
            setDependencies(PlaneRoadModel.class, PDPModel.class, EnergyModel.class);
        }

        @Override
        public DroneRenderer build(DependencyProvider dependencyProvider) {
            final PlaneRoadModel rm = dependencyProvider.get(PlaneRoadModel.class);
            final PDPModel pdpModel = dependencyProvider.get(PDPModel.class);
            final EnergyModel energyModel = dependencyProvider.get(EnergyModel.class);

            return new DroneRenderer(rm, pdpModel, energyModel);
        }
    }
}
