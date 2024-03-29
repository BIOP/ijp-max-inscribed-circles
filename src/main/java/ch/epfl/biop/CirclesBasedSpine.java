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
package ch.epfl.biop;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.geom.Point2D;

public class CirclesBasedSpine {
    // create builder pattern
    private Overlay ov = new Overlay();
    private ImagePlus imp;
    private int minCircleDiameter;
    private PolygonRoi spine = null;
    private Boolean isShowCircles;
    private double minSimilarity = 0.5D;
    private double closenessTolerance = 10.0D;
    private List<Roi> circles = null;

    public CirclesBasedSpine(CirclesBasedSpine.Settings settings) {
        this.imp = settings.image;
        this.minCircleDiameter = settings.minCircleDiameter;
        this.isShowCircles = settings.isShowCircles;
        this.minSimilarity = settings.minSimilarity;
        this.closenessTolerance = settings.closenessTolerance;
        this.circles = settings.circles;
    }

    public PolygonRoi getSpine() {
        if (this.circles.isEmpty()) {
            // If there is no circles, they are computed
            this.circles = MaxInscribedCircles.findCircles(this.imp, this.minCircleDiameter, false);
        }

        Roi circle;
        if (this.isShowCircles) {
            // If isShowCircles they are added to this.ov
            Iterator<Roi> it = this.circles.iterator();

            while(it.hasNext()) {
                circle = it.next();
                this.ov.add(circle);
            }
        }

        // The first circle is the largest
        circle = this.circles.get(0);

        // Do not compute spine if there is no adjacent circle
        List<Roi> adjCircles = this.getAdjacentCircles(this.circles, circle, imp);
        if (adjCircles.size() < 1) {
            IJ.log("Error: No Adjacent Circles found. Consider decreasing 'Minimum Circle Diameter' or increasing 'Closeness Tolerance'");
            return null;
        }

        // Get the largest circle adjacent to circle
        Roi circleB = Collections.max(adjCircles, Comparator.comparing((c) -> c.getFloatWidth() ));

        // A and B are the centroids or circle and circleB
        Point2D pointA = this.getCentroid(circle);
        Point2D pointB = this.getCentroid(circleB);
        // Add a line to this.ov which link the 2 circles
        this.ov.add(this.makeLine(circle, circleB, new Color(128, 255, 128)));
        // Iterate the spine (from circle (excluded) using vector B->A)
        List<Point2D> spineB = this.iterateSpine(this.circles, circleB, circle);
        // Reorder the spine to get extremity -> B -> A
        Collections.reverse(spineB);
        spineB.add(pointA);
        spineB.add(pointB);
        // Iterate the spine (from circleB excluded using A->B)
        // and add it to spineB
        spineB.addAll(this.iterateSpine(this.circles, circle, circleB));
        float[] xPoints = this.toFloatArray(spineB.stream().mapToDouble(m -> m.getX()).toArray());
        float[] yPoints = this.toFloatArray(spineB.stream().mapToDouble(m -> m.getY()).toArray());
        // Create a polygon and set name "Spine"
        this.spine = new PolygonRoi(xPoints, yPoints, 6);
        int pos = circle.getPosition();
        this.spine.setName("Spine");
        return this.spine;
    }

    private Point2D getCentroid(Roi circle) {
        double[] c = circle.getContourCentroid();
        Point2D centroid = new Point2D.Double(c[0], c[1]);
        return centroid;
    }

    float[] toFloatArray(double[] arr) {
        if (arr == null) {
            return null;
        } else {
            int n = arr.length;
            float[] ret = new float[n];

            for(int i = 0; i < n; ++i) {
                ret[i] = (float)arr[i];
            }

            return ret;
        }
    }

    List<Point2D> iterateSpine(List<Roi> circles, Roi circleA, Roi circleB) {
        Boolean done = false;
        ArrayList<Point2D> spinePoints = new ArrayList<Point2D>();

        while(!done) {
            circles.remove(circleA);
            // vectorA is A->B
            Point2D vectorA = this.getVector(circleA, circleB);
            Roi finalCircleB = circleB;
            // Find a circle adjacent to circleB which is not A
            // Which is compatible with similarity
            List<Roi> theCircles = (List<Roi>)this.getAdjacentCircles(circles, circleB, imp).stream().filter((Roi c) -> {
                Point2D vectorB = this.getVector(finalCircleB, c);
                return this.similarity(vectorA, vectorB) > this.minSimilarity;
            }).collect(Collectors.toList());
            if (!theCircles.isEmpty()) {
                // put in circleC the largest
                Roi circleC = (Roi) Collections.max(theCircles, Comparator.comparing((Roi c) -> c.getFloatWidth()));
                // Add to this.ov the line which link circleB to circleC
                Line line = this.makeLine(circleB, circleC, new Color(128, 255, 128));
                this.ov.add(line);
                // Add to the spine circleC center
                spinePoints.add(this.getCentroid(circleC));
                circles.remove(circleB);
                // Go one with B-C instead of A-B
                circleA = circleB;
                circleB = circleC;
            } else {
                // No more compatible circle were found
                done = true;
                // Make a line to go to the extremity of mask
                Line line = this.makeEndLine(circleB, vectorA);
                // add this line to this.ov
                this.ov.add(line);
                // add the extremity point to the spinePoints
                spinePoints.add(new Point2D.Double((double)line.getFloatPolygon(0).xpoints[1], (double)line.getFloatPolygon(0).ypoints[1]));
            }
        }
        // set this.ov as overlay
        this.imp.setOverlay(this.ov);
        return spinePoints;
    }

