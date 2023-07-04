package com.syngenta.imagej.plugins.imagecolours;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.filter.PlugInFilterRunner;

/**
 * An example of how to run apply the HueColoursFilter programmatically.
 */
public class HueColoursExample {

    public static void main(String... args) {

        // Process command-line args.
        if (args.length<2){
            System.err.println("usage: HueColoursExample <input_image> <output_image>");
            System.exit(-1);
        }
        final String inputImageFilename = args[0];
        final String outputImageFilename = args[1];

        // Open the input image and make it the current image.
        final ImagePlus image = IJ.openImage(inputImageFilename);
        WindowManager.setTempCurrentImage(image);

        // Create and run the filter.
        final HueColoursFilter filter = new HueColoursFilter(240, 12, 4, 0.5, 0.05,
                true, true, true, true, true, true,
                true, true, true, true,
                true, true, true, true, true,
                true, false, false, false, true);
        new PlugInFilterRunner(filter, "", "");

        // Save as the output image.
        IJ.saveAs(image, "tif", outputImageFilename);
    }
}
