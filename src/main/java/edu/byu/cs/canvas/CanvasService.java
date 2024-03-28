package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;

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
