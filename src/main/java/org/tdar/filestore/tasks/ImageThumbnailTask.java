/**
 * 
 */
package org.tdar.filestore.tasks;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.tdar.core.bean.resource.InformationResourceFileVersion;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.exception.TdarRecoverableRuntimeException;
import org.tdar.filestore.tasks.Task.AbstractTask;

/**
 * @author Adam Brin
 * 
 */
public class ImageThumbnailTask extends AbstractTask {
    public static final String FMT_ERROR_PROCESSING_COULD_NOT_OPEN = "%s is either corrupt or uses an invalid format (error:\"%s\")";
    private static final long serialVersionUID = -108766461810056577L;
    private static final String JPG_FILE_EXT = ".jpg";
    public static final int LARGE = 600;
    public static final int MEDIUM = 300;
    public static final int SMALL = 96;
    transient ImagePlus ijSource;

    /*
     * public static void main(String[] args) {
     * ImageThumbnailTask task = new ImageThumbnailTask();
     * String baseDir = "C:\\Users\\Adam Brin\\Downloads\\";
     * String orig = "4759782488_ab3452a4eb_b.jpg";
     * WorkflowContext ctx = new WorkflowContext();
     * File origFile = new File(baseDir, orig);
     * 
     * task.setWorkflowContext(ctx);
     * 
     * InformationResourceFileVersion vers = task.generateInformationResourceFileVersion(new File(baseDir, orig), VersionType.UPLOADED);
     * ctx.getOriginalFiles().add(vers);
     * try {
     * task.run(vers);
     * } catch (Exception e) {
     * throw new TdarRecoverableRuntimeException("an image processing error ocurred", e);
     * }
     * }
     */

    @Override
    public void run() throws Exception {
        for (InformationResourceFileVersion version : getWorkflowContext().getOriginalFiles()) {
            run(version);
        }
    }

    public void run(InformationResourceFileVersion version) throws Exception {
        processImage(version, version.getTransientFile());
    }

    public void prepare() {
        // deleteFile(generateFilename(getWorkflowContext().getOutputDirectory(), getWorkflowContext().getOriginalFile().getFilename() , SMALL));
        // deleteFile(generateFilename(getWorkflowContext().getOutputDirectory(), getWorkflowContext().getOriginalFile().getFilename() , MEDIUM));
        // deleteFile(generateFilename(getWorkflowContext().getOutputDirectory(), getWorkflowContext().getOriginalFile().getFilename() , LARGE));
    }

    public void processImage(InformationResourceFileVersion version, File sourceFile) {
        String filename = sourceFile.getName();
        getLogger().debug("sourceFile: " + sourceFile);

        String ext = FilenameUtils.getExtension(sourceFile.getName());
        List<String> exts = Arrays.asList("jpg","gif","tif","tiff","png","jpeg","bmp");
        //FIXME: bad, but necessary for Geospatial images
        if (!exts.contains(ext)) {
            getLogger().error("skipping file with unmatched extension: {} ", ext);
            return;
        }
        
        Opener opener = new Opener();
        opener.setSilentMode(true);
        IJ.redirectErrorMessages(true);
        if (sourceFile == null || !sourceFile.exists()) {
            getWorkflowContext().setErrorFatal(true);
            throw new TdarRecoverableRuntimeException("File does not exist");
        }
        ijSource = opener.openImage(sourceFile.getAbsolutePath());

        String msg = IJ.getErrorMessage();
        if (StringUtils.isNotBlank(msg)) {
            getLogger().error(msg);
        }
        if (ijSource == null) {
            getLogger().debug("Unable to load source image: " + sourceFile);
            if (!msg.contains("Note: IJ cannot open CMYK JPEGs")) {
                getWorkflowContext().setErrorFatal(true);
            }

            String errorMessage = String.format(FMT_ERROR_PROCESSING_COULD_NOT_OPEN, filename, msg);
            throw new TdarRecoverableRuntimeException(errorMessage);
        } else {
            if (getWorkflowContext().getResourceType().hasDemensions()) {
                version.setHeight(ijSource.getHeight());
                version.setWidth(ijSource.getWidth());
                version.setUncompressedSizeOnDisk(ImageThumbnailTask.calculateUncompressedSize(version));
            }
            try {
                createJpegDerivative(version, ijSource, filename, MEDIUM, false);
                createJpegDerivative(version, ijSource, filename, LARGE, false);
                createJpegDerivative(version, ijSource, filename, SMALL, false);
            } catch (Throwable e) {
                getLogger().error("Failed to create jpeg derivative", e);
                throw new TdarRecoverableRuntimeException("processing error", e);
            }
        }
    }

    File generateFilename(String originalFilename, int resolution) {
        String outputFilename = originalFilename;
        outputFilename = outputFilename.substring(0, outputFilename.lastIndexOf("."));

        switch (resolution) {
            case LARGE:
                outputFilename += "_lg";
                break;
            case MEDIUM:
                outputFilename += "_md";
                break;
            case SMALL:
                outputFilename += "_sm";
                break;
            default:
                outputFilename += "_" + resolution;
        }
        outputFilename += JPG_FILE_EXT;
        try {
            outputFilename = URLEncoder.encode(outputFilename, "UTF-8");
        } catch (Exception e) {
            getLogger().debug("exception writing derivative image:", e);
        }
        File outputPath = new File(getWorkflowContext().getWorkingDirectory(), outputFilename);

        return outputPath;
    }

