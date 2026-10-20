package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;

/**
 * Creates the application's {@link CanvasIntegration} implementation.
 * Call once during startup and share the resulting integration with its consumers.
 */
public final class CanvasService {
    private CanvasService() { }

    public static CanvasIntegration createIntegration() {
        if (ApplicationProperties.useCanvas()) {
            return new CanvasIntegrationImpl();
        }
        return new FakeCanvasIntegration();
    }
}
