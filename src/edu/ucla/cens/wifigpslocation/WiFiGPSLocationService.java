/**
  * WiFiGPSLocationService
  *
  * Copyright (C) 2010 Center for Embedded Networked Sensing
  */
package edu.ucla.cens.wifigpslocation;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Calendar;
import java.util.Locale;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


import android.content.BroadcastReceiver;
import android.app.Service;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.RemoteCallbackList;
import android.os.PowerManager;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.database.Cursor;
import android.database.SQLException;


import edu.ucla.cens.systemsens.IPowerMonitor;
import edu.ucla.cens.systemsens.IAdaptiveApplication;

import edu.ucla.cens.systemlog.ISystemLog;
import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.accelservice.IAccelService;


/**
  * WiFiGPSLocation is an Android service to simplify duty-cycling of
  * the GPS receiver when a user is not mobile. The WiFiGPSLocation
  * application runs as an Android Service on the phone. It defines a
  * simple interface using the Android Interface Definition Language
  * (AIDL). All other applications get the last location of the user
  * through this interface from WiFiGPSLocation. Unlike the default
  * Android location API, the location API provided by WiFiGPSLocation
  * is synchronous (i.e., a call to getLocation() is guaranteed to
  * return immediately with the last location of the user.
  *
  * The WiFiGPSLocation constantly queries the GPS receiver to track
  * the location of the user. Upon a getLocation() request, it returns
  * the latest known location of the user. However, it tries to
  * duty-cycle the GPS receiver when it detects the user is not
  * mobile.  WiFiGPSLocation uses the WiFi RF fingerprint to detect
  * when a user is not moving to turn off GPS, and when the user
  * starts moving to turn it back on. This document outlines the
  * design and implementation of WiFiGPSLocation, and provides
  * instructions on how to use it in other applications.
  *
  * @author Hossein Falaki
 */
