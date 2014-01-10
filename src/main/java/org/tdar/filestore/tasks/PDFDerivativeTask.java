/**
 * 
 */
package org.tdar.filestore.tasks;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFImageWriter;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.filestore.WorkflowContext;

/**
 * @author Adam Brin
 */

public class PDFDerivativeTask extends ImageThumbnailTask {

    private static final String ENCRYPTION_WARNING = "This document is encrypted, please remove the encryption before uploading it to tDAR";
    private static final String PROCESSING_ERROR = "An error occurred when processing your PDF";
    private static final long serialVersionUID = -1138753863662695849L;

    public static void main(String[] args) {
        PDFDerivativeTask task = new PDFDerivativeTask();
        String baseDir = "C:\\Users\\abrin\\Desktop\\";
        String orig = "SCIDD_Storage_Basin_Phase_2DR_redacted_pages.pdf";
        File origFile = new File(baseDir, orig);
        WorkflowContext ctx = new WorkflowContext();
        ctx.setWorkingDirectory(TdarConfiguration.getInstance().getTempDirectory());
        task.setWorkflowContext(ctx);
        InformationResourceFileVersion vers = new InformationResourceFileVersion(VersionType.UPLOADED, origFile.getName(), 1, -1L, -1L);
        ctx.getOriginalFiles().add(vers);
        try {
            task.run(vers);
        } catch (Throwable e) {
            throw new TdarRecoverableRuntimeException(PROCESSING_ERROR, e);
        }
    }

    @Override
    public void run() throws Exception {
        for (InformationResourceFileVersion version : getWorkflowContext().getOriginalFiles()) {
            run(version);
        }
    }

    @Override
    public void run(InformationResourceFileVersion version) throws Exception {
        File originalFile = version.getTransientFile();
        try {
            PDDocument document = openPDF("", originalFile);
            File imageFile = new File(extractPage(1, version, document));
            // extractText(originalFile, document);
            closePDF(document);
            if (imageFile.exists()) {
                processImage(version, imageFile);
            }
        } catch (Throwable t) {
            throw new TdarRecoverableRuntimeException(PROCESSING_ERROR, t);
        }
    }

    protected String extractPage(int pageNum, InformationResourceFileVersion originalFile, PDDocument document) {
        // File pdfFile = new File(sourceFile);
        ImageIO.scanForPlugins();
        @SuppressWarnings("unused")
        File pdfFile = originalFile.getTransientFile();
        String imageFormat = "jpg";
        String color = "rgb";
        int resolution;
        try {
            resolution = Toolkit.getDefaultToolkit().getScreenResolution();
        } catch (HeadlessException e) {
            resolution = 96;
        }

        String fn = originalFile.getFilename();
        String outputPrefix = fn.substring(0, fn.lastIndexOf('.'));
        outputPrefix = new File(getWorkflowContext().getWorkingDirectory(), outputPrefix).toString();

        if (document != null) {
            int imageType = determineImageType(color);

            try {
                PDFImageWriter imageWriter = new PDFImageWriter();
                // The following library call will write "Writing: " + the file name to the System.out stream. Naughty!
                // This is fixed in a later version of pdfbox, but we have a transitive dependency via Tika...
                boolean success = imageWriter.writeImage(document, imageFormat, "", pageNum, pageNum, outputPrefix, imageType, resolution);
                if (!success) {
                    getLogger().info("Error: no writer found for image format '" + imageFormat + "'");
                }
            } catch (Throwable e) {
                getLogger().debug("PDF image extraction failed", e);
            }
        }

        return outputPrefix + pageNum + "." + imageFormat;
    }

    private void closePDF(PDDocument document) {
        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                getLogger().warn("cannot close PDF", e);
            }
        }
    }

    private PDDocument openPDF(String password, File pdfFile) {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);

            if (document.isEncrypted()) {
                getLogger().info("access permissions: " + document.getCurrentAccessPermission());
                getLogger().info("security manager: " + document.getSecurityHandler());
                getWorkflowContext().setErrorFatal(true);
                throw new TdarRecoverableRuntimeException(ENCRYPTION_WARNING);
            }

            // try {
            // document.decrypt(password);
            // } catch (InvalidPasswordException e) {
            // getLogger().debug("Error: The document is encrypted.");
            // }
            // }
            getWorkflowContext().setNumPages(document.getNumberOfPages());
        } catch (IOException e) {
            getWorkflowContext().setErrorFatal(true);
            getLogger().info("IO Exception ocurred", e);
            // } catch (CryptographyException ce) {
            // getLogger().info(ce);
            // ce.printStackTrace();
        }
        return document;
    }

    private int determineImageType(String color) {
        int imageType = 24;
        if ("bilevel".equalsIgnoreCase(color)) {
            imageType = BufferedImage.TYPE_BYTE_BINARY;
        } else if ("indexed".equalsIgnoreCase(color)) {
            imageType = BufferedImage.TYPE_BYTE_INDEXED;
        } else if ("gray".equalsIgnoreCase(color)) {
            imageType = BufferedImage.TYPE_BYTE_GRAY;
        } else if ("rgb".equalsIgnoreCase(color)) {
            imageType = BufferedImage.TYPE_INT_RGB;
        } else if ("rgba".equalsIgnoreCase(color)) {
            imageType = BufferedImage.TYPE_INT_ARGB;
        } else {
            getLogger().debug("Error: the number of bits per pixel must be 1, 8 or 24.");
        }
        return imageType;
    }

    @Override
    public String getName() {
        return "PDFDerivativeTask";
    }

}
