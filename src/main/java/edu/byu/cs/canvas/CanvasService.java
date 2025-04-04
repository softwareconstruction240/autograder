package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;

/**
 * A service class providing a {@link CanvasIntegration} to allow communication with Canvas.
 * <br>
 * If enabled to use Canvas, the {@code CanvasService} will provide a {@link CanvasIntegrationImpl}.
 * Otherwise, it will provide a {@link FakeCanvasIntegration}.
 */
public class CanvasService {
    private static CanvasIntegration canvasIntegration;

    static {
        if (ApplicationProperties.useCanvas())
            canvasIntegration = new CanvasIntegrationImpl();
        else
            canvasIntegration = new FakeCanvasIntegration();
    }

    public static CanvasIntegration getCanvasIntegration() {
        return canvasIntegration;
    }

    public static void setCanvasIntegration(CanvasIntegration canvasIntegration) {
        CanvasService.canvasIntegration = canvasIntegration;
    }
}
