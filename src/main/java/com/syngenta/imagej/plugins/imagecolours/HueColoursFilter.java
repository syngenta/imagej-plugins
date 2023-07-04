package com.syngenta.imagej.plugins.imagecolours;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.regex.Pattern;

/**
 * An ImageJ plug-in that performs colour filtering by hue.
 */
public class HueColoursFilter implements ExtendedPlugInFilter, DialogListener {

    // About message.
    private static final String ABOUT_HTML =
            "<html><center><h1>Hue Colour Filter</h1><h2>v1.0.1</h2><h3>Credits</h3>Rob Lind (rob.lin@syngenta.com)" +
                    "<br>Chris Pudney (chris.pudney@syngenta.com)<br>Use this filter to help segment images using hue and " +
                    "saturation.<br>It uses both RGB space (to select blacks, greys and whites)<br>and HSB colour space to " +
                    "classify colours by hue. </center></html>";

    // Parameter names and defaults.
    private static final String WHITE_MIN_PARAM_NAME = "White_min";
    private static final int WHITE_MIN_PARAM_DEFAULT = 200;

    private static final String BLACK_MAX_PARAM_NAME = "Black_max";
    private static final int BLACK_MAX_PARAM_DEFAULT = 10;

    private static final String GREY_TOLERANCE_PARAM_NAME = "Tolerance for black/white/grey RGB values";
    private static final int GREY_TOLERANCE_PARAM_DEFAULT = 10;

    private static final String LIGHT_DARK_PARAM_NAME = "Light_Dark_Threshold";
    private static final double LIGHT_DARK_PARAM_DEFAULT = 0.5;

    private static final String SATURATION_PARAM_NAME = "Saturation_min";
    private static final double SATURATION_PARAM_DEFAULT = 0.5;

    private static final String BINARIZE_PARAM_NAME = "Make Binary";
    private static final boolean BINARIZE_PARAM_DEFAULT = false;

    private static final String HIDE_BACKGROUND_PARAM_NAME = "Hide background image";
    private static final boolean HIDE_BACKGROUND_PARAM_DEFAULT = false;

    private static final String MAKE_GREY_PARAM_NAME = "Make_background greyscale";
    private static final boolean MAKE_GREY_PARAM_DEFAULT = false;

    private static final boolean SHOW_PARAM_DEFAULT = true;
    private static final String WHITE_SHOW_PARAM_NAME = "_White";
    private static final String RED_SHOW_PARAM_NAME = "_Red";
    private static final String GREEN_YELLOW_LIGHT_SHOW_PARAM_NAME = "_GreenYellow_Light";
    private static final String AQUA_SHOW_PARAM_NAME = "_Aqua";
    private static final String BLACK_SHOW_PARAM_NAME = "_Black";
    private static final String GREEN_YELLOW_DARK_SHOW_PARAM_NAME = "_GreenYellow_Dark_";
    private static final String LIGHT_BLUE_SHOW_PARAM_NAME = "_Light_Blue";
    private static final String GREY_SHOW_PARAM_NAME = "_Grey";
    private static final String BROWN_SHOW_PARAM_NAME = "_Brown";
    private static final String GREEN_LIGHT_PARAM_NAME = "_Green_Light_";
    private static final String DARK_BLUE_SHOW_PARAM_NAME = "_Dark_Blue";
    private static final String YELLOW_LIGHT_SHOW_PARAM_NAME = "_Yellow_Light_";
    private static final String GREEN_DARK_SHOW_PARAM_NAME = "_Green_Dark_";
    private static final String MAGENTA_SHOW_PARAM_NAME = "_Magenta";
    private static final String YELLOW_DARK_SHOW_PARAM_NAME = "_Yellow_Dark_";
    private static final String ORANGE_SHOW_PARAM_NAME = "_Orange";

    // Options constants.
    private static final Pattern OPTIONS_REGEX = Pattern.compile("\\s+.*");
    private static final String OPTIONS_SEPARATOR = " ";

    // Checkbox group dimensions.
    private static final int CHECKBOX_GROUP_ROWS = 5;
    private static final int CHECKBOX_GROUP_COLS = 4;

