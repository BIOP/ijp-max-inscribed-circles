//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import ch.epfl.biop.CirclesBasedSpine;
import ch.epfl.biop.MaxInscribedCircles;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import java.util.ArrayList;
import java.util.Iterator;

public class Max_Inscribed_Circles implements PlugIn {
    private static final String PREFIX = "biop.max.inscribed.";
    private RoiManager rm;
    private boolean isSelectionOnly;
    private boolean isGetSpine;
    private double minSimilarity = 0.5D;
    private double closenessTolerance = 10.0D;
    private double minDiameter;

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
        // First get the inscribed circles:
        ArrayList<Roi> circles = MaxInscribedCircles.findCircles(imp, this.minDiameter, this.isSelectionOnly);

        // Get the current Roi Manager or create a new one
        this.rm = RoiManager.getInstance();
        if (this.rm == null) {
            this.rm = new RoiManager();
        }
        // Display it
        this.rm.setVisible(true);

        // Add the circles to the Roi Manager
        Iterator var5 = circles.iterator();

        while(var5.hasNext()) {
            Roi r = (Roi)var5.next();
            this.rm.addRoi(r);
        }

        // Display a message if no circle was found
        if (circles.size() == 0) {
            IJ.log("No circle found, consider increasing the minimum diameter.");
        }

        // Only get spine if checkbox is ticked and there is at least one circle
        if (this.isGetSpine && circles.size() > 0) {
            // Define the parameters
            CirclesBasedSpine sbs = (new CirclesBasedSpine.Settings(imp)).circles(circles).closenessTolerance(this.closenessTolerance).minSimilarity(this.minSimilarity).showCircles(false).build();
            // Get the spine
            Roi spine = sbs.getSpine();
            // If one is found rename and add it to the Roi Manager
            if (spine != null) {
                spine.setName("Spine");
                this.rm.addRoi(spine);
            } else {
                IJ.log("No spine found");
            }
        }

    }

    private void showDialog() {
        GenericDialog gd = new GenericDialog("Find Largest Circles");
        gd.addNumericField("Minimum Disk Diameter (px)", this.minDiameter, 1);
        gd.addMessage("Set to 0 to get only the largest inscribed circle");
        gd.addCheckbox("Use selection instead of mask", this.isSelectionOnly);
        gd.addCheckbox("Get Spine", this.isGetSpine);
        gd.addMessage("Spine Detection Settings");
        gd.addNumericField("Minimum Similarity", 0.5D, 2);
        gd.addNumericField("Closeness Tolerance", 5.0D, 0);
        gd.showDialog();
        if (!gd.wasCanceled()) {
            this.minDiameter = gd.getNextNumber();
            this.isSelectionOnly = gd.getNextBoolean();
            this.isGetSpine = gd.getNextBoolean();
            this.minSimilarity = gd.getNextNumber();
            this.closenessTolerance = gd.getNextNumber();
        }
    }

    private void getParameters() {
        this.minDiameter = Prefs.get("biop.max.inscribed.minDiameter", this.minDiameter);
        this.isSelectionOnly = Prefs.get("biop.max.inscribed.isSelOnly", this.isSelectionOnly);
        this.isGetSpine = Prefs.get("biop.max.inscribed.isGetSpine", this.isGetSpine);
        this.minSimilarity = Prefs.get("biop.max.inscribed.minSimilarity", this.minSimilarity);
        this.closenessTolerance = Prefs.get("biop.max.inscribed.closenessTolerance", this.closenessTolerance);
    }

    private void setParameters() {
        Prefs.set("biop.max.inscribed.minDiameter", this.minDiameter);
        Prefs.set("biop.max.inscribed.isSelOnly", this.isSelectionOnly);
        Prefs.set("biop.max.inscribed.isGetSpine", this.isGetSpine);
        Prefs.set("biop.max.inscribed.minSimilarity", this.minSimilarity);
        Prefs.set("biop.max.inscribed.closenessTolerance", this.closenessTolerance);
    }

    public static void main(String[] args) {
        Class<?> clazz = Max_Inscribed_Circles.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);
        new ImageJ();
        ImagePlus imp = IJ.createImage("Untitled", "8-bit black", 256, 256, 1);
        imp.setRoi(45, 59, 70, 88);
        IJ.setForegroundColor(255, 255, 255);
        IJ.run(imp, "Fill", "slice");
        imp.setRoi(new OvalRoi(156, 68, 54, 95));
        IJ.run(imp, "Fill", "slice");
        imp.show();
    }
}
