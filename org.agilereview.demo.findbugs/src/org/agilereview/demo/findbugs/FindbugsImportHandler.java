package org.agilereview.demo.findbugs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.agilereview.common.ui.PlatformUITools;
import org.agilereview.core.external.exception.NullArgumentException;
import org.agilereview.core.external.preferences.AgileReviewPreferences;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(FindbugsImportHandler.class);
    
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        
        // ISelection sel = HandlerUtil.getCurrentSelection(event);
        // Actually, the code above should be working. But as it sometimes gives the wrong selection in e4, use the line below
        final ISelection selection = PlatformUITools.getActiveWorkbenchPage().getSelection();
        
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
                        r.setResponsibility(InstanceScope.INSTANCE.getNode("org.agilereview.core").get(AgileReviewPreferences.AUTHOR,
                                System.getProperty("user.name")));
                        
                        for (BugInstance bug : bugs) {
                            String commentText = bug.getMessageWithoutPrefix();
                            for (BugAnnotation annotation : bug.getAnnotationsForMessage(true)) {
                                if (annotation instanceof SourceLineAnnotation) {
                                    String fileName = ((SourceLineAnnotation) annotation).getSourceFile();
                                    String packageName = ((SourceLineAnnotation) annotation).getPackageName();
                                    
                                    IJavaProject javaProject = JavaCore.create(project);
                                    if (javaProject != null) {
                                        IPackageFragmentRoot[] roots = javaProject.getPackageFragmentRoots();
                                        for (IPackageFragmentRoot root : roots) {
                                            Path path = new Path(root.getPath().toString() + "/" + packageName.replace(".", "/") + "/" + fileName);
                                            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
                                            if (file.exists()) {
                                                try {
                                                    int startLine = ((SourceLineAnnotation) annotation).getStartLine();
                                                    int endLine = ((SourceLineAnnotation) annotation).getEndLine();
                                                    if (startLine > -1 && endLine > -1) { // TODO insert global comments? or what is the semantic of start=end=-1?
                                                        Comment c = CommentingAPI.createComment(file, startLine, endLine, "findbugs", r.getId());
                                                        c.setText(commentText);
                                                    }
                                                } catch (IOException | NullArgumentException e) {
                                                    LOG.error("Error while importing a findbugs finding." + e);
                                                    errorMessage = "Some errors were encountered while importing fingbugs findings. Pleaes check the logfile for further information.";
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    
                                }
                            }
                        }
                    } catch (CoreException e) {
                        LOG.error("CoreException occured while collecting bug information into AgileReview." + e);
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
