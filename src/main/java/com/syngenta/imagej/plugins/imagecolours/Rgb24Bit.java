package com.syngenta.imagej.plugins.imagecolours;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * 24-bit RGB colour utilities.
 * <p/>
 * $Author$
 * $Revision$
 */
public class Rgb24Bit {

    // Byte masks.
    private static final int R_BYTE_MASK = 0xff0000;
    private static final int G_BYTE_MASK = 0xff00;
    private static final int B_BYTE_MASK = 0xff;

    // Standard colours.
    public static final int RED = pack(255, 0, 0);
    public static final int GREEN = pack(0, 255, 0);
    public static final int BLUE = pack(0, 0, 255);
    public static final int BLACK = pack(0, 0, 0);
    public static final int WHITE = pack(255, 255, 255);
    public static final int GREY = pack(128, 128, 128);
    public static final int LIGHT_GREY = pack(192, 192, 192);
    public static final int ORANGE = pack(255, 119, 0);
    public static final int BROWN = pack(128, 60, 0);
    public static final int LIGHT_YELLOW = pack(242, 255, 0);
    public static final int DARK_YELLOW = pack(121, 128, 0);
    public static final int LIGHT_YELLOW_GREEN = pack(127, 255, 0);
    public static final int DARK_YELLOW_GREEN = pack(63, 128, 0);
    public static final int LIGHT_GREEN = pack(0, 255, 67);
    public static final int DARK_GREEN = pack(0, 128, 33);
    public static final int AQUA = pack(0, 255, 216);
    public static final int LIGHT_BLUE = pack(46, 0, 255);
    public static final int DARK_BLUE = pack(23, 0, 128);
    public static final int MAGENTA = pack(255, 0, 255);

    /**
     * Pack red, green, blue 8-bit values into a 24-bit value.
     *
     * @param vals the triple to pack.
     * @return the packed value.
     */
    public static int pack(final int ... vals) {
        return ((vals[0] & B_BYTE_MASK) << 16) + ((vals[1] & B_BYTE_MASK) << 8) + (vals[2] & B_BYTE_MASK);
    }

    /**
     * Unpack red, green, blue 8-bit values into a 24-bit value.
     *
     * @param val the 24-bit value to unpack.
     * @return the unpacked values as a triple array.
     */
    public static int[] unpack(final int val) {
        return new int[]{(val & R_BYTE_MASK) >> 16, (val & G_BYTE_MASK) >> 8, val & B_BYTE_MASK};
    }

    /**
     * Convert to a binary image with ByteProcessor by thresholding.
     *
     * @param image the image to convert.
     * @param level threshold level.
     */
    public static void binarizeImage(final ImagePlus image, int level) {

        // Convert type byte image and threshold.
        final ImageProcessor processor = image.getProcessor().convertToByte(false);
        processor.threshold(level);
        image.setProcessor(processor);
    }
}