    // Hues.
    private static final double RED_HUE = 0.0277;
    private static final double ORANGE_HUE = 0.1138;
    private static final double YELLOW_HUE = 0.1916;
    private static final double YELLOW_GREEN_HUE = 0.3083;
    private static final double GREEN_HUE = 0.425;
    private static final double AQUA_HUE = 0.475;
    private static final double BLUE_HUE = 0.8;
    private static final double MAGENTA_HUE = 0.9333;
    private static final double MAX_HUE = 1.0;

    // Dialogue headings.
    private static final String[] HEADINGS = {"Greys", "Red-yellow", "Greens", "Blue-magenta"};

    // Parameter labels.
    private static final String BLANK_LABEL = "";
    private static final String[] LABELS =
            {WHITE_SHOW_PARAM_NAME, RED_SHOW_PARAM_NAME, GREEN_YELLOW_LIGHT_SHOW_PARAM_NAME, AQUA_SHOW_PARAM_NAME,
                    BLACK_SHOW_PARAM_NAME, ORANGE_SHOW_PARAM_NAME, GREEN_YELLOW_DARK_SHOW_PARAM_NAME,
                    LIGHT_BLUE_SHOW_PARAM_NAME, GREY_SHOW_PARAM_NAME, BROWN_SHOW_PARAM_NAME, GREEN_LIGHT_PARAM_NAME,
                    DARK_BLUE_SHOW_PARAM_NAME, BLANK_LABEL, YELLOW_LIGHT_SHOW_PARAM_NAME, GREEN_DARK_SHOW_PARAM_NAME,
                    MAGENTA_SHOW_PARAM_NAME, BLANK_LABEL, YELLOW_DARK_SHOW_PARAM_NAME, BLANK_LABEL, BLANK_LABEL};

    // Processing flags for this filter.
    private static final int FLAGS = DOES_RGB | PARALLELIZE_IMAGES | FINAL_PROCESSING;

    // Filter parameters: we make these static so their values persist from one invocation to the next.
    private static int whiteMin;
    private static int blackMax;
    private static int greyTolerance;
    private static double lightDarkCutoff;
    private static double saturationCutoff;
    private static boolean whiteShow;
    private static boolean blackShow;
    private static boolean greyShow;
    private static boolean redShow;
    private static boolean orangeShow;
    private static boolean brownShow;
    private static boolean lightYellowShow;
    private static boolean darkYellowShow;
    private static boolean greenYellowLightShow;
    private static boolean greenYellowDarkShow;
    private static boolean lightGreenShow;
    private static boolean darkGreenShow;
    private static boolean aquaShow;
    private static boolean lightBlueShow;
    private static boolean darkBlueShow;
    private static boolean magentaShow;
    private static boolean hideBackground;
    private static boolean makeGrey;
    private static boolean binarize;

    // Whether to show dialogs.
    private final boolean showDialogs;

    // Progress counter.
    private int progress;

    // Image to process.
    private ImagePlus image;

    /**
     * Create an instance of the filter with default parameter values.
     */
    public HueColoursFilter() {

        this(WHITE_MIN_PARAM_DEFAULT, BLACK_MAX_PARAM_DEFAULT, GREY_TOLERANCE_PARAM_DEFAULT,
                LIGHT_DARK_PARAM_DEFAULT, SATURATION_PARAM_DEFAULT,
                SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT,
                SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT,
                SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT, SHOW_PARAM_DEFAULT,
                SHOW_PARAM_DEFAULT,
                HIDE_BACKGROUND_PARAM_DEFAULT, MAKE_GREY_PARAM_DEFAULT, BINARIZE_PARAM_DEFAULT,
                IJ.getInstance() == null);
    }

