package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.base.RubricConfigDaoTest;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;

public class RubricConfigMemoryDaoTest extends RubricConfigDaoTest {
    @Override
    protected RubricConfigDao getRubricConfigDao() {
        return new RubricConfigMemoryDao();
    }

    @Override
    protected void clearRubricConfigDao() {
        dao = new RubricConfigMemoryDao();
    }
}
