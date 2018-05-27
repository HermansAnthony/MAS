package renderer;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class MapRenderer extends CanvasRenderer.AbstractCanvasRenderer {

    final String map;

    private MapRenderer(String map) {
        this.map = map;
    }

    public static MapRenderer.Builder builder(String map) { return new MapRenderer.Builder(map); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
        // TODO fix me so that resolution is no longer an issue
        Image mapImage = new Image(gc.getDevice(), getClass().getResourceAsStream(map));
        System.out.println("Screen bounds:" +  gc.getDevice().getBounds().width + "x" +  gc.getDevice().getBounds().height);
        System.out.println("Image bounds:" +  mapImage.getBounds().width + "x" +  mapImage.getBounds().height);
        Double srcWidth = (gc.getDevice().getBounds().width*1.0 / mapImage.getBounds().width)*100;
        Double srcHeight = (gc.getDevice().getBounds().height*1.0 / mapImage.getBounds().height)*100;
        System.out.println("Scalefactor:" +  srcWidth + "x" +  srcHeight);
        ImageData imgData = mapImage.getImageData();
        imgData.scaledTo(srcWidth.intValue(), srcHeight.intValue());
        Image resolved = new Image(gc.getDevice(), imgData);
        System.out.println("New resolution:" + resolved.getBounds().width + "x" + resolved.getBounds().height);
//        gc.drawImage(resolved, 0, 0);
        gc.drawImage(mapImage, 0, 0);
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
    }

    @Override
    public Optional<ViewRect> getViewRect() {
        return Optional.of(new ViewRect(new Point(0, 0), new Point(1120, 956)));
    }

    final static class Builder extends
            ModelBuilder.AbstractModelBuilder<MapRenderer, Void> {

        private static final long serialVersionUID = -1772420262312399129L;
        private final String map;

        Builder(String map) {
            this.map = map;
        }

        @Override
        public MapRenderer build(DependencyProvider dependencyProvider) {
            return new MapRenderer(map);
        }
    }

}