    /**
     * Create an instance of the filter.
     *
     * @param whiteMin             white minimum value.
     * @param blackMax             black maximum value.
     * @param greyTolerance        grey tolerance.
     * @param lightDarkCutoff      light-dark threshold value.
     * @param saturationCutoff     saturation threshold value.
     * @param whiteShow            whether to show white.
     * @param blackShow            whether to show black.
     * @param greyShow             whether to show grey.
     * @param redShow              whether to show red.
     * @param orangeShow           whether to show orange.
     * @param brownShow            whether to show brown.
     * @param lightYellowShow      whether to show light yellow.
     * @param darkYellowShow       whether to show dark yellow.
     * @param greenYellowLightShow whether to show light green-yellow.
     * @param greenYellowDarkShow  whether to show dark green-yellow.
     * @param lightGreenShow       whether to show light green.
     * @param darkGreenShow        whether to show dark green.
     * @param aquaShow             whether to show aqua.
     * @param lightBlueShow        whether to show light blue.
     * @param darkBlueShow         whether to show dark blue.
     * @param magentaShow          whether to show magenta.
     * @param hideBackground       whether to hide the background.
     * @param makeGrey             whether to make a greyscale image.
     * @param binarize             whether to make a binary image.
     */
    public HueColoursFilter(final int whiteMin, final int blackMax, final int greyTolerance, final double lightDarkCutoff,
                            final double saturationCutoff, final boolean whiteShow, final boolean blackShow,
                            final boolean greyShow, final boolean redShow, final boolean orangeShow, final boolean brownShow,
                            final boolean lightYellowShow, final boolean darkYellowShow, final boolean greenYellowLightShow,
                            final boolean greenYellowDarkShow, final boolean lightGreenShow, final boolean darkGreenShow,
                            final boolean aquaShow, final boolean lightBlueShow, final boolean darkBlueShow,
                            final boolean magentaShow, final boolean hideBackground, final boolean makeGrey,
                            final boolean binarize, final boolean fromPlugin) {


        this.whiteMin = whiteMin;
        this.blackMax = blackMax;
        this.greyTolerance = greyTolerance;
        this.lightDarkCutoff = lightDarkCutoff;
        this.saturationCutoff = saturationCutoff;
        this.whiteShow = whiteShow;
        this.blackShow = blackShow;
        this.greyShow = greyShow;
        this.redShow = redShow;
        this.orangeShow = orangeShow;
        this.brownShow = brownShow;
        this.lightYellowShow = lightYellowShow;
        this.darkYellowShow = darkYellowShow;
        this.greenYellowLightShow = greenYellowLightShow;
        this.greenYellowDarkShow = greenYellowDarkShow;
        this.lightGreenShow = lightGreenShow;
        this.darkGreenShow = darkGreenShow;
        this.aquaShow = aquaShow;
        this.lightBlueShow = lightBlueShow;
        this.darkBlueShow = darkBlueShow;
        this.magentaShow = magentaShow;
        this.hideBackground = hideBackground;
        this.makeGrey = makeGrey;
        this.binarize = binarize;

        progress = 0;

        // Running in ImageJ?
        showDialogs = !fromPlugin;
    }

    /**
     * Creates an RGB value for a shade of grey.
     *
     * @param level grey value.
     * @return packed integer representation of RGB.
     */
    private static int toGreyRgb(final int level) {

        return Rgb24Bit.pack(level, level, level);
    }

    /**
     * Return current parameters back to a macro so user can set them first manually then process a folder using
     * settings the returned string is split into an array on TAB character (\t)<p>
     * <p>
     * Example:<br>
     * package = "com.syngenta.imagej.plugins.imagecolours.HueColoursFilter.";<br>
     * options = call(package + "getOptions");
     *
     * @return Array of parameters as a string.
     */
    public static String getOptions() {

        return getOptionString(WHITE_MIN_PARAM_NAME, whiteMin) +
                getOptionString(BLACK_MAX_PARAM_NAME, blackMax) +
                getOptionString(GREY_TOLERANCE_PARAM_NAME, greyTolerance) +
                getOptionString(LIGHT_DARK_PARAM_NAME, lightDarkCutoff) +
                getOptionString(SATURATION_PARAM_NAME, saturationCutoff) +
                getOptionString(WHITE_SHOW_PARAM_NAME, whiteShow) +
                getOptionString(RED_SHOW_PARAM_NAME, redShow) +
                getOptionString(GREEN_YELLOW_LIGHT_SHOW_PARAM_NAME, greenYellowLightShow) +
                getOptionString(AQUA_SHOW_PARAM_NAME, aquaShow) +
                getOptionString(BLACK_SHOW_PARAM_NAME, blackShow) +
                getOptionString(ORANGE_SHOW_PARAM_NAME, orangeShow) +
                getOptionString(GREEN_YELLOW_DARK_SHOW_PARAM_NAME, greenYellowDarkShow) +
                getOptionString(LIGHT_BLUE_SHOW_PARAM_NAME, lightBlueShow) +
                getOptionString(GREY_SHOW_PARAM_NAME, greyShow) +
                getOptionString(BROWN_SHOW_PARAM_NAME, brownShow) +
                getOptionString(GREEN_LIGHT_PARAM_NAME, lightGreenShow) +
                getOptionString(DARK_BLUE_SHOW_PARAM_NAME, darkBlueShow) +
                getOptionString(YELLOW_LIGHT_SHOW_PARAM_NAME, lightYellowShow) +
                getOptionString(GREEN_DARK_SHOW_PARAM_NAME, darkGreenShow) +
                getOptionString(MAGENTA_SHOW_PARAM_NAME, magentaShow) +
                getOptionString(YELLOW_DARK_SHOW_PARAM_NAME, darkYellowShow) +
                getOptionString(BINARIZE_PARAM_NAME, binarize) +
                getOptionString(MAKE_GREY_PARAM_NAME, makeGrey) +
                getOptionString(HIDE_BACKGROUND_PARAM_NAME, hideBackground);
    }

