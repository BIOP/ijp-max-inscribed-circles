package ch.epfl.biop;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fiji.process3d.EDT;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.filter.MaximumFinder;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

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
				iptmp = sel.getMask().resize(sel.getPolygon().getBounds().width*2);
			} else {
				iptmp = imp.getProcessor().resize(sel.getPolygon().getBounds().width*2);
				sel.setLocation(offsetX, offsetY);
			}
			
			ip = new ByteProcessor(iptmp.getWidth()+4, iptmp.getHeight()+4);
			ip.copyBits(iptmp, 2,2, Blitter.ADD);
			offsetX-=2;
			offsetY-=2;

		// Otherwise use the whole image
		} else {
			ip = imp.getProcessor().resize(imp.getWidth()*2);
			offsetX = 0;
			offsetY = 0;
		}
		
		// Prepare stack for Distance Transform
		ImageStack tmpstk = new ImageStack(ip.getWidth(), ip.getHeight());
		
		tmpstk.addSlice(ip);

		// This class handles the distance transform
		EDT edt = new EDT();
		double posx, posy, r;
		
		boolean done = false;
		
		while(!done) {
			// Try finding all maxima first then picking the ones with no neighbors closer than the radius
			MaximumFinder mf = new MaximumFinder();
			
			ImageProcessor dist_map_ip = edt.compute(tmpstk).getProcessor();
			Polygon points = mf.getMaxima(dist_map_ip, 1, false);
			
			
			// Sort them and only process those that have the maximum size above the minD
			ArrayList<Point> hits = getSortedPoints(points, minD, dist_map_ip);
						
			// Make this list an ArrayList so we can easily add or pop elements
			// ArrayList<Point> hits = new ArrayList<Point>();
			//for(int i=0;i<points.npoints; i++) {
			//	hits.add(new Point(points.xpoints[i], points.ypoints[i]));
			//}
			
			done=true;
			for (int k =0; k<hits.size(); k++) {
				boolean is_draw = true;
				Point p = hits.get(k);
				r = dist_map_ip.getInterpolatedValue(p.x, p.y);

				ArrayList<Point> neigh = findNeighbors(p, hits, dist_map_ip);
				//IJ.log("Point "+p.toString()+" r:"+r+"has "+neigh.size()+" neighbors");
				// It's not about the NEAREST point. it's IF THERE ARE points whose coordinates are within the radius
				if (neigh.size() > 1) {
					// Draw only if it's the largest.
					for( Point pp : neigh) {
						double r2 = dist_map_ip.getInterpolatedValue(pp.x, pp.y);
						if (r < r2) {
							is_draw = false;
							break;
						}
					}				
				}
				
				if(is_draw) {
					// Get coordinates of circle
					posx = (p.x)-r+0.5;
					posy = (p.y)-r+0.5;
								
					// Create Roi for mask
					Roi circ = new OvalRoi(posx,posy, r*2, r*2);
					// Fill ip to recalculate EDM based on this
					ip.fill(circ);

					// Original size Roi
					posx = ((p.x)-r)/2+0.5;
					posy = ((p.y)-r)/2+0.5;
					circ = new OvalRoi(posx,posy, r, r);
					circ.setStrokeWidth(1);

					circ.setLocation(posx+offsetX, posy+offsetY);
					allrois.add(circ);
					done=false;

				}
				if (minD == 0.0) {
					return allrois;
				}
			}
		}
		
		IJ.showStatus("Done...");
	
		return allrois;
	
	}
	
	private static ArrayList<Point> getSortedPoints(Polygon points, double minD, final ImageProcessor ip) {
		ArrayList<Point> hits = new ArrayList<Point>(points.npoints);
		List<Point> tmpList = new ArrayList<Point>(points.npoints);
		
		for(int i=0; i<points.npoints; i++) {
			double r = ip.getInterpolatedValue(points.xpoints[i], points.ypoints[i]);
			if(r > minD) {
				hits.add(new Point(points.xpoints[i], points.ypoints[i]));
			}
		}
		
		Collections.sort(hits, new Comparator<Point>() {
		    @Override
		    public int compare(Point p1, Point p2) {
		        return new Double(ip.getInterpolatedValue(p2.x, p2.y)).compareTo(new Double(ip.getInterpolatedValue(p1.x, p1.y)));
		    }
		});
		
		int ind = 0;
		for(int i=1; i< hits.size(); i++) {
			Point p = hits.get(i);
			if( ip.getInterpolatedValue(p.x, p.y) < ip.getInterpolatedValue(hits.get(0).x, hits.get(0).y) ) {
				// We should remove all objects from this index onwards
				ind = i;
				break;
			}
		}
		tmpList.addAll(hits);
		if (ind > 0) {
			tmpList = hits.subList(0, ind);
		}
		ArrayList<Point> finalList = new ArrayList<Point>();
		finalList.addAll(tmpList);
		return hits;
	}

	public static ArrayList<Point> findNeighbors(Point p, ArrayList<Point> hits, ImageProcessor ip) {
		ArrayList<Point> neighbors = new ArrayList<Point>();
		double r = ip.getInterpolatedValue(p.x, p.y);

		for(Point k : hits) {
			double dist = p.distance(k);
			double r2 = ip.getInterpolatedValue(k.x, k.y);
			if (dist < (r+r2)) {
				neighbors.add(new Point(k));
			}
		}		
		return neighbors;
			
	}
}