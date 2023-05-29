# Syngenta ImageJ Plugins

This repository contains the following ImageJ plug-ins:

+ Segmentation > Hue Colours: segment an image in colour-space

## Installation

Build `hue-colours-1.0.1.jar` using Maven:

```
maven package
```

Then copy JAR file to ImageJ's plug-ins directory, replacing or deleting any old version of it.

## Run

The filter can be found under the Plugins menu:

Plugins > Segmentation > Hue Colours

To invoke the filter from a macro use the following, e.g.

`run("Hue Colours", "<parameter values>");`