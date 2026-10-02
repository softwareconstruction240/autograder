package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;

/**
 * A service class providing a {@link CanvasIntegration} to allow communication with Canvas.
 * <br>
 * If enabled to use Canvas, the {@code CanvasService} will provide a {@link CanvasIntegrationImpl}.
 * Otherwise, it will provide a {@link FakeCanvasIntegration}.
 */
public class CanvasService {
    private CanvasIntegration canvasIntegration;

    public CanvasService() {
        if (ApplicationProperties.useCanvas())
            canvasIntegration = new CanvasIntegrationImpl();
        else
            canvasIntegration = new FakeCanvasIntegration();
    }

    public CanvasIntegration getCanvasIntegration() {
        return canvasIntegration;
    }

    public void setCanvasIntegration(CanvasIntegration canvasIntegration) {
        this.canvasIntegration = canvasIntegration;
    }
}
