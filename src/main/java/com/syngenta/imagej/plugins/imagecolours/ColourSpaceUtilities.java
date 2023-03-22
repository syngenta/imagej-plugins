package com.syngenta.imagej.plugins.imagecolours;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Useful colour space utilities.
 *
 * @author $Author$
 * @version $Revision$
 */
public final
class ColourSpaceUtilities
{

    // Colour space ranges.
    public static final double LCH_L_MIN = 0.0;
    public static final double LCH_C_MIN = 0.0;
    public static final double LCH_H_MIN = 0.0;
    public static final double LCH_L_MAX = 100.0;
    public static final double LCH_C_MAX = 140.0;
    public static final double LCH_H_MAX = 360.0;
    public static final double LAB_L_MIN = 0.0;
    public static final double LAB_L_MAX = 100.0;
    public static final double LAB_A_MIN = -110.0;
    public static final double LAB_A_MAX = 110.0;
    public static final double LAB_B_MIN = -110.0;
    public static final double LAB_B_MAX = 110.0;

    private static final float[][] LAB2LCH_T1 = new float[3][3];
    private static final float[][] LAB2LCH_T2 = new float[3][3];

    private static final int TRIG_SEGMENTS = 200;
    private static final float[] ARCCOTAN_D = new float[TRIG_SEGMENTS + 1];
    private static final float[] ARCCOTAN_C = new float[TRIG_SEGMENTS + 1];
    private static final float[] ARCTAN_D = new float[TRIG_SEGMENTS + 1];
    private static final float[] ARCTAN_C = new float[TRIG_SEGMENTS + 1];
    private static final float[] ROOT_D = new float[TRIG_SEGMENTS + 1];
    private static final float[] ROOT_C = new float[TRIG_SEGMENTS + 1];

    private static final int XYZ2LAB_SEGMENTS = 1000;
    private static final float[] XYZ2LAB_B_D = new float[XYZ2LAB_SEGMENTS + 1];
    private static final float[] XYZ2LAB_B_C = new float[XYZ2LAB_SEGMENTS + 1];
    private static final float[] XYZ2LAB_A_D = new float[XYZ2LAB_SEGMENTS + 1];
    private static final float[] XYZ2LAB_A_C = new float[XYZ2LAB_SEGMENTS + 1];
    private static final float[] XYZ2LAB_L_D = new float[XYZ2LAB_SEGMENTS + 1];
    private static final float[] XYZ2LAB_L_C = new float[XYZ2LAB_SEGMENTS + 1];

    private static final float XYZ2LAB_Z = XYZ2LAB_SEGMENTS / 108.883f;
    private static final float XYZ2LAB_Y = XYZ2LAB_SEGMENTS / 100.0f;
    private static final float XYZ2LAB_X = XYZ2LAB_SEGMENTS / 95.047f;

    private static final int NUM_XYZ_ENTRIES = 256;
    private static final float[] RGB2XYZ_BZ = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_GZ = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_RZ = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_BY = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_GY = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_RY = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_BX = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_GX = new float[NUM_XYZ_ENTRIES];
    private static final float[] RGB2XYZ_RX = new float[NUM_XYZ_ENTRIES];

    // Computational constants.
    private static final double ONE_THIRD = 1.0 / 3.0;
    private static final double TWO_THIRDS = 2.0 / 3.0;
    private static final double SIXTEENTHS = 16.0 / 116.0;
    private static final double EXPONENT = 1.0 / 2.4;

    static
    {
        createLookUpTables();
    }

    /**
     * Utilities class - no public constructor.
     */
    private
    ColourSpaceUtilities() {
        // No public access.
    }

