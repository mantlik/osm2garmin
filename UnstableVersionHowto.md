# Unstable version #

Unstable version of the software implements the newest features not yet included in the stable version. ReleaseNotes describe changes included in the unstable (SNAPSHOT) version.

Any feedback concerning the unstable version is highly appreciated. Please [report issues](http://code.google.com/p/osm2garmin/issues/entry) or share your experience in the [discussion group](http://groups.google.com/group/osm2garmin-devel).

## Clean installation of the unstable version ##

  1. Uninstall the stable version of the software using the procedure suitable for your operating system.
  1. Download the installer with "Unstable" label for your system.
  1. Install the unstable version using the downloaded installer.

## Upgrade from current stable version to unstable version ##

  1. Select Tools - Plugins. Select Settings tab and click on "Osm2Garmin Update Center" item. At the right part of the window click Edit button and add /snapshot to the end of the "URL" value. New URL value should look like this:
```
http://updates.mantlik.org/updates.xml.php?lib=Osm2garmin/snapshot
```
  1. Click OK to close Update Center Customizer.
  1. Select Updates tab and click Reload Catalog. New update should show and Update button should become active. Press Update to finish the update process. After restart of the application the update will be finished.
  1. You can occasionally select Help -> Check for updates from the main menu. Available newer versions of the software modules will be installed.

Setting the URL value back to
```
http://updates.mantlik.org/updates.xml.php?lib=Osm2garmin
```
will stop further delivery of newly published snapshot updates. Nevertheless, it will NOT roll back the software to the stable release! Please follow precedure described in the next section to replace unstable versions of the modules with stable ones.

## Rolling back to the stable version ##

It is recommended to backup your data before proceeding. Please note that all user settings will be lost.

  1. Uninstall the software using uninstall procedure suitable for your operating system. In the first dialog of the Uninstall wizard check the Remove directory …./dev checkbox.
  1. Download the newest installer for your system.
  1. Run the downloaded installer and follow the installation wizard. Under some circumstances you have to manually delete the installation directory with system administrator rights before proceeding with installation.