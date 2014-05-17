package org.agilereview.demo.findbugs;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.agilereview.core.external.storage.CommentingAPI;
import org.agilereview.core.external.storage.Review;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * 
 * @author Peter Reuter (17.05.2014)
 */
public class FindbugsImportHandler extends AbstractHandler implements IHandler {
    
    /** The Logger for this plugin */
    private final ILog log = Activator.getDefault().getLog();
    
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        
        Job findbugsJob = new Job("Importing FindBugs result into AgileReview") {
            
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
                        
                        Review r = CommentingAPI.createReview();
                        r.setName("Findbugs Import " + new SimpleDateFormat("YYYY-MM-dd HH:mm").format(new Date()));
                        r.setDescription("FindBugs found " + bugs.getCollection().size() + " bugs in project '" + bugs.getProject().getProjectName()
                                + "'.");
                        
                        for (BugInstance bug : bugs) {
                            String commentText = bug.getMessageWithoutPrefix();
                            for (BugAnnotation annotation : bug.getAnnotationsForMessage(true)) {
                                if (annotation instanceof SourceLineAnnotation) {
                                    String fileName = ((SourceLineAnnotation) annotation).getSourceFile();
                                    String packageName = ((SourceLineAnnotation) annotation).getPackageName();
                                    
                                    Path path = new Path("/" + project.getName() + "/src/" + packageName.replace(".", "/") + "/" + fileName);
                                    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
                                    
                                    // XXX this does not work atm
                                    // Comment c = CommentingAPI.createComment("findbugs", r.getId());
                                    // c.setCommentedFile(file);
                                    // c.setText(commentText);
                                }
                            }
                        }
                    } catch (CoreException e) {
                        FindbugsImportHandler.this.log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                "CoreException occured while collecting bug information into AgileReview.\n" + e.getStackTrace()));
                        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error while importing FindBugs result into AgileReview!");
                    }
                }
                return new Status(IStatus.OK, Activator.PLUGIN_ID, "Successfully imported FindBugs result into AgileReview.");
            }
        };
        
        findbugsJob.schedule();
        
        return null;
    }
}