    private static
    void createLookUpTables() {

        // Generate f(x) samples and find the output arrays.
        for (int i = 0; i < NUM_XYZ_ENTRIES; i++)
        {

            final double x = (double) i / (NUM_XYZ_ENTRIES - 1);
            final double x1 = (x + 0.055) / 1.055;
            final float x2 = (float) (x > 0.04045 ? StrictMath.pow(x1, 2.4) : x / 12.92);
            RGB2XYZ_RX[i] = 41.24f * x2;
            RGB2XYZ_GX[i] = 35.76f * x2;
            RGB2XYZ_BX[i] = 18.05f * x2;
            RGB2XYZ_RY[i] = 21.26f * x2;
            RGB2XYZ_GY[i] = 71.52f * x2;
            RGB2XYZ_BY[i] = 7.22f * x2;
            RGB2XYZ_RZ[i] = 1.93f * x2;
            RGB2XYZ_GZ[i] = 11.92f * x2;
            RGB2XYZ_BZ[i] = 95.05f * x2;
        }

        // Find the c, d, and output arrays: g=c*w + d.
        for (int seg = 0; seg <= XYZ2LAB_SEGMENTS; seg++)
        {

            final double f0 = getFx(2 * seg);    // Use doubles until we get
            final double f1 = getFx(2 * seg + 1);// to c and d, for accuracy.
            final double f2 = getFx(2 * seg + 2);
            final double de = f0 + 2.0 * f1 + f2;
            final double r = 4.0 * f1 * (f2 - f0) / de;
            final float d = (float) (4.0 * f0 * f1 / de);
            XYZ2LAB_L_C[seg] = 116.0f * (float) r;
            XYZ2LAB_L_D[seg] = 116.0f * d - 16.0f; //L arrays
            XYZ2LAB_A_C[seg] = 500.0f * (float) r;
            XYZ2LAB_A_D[seg] = 500.0f * d;    //a arrays
            XYZ2LAB_B_C[seg] = 200.0f * (float) r;
            XYZ2LAB_B_D[seg] = 200.0f * d;    //b arrays
        }

        // LAB to LCH look up table generation.
        for (int seg = 0; seg <= TRIG_SEGMENTS; seg++)
        {

            // Square root function. Doubles used for accuracy at start.
            double f00 = getRoot(2 * seg);
            double f11 = getRoot(2 * seg + 1);
            double f22 = getRoot(2 * seg + 2);
            double de = f00 + 2.0 * f11 + f22;
            ROOT_C[seg] = (float) (4.0 * f11 * (f22 - f00) / de);
            ROOT_D[seg] = (float) (4.0 * f00 * f11 / de);

            // Arctan function.
            f00 = getArctan(2 * seg);
            f11 = getArctan(2 * seg + 1);
            f22 = getArctan(2 * seg + 2);
            de = f00 + 2.0 * f11 + f22;
            ARCTAN_C[seg] = (float) (4.0 * f11 * (f22 - f00) / de);
            ARCTAN_D[seg] = (float) (4.0 * f00 * f11 / de);

            // Arccot function.
            f00 = 90.0 - f00;
            f11 = 90.0 - f11;
            f22 = 90.0 - f22;
            de = f00 + 2.0 * f11 + f22;
            ARCCOTAN_C[seg] = (float) (4.0 * f11 * (f22 - f00) / de);
            ARCCOTAN_D[seg] = (float) (4.0 * f00 * f11 / de);
        }

        // Tables for angle H 360 degree calculation.
        LAB2LCH_T1[0][0] = 180.0f;
        LAB2LCH_T1[0][1] = 180.0f;
        LAB2LCH_T1[0][2] = 180.0f;
        LAB2LCH_T1[1][0] = 270.0f;
        LAB2LCH_T1[1][1] = 0.0f;
        LAB2LCH_T1[1][2] = 90.0f;
        LAB2LCH_T1[2][0] = 360.0f;
        LAB2LCH_T1[2][1] = 360.0f;
        LAB2LCH_T1[2][2] = 0.0f;

        LAB2LCH_T2[0][0] = 1.0f;
        LAB2LCH_T2[0][1] = 0.0f;
        LAB2LCH_T2[0][2] = -1.0f;
        LAB2LCH_T2[1][0] = 0.0f;
        LAB2LCH_T2[1][1] = 0.0f;
        LAB2LCH_T2[1][2] = 0.0f;
        LAB2LCH_T2[2][0] = -1.0f;
        LAB2LCH_T2[2][1] = 0.0f;
        LAB2LCH_T2[2][2] = 1.0f;
    }

    private static
    double getArctan(final int i) {

        final double x = i / (2.0 * TRIG_SEGMENTS);
        return StrictMath.atan(x) * 180.0 / Math.PI;
    }

    private static
    double getRoot(final int i) {

        final double x = i / (2.0 * TRIG_SEGMENTS);
        return Math.sqrt(1.0 + x * x);
    }

