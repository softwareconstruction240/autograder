package edu.byu.cs.controller;

import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import spark.Route;

import java.util.Collection;

public class ConfigController {

    public static final Route updateLivePhases = (req, res) -> {

        return null;
    };

    public static final Route updateBannerMessage = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();
        return null;
    };




    public void updateLivePhases(ConfigurationDao dao, Collection<Phase> phases) throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSION_ENABLED, phases, Collection.class);
    }
}
