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

public class MapRenderer extends CanvasRenderer.AbstractCanvasRenderer {

    final String map;

    private MapRenderer(String map) {
        this.map = map;
    }

    public static MapRenderer.Builder builder(String map) { return new MapRenderer.Builder(map); }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
        Image mapImage = new Image(gc.getDevice(), getClass().getResourceAsStream(map));
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