    private static
    double getFx(final int i) {

        final double x = i / (2.0 * XYZ2LAB_SEGMENTS);
        return x > 0.008856 ? StrictMath.pow(x, ONE_THIRD) : 7.787 * x + SIXTEENTHS;
    }

    /**
     * Colour space conversion from RGB (0..255) to HSV (0..1).
     *
     * @param rgb the red, green, blue triple.
     * @return the HSV values in a three element float array (0: H; 1: XYZ2LAB_SEGMENTS; 2: V).
     */
    public static
    double[] convertRgb2Hsv(final int... rgb) {

        final double r = rgb[0] / 255.0;    // R 0..1
        final double g = rgb[1] / 255.0;    // G 0..1
        final double b = rgb[2] / 255.0;    // B 0..1

        final double rgbMin = Math.min(Math.min(r, g), b); // Min. value of RGB
        final double rgbMax = Math.max(Math.max(r, g), b); // Max. value of RGB
        final double rgbDelta = rgbMax - rgbMin;           // Delta RGB value

        double hue = 0.0;
        final double saturation;
        if (rgbDelta == 0.0)
        {                  // This is a grey, no chroma...
            hue = 0.0;                          // HSV results = 0 or 1
            saturation = 0.0;
        }
        else
        {                                // Chromatic data...
            saturation = rgbDelta / rgbMax;
            final double rDelta = ((rgbMax - r) / 6.0 + rgbDelta / 2.0) / rgbDelta;
            final double gDelta = ((rgbMax - g) / 6.0 + rgbDelta / 2.0) / rgbDelta;
            final double bDelta = ((rgbMax - b) / 6.0 + rgbDelta / 2.0) / rgbDelta;

            if (r == rgbMax)
            {
                hue = bDelta - gDelta;
            }
            else if (g == rgbMax)
            {
                hue = ONE_THIRD + rDelta - bDelta;
            }
            else if (b == rgbMax)
            {
                hue = TWO_THIRDS + gDelta - rDelta;
            }

            if (hue < 0.0)
            {
                hue += 1.0;
            }
            else if (hue > 1.0)
            {
                hue -= 1.0;
            }
        }

        return new double[]{hue, saturation, rgbMax};
    }

    /**
     * Colour space conversion from RGB (0.255) to HSB (0..255).
     *
     * @param rgb the red, green blue triple.
     * @return the HSB triple.
     */
    public static
    int[] convertRgb2Hsb(final int... rgb) {

        final double[] hsv = convertRgb2Hsv(rgb);
        return new int[]{(int) Math.round(hsv[0] * 255.0), (int) Math.round(hsv[1] * 255.0),
                (int) Math.round(hsv[2] * 255.0)};
    }

