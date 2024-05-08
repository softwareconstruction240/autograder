package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.util.FileUtils;

import java.io.File;

public class TestFactoryModifier implements StudentCodeInteractor {
    @Override
    public void interact(GradingContext context, StudentCodeReader reader) throws GradingException {
        File phaseTestFactory = new File(context.phasesPath(), "phase6/passoff/server/TestFactory.java");
        File backupTestFactory = new File(context.phasesPath(), "backup/BackupTestFactory.java");
        File studentTestFactory = new File(context.stageRepo(), "server/src/test/java/passoff/server/TestFactory.java");
        
        if(!backupTestFactory.exists()) {
            if(!phaseTestFactory.exists()) throw new GradingException("Could not find phase test factory");
            FileUtils.copyFile(backupTestFactory, phaseTestFactory);
        }

        if(studentTestFactory.exists()) FileUtils.copyFile(phaseTestFactory, studentTestFactory);
        else FileUtils.copyFile(phaseTestFactory, backupTestFactory);
    }
}
