package org.tdar.core.service.workflow.imagej;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.renderer.StaticRenderer;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

/*
 * Licensed under GPL/Public Domain by Johannes Schindelin (Dscho) and generously authorized to reuse and 
 * repackage under the Apache license for use within tDAR source code.  Original version
 * resides [fiji.git]/src-plugins/IO_/src/main/java/io/SVG_Reader.java. 
 * January 3, 2013.
 * http://fiji.sc/
 */

public class SVG_Reader extends ImagePlus implements PlugIn {

    /** Expects path as argument, or will ask for it and then open the image. */
    @Override
    public void run(final String arg) {
        File file = null;
        if ((arg != null) && (arg.length() > 0)) {
            file = new File(arg);
        } else {
            OpenDialog od = new OpenDialog("Choose .svg file", null);
            String directory = od.getDirectory();
            if (null == directory) {
                return;
            }
            file = new File(directory + "/" + od.getFileName());
        }

        UserAgentAdapter userAgent = new UserAgentAdapter();
        StaticRenderer renderer = new StaticRenderer();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext context =
                new BridgeContext(userAgent, loader);
        userAgent.setBridgeContext(context);
        SVGDocument document;
        try {
            document = (SVGDocument)
                    loader.loadDocument(file.toURI().toString());
        } catch (IOException e) {
            IJ.error("Could not open " + file.toURI());
            return;
        }
        GVTBuilder builder = new GVTBuilder();
        renderer.setTree(builder.build(context, document));
        SVGSVGElement root = document.getRootElement();
        float svgX = root.getX().getBaseVal().getValue();
        float svgY = root.getY().getBaseVal().getValue();
        float svgWidth = root.getWidth().getBaseVal().getValue();
        float svgHeight = root.getHeight().getBaseVal().getValue();

        GenericDialog gd = new GenericDialog("SVG dimensions");
        gd.addNumericField("width", svgWidth, 0);
        gd.addNumericField("height", svgHeight, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        int w = (int) gd.getNextNumber();
        int h = (int) gd.getNextNumber();

        AffineTransform transform = new AffineTransform();
        transform.translate(-svgX, -svgY);
        transform.scale(w / svgWidth, h / svgHeight);
        renderer.setTransform(transform);
        renderer.updateOffScreen(w, h);
        Rectangle r = new Rectangle(0, 0, w, h);
        renderer.repaint(r);

        setTitle(file.getName());
        setImage(renderer.getOffScreen());

        if (arg.equals("")) {
            show();
        }
    }
}