    /**
     * Colour space conversion from RGB (0.255) to HSB (0..255) which can be called by a macro that passes strings
     *
     * @param red   the value as a string.
     * @param green the value as a string.
     * @param blue  the value as a string.
     * @return the HSB triple.
     */
    public static
    String convertRgb2Hsb4Macro(final String red, final String green, final String blue) {

        final double[] hsv = convertRgb2Hsv(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        return Integer.toString((int) Math.round(hsv[0] * 255.0)) + '\t' +
               Integer.toString((int) Math.round(hsv[1] * 255.0)) + '\t' +
               Integer.toString((int) Math.round(hsv[2] * 255.0));
    }

    /**
     * Colour space conversion from HSB (0..255) to RGB (0..255)
     *
     * @param hsb hue, saturation, brightness triple.
     * @return the RGB triple.
     */
    public static
    int[] convertHsb2Rgb(final float... hsb) {

        final double hue = hsb[0];
        final double sat = hsb[1];
        final double bright = hsb[2];
        final double red;
        final double green;
        final double blue;

        if (sat == 0.0)
        {
            red = green = blue = bright / 255.0;
        }
        else
        {
            // Convert to 0..1 range.
            final double hueVal = hue / 255.0;
            final double satVal = sat / 255.0;
            final double brightVal = bright / 255.0;

            double varH = hueVal * 6.0;
            if (varH == 6.0)
            {
                varH = 0.0;      // H must be < 1.
            }
            final double varI = Math.floor(varH);
            final double var1 = brightVal * (1.0 - satVal);
            final double var2 = brightVal * (1.0 - satVal * (varH - varI));
            final double var3 = brightVal * (1.0 - satVal * (1.0 - varH + varI));
            if (varI == 0.0)
            {
                red = brightVal;
                green = var3;
                blue = var1;
            }
            else if (varI == 1.0)
            {
                red = var2;
                green = brightVal;
                blue = var1;
            }
            else if (varI == 2.0)
            {
                red = var1;
                green = brightVal;
                blue = var3;
            }
            else if (varI == 3.0)
            {
                red = var1;
                green = var2;
                blue = brightVal;
            }
            else if (varI == 4.0)
            {
                red = var3;
                green = var1;
                blue = brightVal;
            }
            else
            {
                red = brightVal;
                green = var1;
                blue = var2;
            }
        }

        // Return RGB triple (0..255).
        return new int[]{(int) Math.round(red * 255.0), (int) Math.round(green * 255.0),
                (int) Math.round(blue * 255.0)};
    }

    /**
     * Colour space conversion from HSB (0..255) to RGB (0..255).
     *
     * @param hue    is the  hue value.
     * @param sat    is the  saturation value.
     * @param bright is the  brightness value.
     * @return the RGB triple.
     */
    public static
    String convertHsb2Rgb4Macro(final String hue, final String sat, final String bright) {
        final int[] rgb = convertHsb2Rgb(Float.parseFloat(hue), Float.parseFloat(sat), Float.parseFloat(bright));
        return Integer.toString(rgb[0]) + '\t' + Integer.toString(rgb[1]) + '\t' + Integer.toString(rgb[2]);
    }

    /**
     * Colour space conversion from RGB to XYZ.
     *
     * @param rgb the red, green, blue triple.
     * @return the XYZ triple.
     */
    public static
    float[] convertRgb2Xyz(final int... rgb) {

        final int red = rgb[0];
        final int green = rgb[1];
        final int blue = rgb[2];
        return new float[]{RGB2XYZ_RX[red] + RGB2XYZ_GX[green] + RGB2XYZ_BX[blue],
                RGB2XYZ_RY[red] + RGB2XYZ_GY[green] + RGB2XYZ_BY[blue],
                RGB2XYZ_RZ[red] + RGB2XYZ_GZ[green] + RGB2XYZ_BZ[blue]};
    }

    /**
     * Colour space conversion from RGB to Lab for macros.
     *
     * @param red   the value as a string.
     * @param green the value as a string.
     * @param blue  the value as a string.
     * @return the XYZ triple.
     */
    public static
    String convertRgb2Lab4Macro(final String red, final String green, final String blue) {
        final float[] lab =
                convertXyz2Lab(convertRgb2Xyz(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue)));
        return Double.toString(lab[0]) + '\t' + Double.toString(lab[1]) + '\t' + Double.toString(lab[2]);
    }


    /**
     * Colour space conversion from XYZ to LAB.
     *
     * @param xyz the XYZ triple.
     * @return the LAB triple.
     */
    public static
    float[] convertXyz2Lab(final float... xyz) {

        // Find s and w.
        final float ux = XYZ2LAB_X * xyz[0];
        final float uy = XYZ2LAB_Y * xyz[1];
        final float uz = XYZ2LAB_Z * xyz[2];
        final int sx = (int) ux;
        final int sy = (int) uy;
        final int sz = (int) uz;
        final float wx = ux - sx;
        final float wy = uy - sy;
        final float wz = uz - sz;

        // Main calculation.
        final float L = XYZ2LAB_L_C[sy] * wy + XYZ2LAB_L_D[sy];
        final float A = XYZ2LAB_A_C[sx] * wx + XYZ2LAB_A_D[sx] - XYZ2LAB_A_C[sy] * wy - XYZ2LAB_A_D[sy];
        final float B = XYZ2LAB_B_C[sy] * wy + XYZ2LAB_B_D[sy] - XYZ2LAB_B_C[sz] * wz - XYZ2LAB_B_D[sz];

        return new float[]{L, A, B};
    }

