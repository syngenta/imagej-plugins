package com.syngenta.imagej.plugins.imagecolours;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ColorProcessor;
import org.junit.Assert;

import static ij.ImagePlus.*;

/**
 * Utility class of useful test methods.
 *
 * @author $Author$
 * @version $Revision$
 */
public class TestUtilities {
    /**
     * Compare two images.
     *
     * @param expectedImage expected image.
     * @param actualImage   actual image.
     */
    public static void assertEqualImages(final ImagePlus expectedImage, final ImagePlus actualImage) {

        // Open test image and make it current image.
        Assert.assertEquals("Widths differ", expectedImage.getWidth(), actualImage.getWidth());
        Assert.assertEquals("Heights differ", expectedImage.getHeight(), actualImage.getHeight());
        Assert.assertEquals("Types differ", expectedImage.getType(), actualImage.getType());
        Assert.assertEquals("Channel counts differ", expectedImage.getNChannels(), actualImage.getNChannels());

        // Get image processors.
        switch (actualImage.getType()) {
            case GRAY8:
                Assert.assertArrayEquals("Pixels differ", (byte[]) expectedImage.getProcessor().getPixels(),
                        (byte[]) actualImage.getProcessor().getPixels());
                break;
            case GRAY32:
                Assert.assertArrayEquals("Pixels differ", (float[]) expectedImage.getProcessor().getPixels(),
                        (float[]) actualImage.getProcessor().getPixels(), 0.0f);
                break;
            case COLOR_RGB:
                final ColorProcessor expectedProcessor = (ColorProcessor) expectedImage.getProcessor();
                final ColorProcessor actualProcessor = (ColorProcessor) actualImage.getProcessor();
                Assert.assertArrayEquals("Red pixels differ", expectedProcessor.getChannel(1),
                        actualProcessor.getChannel(1));
                Assert.assertArrayEquals("Green pixels differ", expectedProcessor.getChannel(2),
                        actualProcessor.getChannel(2));
                Assert.assertArrayEquals("Blue pixels differ", expectedProcessor.getChannel(3),
                        actualProcessor.getChannel(3));
                // Assert.assertArrayEquals("Alpha pixels differ", expectedProcessor.getChannel(4),
                // actualProcessor.getChannel(4));
                break;
            default:
                Assert.fail("Invalid image type: " + actualImage.getType());
        }
    }

    /**
     * Run a filter and compare actual with expected results.
     *
     * @param filter            the filter
     * @param inputImagePath    path to input image file.
     * @param expectedImagePath path to expected image file.
     */
    public static void testFilter(final PlugInFilter filter, final String inputImagePath, final String expectedImagePath) {

        // Open test image and make it current image.
        final ImagePlus inputImage = new Opener().openImage(inputImagePath);
        WindowManager.setTempCurrentImage(inputImage);

        // Create and run the filter.
        new PlugInFilterRunner(filter, "", "");

        // Compare to expected.
        assertEqualImages(new Opener().openImage(expectedImagePath), inputImage);
    }
}
