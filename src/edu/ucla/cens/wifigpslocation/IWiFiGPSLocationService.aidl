package edu.ucla.cens.wifigpslocation;

import android.location.Location;

import edu.ucla.cens.wifigpslocation.ILocationChangedCallback;

interface IWiFiGPSLocationService {
    /**
     * Returns the current location. 
     *
     * @return		the last known location
     */
    Location getLocation ();


    /**
     * Change the GPS sampling interval. It is interpreted as
     * a suggestion. It will return the actual value that was applied.
     * 
     * @param		interval	GPS sampling interval in milliseconds
     * @return					actual GPS sampling interval in milliseconds
     */
    int suggestInterval (int interval);

    /**
     * Registers the locationChanged callback along with the
     * acceleration threshold to be used. When the service notices a
     * change in location based on WiFi signature, it calls 
     * the callback method.
     *
     * @param   callback        the Callback object
     * @param   threshold       acceleration threshold value
     */
     void registerCallback(ILocationChangedCallback callback);

     /**
      * Unregisteres the callback
      * 
      * @param  callback        the callback to be unregistered
      */
     void unregisterCallback(ILocationChangedCallback callback);


    /**
     * Stops the WiFiGPSLocationService if no other client
     * is using the service. Make sure you make this call when you are
     * certain that your application does not need location
     * information.
     * 
     */
    void stop ();

    /**
     * Starts the WiFiGPSLocationService if it has not been started
     * before. 
     * 
     */
    void start ();

    /**
     * Returns a String dump of last visible WiFi access points. 
     * The returned String can be interpreted as a JSON object. Each
     * key is the BSSID of a WiFi AP and the corresponding value is 
     * the signal strength in dBm.
     *
     * @return              JSON object containing visible WiFi APs
     */
    String getWiFiScan();
}