    /**
     * Colour space conversion from LAB to LCH.
     *
     * @param lab the LAB triple.
     * @return the converted LCH triple.
     */
    public static
    float[] convertLab2Lch(final float... lab) {

        //Inputs
        final float b = lab[2];
        final float a = lab[1];

        final float H;
        final float C;

        // Main calculation:
        final float am = Math.abs(a);
        final float bm = Math.abs(b);
        final int sa = (int) Math.signum(a) + 1;
        final int sb = (int) Math.signum(b) + 1;
        if (am == 0.0f && bm == 0.0f)
        {
            C = 0.0f;
            H = 360.0f;
        }
        else if (am > bm)
        {
            final float u = TRIG_SEGMENTS * bm / am;
            final int s = Math.round(u);
            final float w = u - s;
            C = am * (ROOT_C[s] * w + ROOT_D[s]);
            H = LAB2LCH_T1[sa][sb] + LAB2LCH_T2[sa][sb] * (ARCTAN_C[s] * w + ARCTAN_D[s]);
        }
        else
        {
            final float u = TRIG_SEGMENTS * am / bm;
            final int s = Math.round(u);
            final float w = u - s;
            C = bm * (ROOT_C[s] * w + ROOT_D[s]);
            H = LAB2LCH_T1[sa][sb] + LAB2LCH_T2[sa][sb] * (ARCCOTAN_C[s] * w + ARCCOTAN_D[s]);
        }

        return new float[]{lab[0], C, H};
    }

    /**
     * Colour space conversion from XYZ to RGB.
     *
     * @param xyz the XYZ triple.
     * @return the red, green, blue triple.
     */
    public static
    int[] convertXyz2Rgb(final float... xyz) {

        final float x = xyz[0] / 100.0f;       // X from 0 to  95.047.
        final float y = xyz[1] / 100.0f;       // Y from 0 to 100.000
        final float z = xyz[2] / 100.0f;       // Z from 0 to 108.883

        double r = x * 3.2406f + y * -1.5372f + z * -0.4986f;
        double g = x * -0.9689f + y * 1.8758f + z * 0.0415f;
        double b = x * 0.0557f + y * -0.2040f + z * 1.0570f;

        r = r > 0.0031308f ? 1.055f * StrictMath.pow(r, EXPONENT) - 0.055f : 12.92f * r;
        g = g > 0.0031308f ? 1.055f * StrictMath.pow(g, EXPONENT) - 0.055f : 12.92f * g;
        b = b > 0.0031308f ? 1.055f * StrictMath.pow(b, EXPONENT) - 0.055f : 12.92f * b;

        return new int[]{(int) Math.round(r * 255.0), (int) Math.round(g * 255.0), (int) Math.round(b * 255.0)};
    }

    /**
     * Colour space conversion from LAB to XYZ.
     *
     * @param lab the LAB triple.
     * @return the XYZ triple.
     */
    public static
    float[] convertLab2Xyz(final float... lab) {

        final double x = (lab[0] + 16.0) / 116.0;
        final double y = lab[1] / 500.0 + x;
        final double z = x - lab[2] / 200.0;

        return new float[]{(float) (95.047 * lab2xyz(y)), (float) (100.0 * lab2xyz(x)), (float) (108.883 * lab2xyz(z))};
    }

    private static
    double lab2xyz(final double x) {
        final double x3 = StrictMath.pow(x, 3);
        return x3 > 0.008856 ? x3 : (x - SIXTEENTHS) / 7.787;
    }

    /**
     * Colour space conversion from LCH to LAB
     *
     * @param lch the LCH triple.
     * @return the LAB triple.
     */
    public static
    float[] convertLch2Lab(final float... lch) {

        final double theta = lch[2] * Math.PI / 180.0;
        return new float[]{lch[0], (float) (StrictMath.cos(theta) * lch[1]), (float) (StrictMath.sin(theta) * lch[1])};
    }

    /**
     * Gets the sensitivity (greatest absolute difference between RGB channels).
     *
     * @param red   red channel.
     * @param green green channel.
     * @param blue  blue channel.
     * @return the sensitivity value.
     */
    public static
    int getRgbSensitivity(final int red, final int green, final int blue) {

        int rgbDiff = 0;
        if (Math.abs(red - green) > rgbDiff)
        {
            rgbDiff = Math.abs(red - green);
        }
        if (Math.abs(red - blue) > rgbDiff)
        {
            rgbDiff = Math.abs(red - blue);
        }
        if (Math.abs(green - blue) > rgbDiff)
        {
            rgbDiff = Math.abs(green - blue);
        }
        return rgbDiff;
    }
}