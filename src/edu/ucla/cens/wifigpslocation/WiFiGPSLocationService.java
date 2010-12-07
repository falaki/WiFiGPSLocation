package edu.ucla.cens.wifigpslocation;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ConcurrentModificationException;


import android.content.BroadcastReceiver;
import android.app.Service;
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
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.database.Cursor;
import android.database.SQLException;



import edu.ucla.cens.systemlog.ISystemLog;
import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.accelservice.IAccelService;
//import android.util.Log;


/**
 * WiFiGPSLocationService runs as a service that continuously 
 * scans for visible WiFi access points. 
 * Based on the WiFi AP signature, it infers if
 * the user is at a location with GPS or not. If it detects that 
 * GPS is available it will constantly poll GPS with a given interval,
 * and return the last location to its clients. If the user is at a
 * location without GPS, it will return the last known GPS location to
 * clients.
 */
public class WiFiGPSLocationService 
    extends Service 
    implements LocationListener 
{
	/** name of the service used for debug bridge */
	private static final String TAG = "WiFiGPSLocationService";
	
	/** Version of this service */
	public static final String VER = "1.0";
	
	/** State variable indicating if the services should run or not */
	private boolean mRun;

	/** Operational power consumption regime variable*/
	private int mRegime;

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

	
	/** Operational power consumption regime constant values*/
	public static final int REGIME_RELAXED = 0;
	public static final int REGIME_CONTROLLED = 1;
	
	/** Types of messages used by this service */
	private static final int WIFI_SCAN_TIMER_MSG = 1;
	private static final int CACHE_CLEANUP_TIMER_MSG = 2;
	private static final int LOC_UPDATE_MSG = 3;

	
	/** Time unit constants */
	private static final int ONE_SECOND = 1000;
	private static final int ONE_MINUTE = 60 * ONE_SECOND;
	private static final int ONE_HOUR = 60 * ONE_MINUTE; 
	private static final int ONE_DAY = 24 * ONE_HOUR;
	
	/** Default timers in milliseconds*/
	private static final int DEFAULT_WIFI_SCANNING_INTERVAL 
        = 2 * ONE_MINUTE; // Two minutes
	private static final int DEFAULT_GPS_SCANNING_INTERVAL 
        = 60 * ONE_SECOND; // One minute
	private static final int CLEANUP_INTERVAL 
        = ONE_HOUR; // One hour
	
    private static final int LOC_UPDATE_TIMEOUT = 5 * ONE_SECOND;
	private static final int CACHE_TIMEOUT 
        = 3 * ONE_DAY; // Three days
	private static final int EXTENTION_TIME 
        = 10 * ONE_MINUTE; // Ten minutes
	
	private static final int SIGNAL_THRESHOLD 
        = -90;
    private static final double GPS_ACCURACY_THRESHOLD 
        = 10.0;
	private static final int SIGNIFICANCE_THRESHOLD 
        = 3;
    
    /** WiFi object used for scanning */
    private WifiManager mWifi;
    
    private WifiLock mWifiLock;
    
    private LocationManager mLocManager;
    
    private MessageDigest mDigest;
    
    /** Map of WiFi scan results to to GPS availability */ 
    private HashMap<String, GPSInfo> mScanCache;
    
    /** The last known location object */
    private Location mLastKnownLoc;

    /** Temporary location object that is not accurate enough */
    private Location mTempKnownLoc;

    /** Last seen WiFi set*/
    private String mLastWifiSet;

    /** Fake location object */
    private Location mFakeLocation;
    
    /** Scanning interval variable */
    private int mWifiScanInterval;
    private int mGpsScanInterval;
    
    //private NotificationManager mNotificationManager;
    
    private final IWiFiGPSLocationServiceControl.Stub mControlBinder 
        = new IWiFiGPSLocationServiceControl.Stub()
    {
    	

    	/**
    	 * Sets the current operational regime.  REGIME_RELAXED
         * (0x00000000) is the default regime where the service can
         * take suggestions from its clients.  Other integer values
         * indicate next levels of power consumption limitations
    	 * 
    	 * @param		regime		new power consumption regime
    	 */ 
    	public void setOperationRegime(int regime)
    	{
    		mRegime = regime;
    		
    		if (regime == REGIME_RELAXED)
    			resetToDefault();
    	}
    	
    	
    	/**
    	 * Increases the GPS sampling interval of the service. 
    	 * The value after the modification is returned
    	 *
    	 * @return			current GPS sampling interval in milliseconds
    	 */ 
    	public int increaseInterval()
    	{
    		mRegime = REGIME_CONTROLLED;
    		mGpsScanInterval = mGpsScanInterval * 2;
    		
    		return mGpsScanInterval;
    		
    	}
    	
    	/**
    	 * Decreases the GPS sampling interval of the service.
    	 * The value after the modification is returned.
    	 *
    	 * @return			current GPS sampling interval in milliseconds
    	 */
    	public int decreaseInterval()
    	{
    		mRegime = REGIME_CONTROLLED;
    		mGpsScanInterval = mGpsScanInterval / 2;
    		
    		return mGpsScanInterval;
    		
    		
    	}
    	
    	/**
    	 * Sets the sampling interval of GPS and returns the current
         * value to verify.
         * 
    	 * @param		interval	new sampling interval in milliseconds
    	 * @return				current sampling interval in milliseconds
    	 */
    	public int setInterval(int interval)
    	{
    		mRegime = REGIME_CONTROLLED;
    		
    		mGpsScanInterval = interval;
    		
    		return mGpsScanInterval;
    	}
    	
    	/**
    	 * Retruns the current GPS sampling interval.
    	 *
    	 * @return			current GPS sampling interval in milliseconds
    	 */
    	 public int getInterval()
    	 {
    		 return mGpsScanInterval;
    	 }
    	
    	/**
    	 * Sets teh power consumption level.
    	 * 
    	 * @param		power		new power consumption level
    	 */
    	public void setPower(int power)
    	{
    		//TODO: Not implemented yet
    		
    	}
    	
    	/**
    	 * Returns the current power consumption level.
    	 * 
    	 * @return					current power consumption level
    	 */
    	public int getPower()
    	{
    		//TODO: not implemented yet.
    		return 0;
    	}
    	
    };
	
	private final IWiFiGPSLocationService.Stub mBinder 
        = new IWiFiGPSLocationService.Stub()
        {
		
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
			
			return mLastKnownLoc;
		}
		
		/**
		 * Change the GPS sampling interval. 
		 * 
		 * @param		interval	GPS sampling interval in milliseconds
		 */
		public int suggestInterval (int interval)
		{
			if (mRegime == REGIME_RELAXED)
				mGpsScanInterval = interval;
			
			return mGpsScanInterval;
		}


        /**
         * Registers a callback to be called when location changes.
         *
         *
         */
        public void registerCallback(ILocationChangedCallback callback, 
                double threshold)
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
        public void unregisterCallback(ILocationChangedCallback
                callback)
        {
            if (callback != null)
            {
                mCallbacks.unregister(callback);
                mCallbackCount--;

            }

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
                mHandler.removeMessages(WIFI_SCAN_TIMER_MSG);
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
				setupWiFi();
				
				mHandler.sendMessageAtTime( 
	        			mHandler.obtainMessage( WIFI_SCAN_TIMER_MSG), 
	        			SystemClock.uptimeMillis());
				mRun = true;
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
	    		List<ScanResult> results = mWifi.getScanResults();

	    		Log.v(TAG, "WiFi scan found " + results.size() 
                        + " APs");
	
	    		List<String> sResult = new ArrayList<String>();
	    		
	    		for (ScanResult result : results)
	    		{
	    			//It seems APs with higher signal strengths are
                    //more stable.  So I am ignoring weak APs.
	    			if (result.level > SIGNAL_THRESHOLD)
	    				sResult.add(result.BSSID);
                    Log.v(TAG, result.BSSID + " (" + result.level 
                            + "dBm)");
	    		}
	    	
	    		Log.v(TAG, "Filtered " 
                        + (results.size() - sResult.size()) 
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
	    		
    	}
    };
    

		
	public synchronized void onLocationChanged(Location location) {
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
                    Log.i(TAG, "updating the record: " + 
                            cacheEntry(mLastWifiSet));
                    mScanCache.get(mLastWifiSet).known = true;
                    mScanCache.get(mLastWifiSet).loc = location;
                    mLastKnownLoc = location;
                }
                else
                {
                    Log.v(TAG, "There is a valid record. "  
                            + "but still updating " 
                            + cacheEntry(mLastWifiSet) );
                    mScanCache.get(mLastWifiSet).loc = location;
                    mLastKnownLoc = location;				
                }
            }
        }
        else
        {
            Log.i(TAG, "Not accurate enough.");
            mTempKnownLoc = location;
            mHandler.removeMessages(LOC_UPDATE_MSG);
            mHandler.sendMessageAtTime(
                    mHandler.obtainMessage(LOC_UPDATE_MSG),
                    SystemClock.uptimeMillis() + LOC_UPDATE_TIMEOUT);

        }
	}
	
	//@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
    {
		// TODO Auto-generated method stub
		
	}
	
	//@Override
	public void onProviderEnabled(String provider) 
    {
		// TODO Auto-generated method stub
		
	}
	
	//@Override
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
	    		Log.i(TAG, "Concluded no lock for ["
                        + mLastWifiSet + "]");
	    		
	    		mScanCache.get(mLastWifiSet).known = true;
    		}
    	}
    	
    	
    	// First thing, if the set is "empty", I am at a location with
        // no WiFi coverage. We default to GPS scanning in such
        // situations. So turn on GPS and return
    	if (wifiSet.size() == 0)
    	{
			if (!mGPSRunning)
			{
				Log.i(TAG, "No WiFi AP found. Starting GPS.");
				mLocManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 
                        mGpsScanInterval, 0, this);
				mGPSRunning = true;
			}
			return;
    	}
    	
    	
    	Log.v(TAG, "Current cache has " + mScanCache.size() + " entries.");
    	
    	if (mScanCache.containsKey(key))
    	{
    		mScanCache.get(key).increment();
			record = mScanCache.get(key);
			Log.i(TAG, "Found a record: " + record.toString());
			
	    	if (record.count <= SIGNIFICANCE_THRESHOLD)
	    	{
	    		Log.i(TAG, "Not significant yet. Still need to run GPS.");
				if (!mGPSRunning)
				{
					Log.i(TAG, "Starting GPS.");
					mLocManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, 
                            mGpsScanInterval, 0, this);
					mGPSRunning = true;
				}
	    	}
			else if (record.count > SIGNIFICANCE_THRESHOLD)
			{
				
                Log.i(TAG, "Significant record.");
				// update the time stamp of the last known GPS location
				if (record.loc != null)
				{
                    // If the matching record has a location object use
                    // that
                    Log.v(TAG, "Using known location.");
					mLastKnownLoc = record.loc;
				}
                else
                {
                    // If the matching record does not have a location
                    // object
                    Log.i(TAG, "Using fake location.");
                    mLastKnownLoc = mFakeLocation;
                }
                
				
				mLastKnownLoc.setTime(curTime);
				mLastKnownLoc.setSpeed(0);
				
				if (mGPSRunning)
				{
					Log.i(TAG, "Stop scanning GPS" );
					mLocManager.removeUpdates(this);
					mGPSRunning = false;
				}
			}
		}
		else
		{
			Log.i(TAG, "Found a new WiFi set:" + wifiSet.toString());
			//Schedule a GPS scan
			record = new GPSInfo(false, curTime);
			mScanCache.put(key, record);
			if (!mGPSRunning)
			{
				Log.i(TAG, "Starting GPS.");
				mLocManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 
                        mGpsScanInterval, 0, this);
				mGPSRunning = true;
			}
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
            if (msg.what == WIFI_SCAN_TIMER_MSG)
            {
                mWifi.startScan();
                
                if (mRun)
                	mHandler.sendMessageAtTime( 
                			mHandler.obtainMessage( WIFI_SCAN_TIMER_MSG), 
                			SystemClock.uptimeMillis() 
                            + mWifiScanInterval);
            }
            else if (msg.what == CACHE_CLEANUP_TIMER_MSG)
            {
            	cleanCache();
            	if (mRun)
                    mHandler.sendMessageAtTime(
                    		mHandler.obtainMessage(CACHE_CLEANUP_TIMER_MSG
                                ),
                    		SystemClock.uptimeMillis() + CLEANUP_INTERVAL);
            }
            else if (msg.what == LOC_UPDATE_MSG)
            {
                Log.i(TAG, "Dealing with inaccurate location. "
                        + "Accuracy: " + mTempKnownLoc.getAccuracy());
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
    		
    		Log.i(TAG, "Checking " + cacheEntry(key));
    		
    		if (count < SIGNIFICANCE_THRESHOLD)
    		{
    			if (curTime - cacheTime > ONE_HOUR)
    			{
    				Log.v(TAG, "Marking transient record for deletion: " + 
                            record.toString());
    				toBeDeleted.add(key);
    			}
    		} 
    		else if (timeout > CACHE_TIMEOUT )
			{
				Log.v(TAG, "Marking stale record for deletion: " + 
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
    	
    }


    private ServiceConnection mAccelServiceConnection 
        = new ServiceConnection() {
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
    

	
	@Override
	public IBinder onBind(Intent intent)
	{
        if (IWiFiGPSLocationService.class.getName().equals(
                    intent.getAction())) 
        {
            return mBinder;
        }
        if (IWiFiGPSLocationServiceControl.class.getName().equals(
                    intent.getAction())) 
        {
            return mControlBinder;
        }
        
		return null;		
	}

    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);
        Log.i(TAG, "onStart");

    }
	
    @Override
    public void onCreate() {
        super.onCreate();

        bindService(new Intent(ISystemLog.class.getName()),
                Log.SystemLogConnection, Context.BIND_AUTO_CREATE);

        bindService(new Intent(IAccelService.class.getName()),
                mAccelServiceConnection, Context.BIND_AUTO_CREATE);



        mCallbacks = new RemoteCallbackList<ILocationChangedCallback>();

        
     
        Log.i(TAG, "onCreate");

        mDbAdaptor = new DbAdaptor(this);

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


        
        mFakeLocation = new Location("fake");
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


        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        setupWiFi();
        
        //Register to receive WiFi scans 
		registerReceiver(mWifiScanReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		//Register to receive WiFi state changes
		registerReceiver(mWifiScanReceiver, new IntentFilter(
				WifiManager.WIFI_STATE_CHANGED_ACTION));
		
		//Send a message to schedule the first scan
        mHandler.sendMessageAtTime( 
        		mHandler.obtainMessage(WIFI_SCAN_TIMER_MSG), 
        		SystemClock.uptimeMillis() + mWifiScanInterval);
        
        mHandler.sendMessageAtTime(
        		mHandler.obtainMessage(CACHE_CLEANUP_TIMER_MSG),
        		SystemClock.uptimeMillis() + CLEANUP_INTERVAL);

		//The services is running by default. Stop() needs to be called
		//to stop it.
		mRun = true;
		
		
		// Start running GPS to get current location ASAP
		Log.i(TAG, "Starting GPS.");
		mLocManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 
                mGpsScanInterval, 0, this);
		mGPSRunning = true;

    }
    
    @Override
    public void onDestroy()
    {
        mDbAdaptor.syncDb(mScanCache);
    	// Remove pending WiFi scan messages
    	mHandler.removeMessages(WIFI_SCAN_TIMER_MSG);
    	mHandler.removeMessages(CACHE_CLEANUP_TIMER_MSG);
    	
    	// Cancel location update registration 
		mLocManager.removeUpdates(this);
		mGPSRunning = false;
		
		// Cancel WiFi scan registration
		unregisterReceiver(mWifiScanReceiver);

        unbindService(Log.SystemLogConnection);


        unbindService(mAccelServiceConnection);



		
    }

    private void readDb()
    {
        String sign;
        int count, hasloc;
        double lat, lon, acc;
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
        int providerIndex = c.getColumnIndex(DbAdaptor.KEY_PROVIDER);
        int haslocIndex = c.getColumnIndex(DbAdaptor.KEY_HASLOC);

        int dbSize = c.getCount();

        Log.i(TAG, "Found " + dbSize + " entries in database.");

        c.moveToFirst();

        for (int i = 0; i < dbSize; i++)
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
                provider = c.getString(providerIndex);

                curLoc = new Location(provider);
                curLoc.setLatitude(lat);
                curLoc.setLongitude(lon);
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

            c.moveToNext();

        }

        c.close();


    }
    

    
    private void setupWiFi()
    {
        // Check if WiFi is enabled
        if (!mWifi.isWifiEnabled())
        	mWifi.setWifiEnabled(true);
        
        mWifiLock = mWifi.createWifiLock(
                WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
        mWifiLock.acquire();
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
    
}
    

