# RivetZ
(a fork of Rivet, formerly developed by Ian Wraith and danielekmann)  
For the old readme, please see oldreadme.txt
## System requirements  
- For live decoding: an audio passthrough, such as [VB Audio Cable](https://vb-audio.com/Cable/) or [VoiceMeeter](https://vb-audio.com/Voicemeeter/)
- For the Jar: [Java **15**](https://www.oracle.com/de/java/technologies/javase-jdk15-downloads.html) (Releases will include a (hopefully working) win64 bundle)
## Supported modes
- Baudot (various speeds)
- CCIR493-5
- CIS36-50 (50 baud only currently)
- (CROWD36)  
- FSK 200 Baud, 500 Hz Shift
- FSK 200 Baud, 1000 Hz Shift
- FSK Raw (Various Baud/Shift modes)
- GW FSK
- XPA1 (10/20 Baud)
- XPA2
- XPB (Beta)
## Libraries used
- [JTransforms](http://sites.google.com/site/piotrwendykier/software/jtransforms)
## Changelog
(This only includes the RivetZ changes. For the old changelog, please see oldreadme.txt)
### Build 90
- Now based on Java 15
- Various code improvements in Rivet.java 
- UTC now default time zone
- Shows info on startup if the settings xml doesn't exist
### Build 90.1
- New "Smooth XPA message decoding" option (Displays numbers in the main part live instead of using a line buffer). Looks better
### Build 91
- Added XPB Support (Beta)
