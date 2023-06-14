package com.syngenta.imagej.plugins.imagecolours;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.filter.PlugInFilterRunner;


public class test_hue_colours {



    public static void main(String[] args) {

        // open up image to filter
        ImagePlus imp = IJ.openImage("Your path");

        // set hue colours filter parameters
        HueColoursFilter HC = new HueColoursFilter(240, 12, 4, 0.5, 0.05,
                true, true, true, true, true, true,
                true, true, true, true,
                true, true, true, true, true,
                true, false, false, false, true);

        // run filter on target image
        setImageForFilter(imp);
        new PlugInFilterRunner(HC, "", "");

        // save out filtered image if required
        IJ.saveAs(imp, "Tiff", "your save path");

    }

    public static void setImageForFilter(ImagePlus img) {
        WindowManager.setTempCurrentImage(img);
    }

}
