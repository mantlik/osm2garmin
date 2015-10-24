# Frequently Asked Questions #

**On this page:**


## Program can't start after installation with message JVM creation failed. What can I do? ##

The most frequent source of the problem is that Java virtual machine can't reserve enough memory for it's run. The program needs 1400 Mb of heap space when installed with default settings (startup parameter `-J-Xmx1400m`). With this setting normal run of the program should be successful in most cases (without mkgmap address search option).

If you have 32bit Windows or 32bit Java (JRE), depending on your system and JRE version it could be impossible to reserve 1400 Mb of heap memory. In that case you can try to start the program with less heap space, e.g. `-J-Xmx1200m`. Nevertheless, you can achieve frequent Out-of-memory issues when trying to generate maps. Re-run of the program can work around mostly, but it can be time consuming.

If you have 64-bit Windows AND 64-bit JRE and more than 2 Gb of memory, you can increase memory limit with start-up parameter, e.g. `-J-Xmx2000m`. Please note that if you have both 32bit and 64bit JRE installed, it could be necessary to point the program to the proper 64-bit JRE version with the start-up parameter something like  `--jdkhome "c:/program files/Java/jre6"` (pointing to 64-bit JRE, of course).

Your customized command line can look e.g. like this:

```
"c:\program files\osm2garmin_1.0\bin\osm2garmin.exe" -J-Xmx2000m --jdkhome "c:/program files/Java/jre6"
```

In Windows, startup parameters can be added permanently to the program startup link, e.g. associated to program desktop icon. Right-click the icon, select Properties and adjust the settings as needed.

More information how to make your JVM startup permanent for your system can be found at "Startup options" chapter of the manual - section "Assign more memory to the program".

## Program execution fails with message java.lang.OutOfMemoryError: PermGen space ##

This problem can be solved by adding the following start-up parameters:

`-J-XX:PermSize=64M -J-XX:MaxPermSize=256m`

## How can I find whether my Java installation is 32-bit or 64-bit? ##

You can find out whether the particular JRE installation is 32-bit or 64-bit with a command similar to:

```
"c:\Program files\Java\jre6\bin\java.exe" -version
```

in the Commandline window. Last line of the output will show 32-Bit or 64-Bit version of the installation. Output can look like this:

```
java version "1.6.0_29"
Java(TM) SE Runtime Environment (build 1.6.0_29-b11)
Java HotSpot(TM) 64-Bit Server VM (build 20.4-b02, mixed mode)
```

for 64-Bit version.

## How to change the location of the temporary directory? ##

Osmosis, the tool used by Osm2garmin, is creating a lot of large files in the temporary directory. If your temporary space is limited, you can change location of the temporary directory by the startup switch, e.g.

`-J-Djava.io.tmpdir=d:/myosmosistempdir/`