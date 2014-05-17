package org.agilereview.demo.findbugs;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.XMLBugReporter;

public class FindbugsHandler extends AbstractHandler implements IHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            FindBugs2 findBugs = new FindBugs2();
            
            findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            
            SortedBugCollection bugs = new SortedBugCollection();
            
            Project project = bugs.getProject().duplicate();
            
            XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
            xmlBugReporter.setAddMessages(true);
            xmlBugReporter.setMinimalXML(false);
            
            xmlBugReporter.setOutputStream(System.out);
            
            xmlBugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);
            xmlBugReporter.setPriorityThreshold(Detector.NORMAL_PRIORITY);
            
            findBugs.setBugReporter(xmlBugReporter);
            findBugs.setProject(project);
            
            findBugs.finishSettings();
            
            findBugs.execute();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
