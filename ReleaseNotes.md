# Release notes #

## 1.2 - Unstable Development branch ##
New features:

  * Window with Mkgmap tool help opens together with parameters files editors - [Issue 102](https://code.google.com/p/osm2garmin/issues/detail?id=102)
  * Skip planet update option skips planet updates and re-splitting of regions OSM data on request - [Issue 60](https://code.google.com/p/osm2garmin/issues/detail?id=60)
  * Processing parameters files for mkgmap can be edited for three stages of processing: contours preparation step, OSM maps authoring step and final gmapsupp.img creation step - [Issue 53](https://code.google.com/p/osm2garmin/issues/detail?id=53)
  * New parameter at the Processing parameters page Splitter overlap controls splitter parameter --overlap - [Issue 53](https://code.google.com/p/osm2garmin/issues/detail?id=53)
  * Typ files support - [Issue 53](https://code.google.com/p/osm2garmin/issues/detail?id=53)

Bug fixes:

  * SRTM 2.1 file naming bug - [Issue 101](https://code.google.com/p/osm2garmin/issues/detail?id=101)
  * Problem with state.txt download - [Issue 119](https://code.google.com/p/osm2garmin/issues/detail?id=119), [Issue 123](https://code.google.com/p/osm2garmin/issues/detail?id=123)

## 1.1 (Build 250) - Stable release ##
New features:

  * Regions can be newly defined by bounding polygons in addition to rectangles. Polygon definition file `<region_name>.poly` in the working directory overrides region rectangular boundaries - [Issue 55](https://code.google.com/p/osm2garmin/issues/detail?id=55)
  * Planet download type changed from bz2 to pbf for BitTorrent method - [Issue 45](https://code.google.com/p/osm2garmin/issues/detail?id=45)
  * Changed installer wizard - removed "Run application when finished" option at the last installer wizard page - [Issue 39](https://code.google.com/p/osm2garmin/issues/detail?id=39)
  * Added installer for MacOsx - [Issue 61](https://code.google.com/p/osm2garmin/issues/detail?id=61)
  * Added system recognition into installer. Default start-up parameters are adjusted in post-install step - [Issue 21](https://code.google.com/p/osm2garmin/issues/detail?id=21), [Issue 41](https://code.google.com/p/osm2garmin/issues/detail?id=41) and [Issue 61](https://code.google.com/p/osm2garmin/issues/detail?id=61)
  * Added support for Redaction-period updates structure change after 1-st of April 2012 - [Issue 65](https://code.google.com/p/osm2garmin/issues/detail?id=65)
  * Removed HTTP download for updates. Only planet file can be downloaded via HTTP, updates are downloaded via BitTorrent only - [Issue 65](https://code.google.com/p/osm2garmin/issues/detail?id=65)

Bug fixes:

  * Fixed PermGen Space error - [Issue 41](https://code.google.com/p/osm2garmin/issues/detail?id=41)
  * Fixed missing license agreement when updating modules - [Issue 33](https://code.google.com/p/osm2garmin/issues/detail?id=33)
  * Private tracker for BitTorrent planet updates download has been established - [Issue 54](https://code.google.com/p/osm2garmin/issues/detail?id=54)
  * Minor fixes - [Issue 25](https://code.google.com/p/osm2garmin/issues/detail?id=25), [Issue 35](https://code.google.com/p/osm2garmin/issues/detail?id=35), [Issue 42](https://code.google.com/p/osm2garmin/issues/detail?id=42), [Issue 49](https://code.google.com/p/osm2garmin/issues/detail?id=49), [Issue 50](https://code.google.com/p/osm2garmin/issues/detail?id=50), [Issue 51](https://code.google.com/p/osm2garmin/issues/detail?id=51), [Issue 59](https://code.google.com/p/osm2garmin/issues/detail?id=59), [Issue 63](https://code.google.com/p/osm2garmin/issues/detail?id=63), [Issue 64](https://code.google.com/p/osm2garmin/issues/detail?id=64), [Issue 69](https://code.google.com/p/osm2garmin/issues/detail?id=69), [Issue 73](https://code.google.com/p/osm2garmin/issues/detail?id=73), [Issue 75](https://code.google.com/p/osm2garmin/issues/detail?id=75), [Issue 86](https://code.google.com/p/osm2garmin/issues/detail?id=86)

## 1.0 (Build 138) - Previous stable release ##

Initial release.

This release is obsolete and is not working anymore - see UpdateRedactionPeriod Wiki.