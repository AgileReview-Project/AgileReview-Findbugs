package org.agilereview.demo.findbugs;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;

public class FindbugsHandler extends AbstractHandler implements IHandler {
    
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        
        Job findbugsJob = new Job("AgileReview Findbugs Job") {
            
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IProject project = null;
                
                if (selection instanceof IStructuredSelection) {
                    Object firstElement = ((IStructuredSelection) selection).getFirstElement();
                    if (firstElement instanceof IAdaptable) {
                        project = (IProject) ((IAdaptable) firstElement).getAdapter(IProject.class);
                    }
                }
                
                if (project != null) {
                    try {
                        SortedBugCollection bugs = FindbugsPlugin.getBugCollection(project, monitor);
                        for (BugInstance bug : bugs) {
                            System.out.println(bug);
                        }
                    } catch (CoreException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return new Status(IStatus.OK, Activator.PLUGIN_ID, "Done");
            }
        };
        
        findbugsJob.schedule();
        
        return null;
    }
}
