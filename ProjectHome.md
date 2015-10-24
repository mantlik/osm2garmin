# MIGRATION #
<font color='red'>
Because Google Code does not support downloads anymore, this project is in the process of MIGRATION to <a href='http://osm2garmin.mantlik.org'>http://osm2garmin.mantlik.org</a>.<br>
<br>
GoogleCode hosting of this project is not maintained. All downloads at GoogleCode are obsolete and will not work because of recent changes in source systems. Please visit <a href='http://osm2garmin.mantlik.org'>http://osm2garmin.mantlik.org</a> for updated downloads and instructions.<br>
</font>

# Introduction #
Download OpenStreetMap data for the whole planet, update existing planet data, add terrain contours generated from SRTM3 database and generate garmin-compatible maps in a single step.

The application wraps following OpenStreetMap processing utilities: [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis), [Tile Splitter](http://www.mkgmap.org.uk/page/tile-splitter) and [Mkgmap](http://www.mkgmap.org.uk/). In addition, a utility similar to [Srtm2osm](http://wiki.openstreetmap.org/wiki/Srtm2Osm) program has been developed as a part of this application.

**On this page:**


# System requirements #
  * Windows XP/Vista/7, Linux 2.26 or Mac OS 10
  * minimum 2 Gb RAM
  * minimum 150 Gb free disk space for the whole planet maps data (200+ Gb recommended)
  * [Sun Java JRE 7](http://java.com/download) or newer

On 64bit systems use 64bit version of Java to avoid out of memory issues. Please visit FrequentlyAskedQuestions for more information.

# Downloads #
Installers and user's guide can be downloaded from here:
http://osm2garmin.mantlik.org/downloads/

  * Recommended version:   1.2-SNAPSHOT- [see instructions](UnstableVersionHowto.md)
  * Previous stable version: 1.1. This version still can work on existing installations. It will not work when newly installed because the Planet torrent source used in this version is not active anymore. See [Issue 132](https://code.google.com/p/osm2garmin/issues/detail?id=132) for more details.

Version 1.0 has been obsoleted. It is not working properly anymore because of recent changes in http://planet.openstreetmap.org structure. Please read UpdateRedactionPeriod for more information. Recommended version handles correctly OSM data license and download site structure changes.

[Release notes](ReleaseNotes.md)

# Help and support #
[Frequently Asked Questions](FrequentlyAskedQuestions.md) page covers the most frequent problems with the program.

User's guide can be downloaded in PDF format from the [Download page](http://code.google.com/p/osm2garmin/downloads/list).

Please post your questions to the [Osm2garmin Development Group](http://groups.google.com/group/osm2garmin-devel).

You are welcome to submit a bug report or suggest an enhancement at the [Issues page](http://code.google.com/p/osm2garmin/issues/entry).

# Screenshots #
Sample MapSource screenshot of a resulting map - surrounding of the Prague Zoo:

http://osm2garmin.googlecode.com/svn/wiki/map_example_prg_zoo.PNG

Application ready to run:

![http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/application.png](http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/application.png)

Processing parameters:

![http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/parameters.png](http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/parameters.png)

Download sources:

![http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/download_sources.png](http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/download_sources.png)

Regions definitions:

![http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/regions.png](http://osm2garmin.googlecode.com/svn/trunk/Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/regions.png)

# Development #
Please report bugs or propose enhancements and new features at http://code.google.com/p/osm2garmin/issues
or http://groups.google.com/group/osm2garmin-devel

Source code checkout instructions: http://code.google.com/p/osm2garmin/source/checkout

Java developers are welcome to submit proposed patches to the Issue tracker.