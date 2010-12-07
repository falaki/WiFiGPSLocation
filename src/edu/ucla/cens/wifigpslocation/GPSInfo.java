package edu.ucla.cens.wifigpslocation;

import android.location.Location;
import android.os.SystemClock;
import java.util.Random;

public class GPSInfo
{
	public Boolean known;
	public long time;
	public Location loc;
	public int count = 0; 
    public int randId;
	
	
	public GPSInfo(long iTime, Location iLoc)
	{
		time = iTime;
		loc = iLoc;
		known = true;
		count = 1;

        Random rnd = new Random(SystemClock.uptimeMillis());
        randId = rnd.nextInt(Integer.MAX_VALUE);
	}
	
	public GPSInfo(Boolean iKnown, long iTime)
	{
		known = iKnown;
		time = iTime;
		count = 1;

        Random rnd = new Random(SystemClock.uptimeMillis());
        randId = rnd.nextInt(Integer.MAX_VALUE);

	}
	
	public void increment()
	{
		count++;
	}
	
	public String toString()
	{
        String res = "[" + randId + "]:";
		res +=  "<Count: " + count + ", Known: " + known;
		
		if (loc != null)
			res += ", Has location";
		else
			res += ", No location";

        res += ">";
		
		return res;
	}
}
