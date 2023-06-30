# Syngenta ImageJ Plugins

This repository contains the following ImageJ plug-ins:

+ Segmentation > Hue Colours: segment an image in colour-space

## Installation

Build `hue-colours-N.N.N.jar` using Maven:

```
maven package
```

Then copy the JAR file to ImageJ's `plugins` folder, replacing or deleting any old version of the JAR. **Note**, ImageJ
requires that JAR filenames in the `plugins` folder must have a leading underscore, e.g. `_hue-colours.jar`.

## Run

The filter can be found under the ImageJ Plugins menu:

Plugins > Segmentation > Hue Colours

To invoke the filter from a macro use the following, e.g.

`run("Hue Colours", "<parameter values>");`

To invoke the filter programmatically use the `PlugInFilterRunner`, e.g.

```java
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

import com.syngenta.imagej.plugins.imagecolours.HueColoursFilter;

// Open an image and make it current image.
final ImagePlus image=new Opener().openImage(inputImagePath);
        WindowManager.setTempCurrentImage(image);

// Create and apply the filter to the current image.
final PluginFilter filter=new HueColoursFilter();
        new PlugInFilterRunner(filter,"","");
```
See also, `HueColoursExample.java` for a more complete example.