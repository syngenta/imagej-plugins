package com.syngenta.imagej.plugins.imagecolours;

import org.junit.Assert;
import org.junit.Test;

public class TestHueColoursFilter {

    @Test
    public void testGetOptions() {
        // Test default options.
        Assert.assertEquals("Incorrect default options",
                "white_min=0 black_max=0 tolerance=0 light_dark_threshold=0.0 saturation_min=0.0 ", HueColoursFilter.getOptions());

        // Test custom options.
        new HueColoursFilter(240, 12, 4, 0.5, 0.05,
                true, true, true, true, true, true,
                true, true, true, true,
                true, true, true, true, true,
                true, true, true, true, false);
        Assert.assertEquals("Incorrect default options",
                "white_min=240 black_max=12 tolerance=4 light_dark_threshold=0.5 saturation_min=0.05 _white _red _greenyellow_light _aqua _black _orange _greenyellow_dark_ _light_blue _grey _brown _green_light_ _dark_blue _yellow_light_ _green_dark_ _magenta _yellow_dark_ make make_background hide ", HueColoursFilter.getOptions());
    }
}
