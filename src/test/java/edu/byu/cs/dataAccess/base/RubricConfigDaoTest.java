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
        Assertions.assertDoesNotThrow(() -> dao.setRubricConfig(phase, config));
        RubricConfig obtained = dao.getRubricConfig(phase);
        Assertions.assertEquals(config, obtained);
    }

    @Test
    void setRubricIdAndPoints(){
        //dao.setRubricIdAndPoints();
    }

    @Test
    void getPhaseTotalPossiblePoints(){
        //dao.getPhaseTotalPossiblePoints();
    }


    RubricConfig generateRubricConfig(Phase phase){
        EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> items = new EnumMap<>(Rubric.RubricType.class);
        Collection<Rubric.RubricType> types = PhaseUtils.getRubricTypesFromPhase(phase);
        for (Rubric.RubricType type : Rubric.RubricType.values()){
            if (types.contains(type)){
                var item = new RubricConfig.RubricConfigItem(
                        "Testing: " + type.toString(),
                        "This is a test for category " + type +
                                " for Phase:" + PhaseUtils.getPhaseAsString(phase),
                        240,
                        generateRandomRubricID()
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
