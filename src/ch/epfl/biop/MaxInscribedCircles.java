package ch.epfl.biop;

import fiji.process3d.SEDT;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;

public class MaxInscribedCircles {

	/**
	 * main method for finding max inscribed circles based on Distance Map. 
	 * @param imp ImagePlus of the mask image
	 * @param minD minimum diameter before which to stop
	 * @param isSelectionOnly should the plugin only use the current selection as a mask and ignore the pixels?
	 * @return an ArrayList of ROIs containing all found circles 
	 */
	public static ArrayList<Roi> findCircles(ImagePlus imp, double minD, boolean isSelectionOnly) {
		
		IJ.showStatus("Finding Largest Inscribed Circles based on Distance Map...");
		ArrayList<Roi> allrois = new ArrayList<Roi>();
		ImageProcessor ip;
		int offsetX;
		int offsetY;
		
		Roi sel = imp.getRoi();
		

		// Check image and selection
		// If there is a selection, only search within 
		// if asked, work on the selection only and not on the mask
		if (sel != null && sel.isArea()) {
			ImageProcessor iptmp;
			offsetX = sel.getBounds().x;
			offsetY = sel.getBounds().y;
			
			if(isSelectionOnly) {	
				iptmp = sel.getMask();
			} else {
				iptmp = imp.getProcessor().crop();
				sel.setLocation(0, 0);
				iptmp.fillOutside(sel);
				sel.setLocation(offsetX, offsetY);

			}
			ip = new ByteProcessor(iptmp.getWidth()+4, iptmp.getHeight()+4);
			ip.copyBits(iptmp, 2,2, Blitter.ADD);
			offsetX-=2;
			offsetY-=2;
			

		// Otherwise use the whole image
		} else {
			ip = imp.getProcessor().duplicate();
			offsetX = 0;
			offsetY = 0;
		}
		
		// Prepare stack for Distance Transform
		ImageStack tmpstk = new ImageStack(ip.getWidth(), ip.getHeight(), 1);
		tmpstk.setProcessor(ip, 1);
		
		// This class handles the distance transform
		SEDT sedt = new SEDT();
		
		double r;
		
		int width = ip.getWidth();
		boolean done = false;
		while (!done) {
			//Find the position of the max;
			float[] px = (float[]) sedt.compute(tmpstk).getProcessor().getPixels();
			float themax = 0;
			int   idx = 0;	
			for(int i=0; i<px.length; i++) {
				if (px[i] > themax) {
					idx = i;
					themax = px[i];
				}
			}
	
			// Get Diameter of circle
			r = 2*themax;		

			if (r>minD) {
				
				IJ.showStatus("Finding Largest Inscribed Circles D="+r+"...");
									
				// Get coordinates of circle
				double posx = (idx%width)-r/2+0.5;
				double posy = (idx/width)-r/2+0.5;
				
				// Create Roi
				Roi circ = new OvalRoi(posx,posy, r, r);
				circ.setStrokeWidth(2);
				
				// Remove from mask for next iteration
				ip.fill(circ);
	
				// Add to Roi Manager on original image
				circ.setLocation(posx+offsetX, posy+offsetY);
				allrois.add(circ);
				
				// If the minD was 0, the user only gets the largest circle, singular
				if (minD==0) {
					done = true;
				}
				
			} else {
				done = true;
			}
		}
		IJ.showStatus("Done...");

		return allrois;
	}
}