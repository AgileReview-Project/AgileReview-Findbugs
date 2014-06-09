package org.agilereview.demo.findbugs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.agilereview.common.exception.ExceptionHandler;
import org.agilereview.core.external.exception.NullArgumentException;
import org.agilereview.core.external.storage.Comment;
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
                
                String errorMessage = null;
                
                if (selection instanceof IStructuredSelection) {
                    Object firstElement = ((IStructuredSelection) selection).getFirstElement();
                    if (firstElement instanceof IAdaptable) {
                        project = (IProject) ((IAdaptable) firstElement).getAdapter(IProject.class);
                    } else {
                        errorMessage = "No suitable selection. Please select a Project.";
                    }
                } else {
                    errorMessage = "No suitable selection. Please select a Project.";
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
                                    
                                    try {
                                        Comment c = CommentingAPI.createComment(file, ((SourceLineAnnotation) annotation).getStartLine(),
                                                ((SourceLineAnnotation) annotation).getEndLine(), "findbugs", r.getId());
                                        c.setText(commentText);
                                    } catch (IOException | NullArgumentException e) {
                                        FindbugsImportHandler.this.log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                                "Error while importing a findbugs finding:\n" + ExceptionHandler.getStackTrace(e)));
                                        errorMessage = "Some errors were encountered while importing fingbugs findings. Pleaes check the logfile for further information.";
                                    }
                                }
                            }
                        }
                    } catch (CoreException e) {
                        FindbugsImportHandler.this.log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                "CoreException occured while collecting bug information into AgileReview.\n" + ExceptionHandler.getStackTrace(e)));
                        errorMessage = "Error while importing FindBugs result into AgileReview!";
                    }
                }
                
                if (errorMessage != null) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage);
                } else {
                    return new Status(IStatus.OK, Activator.PLUGIN_ID, "Successfully imported FindBugs result into AgileReview.");
                }
            }
        };
        
        findbugsJob.schedule();
        
        return null;
    }
}
