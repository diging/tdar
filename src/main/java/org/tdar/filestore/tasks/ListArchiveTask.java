/**
 * $Id$
 * 
 * @author $Author$
 * @version $Revision$
 */
package org.tdar.filestore.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.filestore.tasks.Task.AbstractTask;

/**
 * @author Adam Brin
 * 
 */
public class ListArchiveTask extends AbstractTask {

    private static final long serialVersionUID = 5392550508417818439L;

    private long effectiveSize = 0l;


    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.filestore.tasks.Task#run(java.io.File)
     */
    @Override
    public void run() throws IOException {
        for (InformationResourceFileVersion version : getWorkflowContext().getOriginalFiles()) {
            File f_ = version.getTransientFile();
            // take the file
            getLogger().debug("listing contents of: " + f_.getName());
            File f = new File(getWorkflowContext().getWorkingDirectory(), f_.getName() + ".contents.txt");
            File f2 = new File(getWorkflowContext().getWorkingDirectory(), f_.getName() + ".index.txt");
            StringBuilder archiveContents = new StringBuilder();

            ArchiveInputStream ais = null;
            boolean successful = false;
            try {
                ArchiveStreamFactory factory = new ArchiveStreamFactory();
                InputStream stream = null;
                String filename = f_.getName().toLowerCase();
                if (filename.endsWith(".tgz") || filename.endsWith("tar.gz")) {
                    stream = new GzipCompressorInputStream(new FileInputStream(f_));
                } else if (filename.endsWith(".bz2") ) {
                    stream = new BZip2CompressorInputStream(new FileInputStream(f_));
                } else {
                    stream = new FileInputStream(f_);
                }
                
                ais = factory.createArchiveInputStream(new BufferedInputStream(stream));
                ArchiveEntry entry = ais.getNextEntry();
                while (entry != null) {
                    writeToFile(archiveContents, entry.getName());
                    entry = ais.getNextEntry();
                }
                successful = true;
            } catch (ArchiveException e) {
              throw new TdarRecoverableRuntimeException("Could find files within the archive:" + f_.getName());
            } finally {
                if (ais != null) {
                    IOUtils.closeQuietly(ais);
                }
            }
            
            // write that to a file with a known format (one file per line)
            FileUtils.writeStringToFile(f, archiveContents.toString());
            InformationResourceFileVersion version_ = generateInformationResourceFileVersionFromOriginal(version, f, VersionType.TRANSLATED);
            FileUtils.writeStringToFile(f2, archiveContents.toString());
            InformationResourceFileVersion version2_ = generateInformationResourceFileVersionFromOriginal(version, f2, VersionType.INDEXABLE_TEXT);
            version.setUncompressedSizeOnDisk(getEffectiveSize());
            getWorkflowContext().addVersion(version_);
            getWorkflowContext().addVersion(version2_);
        }
    }

    private void writeToFile(StringBuilder archiveContents, String uri) {
        archiveContents.append(uri).append(System.getProperty("line.separator"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tdar.filestore.tasks.Task#getName()
     */
    @Override
    public String getName() {
        return "list archive task";
    }

    public long getEffectiveSize() {
        return effectiveSize;
    }

    public void setEffectiveSize(long effectiveSize) {
        this.effectiveSize = effectiveSize;
    }

}
