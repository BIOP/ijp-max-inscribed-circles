/*-
 * #%L
 * Mavenized Improved fit largest circle plugin
 * %%
 * Copyright (C) 2015 - 2023 EPFL
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

package ch.epfl.biop;

import fiji.process3d.EDT;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.geom.Point2D.Double;
import java.util.*;

public class MaxInscribedCircles {

	private ImagePlus imp;
	private double minimumDiameter;
	private double spineClosenessTolerance;
	private double spineMinimumSimilarity;
	private boolean getSpine;
	private boolean useSelectionOnly;
	private boolean appendPositionToName;

	private List<Roi> circles;
	private List<Roi> spines;
	private List<Roi> spineParts;
	MaxInscribedCircles() {
	}

	/**
	 * Calling the Circle Finder using a Builder Pattern
	 * @param imp the ImagePlus (can be a stack) to process
	 * @return the builder
	 */
	public static Builder builder(ImagePlus imp) {
		return new Builder(imp);
	}

	/**
	 * Perform the circle and spine finding
	 */
	public void process() {

		circles = new ArrayList<>();
		spines = new ArrayList<>();
		spineParts = new ArrayList<>();

		int nSlices = imp.getStackSize();

		// Process stack only if it's not "selectiononly, otherwise process current slice only
		int start = useSelectionOnly ? imp.getCurrentSlice() : 1;
		int end = useSelectionOnly ? imp.getCurrentSlice() : nSlices;
		Roi roi = useSelectionOnly ?imp.getRoi() : null;

		for (int i = start; i <= end; i++) {
			IJ.log("Processing Slice " + i + " of " + nSlices);
			ImagePlus tmpImp = new ImagePlus(imp.getTitle() + " - Slice " + i, imp.getStack().getProcessor(i));
			tmpImp.setRoi(roi);
			List<Roi> circles = MaxInscribedCircles.findCircles(tmpImp, minimumDiameter, useSelectionOnly);

			// Add the position of the stack
			for (Roi r : circles) {
				r.setPosition(i);
				if (appendPositionToName) r.setName(r.getName() + "-P_" + i);
			}

			// Display a message if no circle was found
			if (circles.size() == 0) {
				IJ.log("No circles found, consider decreasing 'Minimum Circle Diameter'.");
			}

			this.circles.addAll(circles);

			// Display a message if no circle was found
			if (getSpine && circles.size() == 1) {
				IJ.log("A single circle was found. Spine cannot be computed, consider decreasing 'Minimum Circle Diameter'.");
			}

			// Only get spine if checkbox is ticked and there is at least 2 circles
			if (getSpine && circles.size() > 1) {
				// Define the parameters
				CirclesBasedSpine sbs = (new CirclesBasedSpine.Settings(tmpImp)).circles(circles).closenessTolerance(spineClosenessTolerance).minSimilarity(spineMinimumSimilarity).showCircles(false).build();
				// Get the spine
				Roi spine = sbs.getSpine();

				// If one is found rename and add it to the Roi Manager
				if (spine != null) {
					spine.setPosition(i);
					if (appendPositionToName) spine.setName(spine.getName() + "-P_" + i);

					// Get the overlay and add it back as well
					Overlay ov = tmpImp.getOverlay();
					for (Roi r : ov) {
						if (appendPositionToName) r.setName(r.getName() + "-P_" + i);
						r.setPosition(i);
						this.spineParts.add(r);
					}

					this.spines.add(spine);

				} else {
					IJ.log("No spine found");
				}
			}
		}
	}

	/**
	 * Get the circles found after process()
	 * @return a list of circles that were found
	 */
	public List<Roi> getCircles() {
		return circles;
	}
	/**
	 * Get the spines found after process()
	 * @return the spine or spines found if it's a stack
	 */
	public List<Roi> getSpines() {
		return spines;
	}
	/**
	 * Get the spine parts found after process()
	 * This is purely for aesthetic reasons
	 * @return the spine or spine parts (colored segments) if it's a stack. It's a spine broken into pieces.
	 */
	public List<Roi> getSpineParts() {
		return spineParts;
	}

	public static List<Roi> findCircles(ImagePlus imp, double minD, boolean isSelectionOnly) {
		IJ.showStatus("Finding Largest Inscribed Circles based on Distance Map...");
		List<Roi> allrois = new ArrayList<>();
		Roi sel = imp.getRoi();

		ImageProcessor ip;
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
			ip.copyBits(iptmp, 2, 2, 3);
			--offsetX;
			--offsetY;
		} else {
			ip = imp.getProcessor().resize(imp.getWidth() * 2);
			offsetX = 0;
			offsetY = 0;
		}

		ImageStack tmpstk = new ImageStack(ip.getWidth(), ip.getHeight());
		tmpstk.addSlice(ip);
		EDT edt = new EDT();
		boolean done = false;

		while (!done) {
			MaximumFinder mf = new MaximumFinder();
			ImageProcessor dist_map_ip = edt.compute(tmpstk).getProcessor();
			Polygon points = mf.getMaxima(dist_map_ip, 1.0D, false);
			ArrayList<Double> hits = getSortedPoints(points, minD, dist_map_ip);
			done = true;

			for (int k = 0; k < hits.size(); ++k) {
				boolean is_draw = true;
				Double p = hits.get(k);
				double r = dist_map_ip.getInterpolatedValue(p.x, p.y);
				ArrayList<Double> neigh = findNeighbors(p, hits, dist_map_ip);
				if (neigh.size() > 1) {

					for (Double pp : neigh) {
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
					ip.fill(circ);
					posx = (p.x - r) / 2.0D;
					posy = (p.y - r) / 2.0D;
					circ = new OvalRoi(posx, posy, r, r);
					circ.setName(String.format("Circle-r_%.3f", r));
					circ.setStrokeWidth(1.0F);
					circ.setLocation(posx + (double) offsetX, posy + (double) offsetY);
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
		ArrayList<Double> hits = new ArrayList<>(points.npoints);
		List<Double> tmpList = new ArrayList<>(points.npoints);

		int ind;
		for (ind = 0; ind < points.npoints; ++ind) {
			double r = ip.getInterpolatedValue( points.xpoints[ind], points.ypoints[ind]);
			if (r > minD) {
				hits.add(new Double( points.xpoints[ind], points.ypoints[ind]));
			}
		}

		hits.sort((p1, p2) -> java.lang.Double.compare(ip.getInterpolatedValue(p2.x, p2.y), ip.getInterpolatedValue(p1.x, p1.y)));
		ind = 0;

		for (int i = 1; i < hits.size(); ++i) {
			Double p = hits.get(i);
			if (ip.getInterpolatedValue(p.x, p.y) < ip.getInterpolatedValue(hits.get(0).x, hits.get(0).y)) {
				ind = i;
				break;
			}
		}

		tmpList.addAll(hits);
		if (ind > 0) {
			tmpList = hits.subList(0, ind);
		}

		return new ArrayList<>(tmpList);
	}

	public static ArrayList<Double> findNeighbors(Double p, ArrayList<Double> hits, ImageProcessor ip) {
		ArrayList<Double> neighbors = new ArrayList<>();
		double r = ip.getInterpolatedValue(p.x, p.y);

		for (Double k : hits) {
			double dist = p.distance(k);
			double r2 = ip.getInterpolatedValue(k.x, k.y);
			if (dist < r + r2) {
				neighbors.add(k);
			}
		}

		return neighbors;
	}

	public static class Builder {
		private final ImagePlus imp;
		private double minimumDiameter = 10;
		private double spineClosenessTolerance = 10;
		private double spineMinimumSimilarity = 0.5;
		private boolean useSelectionOnly = false;
		private boolean getSpine = false;

		private boolean appendPositionToName = true;

		Builder(ImagePlus imp) {
			this.imp = imp;
		}

		/**
		 * Set the minimum diameter in pixels of the circles to be found
		 *
		 * @param minimumDiameter smallest diameter in pixels of circles to be found. Can be very slow if < 10 pixels
		 * @return this builder
		 */
		public Builder minimumDiameter(double minimumDiameter) {
			if (minimumDiameter < 1) throw new IllegalArgumentException("Minimum diameter must be larger than 1 pixel");
			this.minimumDiameter = minimumDiameter;
			return this;
		}

		/**
		 * Use the current selection of the current image for circle finding instead of a mask
		 *
		 * @param useSelectionOnly true to use the current selection instead of a mask
		 * @return this builder
		 */
		public Builder useSelectionOnly(boolean useSelectionOnly) {
			if (this.imp.getRoi() == null) {
				IJ.log("No selection found, using mask instead");
				useSelectionOnly = false;
			}
			this.useSelectionOnly = useSelectionOnly;
			return this;
		}

		/**
		 * Use the current selection of the current image for circle finding instead of a mask
		 *
		 * @param appendPositionToName Whether to append the position of the circle to the name of the circle (default: true)
		 * @return this builder
		 */
		public Builder appendPositionToName(boolean appendPositionToName) {
			this.appendPositionToName = appendPositionToName;
			return this;
		}

		/**
		 * Set the closeness tolerance in pixels of the circles to be found
		 *
		 * @param closenessTolerance The closeness tolerance in pixels of the circles to be found
		 * @return this builder
		 */
		public Builder spineClosenessTolerance(double closenessTolerance) {
			if (closenessTolerance < 1)
				throw new IllegalArgumentException("Closeness tolerance must be larger than 1 pixel");
			this.spineClosenessTolerance = closenessTolerance;
			return this;
		}

		/**
		 * Set the minimum similarity of the circles to be found
		 *
		 * @param minimumSimilarity minimum cosine similarity (0 to 1)
		 * @return this builder
		 */
		public Builder spineMinimumSimilarity(double minimumSimilarity) {
			this.spineMinimumSimilarity = minimumSimilarity;
			return this;
		}

		/**
		 * working from the largest circle, the closeness and similarity values, compute a spine going through the circles
		 *
		 * @param getSpine wether to return a circles based spine
		 * @return this builder
		 */
		public Builder getSpine(boolean getSpine) {
			this.getSpine = getSpine;
			return this;
		}

		public MaxInscribedCircles build() {
			MaxInscribedCircles mic = new MaxInscribedCircles();
			mic.imp = this.imp;
			mic.minimumDiameter = this.minimumDiameter;
			mic.spineClosenessTolerance = this.spineClosenessTolerance;
			mic.spineMinimumSimilarity = this.spineMinimumSimilarity;
			mic.getSpine = this.getSpine;
			mic.useSelectionOnly = this.useSelectionOnly;
			mic.appendPositionToName = this.appendPositionToName;
			return mic;

		}
	}
}
