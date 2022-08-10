import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import net.imagej.ImageJ;
import org.scijava.plugin.Parameter;

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


