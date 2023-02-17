/*-
 * #%L
 * Mavenized Improved fit largest circle plugin
 * %%
 * Copyright (C) 2015 - 2022 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import ch.epfl.biop.CirclesBasedSpine;
import ch.epfl.biop.MaxInscribedCircles;
import ij.IJ;
//import ij.ImageJ;
import net.imagej.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import java.util.List;

public class Max_Inscribed_Circles implements PlugIn {
    private static final String PREFIX = "biop.max.inscribed.";
    private RoiManager rm;
    private boolean isSelectionOnly;
    private boolean isGetSpine;
    private double minSimilarity = 0.5D;
    private double closenessTolerance = 10.0D;
    private double minDiameter;

    private boolean appendPositionToName = false;

    public Max_Inscribed_Circles() {
    }

    public void run(String arg) {
        this.getParameters();
        ImagePlus imp = IJ.getImage();
        if (!imp.getProcessor().isBinary() && imp.getRoi() == null) {
            IJ.error("Need selection or a binary image!");
        }

        this.showDialog();
        this.setParameters();

        // Get the current Roi Manager or create a new one
        this.rm = RoiManager.getInstance();
        if (this.rm == null) {
            this.rm = new RoiManager();
        }
        // Display it
        this.rm.setVisible(true);


        Overlay finalOverlay = new Overlay();

        MaxInscribedCircles mic = MaxInscribedCircles.builder(imp)
                .minimumDiameter(this.minDiameter)
                .useSelectionOnly(this.isSelectionOnly)
                .getSpine(this.isGetSpine)
                .closenessTolerance(this.closenessTolerance)
                .minimumSimilarity(this.minSimilarity)
                .appendPositionToName(this.appendPositionToName)
                .build();

        List<Roi> allElements = mic.findCircles();

        for ( Roi roi: allElements ) {
            if( roi.getName() != null && roi.getName().contains("Spine"))  finalOverlay.add( roi );
            else rm.addRoi( roi );
        }
        imp.setOverlay(finalOverlay);
    }

    private void showDialog() {
        GenericDialog gd = new GenericDialog("Find Largest Circles");
        gd.addNumericField("Minimum_Disk Diameter (px)", this.minDiameter, 1);
        gd.addMessage("Set to 0 to get only the largest inscribed circle");
        gd.addCheckbox("Use selection instead of mask", this.isSelectionOnly);
        gd.addCheckbox("Get Spine", this.isGetSpine);
        gd.addMessage("Spine Detection Settings");
        gd.addNumericField("Minimum_Similarity", 0.5D, 2);
        gd.addNumericField("Closeness Tolerance", 5.0D, 0);

        gd.addCheckbox("Append Position to ROI Name", this.appendPositionToName);

        gd.showDialog();
        if (!gd.wasCanceled()) {
            this.minDiameter = gd.getNextNumber();
            this.isSelectionOnly = gd.getNextBoolean();
            this.isGetSpine = gd.getNextBoolean();
            this.minSimilarity = gd.getNextNumber();
            this.closenessTolerance = gd.getNextNumber();

            this.appendPositionToName = gd.getNextBoolean();
        }
    }

    private void getParameters() {
        this.minDiameter = Prefs.get("biop.max.inscribed.minDiameter", this.minDiameter);
        this.isSelectionOnly = Prefs.get("biop.max.inscribed.isSelOnly", this.isSelectionOnly);
        this.isGetSpine = Prefs.get("biop.max.inscribed.isGetSpine", this.isGetSpine);
        this.minSimilarity = Prefs.get("biop.max.inscribed.minSimilarity", this.minSimilarity);
        this.closenessTolerance = Prefs.get("biop.max.inscribed.closenessTolerance", this.closenessTolerance);
        this.appendPositionToName = Prefs.get("biop.max.inscribed.isAddName", this.appendPositionToName);

    }

    private void setParameters() {
        Prefs.set("biop.max.inscribed.minDiameter", this.minDiameter);
        Prefs.set("biop.max.inscribed.isSelOnly", this.isSelectionOnly);
        Prefs.set("biop.max.inscribed.isGetSpine", this.isGetSpine);
        Prefs.set("biop.max.inscribed.minSimilarity", this.minSimilarity);
        Prefs.set("biop.max.inscribed.closenessTolerance", this.closenessTolerance);
        Prefs.set("biop.max.inscribed.isAddName", this.appendPositionToName);

    }

    public static void main(String[] args) {
        //Class<?> clazz = Max_Inscribed_Circles.class;
        //String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        //String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        //System.setProperty("plugins.dir", pluginsDir);
       // new ImageJ();
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ImagePlus imp = IJ.createImage("Untitled", "8-bit black", 256, 256, 1);
        imp.setRoi(45, 59, 70, 88);
        IJ.setForegroundColor(255, 255, 255);
        IJ.run(imp, "Fill", "slice");
        imp.setRoi(new OvalRoi(156, 68, 54, 95));
        IJ.run(imp, "Fill", "slice");
        imp.show();
    }
}
