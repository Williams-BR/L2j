##############################################
GEODATA COMPENDIUM
##############################################

Comprehensive guide for geodata.

How to configure it
	a - Prerequisites
	b - Make it work

##############################################
How to configure it
##############################################

----------------------------------------------
a - Prerequisites
----------------------------------------------

* A 64bits Windows/Java JDK is a must-have to run server with geodata. Linux servers don't have the issue.
* The server can start (hardly) with -Xmx3000m. -Xmx4g is recommended.

----------------------------------------------
b - Make it work
----------------------------------------------

To make geodata work:
* unpack your geodata files into "/data/geodata" folder (or any other folder)
* open "/config/GeoEngine.ini" with your favorite text editor and then edit following configs:
- GeoDataPath = set path to your geodata, if elsewhere than "./data/geodata/"
- GeoDataType = set the geodata format, which you are using.
