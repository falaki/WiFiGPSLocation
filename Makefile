all:
	ant clean
	ant release
	jarsigner -verbose -keystore ~/.android/my-release-key.keystore bin/WiFiGPSLocation-unsigned.apk mhf
	zipalign -v 4 bin/WiFiGPSLocation-unsigned.apk bin/WiFiGPSLocation.apk 

clean: 
	ant clean