    ResultsTable getSpineResults() {
        ResultsTable rt = ResultsTable.getResultsTable() == null ? new ResultsTable() : ResultsTable.getResultsTable();
        double length = this.spine.getLength();
        double width = ((Roi)this.circles.get(0)).getFloatWidth();
        rt.incrementCounter();
        rt.addLabel(this.imp.getTitle());
        rt.addValue("Length", length);
        rt.addValue("Width", this.imp.getCalibration().getX(width));
        rt.show("Results");
        return rt;
    }

    Roi addToOverlay(Roi roi) {
        Roi nRoi = (Roi)roi.clone();
        nRoi.setStrokeColor(new Color(255, 0, 0));
        nRoi.setStrokeWidth(3.0F);
        return nRoi;
    }

    List<Roi> getAdjacentCircles(List<Roi> circles, Roi circle, ImagePlus imp) {
        double r0 = circle.getFloatWidth() / 2.0D;
        Point2D c0 = this.getCentroid(circle);
        List<Roi> adjacent = (List<Roi>)circles.stream().filter((c) -> {
            double r1 = c.getFloatWidth() / 2.0D;
            Point2D c1 = this.getCentroid(c);
            return c1.distance(c0) < r1 + r0 + this.closenessTolerance && c1.distance(c0) > 0 && this.allLineInMask(c0, c1, imp);
        }).collect(Collectors.toList());
        return adjacent;
    }

    Point2D getVector(Roi circleA, Roi circleB) {
        Point2D A = this.getCentroid(circleA);
        Point2D B = this.getCentroid(circleB);

        Point2D.Double C = new Point2D.Double(B.getX() - A.getX(), B.getY() - A.getY());
        return C;
    }

    Line makeLine(Roi circleA, Roi circleB, Color color) {
        Point2D ca = this.getCentroid(circleA);
        Point2D cb = this.getCentroid(circleB);
        Line line = new Line(ca.getX(), ca.getY(), cb.getX(), cb.getY());
        line.setStrokeColor(color);
        line.setStrokeWidth(2.0F);
        line.setPosition(circleA.getPosition());
        line.setName("SpinePart");
        return line;
    }

    Line makeEndLine(Roi circle, Point2D vector) {
        Point2D c1 = this.getCentroid(circle);
        double r1 = circle.getFloatWidth() / 2.0D;

        // magnitude is the length of the vector:
        double magnitude = vector.distance(0,0);

        // c2 is 1 pixel before the extremity of the circle in the direction of vector
        Point2D c2 = new Point2D.Double(c1.getX() + (r1 - 1.0D) * vector.getX() / magnitude, c1.getY() + (r1 - 1.0D) * vector.getY() / magnitude );
        // c2temp is the extremity of the circle in the direction of vector
        Point2D c2temp = new Point2D.Double(c1.getX() + r1 * vector.getX() / magnitude, c1.getY() + r1 * vector.getY() / magnitude );


        // check if the pixel close to c2temp is inMask
        Boolean inMask = this.imp.getProcessor().getf((int)Math.round(c2temp.getX()), (int)Math.round(c2temp.getY())) == 255.0F;
        // while it is inMask increase by one pixel in vector direction
        for(int i = 1; inMask; ++i) {
            c2 = c2temp;
            c2temp = new Point2D.Double(c1.getX() + ( r1 + (double) i ) * vector.getX() / magnitude, c1.getY() + (r1 +(double) i) * vector.getY() / magnitude );
            inMask = this.imp.getProcessor().getf((int)Math.round(c2temp.getX()), (int)Math.round(c2temp.getY())) == 255.0F;
        }
        // At this step the pixel close to c2 is in mask but c2temp is not.
        // create a circle c with centroid c2 radius 10
        Roi c = new OvalRoi((int)Math.round(c2.getX()) - 10, (int)Math.round(c2.getY()) - 10, 20, 20);
        // return a line between circle and c
        return this.makeLine(circle, c, new Color(255, 0, 0));
    }

    double similarity(Point2D p1, Point2D p2) {
        return (p1.getX() * p2.getX() + p1.getY() * p2.getY()) / (Math.sqrt(p1.getX() * p1.getX() + p1.getY() * p1.getY()) * Math.sqrt(p2.getX() * p2.getX() + p2.getY() * p2.getY()));
    }

    boolean allLineInMask(Point2D c0, Point2D c1, ImagePlus imp) {
        double[] values = imp.getProcessor().getLine(c0.getX(), c0.getY(), c1.getX(), c1.getY());
        for (int i = 0; i < values.length; ++i) {
            if (values[i] != 255.0F) {
                return false;
            }
        }
        return true;
    }

    public static class Settings {
        private final ImagePlus image;
        private int minCircleDiameter = 10;
        private Boolean isShowCircles = false;
        private double minSimilarity = 0.5D;
        private double closenessTolerance = 10.0D;
        private List<Roi> circles = new ArrayList<Roi>();

        public Settings(ImagePlus image) {
            this.image = image;
        }

        public CirclesBasedSpine.Settings minCircleDiameter(int circleD) {
            this.minCircleDiameter = circleD;
            return this;
        }

        public CirclesBasedSpine.Settings showCircles(Boolean showCircles) {
            this.isShowCircles = showCircles;
            return this;
        }

        public CirclesBasedSpine.Settings minSimilarity(double similarity) {
            this.minSimilarity = similarity;
            return this;
        }

        public CirclesBasedSpine.Settings closenessTolerance(double tol) {
            this.closenessTolerance = tol;
            return this;
        }

        public CirclesBasedSpine.Settings circles(List<Roi> circles) {
            this.circles = circles;
            return this;
        }

        public CirclesBasedSpine build() {
            return new CirclesBasedSpine(this);
        }
    }
}

