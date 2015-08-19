package ch.epfl.biop;

import fiji.process3d.SEDT;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.ArrayList;

public class MaxInscribedCircles {

	/**
	 * main method for finding max inscribed circles based on Distance Map. 
	 * @param imp the original mask
	 * @param minD minimum diameter after which to stop
	 * @return an ArrayList of ROIs containing all found circles 
	 */
	public static ArrayList<Roi> findCircles(ImagePlus imp, double minD) {
		IJ.showStatus("Finding Largest Inscribed Circles based on Distance Map...");
		ArrayList<Roi> allrois = new ArrayList<Roi>();
		ImageProcessor ip = imp.getProcessor().duplicate();
		ImageStack tmpstk = new ImageStack(ip.getWidth(), ip.getHeight(), 1);
		tmpstk.setProcessor(ip, 1);
		SEDT sedt = new SEDT();
		
		double r = minD+1;
		int width = ip.getWidth();

		
		ImagePlus edm;
		
		while (r>minD) {
			edm = sedt.compute(tmpstk);

			//Find the position of the max;
			float[] px = (float[]) edm.getProcessor().getPixels();
			edm.close();
			float themax = 0;
			int   idx = 0;	
			for(int i=0; i<px.length; i++) {
				if (px[i] > themax) {
					idx = i;
					themax = px[i];
				}
			}
			IJ.showStatus("Finding Largest Inscribed Circles D="+(themax*2)+"...");

			// Get coordinates of circle
			double posx = idx%width;
			double posy = idx/width;
			// Get Radius of circle
			r = 2*themax;			
			// Add circle
			Roi circ = new OvalRoi(posx-r/2+0.5,posy-r/2+0.5, r, r);
			circ.setStrokeWidth(2);
			ip.fill(circ);
			allrois.add(circ);
			
			if (minD==0) {
				minD=r+1;
			}
		}
		IJ.showStatus("Done...");

		return allrois;
	}
}
