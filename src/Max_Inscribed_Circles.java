
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.util.ArrayList;

import ch.epfl.biop.MaxInscribedCircles;

/**
 * PlugIn to find the largest circle or circles inside a mask.
 * Macro recordable
 * @author Olivier Burri
 * @version 1.0
 */
public class Max_Inscribed_Circles implements PlugIn {

	
	private RoiManager rm;
	private double minD;
	
	
	@Override
	public void run(String arg0) {
		
		ImagePlus imp = IJ.getImage();
		this.minD = Prefs.get("largest.circ.minD", 4);
		
		
GenericDialog gd = new GenericDialog("Find Largest Circles");
		
		gd.addNumericField("Minimum Disk Diameter (px)", this.minD, 1);
		gd.addMessage("Set to 0 to get only the largest inscribed circle");
		gd.showDialog();
		
		if(gd.wasCanceled()) {
			return;			
		}
		
		minD = gd.getNextNumber();
		Prefs.set("largest.circ.minD", minD);

		
		
		//Run the function
		ArrayList<Roi> circles = MaxInscribedCircles.findCircles(imp, minD);
	
		//Add ROIS
		rm = RoiManager.getInstance();
		if (rm == null) {
			rm = new RoiManager();
		}
		rm.setVisible(true);
		
		for(Roi r : circles) {
			rm.addRoi(r);
		}
		
		
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 * A charming example by Lord Dr. Romain G, Queen Emeritus of Deadly Neo Australia 
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Max_Inscribed_Circles.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
		
		ImagePlus imp1 = IJ.openImage("http://imagej.nih.gov/ij/images/blobs.gif");
		IJ.setAutoThreshold(imp1, "Default");
		Prefs.blackBackground = false;
		IJ.run(imp1, "Convert to Mask", "");
		imp1.show();
		
	}


}
