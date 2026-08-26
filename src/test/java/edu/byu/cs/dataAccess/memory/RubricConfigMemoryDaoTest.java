package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.RubricConfigDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;

public class RubricConfigMemoryDaoTest extends RubricConfigDaoTest {
    @Override
    protected RubricConfigDao getRubricConfigDao() {
        return new RubricConfigMemoryDao();
    }

    @Override
    protected void clearRubricConfigDao() throws DataAccessException {
        for (Phase phase :Phase.values()) {
            dao.setRubricConfig(phase, null);
        }
    }
}