public class WiFiGPSLocationService 
    extends Service 
    implements LocationListener 
{
    /** Name of the service used logging tag */
    private static final String TAG = "WiFiGPSLocationService";

    /** Version of this service */
    public static final String VER = "2.0";


    /** Name of this application */
    public static final String APP_NAME = "WiFiGPSLocation";


    /** Work unit names */
    public static final String GPS_UNIT_NAME = "GPS";
    public static final String WIFISCAN_UNIT_NAME = "WiFiScan";


    /** Types of messages used by this service */
    private static final int LOC_UPDATE_MSG = 1;


    /** Action strings for alarm events */
    private static final String WIFISCAN_ALARM_ACTION =
        "wifiscan_alarm";
    private static final String CLEANUP_ALARM_ACTION =
        "cleanup_alarm";


    /** Time unit constants */
    public static final int  ONE_SECOND = 1000;
    public static final int  ONE_MINUTE = 60 * ONE_SECOND;
    public static final int  ONE_HOUR = 60 * ONE_MINUTE; 
    public static final int  ONE_DAY = 24 * ONE_HOUR;

    /** Default timers in milliseconds*/
    private static final int  DEFAULT_WIFI_SCANNING_INTERVAL = 2 * ONE_MINUTE; 
    private static final int  DEFAULT_GPS_SCANNING_INTERVAL = 60 * ONE_SECOND; 
    private static final int  CLEANUP_INTERVAL = ONE_HOUR; 
    private static final int  DEFAULT_POWERCYCLE_HORIZON = ONE_HOUR;


    private static final int  LOC_UPDATE_TIMEOUT = 5 * ONE_SECOND;
    private static final int  CACHE_TIMEOUT = 3 * ONE_DAY; 
    private static final int  EXTENTION_TIME = ONE_HOUR;;


    /** Threshold values */
    private static final int SIGNAL_THRESHOLD = -80;
    private static final double GPS_ACCURACY_THRESHOLD = 10.0;
    private static final int SIGNIFICANCE_THRESHOLD = 3;

    /** Provider strings */
    private static final String WIFIGPS_PROVIDER =
        "WiFiGPSLocation:GPS";
    private static final String FAKE_PROVIDER =
        "WiFiGPSLocation:Fake";
    private static final String APPROX_PROVIDER =
        "WiFiGPSLocation:Approx";




    /** State variable indicating if the services is running or not */
    private boolean mRun;

    /** DB Adaptor */
    private DbAdaptor mDbAdaptor;

    /** State variable indicating if the GPS location is being used */
    private boolean mGPSRunning;

    /** Counter for the number of connected clients */
    private int mClientCount = 0;

    /** List of callback objects */
    private RemoteCallbackList<ILocationChangedCallback> mCallbacks;

    /** Counter for the callbacks */
    private int mCallbackCount = 0;

    /** AccelService object */
    private IAccelService mAccelService;
    private boolean mAccelConnected;


    /** PowerMonitor object */
    private IPowerMonitor mPowerMonitor;
    private boolean mPowerMonitorConnected = false;



    /** WiFi object used for scanning */
    private WifiManager mWifi;


    /** WiFi wake lock object */
    private WifiLock mWifiLock;


    /** CPU wake lock */
    private PowerManager.WakeLock mCpuLock;

    /** Alarm Manager object */
    private AlarmManager mAlarmManager;


    /** Pending Intent objects */
    private PendingIntent mScanSender;
    private PendingIntent mCleanupSender;


    /** Location manager object to receive location objects */
    private LocationManager mLocManager;


    /** GPS manager object */
    private GPSManager mGPSManager;

    /** Scan manager object */
    private ScanManager mScanManager;

    /** MessageDigest object to compute MD5 hash */
    private MessageDigest mDigest;

    /** Map of WiFi scan results to to GPS availability */ 
    private HashMap<String, GPSInfo> mScanCache;

    /** The last known location object */
    private Location mLastKnownLoc;

    /** Temporary location object that is not accurate enough */
    private Location mTempKnownLoc;

    /** Digetst String of the last seen WiFi set*/
    private String mLastWifiSet;

    /** Set of the last scan result  */
    private List<ScanResult> mScanResults;
    private Calendar mWifiScanTime;

    /** Fake location object */
    private Location mFakeLocation;

    /** Scanning interval variable */
    private int mWifiScanInterval;
    private int mGpsScanInterval;




    private final IAdaptiveApplication mAdaptiveControl
        = new IAdaptiveApplication.Stub()
    {
        public String getName()
        {
            return APP_NAME;
        }

        public List<String> identifyList()
        {
            ArrayList<String> unitNames = new ArrayList(2);

            unitNames.add(GPS_UNIT_NAME);
            unitNames.add(WIFISCAN_UNIT_NAME);

            return unitNames;
        }

        public List<Double> getWork()
        {
            ArrayList<Double> totalWork = new ArrayList<Double>();

            totalWork.add(mGPSManager.getMinutes());
            totalWork.add(mScanManager.getWork());

            return totalWork;
        }

        public void setWorkLimit(List  workLimit)
        {
            double gpsLimit = (Double) workLimit.get(0);
            double scanLimit = (Double) workLimit.get(1);

            mGPSManager.setLimit(gpsLimit);
            mScanManager.setLimit(scanLimit);
        }


    };
    

    private final IWiFiGPSLocationService.Stub mBinder = new IWiFiGPSLocationService.Stub()
    {

        public static final String FAKE_PROVIDER =  
            WiFiGPSLocationService.FAKE_PROVIDER; 

        public static final String WIFIGPS_PROVIDER =
            WiFiGPSLocationService.WIFIGPS_PROVIDER;

        public static final String APPROX_PROVIDER=
            WiFiGPSLocationService.APPROX_PROVIDER;



        /**
         * Returns the current location. 
         * If the service is not active, this call will activate it.
         *
         * @return		the last known location
         */
        public Location getLocation ()
        {
            if (!mRun)
                start();

            if (mLastKnownLoc != null)
                return mLastKnownLoc;
            else
                return mFakeLocation;
        }


        /**
         * Returns a String dump of last visible WiFi access points. 
         * The returned String can be interpreted as a JSON object. Each
         * key is the BSSID of a WiFi AP and the corresponding value is 
         * the signal strength in dBm.
         *
         * @return              JSON object containing visible WiFi APs
         */
        public String getWiFiScan()
        {
            JSONObject scanJson, scanResult = new JSONObject();
            JSONArray scanList = new JSONArray();
            SimpleDateFormat sdf = 
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);


            try
            {
                for (ScanResult result: mScanResults)
                {
                    scanJson = new JSONObject();
                    scanJson.put("ssid", result.BSSID);
                    scanJson.put("strength", result.level);
                    scanList.put(scanJson);
                }
                scanResult.put("scan", scanList);
                scanResult.put("timestamp", 
                        sdf.format(mWifiScanTime.getTime()));


            }
            catch (JSONException je)
            {
                Log.e(TAG, "Could not write to JSONObject", je);
            }

            return scanResult.toString();

        }


        /**
         * Change the GPS sampling interval. 
         * 
         * @param		interval	GPS sampling interval in milliseconds
         */
        public int suggestInterval (int interval)
        {
            mGpsScanInterval = interval;

            return mGpsScanInterval;
        }


        /**
         * Registers a callback to be called when location changes.
         *
         *
         */
        public void registerCallback(ILocationChangedCallback callback, double threshold)
        {
            if (callback != null)
            {
                mCallbacks.register(callback, new Double(threshold));
                mCallbackCount++;
            }


            if ((mCallbackCount == 1) && mAccelConnected)
            {
                try
                {
                    mAccelService.start();
                }
                catch (RemoteException re)
                {
                    Log.e(TAG, "Exception when starting AccelService", re);
                }
            }
            else
            {
                Log.i(TAG, "Not connected to AccelService");
            }


        }

        /**
         * Unregisters the callback
         *
         */
        public void unregisterCallback(ILocationChangedCallback callback)
        {
            if (callback != null)
            {
                mCallbacks.unregister(callback);
                mCallbackCount--;
            }

            if (mCallbackCount < 0 )
                mCallbackCount = 0;

            if ((mCallbackCount == 0) && mAccelConnected)
            {
                try
                {
                    mAccelService.stop();
                }
                catch (RemoteException re)
                {
                    Log.e(TAG, "Exception when stoping AccelService", re);
                }
            }
            else
            {
                Log.w(TAG, "Not connected to AccelService");
            }



        }

        /**
         * Puts the WiFiGPSLocationService in an "inactive" mode.
         * Cancels are pending scans and sets the Run flag to stop.
         * The Android service is still running and can receive calls,
         * but it does not perform any energy consuming tasks.
         * 
         */
        public void stop ()
        {
            mClientCount--;
            Log.i(TAG, "Received a stop() call");

            if (mClientCount <= 0)
            {
                Log.i(TAG, "Stoping operations");
                // Cancel pending alarms
                mAlarmManager.cancel(mScanSender);
                mAlarmManager.cancel(mCleanupSender);
 
                if (mWifiLock.isHeld())
                    mWifiLock.release();
                mRun = false; 
                mClientCount = 0;
            }
            else
            {
                Log.i(TAG, "Continuing operations");
            }

        }

        /**
         * Starts the WiFiGPSLocationService.
         * Schedules a new scan message and sets the Run flag to true.
         */
        public void start ()
        {			
            Log.i(TAG, "Received a start() call");
            mClientCount++;
            if (!mRun)
            {
                mRun = true;
                setupWiFi();

                // Fire the WiFi scanning alarm
                long now = SystemClock.elapsedRealtime();
                mAlarmManager.setRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        now, mWifiScanInterval, mScanSender);


                // Fire the cleanup alarm
                mAlarmManager.setRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        now + CLEANUP_INTERVAL, 
                        CLEANUP_INTERVAL, mCleanupSender);


                // Start running GPS to get current location ASAP
                mGPSManager.start();
            }
        }


    };

	
    /**
     * Broadcast receiver for WiFi scan updates.
     * An object of this class has been passed to the system through 
     * registerReceiver. 
     *
     */
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {

            String action = intent.getAction();


            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            {
                mScanResults = mWifi.getScanResults();
                mWifiScanTime = Calendar.getInstance();
                

                Log.v(TAG, "WiFi scan found " + mScanResults.size() 
                        + " APs");

                List<String> sResult = new ArrayList<String>();

                for (ScanResult result : mScanResults)
                {
                    //It seems APs with higher signal strengths are
                    //more stable.  So I am ignoring weak APs.
                    if (result.level > SIGNAL_THRESHOLD)
                        sResult.add(result.BSSID);

                    Log.v(TAG, result.BSSID + " (" + result.level + "dBm)");
                }

                Log.v(TAG, "Filtered " 
                    + (mScanResults.size() - sResult.size()) 
                    + " APs.");

                Collections.sort(sResult);
                updateLocation(sResult);
            }
            else if (action.equals( 
                        WifiManager.WIFI_STATE_CHANGED_ACTION)) 
            {
                int wifiState = (int) 
                intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (wifiState == WifiManager.WIFI_STATE_DISABLED)
                {
                    Log.v(TAG, "User disabeled Wifi." +
                        " Setting up WiFi again");
                        setupWiFi();
                }
            }


            if (mCpuLock.isHeld())
                mCpuLock.release();

        }
    };



    public synchronized void onLocationChanged(Location location) 
    {
        double accuracy =  location.getAccuracy();
        Log.i(TAG, "Received location update. Accuracy: " + accuracy);

        if ( accuracy < GPS_ACCURACY_THRESHOLD)
        {

            mHandler.removeMessages(LOC_UPDATE_MSG);
            mLastKnownLoc = location;

            if (mScanCache.containsKey(mLastWifiSet))
            {
                if (!mScanCache.get(mLastWifiSet).known)
                {
                    Log.i(TAG, "Updating the record: " + 
                    cacheEntry(mLastWifiSet));
                    mScanCache.get(mLastWifiSet).known = true;
                    mScanCache.get(mLastWifiSet).loc = location;
                    mLastKnownLoc = location;
                    mLastKnownLoc.setProvider(WIFIGPS_PROVIDER);
                }
                else
                {
                    Log.v(TAG, "There is a valid record. "  
                        + "but still updating " 
                        + cacheEntry(mLastWifiSet) );
                    mScanCache.get(mLastWifiSet).loc = location;
                    mLastKnownLoc = location;				
                    mLastKnownLoc.setProvider(WIFIGPS_PROVIDER);
                }
            }
        }
        else
        {
            //Log.v(TAG, "Not accurate enough.");
            mTempKnownLoc = location;
            mTempKnownLoc.setProvider(WIFIGPS_PROVIDER);
            mHandler.removeMessages(LOC_UPDATE_MSG);
            mHandler.sendMessageAtTime(
                mHandler.obtainMessage(LOC_UPDATE_MSG),
                SystemClock.uptimeMillis() + LOC_UPDATE_TIMEOUT);

        }
    }
	
    public void onStatusChanged(String provider, 
            int status, Bundle extras) 
    {
        // TODO Auto-generated method stub

    }
        
    public void onProviderEnabled(String provider) 
    {
        // TODO Auto-generated method stub

    }

    public void onProviderDisabled(String provider) 
    {
        // TODO Auto-generated method stub

    }
    
    private synchronized void updateLocation(List<String> wifiSet)
    {
        long curTime = System.currentTimeMillis();
        GPSInfo record;
        byte[] byteKey = mDigest.digest(wifiSet.toString().getBytes());
        String key = new String(byteKey);


        Log.v(TAG, "Updating cache for: " + wifiSet.toString());


        //TODO: If there is no wifi still check for accelearion.


        // First check if the current WiFi signature is different
        // from the last visited Wifi set. If they are different,
        // check acceleration. If acceleration is "high" call
        // each registered client
        if ((!mLastWifiSet.equals(key)) || (wifiSet.size() == 0) )
        {
            final int N = mCallbacks.beginBroadcast();
            if ((N > 0) && mAccelConnected)
            {
                Log.v(TAG, "Checking for acceleration threshold.");

                double threshold;
                ILocationChangedCallback callBack;

                for (int i = 0; i < N; i++)
                {
                    threshold = (Double) mCallbacks.getBroadcastCookie(i);
                    callBack = mCallbacks.getBroadcastItem(i);

                    try
                    {
                        if (mAccelService.significantForce(threshold))
                            callBack.locationChanged();
                        Log.i(TAG, "Exceeded " + threshold); 
                    }
                    catch (RemoteException re)
                    {
                        Log.e(TAG, "Exception when calling AccelService", 
                                re);
                    }
                }

                }
                mCallbacks.finishBroadcast();

            }


            if (mScanCache.containsKey(mLastWifiSet))
            {
                if (!mScanCache.get(mLastWifiSet).known)
                {
                    Log.i(TAG, "Concluded no lock for last WiFi set");
                    mScanCache.get(mLastWifiSet).known = true;
                }
            }


            // First thing, if the set is "empty", I am at a location with
            // no WiFi coverage. We default to GPS scanning in such
            // situations. So turn on GPS and return
            if (wifiSet.size() == 0)
            {
                Log.i(TAG, "No WiFi AP found.");
                mGPSManager.start();
                return;
            }


            Log.v(TAG, "Current cache has " 
                    + mScanCache.size() 
                    + " entries.");

            if (mScanCache.containsKey(key))
            {
                mScanCache.get(key).increment();
                record = mScanCache.get(key);
                Log.i(TAG, "Found a record: " 
                        + record.toString());

                if (record.count <= SIGNIFICANCE_THRESHOLD)
                {
                    Log.i(TAG, "Not significant yet. "
                           + "Still need to run GPS.");

                    mGPSManager.start();

                }
                else if (record.count > SIGNIFICANCE_THRESHOLD)
                {

                    Log.i(TAG, "Significant record.");

                    // update the time stamp of the 
                    // last known GPS location
                    if (record.loc != null)
                    {
                        // If the matching record has a 
                        // location object use that
                        Log.v(TAG, "Using known location.");
                        mLastKnownLoc = record.loc;
                    }
                    // Instead of passing a fake location object
                    // we will return the last observed location 
                    // as an approximation
                    else
                    {
                        // If the matching record does not 
                        // have a location object
                        Log.i(TAG, "Using approx location.");
                        mLastKnownLoc.setProvider(APPROX_PROVIDER);
                    }


                    // We do not update the 'fix' time 
                    // of the location to allow the client
                    // application have a measure of how stale
                    // the location object is.
                    /* mLastKnownLoc.setTime(curTime); */

                    mLastKnownLoc.setSpeed(0);

                    mGPSManager.stop();
                }
            }
            else
            {
                Log.i(TAG, "New WiFi set.");

                // Schedule a GPS scan
                record = new GPSInfo(false, curTime);
                mScanCache.put(key, record);
                Log.i(TAG, "Created new cache entry: " 
                        + record.toString());

                mGPSManager.start();

            }

        mLastWifiSet = key;

    }
    
    /**
     * Message handler object.
     */
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what == LOC_UPDATE_MSG)
            {
                Log.i(TAG, "Dealing with inaccurate location. "
                        + "Accuracy: " 
                        + mTempKnownLoc.getAccuracy());

                mLastKnownLoc = mTempKnownLoc;

                if (mScanCache.containsKey(mLastWifiSet))
                {
                    if (!mScanCache.get(mLastWifiSet).known)
                    {
                        Log.i(TAG, "Updating the record: " + 
                                cacheEntry(mLastWifiSet));
                        mScanCache.get(mLastWifiSet).known = true;
                        mScanCache.get(mLastWifiSet).loc =
                        mTempKnownLoc;
                    }
                }
                else
                {
                    Log.v(TAG, "No familar WiFi signature");
                }
            }

        }
    };

    
    /** 
     * Finds records in the cache that have been timed out.
     */
    private synchronized void cleanCache()
    {
    	long curTime = System.currentTimeMillis();
    	GPSInfo record, removed;
    	long cacheTime;
    	int count;
    	long timeout;
    	
    	HashSet<String> toBeDeleted = new HashSet<String>();
    	
    	Log.i(TAG, "Cleaning up the cache.");
    	Log.i(TAG, "Current cache has " + mScanCache.size() + " entries.");
    	
    	
    	for (String key: mScanCache.keySet())
    	{
    		record = mScanCache.get(key);
    		cacheTime = record.time;
    		count = record.count;
    		timeout = curTime - (cacheTime + count*EXTENTION_TIME);
    		Log.v(TAG, "Checking " + cacheEntry(key));

    		if (count < SIGNIFICANCE_THRESHOLD)
    		{
    			if (curTime - cacheTime > ONE_HOUR)
    			{
    				Log.i(TAG, "Marking transient record for deletion: " 
                            + record.toString());
    				toBeDeleted.add(key);
    			}
    		} 
    		else if (timeout > CACHE_TIMEOUT )
			{
				Log.i(TAG, "Marking stale record for deletion: " + 
                        record.toString());
				// The cache entry has timed out. Remove it!
				toBeDeleted.add(key);
			}
		}

    	try
    	{
	    	for (String delKey : toBeDeleted)
	    	{
	    		Log.i(TAG, "Deleting " + cacheEntry(delKey));
	    		removed = mScanCache.remove(delKey);
	    	}
    	}
    	catch (ConcurrentModificationException cme)
    	{
    		Log.e(TAG, "Exception while cleaning cache.", cme);
    	}


        mDbAdaptor.syncDb(mScanCache);

        if (mCpuLock.isHeld())
            mCpuLock.release();

    }


    private ServiceConnection mAccelServiceConnection 
            = new ServiceConnection() 
    {
        public void onServiceConnected(ComponentName className,
                IBinder service)
        {
            mAccelService = IAccelService.Stub.asInterface(service);
            mAccelConnected = true;

        }

        public void onServiceDisconnected(ComponentName className)
        {
            mAccelService = null;
            mAccelConnected = false;
        }

    };
    

    private ServiceConnection mPowerMonitorConnection 
            = new ServiceConnection() 
    {

        public void onServiceConnected(ComponentName className,
                IBinder service)
        {
            mPowerMonitor = IPowerMonitor.Stub.asInterface(service);
            try
            {
              mPowerMonitor.register(mAdaptiveControl, 
                      DEFAULT_POWERCYCLE_HORIZON);
            }
            catch (RemoteException re)
            {
                Log.e(TAG, "Could not register AdaptivePower object.",
                        re);
            }

            mPowerMonitorConnected = true;
        }

        public void onServiceDisconnected(ComponentName className)
        {

            try
            {
                mPowerMonitor.unregister(mAdaptiveControl);
            }
            catch (RemoteException re)
            {
                Log.e(TAG, "Could not unregister AdaptivePower object.",
                        re);
            }

            mPowerMonitor = null;
            mPowerMonitorConnected = false;

        }



    };


	
    @Override
    public IBinder onBind(Intent intent)
    {
        if (IWiFiGPSLocationService.class.getName().equals( 
                    intent.getAction())) 
        {
            return mBinder;
        }

        return null;		
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        if (!mPowerMonitorConnected)
        {
            Log.i(TAG, "Rebinding to PowerMonitor");
            bindService(new Intent(IPowerMonitor.class.getName()),
                    mPowerMonitorConnection, Context.BIND_AUTO_CREATE);
        }

        if (!mAccelConnected)
        {
            Log.i(TAG, "Rebinding to AccelService");
            bindService(new Intent(IAccelService.class.getName()),
                    mAccelServiceConnection, Context.BIND_AUTO_CREATE);
        }

        if (intent != null)
        {
            String action = intent.getAction();

            if (action != null)
            {

                Log.i(TAG, "Received action: " + action);
                if (action.equals(WIFISCAN_ALARM_ACTION))
                {
                    if (!mCpuLock.isHeld())
                        mCpuLock.acquire(); // Released by WiFi receiver
                    mScanManager.scan();
                }
                else if (action.equals(CLEANUP_ALARM_ACTION))
                {
                    if (!mCpuLock.isHeld())
                        mCpuLock.acquire(); // Released by cleanCache()
                    cleanCache();
                }
            }

        }

        //super.onStart(intent, startId);
        //Log.i(TAG, "onStart");

    }
	
    @Override
    public void onCreate() 
    {
        super.onCreate();

        bindService(new Intent(ISystemLog.class.getName()),
                Log.SystemLogConnection, Context.BIND_AUTO_CREATE);

        bindService(new Intent(IAccelService.class.getName()),
                mAccelServiceConnection, Context.BIND_AUTO_CREATE);

        bindService(new Intent(IPowerMonitor.class.getName()),
                mPowerMonitorConnection, Context.BIND_AUTO_CREATE);




        Log.setAppName(APP_NAME);


        mCallbacks = new RemoteCallbackList<ILocationChangedCallback>();

        
     
        Log.i(TAG, "onCreate");

        mDbAdaptor = new DbAdaptor(this);

        mGPSManager = new GPSManager();
        mScanManager = new ScanManager();

        try
        {
            mDbAdaptor.open();
        }
        catch(SQLException e)
        {
            Log.e(TAG, "Exception", e);
        }

		//Initialize the scan cache 
		mScanCache = new HashMap<String, GPSInfo>();


        Log.i(TAG, "Reading last saved cache");
        readDb();


        
        mFakeLocation = new Location(FAKE_PROVIDER);
        mFakeLocation.setLatitude(Double.NaN);
        mFakeLocation.setLongitude(Double.NaN);
        mFakeLocation.setSpeed(0);

        mLastKnownLoc = mFakeLocation;
        
        mLastWifiSet = " ";
        
        resetToDefault();
        
        try
        {
     	   mDigest = java.security.MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException nae)
        {
     	   Log.e(TAG, "Exception", nae);
        }


        PowerManager pm = (PowerManager) this.getSystemService(
                Context.POWER_SERVICE);
        mCpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                APP_NAME);
        mCpuLock.setReferenceCounted(false);


        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        setupWiFi();
        
        //Register to receive WiFi scans 
        registerReceiver(mWifiScanReceiver, new IntentFilter( 
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //Register to receive WiFi state changes
        registerReceiver(mWifiScanReceiver, new IntentFilter( 
                    WifiManager.WIFI_STATE_CHANGED_ACTION));


        // Set up alarms for repeating events.
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // WiFi scan Intent objects
        Intent scanAlarmIntent = new Intent(WiFiGPSLocationService.this,
                WiFiGPSLocationService.class);
        scanAlarmIntent.setAction(WIFISCAN_ALARM_ACTION);
        mScanSender = PendingIntent.getService(
                WiFiGPSLocationService.this, 0, scanAlarmIntent, 0);


        // Cache cleanup Intent objects
        Intent cleanupAlarmIntent = new Intent(WiFiGPSLocationService.this,
                WiFiGPSLocationService.class);
        cleanupAlarmIntent.setAction(CLEANUP_ALARM_ACTION);
        mCleanupSender = PendingIntent.getService(
                WiFiGPSLocationService.this, 0, cleanupAlarmIntent, 0);




		
    }
    
    @Override
    public void onDestroy()
    {
        mDbAdaptor.syncDb(mScanCache);

    	// Remove pending WiFi scan messages
    	mHandler.removeMessages(LOC_UPDATE_MSG);


        // Cancel the pending alarms
        mAlarmManager.cancel(mScanSender);
        mAlarmManager.cancel(mCleanupSender);
    	
    	// Cancel location update registration 
		mLocManager.removeUpdates(this);
		mGPSRunning = false;
        mGPSManager.stop();
		
		// Cancel WiFi scan registration
		unregisterReceiver(mWifiScanReceiver);
        unbindService(Log.SystemLogConnection);
        unbindService(mAccelServiceConnection);
    }

    private void readDb()
    {
        String sign;
        int count, hasloc;
        double lat, lon, acc, loctime;
        long time;
        GPSInfo gpsInfo;
        Location curLoc;
        String provider;

        Cursor c = mDbAdaptor.fetchAllEntries();

        int timeIndex = c.getColumnIndex(DbAdaptor.KEY_TIME);
        int countIndex = c.getColumnIndex(DbAdaptor.KEY_COUNT);
        int signIndex = c.getColumnIndex(DbAdaptor.KEY_SIGNATURE);
        int latIndex = c.getColumnIndex(DbAdaptor.KEY_LAT);
        int lonIndex = c.getColumnIndex(DbAdaptor.KEY_LON);
        int accIndex = c.getColumnIndex(DbAdaptor.KEY_ACC);
        int locTimeIndex = c.getColumnIndex(DbAdaptor.KEY_LOCTIME);
        int providerIndex = c.getColumnIndex(DbAdaptor.KEY_PROVIDER);
        int haslocIndex = c.getColumnIndex(DbAdaptor.KEY_HASLOC);

        int dbSize = c.getCount();

        Log.i(TAG, "Found " + dbSize + " entries in database.");

        c.moveToFirst();

        for (int i = 0; i < dbSize; i++)
        {

            try
            {
                time = c.getInt(timeIndex);
                count = c.getInt(countIndex);
                sign = c.getString(signIndex);
                hasloc = c.getInt(haslocIndex);




                gpsInfo = new GPSInfo(true, time);


                if (hasloc == DbAdaptor.YES)
                {
                    lat = c.getDouble(latIndex);
                    lon = c.getDouble(lonIndex);
                    acc = c.getDouble(accIndex);
                    loctime = c.getDouble(locTimeIndex);
                    provider = c.getString(providerIndex);

                    curLoc = new Location(provider);
                    curLoc.setLatitude(lat);
                    curLoc.setLongitude(lon);
                    curLoc.setTime((long)loctime);
                    curLoc.setAccuracy((float)acc);
                    gpsInfo.loc = curLoc;
                }
                else
                {
                    Log.i(TAG, "Entry with no location.");
                }

                gpsInfo.count = count;
                mScanCache.put(sign, gpsInfo);
                Log.i(TAG, "Synced " + gpsInfo.toString());

            }
            catch (Exception dbe)
            {
                Log.e(TAG, "Error reading a db entry", dbe);
            }
            c.moveToNext();
        }
        c.close();
    }
    

    
    private void setupWiFi()
    {
        // Check if WiFi is enabled
        if (!mWifi.isWifiEnabled())
            mWifi.setWifiEnabled(true);
        
        if (mWifi == null)
        {
            mWifiLock = mWifi.createWifiLock(
                    WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
            mWifiLock.setReferenceCounted(false);

            if (!mWifiLock.isHeld())
                mWifiLock.acquire();
        }
        else
        {
            mWifiLock = mWifi.createWifiLock(
                    WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
            if (!mWifiLock.isHeld())
                mWifiLock.acquire();
        }
    }
    
    /*
     * Sets all operational parameters to their default values
     */
    private void resetToDefault()
    {
        mWifiScanInterval = DEFAULT_WIFI_SCANNING_INTERVAL;
        mGpsScanInterval = DEFAULT_GPS_SCANNING_INTERVAL;
    }


    private String cacheEntry(String wifiSet)
    {
        String res = "";

        if (mScanCache.containsKey(wifiSet))
        {
            res +=  mScanCache.get(wifiSet);
        }
        else
        {
            res += "null";
        }

        return res;
    }


    class GPSManager
    {
        private double mLimit = Double.NaN;

        private double mTotal;
        private double mCurTotal;
        private double mStart;


        public GPSManager()
        {
            mTotal = 0.0;
        }

        public void setLimit(double workLimit)
        {
            mLimit = workLimit * ONE_MINUTE;
            mCurTotal = 0.0;
        }


        public boolean start()
        {
            if (!mGPSRunning)
            {
                if ( Double.isNaN(mLimit) || (mCurTotal < mLimit) )
                {
                    mStart = SystemClock.elapsedRealtime();
                    Log.i(TAG, "Starting GPS.");
                    mLocManager.requestLocationUpdates( 
                            LocationManager.GPS_PROVIDER, 
                            mGpsScanInterval, 0,
                            WiFiGPSLocationService.this);
                    mGPSRunning = true;
                    return mGPSRunning;
                }
                else 
                {
                    Log.i(TAG, "No budget to start GPS.");
                    return mGPSRunning;
                }
            }
            else
            {
                if ( !Double.isNaN(mLimit) && (mCurTotal > mLimit) )
                {

                    Log.i(TAG, "Ran out of GPS budget.");
                    mLocManager.removeUpdates(WiFiGPSLocationService.this);
                    Log.i(TAG, "Stopping GPS.");
                    mGPSRunning = false;
                    return mGPSRunning;
                }
                else
                {
                    Log.i(TAG, "Continue scanning GPS.");
                    return mGPSRunning;
                }
            }


        }

        public void stop()
        {
            if (mGPSRunning)
            {
                mLocManager.removeUpdates(WiFiGPSLocationService.this);
                Log.i(TAG, "Stopping GPS.");

                double current =  SystemClock.elapsedRealtime();
                mTotal += (current - mStart);
                mCurTotal += (current - mStart);
                mGPSRunning = false;
            }

        }

        public double getMinutes()
        {
            double res;

            if (!mGPSRunning)
            {
                res =  mTotal/ONE_MINUTE;
            }
            else
            {
                double current =  SystemClock.elapsedRealtime();
                double currentTotal = mTotal + (current - mStart);
                res =  currentTotal/ONE_MINUTE;
            }

            return res;
        }
    }


    class ScanManager
    {
        double mTotal = 0.0;
        double mCurTotal = 0.0;

        double mLimit = Double.NaN;

        public boolean scan()
        {

            if ( Double.isNaN(mLimit) || (mCurTotal < mLimit) )
            {
                mWifi.startScan();
                mTotal += 1.0;
                mCurTotal += 1.0;
                return true;
            }

            if ( !Double.isNaN(mLimit) && (mCurTotal >= mLimit) )
            {
                Log.i(TAG, "No budget to scan WiFi.");
                return false;
            }
            
            return false;

        }

        public void setLimit(double workLimit)
        {
            mLimit = workLimit;
            mCurTotal = 0.0;
        }

        public double getWork()
        {
            return mTotal;
        }
    }

        
    
}


