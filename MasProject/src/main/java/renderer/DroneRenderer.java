package renderer;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import energy.EnergyModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import pdp.Drone;
import pdp.DroneHW;
import pdp.DroneLW;

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

    public static DroneRenderer.Builder builder() { return new Builder(); }

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

    private Color getChargeColor(Drone drone){
        final int chargeStatus = drone.getChargingStatus();
        Device device = Display.getCurrent();
        if (chargeStatus == 0) return new Color(device, 255,140,0);
        if (chargeStatus == 1) return new Color(device, 21, 82, 0);
        if (chargeStatus == 2) return new Color(device, 174,202,0);
        if (chargeStatus == 3) return new Color(device, 198,165,0);
        if (chargeStatus == 4) return new Color(device, 194,104,0);
        if (chargeStatus == 5) return new Color(device, 191,45,0);
        return new Color(device, 0,0,0);
    }

    private void renderDrone(RoadUser user, GC gc, ViewPort vp, int xpx, int ypx, int r){
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
        Color chargeColor = getChargeColor(d);
        final org.eclipse.swt.graphics.Point nameExtent = gc.textExtent(droneInfo);
        gc.setBackground(chargeColor);
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

        public Builder() {
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
