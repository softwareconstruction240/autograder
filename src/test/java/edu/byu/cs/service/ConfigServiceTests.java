package edu.byu.cs.service;

import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao.Configuration;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigServiceTests {
    private ConfigurationDao configurationDao;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configurationDao = mock(ConfigurationDao.class);
        configService = new ConfigService(configurationDao, mock(RubricConfigDao.class));
    }

    @Test
    void getSlackLink_readsFromInjectedConfigurationDao() throws Exception {
        when(configurationDao.getConfiguration(Configuration.SLACK_LINK, String.class))
                .thenReturn("https://example.com/class-chat");

        assertEquals("https://example.com/class-chat", configService.getSlackLink());
    }

    @Test
    void updateSlackLink_writesThroughInjectedConfigurationDao() throws Exception {
        when(configurationDao.getConfiguration(Configuration.SLACK_LINK, String.class))
                .thenReturn("https://example.com/old");

        configService.updateSlackLink(new edu.byu.cs.model.User(
                "admin", 1, "Admin", "User", null, edu.byu.cs.model.User.Role.ADMIN),
                "https://example.com/new");

        verify(configurationDao).setConfiguration(Configuration.SLACK_LINK,
                "https://example.com/new", String.class);
    }
}
