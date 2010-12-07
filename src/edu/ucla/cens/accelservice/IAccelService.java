/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/falaki/phd/projects/AccelService/src/edu/ucla/cens/accelservice/IAccelService.aidl
 */
package edu.ucla.cens.accelservice;
import java.lang.String;
import java.util.List;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Binder;
import android.os.Parcel;
public interface IAccelService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.ucla.cens.accelservice.IAccelService
{
private static final java.lang.String DESCRIPTOR = "edu.ucla.cens.accelservice.IAccelService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an IAccelService interface,
 * generating a proxy if needed.
 */
public static edu.ucla.cens.accelservice.IAccelService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.ucla.cens.accelservice.IAccelService))) {
return ((edu.ucla.cens.accelservice.IAccelService)iin);
}
return new edu.ucla.cens.accelservice.IAccelService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_start:
{
data.enforceInterface(DESCRIPTOR);
this.start();
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_suggestRate:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.suggestRate(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setReadingLength:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setReadingLength(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_suggestInterval:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _result = this.suggestInterval(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getInterval:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getInterval();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getLastForce:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getLastForce();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getLastXValues:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getLastXValues();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getLastYValues:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getLastYValues();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getLastZValues:
{
data.enforceInterface(DESCRIPTOR);
java.util.List _result = this.getLastZValues();
reply.writeNoException();
reply.writeList(_result);
return true;
}
case TRANSACTION_getLastTimeStamp:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getLastTimeStamp();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_significantForce:
{
data.enforceInterface(DESCRIPTOR);
double _arg0;
_arg0 = data.readDouble();
boolean _result = this.significantForce(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.ucla.cens.accelservice.IAccelService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
	 * Starts the accelerometer service.
	 */
public void start() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
	 * Stops the accelerometer service to save maximum power.
	 */
public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
	 * Set the rate of accelerometer sampling. This is only a 
	 * suggestion and the service may choose a lower rate to save power. 
	 * Possible values are:
	 * SENSOR_DELAY_FASTEST, SENSOR_DELAY_GAME, SENSOR_DELAY_NORMA, SENSOR_DELAY_UI
	 * 
	 * @param 	rate	rate of sensor reading
	 * @return 			the actual rate that was set
	 */
public int suggestRate(int rate) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(rate);
mRemote.transact(Stub.TRANSACTION_suggestRate, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	 * Set the length of the interval that accelerometer is recorded 
	 * before it is turned of (for duty-cycling).
	 *
	 * @param 	length		length of the interval for sensor reading in milliseconds
	 */
public void setReadingLength(int length) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(length);
mRemote.transact(Stub.TRANSACTION_setReadingLength, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
	 * Suggest length of the duty-cycling interval. The accelerometer sensor
	 * will be turned off for some time between readings.  This is only a 
	 * suggestion and the service may choose a longer interval to save power
	 * 
	 * @param	interval	suggested length of off interval in milliseconds
	 * @return				the actual interval in milliseconds
	 */
public int suggestInterval(int interval) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(interval);
mRemote.transact(Stub.TRANSACTION_suggestInterval, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	 * Returns the current sleeping interval.
	 * 
	 * @return				current sleep interval used by the service in milliseconds
	 */
public int getInterval() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getInterval, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	 * Returns the latest recorded force vector.
	 * 
	 * @return				latest recorded force vector
	 */
public java.util.List getLastForce() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastForce, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Returns the list of latest recorded X values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastXValues() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastXValues, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Returns the list of latest recorded Y values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastYValues() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastYValues, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Returns the list of latest recorded Z values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastZValues() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastZValues, _data, _reply, 0);
_reply.readException();
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_result = _reply.readArrayList(cl);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	   * Returns the time-stamp of the last recorded value.
	   * This method can be used to verify the freshness of the values.
	   *
	   * @return 			time-stamp of the latest recorded sensor value in milliseconds
	   */
public long getLastTimeStamp() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastTimeStamp, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	   * Returns true if the mean of last Force Values is greater than
       * the threshold.
	   *
       * @param     threshold   threshold value
	   * @return 			    true if Force mean is greater than
	   *                        threshold
	   */
public boolean significantForce(double threshold) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeDouble(threshold);
mRemote.transact(Stub.TRANSACTION_significantForce, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_start = (IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_stop = (IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_suggestRate = (IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_setReadingLength = (IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_suggestInterval = (IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getInterval = (IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getLastForce = (IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getLastXValues = (IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_getLastYValues = (IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getLastZValues = (IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getLastTimeStamp = (IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_significantForce = (IBinder.FIRST_CALL_TRANSACTION + 11);
}
/**
	 * Starts the accelerometer service.
	 */
public void start() throws android.os.RemoteException;
/**
	 * Stops the accelerometer service to save maximum power.
	 */
public void stop() throws android.os.RemoteException;
/**
	 * Set the rate of accelerometer sampling. This is only a 
	 * suggestion and the service may choose a lower rate to save power. 
	 * Possible values are:
	 * SENSOR_DELAY_FASTEST, SENSOR_DELAY_GAME, SENSOR_DELAY_NORMA, SENSOR_DELAY_UI
	 * 
	 * @param 	rate	rate of sensor reading
	 * @return 			the actual rate that was set
	 */
public int suggestRate(int rate) throws android.os.RemoteException;
/**
	 * Set the length of the interval that accelerometer is recorded 
	 * before it is turned of (for duty-cycling).
	 *
	 * @param 	length		length of the interval for sensor reading in milliseconds
	 */
public void setReadingLength(int length) throws android.os.RemoteException;
/**
	 * Suggest length of the duty-cycling interval. The accelerometer sensor
	 * will be turned off for some time between readings.  This is only a 
	 * suggestion and the service may choose a longer interval to save power
	 * 
	 * @param	interval	suggested length of off interval in milliseconds
	 * @return				the actual interval in milliseconds
	 */
public int suggestInterval(int interval) throws android.os.RemoteException;
/**
	 * Returns the current sleeping interval.
	 * 
	 * @return				current sleep interval used by the service in milliseconds
	 */
public int getInterval() throws android.os.RemoteException;
/**
	 * Returns the latest recorded force vector.
	 * 
	 * @return				latest recorded force vector
	 */
public java.util.List getLastForce() throws android.os.RemoteException;
/**
	  * Returns the list of latest recorded X values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastXValues() throws android.os.RemoteException;
/**
	  * Returns the list of latest recorded Y values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastYValues() throws android.os.RemoteException;
/**
	  * Returns the list of latest recorded Z values.
	  * Each element of the list contains an array of values.
	  *
	  * @return				latest recorded values
	  */
public java.util.List getLastZValues() throws android.os.RemoteException;
/**
	   * Returns the time-stamp of the last recorded value.
	   * This method can be used to verify the freshness of the values.
	   *
	   * @return 			time-stamp of the latest recorded sensor value in milliseconds
	   */
public long getLastTimeStamp() throws android.os.RemoteException;
/**
	   * Returns true if the mean of last Force Values is greater than
       * the threshold.
	   *
       * @param     threshold   threshold value
	   * @return 			    true if Force mean is greater than
	   *                        threshold
	   */
public boolean significantForce(double threshold) throws android.os.RemoteException;
}
