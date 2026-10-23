package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;

/**
 * Creates the application's {@link CanvasIntegration} implementation.
 * Call once during startup and share the resulting integration with its consumers.
 */
public final class CanvasService {
    private CanvasService() { }

    public static CanvasIntegration createIntegration(ConfigurationDao configurationDao, UserDao userDao) {
        if (ApplicationProperties.useCanvas()) {
            return new CanvasIntegrationImpl(configurationDao);
        }
        return new FakeCanvasIntegration(userDao);
    }
}
