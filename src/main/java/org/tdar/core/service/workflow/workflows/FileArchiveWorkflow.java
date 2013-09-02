package org.tdar.core.service.workflow.workflows;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.tdar.core.bean.resource.InformationResourceFile.FileType;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.service.workflow.workflows.Workflow.BaseWorkflow;
import org.tdar.filestore.tasks.PrepareArchiveForKettleTask;
import org.tdar.filestore.tasks.IndexableTextExtractionTask;
import org.tdar.filestore.tasks.ListArchiveTask;

/**
 * $Id$
 * 
 * @author Adam Brin
 * @version $Revision$
 */
@Component
public class FileArchiveWorkflow extends BaseWorkflow {
    
    public static final Collection<String> ARCHIVE_EXTENSIONS_SUPPORTED = java.util.Arrays.asList(new String[]{"zip", "tar", "bz2", "tgz"});

    public FileArchiveWorkflow() {
        for (String extension: ARCHIVE_EXTENSIONS_SUPPORTED) {
            registerFileExtension(extension, ResourceType.SENSORY_DATA, ResourceType.ARCHIVE);
        }

        addTask(ListArchiveTask.class, WorkflowPhase.PRE_PROCESS);
        addTask(PrepareArchiveForKettleTask.class, WorkflowPhase.POST_PROCESS);
        addTask(IndexableTextExtractionTask.class, WorkflowPhase.CREATE_DERIVATIVE);
    }

    @Override
    public FileType getInformationResourceFileType() {
        return FileType.FILE_ARCHIVE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
