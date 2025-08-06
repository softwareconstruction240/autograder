package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Random;

public abstract class RubricConfigDaoTest {

    protected RubricConfigDao dao;
    protected abstract RubricConfigDao getRubricConfigDao();
    protected abstract void clearRubricConfigDao() throws DataAccessException;
    static Random random = new Random();


    @BeforeEach
    void setup() throws DataAccessException {
        dao = getRubricConfigDao();
        clearRubricConfigDao();
    }

    @ParameterizedTest
    @EnumSource(Phase.class)
    void setAndGetRubricConfig(Phase phase) throws DataAccessException{
        RubricConfig config = generateRubricConfig(phase);
        assertSetAndGetRubricConfig(phase, config);
    }

    private void assertSetAndGetRubricConfig(Phase phase, RubricConfig config) throws DataAccessException {
        Assertions.assertDoesNotThrow(() -> dao.setRubricConfig(phase, config));
        RubricConfig obtained = dao.getRubricConfig(phase);
        Assertions.assertEquals(config, obtained);
    }

    @Test
    void setAndGetRubricConfigAllPhasesTogether() throws DataAccessException {
        for (Phase phase : Phase.values()){
            setAndGetRubricConfig(phase);
        }
    }

    @ParameterizedTest
    @EnumSource(Phase.class)
    void setAndGetWithNullIds(Phase phase) throws DataAccessException {
        RubricConfig config = generateRubricConfig(phase, 240, null);
        assertSetAndGetRubricConfig(phase, config);
    }

    @ParameterizedTest
    @EnumSource (Phase.class)
    void setRubricIdAndPoints(Phase phase) throws DataAccessException{
        RubricConfig firstConfig = generateRubricConfig(phase, 240);
        //the memory dao will actually change the first Rubric Config--which is fine--but we want to verify the change
        RubricConfig deepCopy = new RubricConfig(firstConfig.phase(), firstConfig.items().clone());
        dao.setRubricConfig(phase, firstConfig);
        RubricConfig changedConfig = generateRubricConfig(phase, 0);
        for (Rubric.RubricType type : PhaseUtils.getRubricTypesFromPhase(phase)){
            dao.setRubricIdAndPoints(phase, type, 0, changedConfig.items().get(type).rubric_id());
        }
        RubricConfig obtained = dao.getRubricConfig(phase);
        for (RubricConfig.RubricConfigItem item : obtained.items().values()){
            if (item != null){
                Assertions.assertNotEquals(deepCopy, obtained);
            }
        }
        Assertions.assertEquals(changedConfig, obtained);
    }

    @ParameterizedTest
    @EnumSource (Phase.class)
    void getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException {
        RubricConfig config = generateRubricConfig(phase, 240);
        dao.setRubricConfig(phase, config);
        int expectedTotal = 0;
        for (Rubric.RubricType type : PhaseUtils.getRubricTypesFromPhase(phase)) {
            expectedTotal += 240;
        }
        int obtained = dao.getPhaseTotalPossiblePoints(phase);
        Assertions.assertEquals(expectedTotal, obtained);
    }

    //TODO: test where there is no rubric config calculates points to 0

    RubricConfig generateRubricConfig(Phase phase){
        return generateRubricConfig(phase, 240);
    }

    RubricConfig generateRubricConfig(Phase phase, int points){
        return generateRubricConfig(phase, points, generateRandomRubricID());
    }

    RubricConfig generateRubricConfig(Phase phase, int points, String rubricId){
        EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> items = new EnumMap<>(Rubric.RubricType.class);
        Collection<Rubric.RubricType> types = PhaseUtils.getRubricTypesFromPhase(phase);
        for (Rubric.RubricType type : Rubric.RubricType.values()){
            if (types.contains(type)){
                var item = new RubricConfig.RubricConfigItem(
                        "Testing: " + type.toString(),
                        "This is a test for category " + type +
                                " for Phase:" + PhaseUtils.getPhaseAsString(phase),
                        points,
                        rubricId
                );
                items.put(type, item);
            }
            else{
                items.put(type, null);
            }
        }
        return new RubricConfig(phase, items);
    }

    /*
    * This functions strives to mimic rubric ids that come from canvas, but the algorithm isn't known
    * so this function could produce rubric ids that are not valid.
    * The dummy data could be less accurate if the api changes,
    * but it should be fine so long this isn't used to test Canvas integration.
    * */
    String generateRandomRubricID(){
        int first = random.nextInt(90000,99999);
        int last = random.nextInt(100, 9999);
        if (random.nextBoolean()){
            return "_" + last;
        }
        return first + "_" + last;
    }
}
