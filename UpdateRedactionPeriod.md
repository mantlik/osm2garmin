# Update planet data in Redaction period #

1-st of April 2012 at 8:00 planet updates of OSM data were stopped. Starting from 4-th of April, hourly replication updates are available in the redaction-period folder of the planet server. For details please read  http://blog.osmfoundation.org/2012/04/04/api-read-write-returns/ .

**Osm2garmin 1.0** is NOT able to handle new structure of the planet updates. If you are using version 1.0 of the software, all planet updates will stop 1-st of April 2012.

Starting from the **Release 187**, **Osm2garmin version 1.1** is able to download redaction-period updates, i.e. updates starting after 1-st of April 2012.

Please take into consideration following notes when using Osm2garmin version 1.1 Release 187 or later after 1-st of April:

  * Updates from redaction-period folder are currently implemented for BitTorrent download method only. If you use HTTP download method, your updates will stop 1-st of April 2012. You can switch download method to BitTorrent to recover download process safely.

  * When using BitTorrent download method, after 1-st of April 8:00 a.m., sequence numbers of the state.txt file in the osmosiswork folder are increased further following updates from redaction-period folder. It is not compatible with Osmosis. Do NOT switch download method back to HTTP, otherwise structure of your working directory and planet.osm.pbf can be corrupted.

  * Currently, updates extension works for data redaction period only. As soon as the final updates for the new ODbL-licensed data will be available, current [r187](https://code.google.com/p/osm2garmin/source/detail?r=187) updates procedure will stop again. Because of this, final 1.1 Osm2garmin release will be postponed until OpenStreatMap data distribution structure will be stabilized.

**Release 193** introduced important bug fixes which should repair broken full planet download after 15-th of April 2012. Fixes will have effect for approx. next two weeks.

**Release 198** fixes serious bug in planet update routine blocking further planet updates when a successful planet update was completed after 1-st of April - see [Issue 69](https://code.google.com/p/osm2garmin/issues/detail?id=69). All users are advised to upgrade to Release 198. In addition, fix introduced in the Release 193 was extended for next 30 days, i.e. approx. to the end of May 2012.

**Osm2garmin Release 198** temporarily replaces current stable version 1.0 until final 1.1 release. It is offered to all Osm2garmin 1.0 users via automatic application updates.

**8-th of May 2012** new full planet file was published at planet.openstreetmap.org. This extends proper functionality of Release 198 until approx. 7-th of June 2012.

**1-st of August 2012** According to information at http://blog.osmfoundation.org/2012/07/26/automated-redactions-complete/, redaction of the planet data has been finished, nevertheless final structure of updates has not been published yet. Despite of this lack of information, new updates are still published in the redaction-period folder and new planet snapshots are updated every month. This keeps current Osm2garmin release 1.1 build 209 operational until further announcement.

**23-th of September 2012** First ODbL licensed planet file was released 12-th of September. **Osm2garmin Release 220** or newer can handle new data structure. When old CC-BY-SA licensed planet file exists, it is discarded and the latest available ODbL licensed planet file is downloaded.

**Please note** that current mkgmap version does not handle license information correctly. You have to manually edit license information in the file osmmap\_license.txt for each newly generated region.