//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ch.epfl.biop;

import fiji.process3d.EDT;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MaxInscribedCircles {
	public MaxInscribedCircles() {
	}

	public static ArrayList<Roi> findCircles(ImagePlus imp, double minD, boolean isSelectionOnly) {
		IJ.showStatus("Finding Largest Inscribed Circles based on Distance Map...");
		ArrayList<Roi> allrois = new ArrayList();
		Roi sel = imp.getRoi();
		Object ip;
		int offsetX;
		int offsetY;
		if (sel != null && sel.isArea()) {
			offsetX = sel.getBounds().x;
			offsetY = sel.getBounds().y;
			ImageProcessor iptmp;
			if (isSelectionOnly) {
				iptmp = sel.getMask().resize(sel.getPolygon().getBounds().width * 2);
			} else {
				iptmp = imp.getProcessor().resize(sel.getPolygon().getBounds().width * 2);
				sel.setLocation(offsetX, offsetY);
			}

			ip = new ByteProcessor(iptmp.getWidth() + 4, iptmp.getHeight() + 4);
			((ImageProcessor)ip).copyBits(iptmp, 2, 2, 3);
			--offsetX;
			--offsetY;
		} else {
			ip = imp.getProcessor().resize(imp.getWidth() * 2);
			offsetX = 0;
			offsetY = 0;
		}

		ImageStack tmpstk = new ImageStack(((ImageProcessor)ip).getWidth(), ((ImageProcessor)ip).getHeight());
		tmpstk.addSlice((ImageProcessor)ip);
		EDT edt = new EDT();
		boolean done = false;

		while(!done) {
			MaximumFinder mf = new MaximumFinder();
			ImageProcessor dist_map_ip = edt.compute(tmpstk).getProcessor();
			Polygon points = mf.getMaxima(dist_map_ip, 1.0D, false);
			ArrayList<Double> hits = getSortedPoints(points, minD, dist_map_ip);
			done = true;

			for(int k = 0; k < hits.size(); ++k) {
				boolean is_draw = true;
				Double p = (Double)hits.get(k);
				double r = dist_map_ip.getInterpolatedValue(p.x, p.y);
				ArrayList<Double> neigh = findNeighbors(p, hits, dist_map_ip);
				if (neigh.size() > 1) {
					Iterator var27 = neigh.iterator();

					while(var27.hasNext()) {
						Double pp = (Double)var27.next();
						double r2 = dist_map_ip.getInterpolatedValue(pp.x, pp.y);
						if (r < r2) {
							is_draw = false;
							break;
						}
					}
				}

				if (is_draw) {
					double posx = p.x - r;
					double posy = p.y - r;
					Roi circ = new OvalRoi(posx, posy, r * 2.0D, r * 2.0D);
					((ImageProcessor)ip).fill(circ);
					posx = (p.x - r) / 2.0D;
					posy = (p.y - r) / 2.0D;
					circ = new OvalRoi(posx, posy, r, r);
					circ.setStrokeWidth(1.0F);
					circ.setLocation(posx + (double)offsetX, posy + (double)offsetY);
					allrois.add(circ);
					done = false;
				}

				if (minD == 0.0D) {
					return allrois;
				}
			}
		}

		IJ.showStatus("Done...");
		return allrois;
	}

	private static ArrayList<Double> getSortedPoints(Polygon points, double minD, final ImageProcessor ip) {
		ArrayList<Double> hits = new ArrayList(points.npoints);
		List<Double> tmpList = new ArrayList(points.npoints);

		int ind;
		for(ind = 0; ind < points.npoints; ++ind) {
			double r = ip.getInterpolatedValue((double)points.xpoints[ind], (double)points.ypoints[ind]);
			if (r > minD) {
				hits.add(new Double((double)points.xpoints[ind], (double)points.ypoints[ind]));
			}
		}

		Collections.sort(hits, new Comparator<Double>() {
			public int compare(Double p1, Double p2) {
				return (new java.lang.Double(ip.getInterpolatedValue(p2.x, p2.y))).compareTo(new java.lang.Double(ip.getInterpolatedValue(p1.x, p1.y)));
			}
		});
		ind = 0;

		for(int i = 1; i < hits.size(); ++i) {
			Double p = (Double)hits.get(i);
			if (ip.getInterpolatedValue(p.x, p.y) < ip.getInterpolatedValue(((Double)hits.get(0)).x, ((Double)hits.get(0)).y)) {
				ind = i;
				break;
			}
		}

		((List)tmpList).addAll(hits);
		if (ind > 0) {
			tmpList = hits.subList(0, ind);
		}

		ArrayList<Double> finalList = new ArrayList();
		finalList.addAll((Collection)tmpList);
		return finalList;
	}

	public static ArrayList<Double> findNeighbors(Double p, ArrayList<Double> hits, ImageProcessor ip) {
		ArrayList<Double> neighbors = new ArrayList();
		double r = ip.getInterpolatedValue(p.x, p.y);
		Iterator var7 = hits.iterator();

		while(var7.hasNext()) {
			Double k = (Double)var7.next();
			double dist = p.distance(k);
			double r2 = ip.getInterpolatedValue(k.x, k.y);
			if (dist < r + r2) {
				neighbors.add(k);
			}
		}

		return neighbors;
	}
}