    protected float getScalingRatio(int sourceWidth, int sourceHeight, int resolution) {
        float ratio;
        if (sourceWidth >= sourceHeight)
            ratio = (float) ((float) resolution / (float) sourceWidth);
        else
            ratio = (float) ((float) resolution / (float) sourceHeight);

        return ratio;
    }

    protected void createJpegDerivative(InformationResourceFileVersion originalVersion, ImagePlus ijSource, String origFileName, int resolution,
            boolean canSwitchSource)
            throws Throwable {
        File outputFile = generateFilename(origFileName, resolution);

        VersionType type = null;
        switch (resolution) {
            case LARGE:
                type = VersionType.WEB_LARGE;
                break;
            case MEDIUM:
                type = VersionType.WEB_MEDIUM;
                break;
            case SMALL:
                type = VersionType.WEB_SMALL;
                break;
        }

        ImageProcessor ip = ijSource.getProcessor();
        int sourceWidth = ip.getWidth();
        int sourceHeight = ip.getHeight();
        float scalingRatio = getScalingRatio(sourceWidth, sourceHeight,
                resolution);

        FileOutputStream outputStream = null;
        mkParentDirs(outputFile);
        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            getLogger().debug("exception writing derivative image:", e);
        }

        try {
            if (scalingRatio > 1.0f) {
                ip = averageReductionScale(ip, ijSource,
                        (sourceWidth >= sourceHeight ? sourceWidth : sourceHeight));
            } else if (scalingRatio != 1.0f) {
                // First interpolate to this size, then use the new image for scaling to all the smaller sizes
                ip.setInterpolate(true);
                ip.smooth();
                ip = ip.resize((int) Math.round(ip.getWidth() * scalingRatio),
                        (int) Math.round(ip.getHeight() * scalingRatio));
            } else {
                ip = averageReductionScale(ip, ijSource, resolution);
            }

            // ip is now a scaled image processor
            if (resolution != SMALL)
                ip.sharpen();

            // Get the destination width and height.
            int destWidth = ip.getWidth();
            int destHeight = ip.getHeight();

            BufferedImage bImage = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
            Graphics g = bImage.createGraphics();
            g.drawImage(ip.createImage(), 0, 0, null);
            g.dispose();

            ip = null;

            // evaluate http://www.universalwebservices.net/web-programming-resources/java/adjust-jpeg-image-compression-quality-when-saving-images-in-java
            // at a later date to see about compression needs
            ImageIO.write(bImage, "jpg", outputFile);
            bImage.flush();
            bImage = null;
            InformationResourceFileVersion version = generateInformationResourceFileVersionFromOriginal(originalVersion, outputFile, type);
            version.setHeight(destHeight);
            version.setWidth(destWidth);
            version.setUncompressedSizeOnDisk(ImageThumbnailTask.calculateUncompressedSize(version));
            getWorkflowContext().addVersion(version);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioe) {
                    getLogger().debug("generateImagesIj(): IOException closing FileOutputStream: ", ioe);
                }
            }
        }
    }

    private static Long calculateUncompressedSize(InformationResourceFileVersion version) {
        try {
            return version.getHeight() * version.getWidth() * 3L * 8L;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ImageProcessor averageReductionScale(ImageProcessor ip, ImagePlus source, int res) {
        int factor = (ip.getWidth() > ip.getHeight() ? ip.getWidth() : ip.getHeight());
        double dxshrink = (double) factor / (double) res;
        getLogger().trace("factor: " + factor + " res:" + res);
        int xshrink = (int) dxshrink;
        getLogger().trace("\t\txshrink:" + xshrink);
        getLogger().trace("\t\tdxshrink:" + dxshrink);
        double product = xshrink * xshrink;
        int samples;

        if (source.getBitDepth() == 32)
            return null;

        samples = (ip instanceof ColorProcessor) ? 3 : 1;
        int w = (int) ((double) ip.getWidth() / dxshrink);
        int h = (int) ((double) ip.getHeight() / dxshrink);
        ImageProcessor ip2 = ip.createProcessor(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                ip2.putPixel(x, y, getAverage(ip, x, y, xshrink, product, samples));
            }
        }
        ip2.resetMinAndMax();
        getLogger().trace("\twidth:" + ip.getWidth() + " ->" + ip2.getWidth());
        getLogger().trace("\theight:" + ip.getHeight() + " ->" + ip2.getHeight());
        return ip2;
    }

    private int[] getAverage(ImageProcessor ip, int x, int y, int xshrink, double product, int samples) {
        int[] sum = new int[samples];
        int[] pixel = new int[3];

        for (int i = 0; i < samples; i++) {
            sum[i] = 0;
        }

        for (int y2 = 0; y2 < xshrink; y2++) {
            for (int x2 = 0; x2 < xshrink; x2++) {
                pixel = ip.getPixel(x * xshrink + x2, y * xshrink + y2, pixel);
                for (int i = 0; i < samples; i++)
                    sum[i] += pixel[i];
            }
        }
        for (int i = 0; i < samples; i++)
            sum[i] = (int) (sum[i] / product + 0.5d);
        return sum;
    }

    @Override
    public String getName() {
        return "ImageThumbnailGenerator";
    }

}
