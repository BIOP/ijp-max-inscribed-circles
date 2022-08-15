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
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import net.imagej.ImageJ;

public class Main {

    public static void main( String[] args ) {
        ImageJ ij = new ImageJ(  );
        ij.ui().showUI();

        ImagePlus imp = IJ.createImage( "Untitled", "8-bit black", 256, 256, 1 );
        imp.setRoi( 45, 59, 70, 88 );
        IJ.setForegroundColor( 255, 255, 255 );
        IJ.run( imp, "Fill", "slice" );
        //IJ.setTool("oval");
        imp.setRoi( new OvalRoi( 156, 68, 54, 95 ) );
        IJ.run( imp, "Fill", "slice" );

        imp.show( );
    }

}