    private static String getOptionKey(final CharSequence name) {
        return OPTIONS_REGEX.matcher(name).replaceAll("").toLowerCase().trim();
    }

    private static String getOptionString(final String name, final String value) {

        return getOptionKey(name) + '=' + value + OPTIONS_SEPARATOR;
    }

    private static String getOptionString(final String name, final boolean value) {

        return value ? getOptionKey(name) + OPTIONS_SEPARATOR : "";
    }

    private static String getOptionString(final String name, final int value) {
        return getOptionString(name, String.valueOf(value));
    }

    private static String getOptionString(final String name, final double value) {
        return getOptionString(name, String.valueOf(value));
    }

    /**
     * Set the image to run the filter on.
     *
     * @param img the image.
     */
    public void setImageForFilter(final ImagePlus img) {
        image = img;

        if (binarize) {
            Rgb24Bit.binarizeImage(image, 128);
        }

        WindowManager.setTempCurrentImage(image);
    }

    /**
     * Gets the image the filter is applied to.
     *
     * @return the image.
     */
    public ImagePlus getImage() {
        return image;
    }

    /**
     * Calculates the hue colour for a pixel.
     *
     * @param pixel the pixel value (packed RGB)
     * @return the calculated hue.
     */
    private int calculateHueColour(final int pixel) {

        // Split RGB pixel value into it's components.
        final int[] rgb = Rgb24Bit.unpack(pixel);

        // Convert to HSV.
        final double[] hsv = ColourSpaceUtilities.convertRgb2Hsv(rgb);
        final double hue = hsv[0];
        final double saturation = hsv[1];
        final double variance = hsv[2];

        // Calculate RGB sensitivity and mean.
        final int rgbDiff = ColourSpaceUtilities.getRgbSensitivity(rgb[0], rgb[1], rgb[2]);
        final int rgbMean = (rgb[0] + rgb[1] + rgb[2]) / 3;

        // Work out in RGB space if colour is black, gray or white first and if not go into HSB to colour pixel.
        return binarize ? calculateBinaryHueColour(hue, saturation, variance, rgbDiff, rgbMean) :
                calculateRgbHueColour(hue, saturation, variance, rgbDiff, rgbMean, pixel);
    }

    /**
     * Calculate the hue colour.
     *
     * @param hue        hue value.
     * @param saturation saturation value.
     * @param variance   variance value.
     * @param diff       RGB sensitivity.
     * @param mean       RGB mean.
     * @param pixel      original pixel value.
     * @return the hue colour.
     */
    private int calculateRgbHueColour(final double hue, final double saturation, final double variance, final int diff,
                                      final int mean, final int pixel) {
        int result = pixel;
        final boolean isLight = variance >= lightDarkCutoff;
        final boolean notSaturated = saturation <= saturationCutoff;

        if (hideBackground) {
            result = Rgb24Bit.LIGHT_GREY;
        } else if (makeGrey) {
            result = toGreyRgb(mean);
        }

        if (diff < greyTolerance || notSaturated) {

            if (mean <= blackMax) {
                if (blackShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (mean < whiteMin) {
                if (greyShow) {
                    result = Rgb24Bit.GREY;
                }
            } else if (mean >= whiteMin) {
                if (whiteShow) {
                    result = Rgb24Bit.WHITE;
                }
            }
        } else //if (!notSaturated) is always true.
        {
            if (hue <= RED_HUE) {
                if (redShow) {
                    result = Rgb24Bit.RED;
                }
            } else if (hue <= ORANGE_HUE) {
                if (isLight && orangeShow) {
                    result = Rgb24Bit.ORANGE;
                } else if (!isLight && brownShow) {
                    result = Rgb24Bit.BROWN;
                }
            } else if (hue <= YELLOW_HUE) {
                if (isLight && lightYellowShow) {
                    result = Rgb24Bit.LIGHT_YELLOW;
                } else if (!isLight && darkYellowShow) {
                    result = Rgb24Bit.DARK_YELLOW;
                }
            } else if (hue <= YELLOW_GREEN_HUE) {
                if (isLight && greenYellowLightShow) {
                    result = Rgb24Bit.LIGHT_YELLOW_GREEN;
                } else if (!isLight && greenYellowDarkShow) {
                    result = Rgb24Bit.DARK_YELLOW_GREEN;
                }
            } else if (hue <= GREEN_HUE) {
                if (isLight && lightGreenShow) {
                    result = Rgb24Bit.LIGHT_GREEN;
                } else if (!isLight && darkGreenShow) {
                    result = Rgb24Bit.DARK_GREEN;
                }
            } else if (hue <= AQUA_HUE) {
                if (aquaShow) {
                    result = Rgb24Bit.AQUA;
                }
            } else if (hue <= BLUE_HUE) {
                if (isLight && lightBlueShow) {
                    result = Rgb24Bit.LIGHT_BLUE;
                } else if (!isLight && darkBlueShow) {
                    result = Rgb24Bit.DARK_BLUE;
                }
            } else if (hue <= MAGENTA_HUE) {
                if (magentaShow) {
                    result = Rgb24Bit.MAGENTA;
                }
            } else if (hue <= MAX_HUE) {
                if (redShow) {
                    result = Rgb24Bit.RED;
                }
            }
        }

        return result;
    }

    /**
     * Calculate the hue colour.
     *
     * @param hue        hue value.
     * @param saturation saturation value.
     * @param variance   variance value.
     * @param diff       RGB sensitivity.
     * @param mean       RGB mean.
     * @return binary colour.
     */
    private int calculateBinaryHueColour(final double hue, final double saturation, final double variance, final int diff,
                                         final int mean) {
        int result = Rgb24Bit.WHITE;
        final boolean isLight = variance >= lightDarkCutoff;
        final boolean notSaturated = saturation <= saturationCutoff;

        if (diff < greyTolerance || notSaturated) {

            if (mean <= blackMax) {
                if (blackShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (mean < whiteMin) {
                if (greyShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (mean >= whiteMin) {
                if (whiteShow) {
                    result = Rgb24Bit.BLACK;
                }
            }
        } else //if (!isNotSaturated) always true.
        {
            if (hue <= RED_HUE) {
                if (redShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= ORANGE_HUE) {
                if (isLight && orangeShow || !isLight && brownShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= YELLOW_HUE) {
                if (isLight && lightYellowShow || !isLight && darkYellowShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= YELLOW_GREEN_HUE) {
                if (isLight && greenYellowLightShow || !isLight && greenYellowDarkShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= GREEN_HUE) {
                if (isLight && lightGreenShow || !isLight && darkGreenShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= AQUA_HUE) {
                if (aquaShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= BLUE_HUE) {
                if (isLight && lightBlueShow || !isLight && darkBlueShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= MAGENTA_HUE) {
                if (magentaShow) {
                    result = Rgb24Bit.BLACK;
                }
            } else if (hue <= MAX_HUE) {
                if (redShow) {
                    result = Rgb24Bit.BLACK;
                }
            }
        }

        return result;
    }

    /**
     * This method is called once when the filter is loaded. 'arg',
     * which may be blank, is the argument specified for this plugin
     * in IJ_Props.txt or in the plugins.config file of a jar archive
     * containing the plugin. 'imp' is the currently active image.
     * This method should return a flag word that specifies the
     * filters capabilities.
     * <p/>
     * For Plugin-filters specifying the FINAL_PROCESSING flag,
     * the setup method will be called again, this time with
     * arg = "final" after all other processing is done.
     *
     * @param arg arguments the plug-in was called with.
     * @param imp image the plug-in is to be applied to.
     */
    @Override
    public int setup(final String arg, final ImagePlus imp) {

        if ("about".equals(arg)) {
            IJ.showMessage(ABOUT_HTML);
            return DONE;
        }

        if (imp == null) {
            IJ.noImage();
            return DONE;
        }

        // Final pass.
        if ("final".equalsIgnoreCase(arg)) {
            if (binarize) {
                Rgb24Bit.binarizeImage(image, 128);
            }
            return DONE;
        }

        // Set image.
        image = imp;

        return FLAGS;
    }

    /**
     * This method is called after {@code setup(arg, imp)} unless the
     * {@code DONE} flag has been set.
     *
     * @param imp     The active image already passed in the
     *                {@code setup(arg, imp)} call. It will be null, however, if
     *                the {@code NO_IMAGE_REQUIRED} flag has been set.
     * @param command The command that has led to the invocation of
     *                the plugin-filter. Useful as a title for the dialog.
     * @param pfr     The PlugInFilterRunner calling this plugin-filter.
     *                It can be passed to a GenericDialog by {@code addPreviewCheckbox}
     *                to enable preview by calling the {@code run(ip)} method of this
     *                plugin-filter. {@code pfr} can be also used later for calling back
     *                the PlugInFilterRunner, e.g., to obtain the slice number
     *                currently processed by {@code run(ip)}.
     * @return The method should return a combination (bitwise OR)
     * of the flags specified in interfaces {@code PlugInFilter} and
     * {@code ExtendedPlugInFilter}.
     */
    @Override
    public int showDialog(final ImagePlus imp, final String command, final PlugInFilterRunner pfr) {

        // Show dialogs?
        if (!showDialogs) {
            return FLAGS;
        }

        final GenericDialog gd = new GenericDialog(command);
        gd.addMessage("--------------This slider determines when grey becomes white----------------");
        gd.addSlider(WHITE_MIN_PARAM_NAME, 0.0, 255.0, whiteMin);
        gd.addMessage("--------------This slider determines when grey becomes black----------------");
        gd.addSlider(BLACK_MAX_PARAM_NAME, 0.0, 256.0, blackMax);
        gd.addMessage("--------------This slider determines how close RGB channels are to be grey----------------");
        gd.addSlider(GREY_TOLERANCE_PARAM_NAME, 0.0, 256.0, greyTolerance);
        gd.addMessage(
                "--------------Alter this slide to set the threshold between light and dark colours----------------");
        gd.addSlider(LIGHT_DARK_PARAM_NAME, 0.0, 1.0001, lightDarkCutoff);
        gd.addMessage("--------------Alter this slide to set the min threshold for saturation----------------");
        gd.addSlider(SATURATION_PARAM_NAME, 0.0, 1.0001, saturationCutoff);

        gd.addCheckboxGroup(CHECKBOX_GROUP_ROWS, CHECKBOX_GROUP_COLS, LABELS,
                new boolean[]{whiteShow, redShow, greenYellowLightShow, aquaShow, blackShow, orangeShow,
                        greenYellowDarkShow, lightBlueShow, greyShow, brownShow, lightGreenShow,
                        darkBlueShow, false, lightYellowShow, darkGreenShow, magentaShow, false,
                        darkYellowShow, false, false}, HEADINGS);

        gd.addMessage("--------------Click on 'Make Binary' to convert selected colours to black and non-selected to " +
                "white----------------");
        gd.addCheckbox(BINARIZE_PARAM_NAME, binarize);
        gd.addMessage(
                "--------------Click on 'Hide background image' to turn deselected colours to grey----------------");
        gd.addCheckbox(HIDE_BACKGROUND_PARAM_NAME, hideBackground);
        gd.addCheckbox(MAKE_GREY_PARAM_NAME, makeGrey);
        gd.addMessage("--------------Click on Preview to filter the image LIVE----------------");

        gd.addPreviewCheckbox(pfr);
        gd.addDialogListener(this);
        gd.showDialog();

        // Dialog exit status.
        return gd.wasCanceled() ? DONE : IJ.setupDialog(imp, FLAGS);
    }

    /**
     * This method is invoked by a Generic Dialog if any of the inputs have changed
     * (CANCEL does not trigger it; OK and running the dialog from a macro only
     * trigger the first DialogListener added to a GenericDialog).
     *
     * @param gd A reference to the GenericDialog.
     * @param e  The event that has been generated by the user action in the dialog.
     *           Note that {@code e} is {@code null} if the
     *           dialogItemChanged method is called after the user has pressed the
     *           OK button or if the GenericDialog has read its parameters from a
     *           macro.
     * @return Should be true if the dialog input is valid. False disables the
     * OK button and preview (if any).
     */
    @Override
    public boolean dialogItemChanged(final GenericDialog gd, final AWTEvent e) {

        // Reset counter.
        progress = 0;

        whiteMin = (int) gd.getNextNumber();
        blackMax = (int) gd.getNextNumber();
        greyTolerance = (int) gd.getNextNumber();
        lightDarkCutoff = gd.getNextNumber();
        saturationCutoff = gd.getNextNumber();
        whiteShow = gd.getNextBoolean();
        redShow = gd.getNextBoolean();
        greenYellowLightShow = gd.getNextBoolean();
        aquaShow = gd.getNextBoolean();
        blackShow = gd.getNextBoolean();
        orangeShow = gd.getNextBoolean();
        greenYellowDarkShow = gd.getNextBoolean();
        lightBlueShow = gd.getNextBoolean();
        greyShow = gd.getNextBoolean();
        brownShow = gd.getNextBoolean();
        lightGreenShow = gd.getNextBoolean();
        darkBlueShow = gd.getNextBoolean();
        lightYellowShow = gd.getNextBoolean();
        darkGreenShow = gd.getNextBoolean();
        magentaShow = gd.getNextBoolean();
        darkYellowShow = gd.getNextBoolean();
        binarize = gd.getNextBoolean();
        hideBackground = gd.getNextBoolean();
        makeGrey = gd.getNextBoolean();
        return true;
    }

    /**
     * This method is called by ImageJ to inform the plugin-filter
     * about the passes to its run method. During preview, the number of
     * passes is one (or 3 for RGB images, if {@code CONVERT_TO_FLOAT}
     * has been specified). When processing a stack, it is the number
     * of slices to be processed (minus one, if one slice has been
     * processed for preview before), and again, 3 times that number
     * for RGB images processed with {@code CONVERT_TO_FLOAT}.
     *
     * @param nPasses number of passes.
     */
    @Override
    public void setNPasses(final int nPasses) {

    }

    /**
     * Filters use this method to process the image. If the
     * SUPPORTS_STACKS flag was set, it is called for each slice in
     * a stack. With CONVERT_TO_FLOAT, the filter is called with
     * the image data converted to a FloatProcessor (3 times per
     * image for RGB images).
     * ImageJ will lock the image before calling
     * this method and unlock it when the filter is finished.
     * For PlugInFilters specifying the NO_IMAGE_REQUIRED flag
     * and not the DONE flag, run(ip) is called once with the
     * argument {@code null}.
     *
     * @param ip image to process.
     */
    @Override
    public void run(final ImageProcessor ip) {

        IJ.showStatus("Calculating hues colours...");

        try {

            // Image pixels.
            final int[] pixels = (int[]) ip.getPixels();
            final int height = ip.getHeight();
            final int width = ip.getWidth();

            // ROI rectangle (used for masking).
            final byte[] mask;
            final int mX;
            final int mY;
            final int mW;
            if (image.getRoi() != null && image.getMask() != null) {

                mask = (byte[]) image.getMask().getPixels();
                final Rectangle maskRect = image.getRoi().getBounds();
                mX = maskRect.x;
                mY = maskRect.y;
                mW = maskRect.width;

            } else {
                mask = null;
                mX = 0;
                mY = 0;
                mW = 0;
            }

            // ROI rectangle (used for parallel processing).
            final Rectangle roiRect = ip.getRoi();
            final int rX = roiRect.x;
            final int rY = roiRect.y;
            final int rW = roiRect.width;
            final int rH = roiRect.height;

            // Loop through ROI rectangle (it is clipped against the mask ROI).
            final int maxY = rY + rH;
            final int maxX = rX + rW;
            for (int y = rY; y < maxY; y++) {

                // Calculate offsets into pixel arrays.
                int roiOffset = rX + y * width;
                int maskOffset = mask == null ? 0 : (y - mY) * mW + rX - mX;

                for (int x = rX; x < maxX; x++) {

                    // Perform calculation (if masked).
                    if (mask == null || mask[maskOffset] != 0) {

                        // Calculate hue.
                        pixels[roiOffset] = calculateHueColour(pixels[roiOffset]);
                    }

                    // Increment offsets.
                    roiOffset++;
                    maskOffset++;
                }

                IJ.showProgress(progress++, height - 1);
            }
        } catch (final Throwable e) {
            IJ.error("Runtime Error", e.getMessage());
        }
    }
}
