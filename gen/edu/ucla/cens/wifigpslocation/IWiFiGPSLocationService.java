/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/falaki/phd/projects/WiFiGPSLocation/src/edu/ucla/cens/wifigpslocation/IWiFiGPSLocationService.aidl
 */
package edu.ucla.cens.wifigpslocation;
public interface IWiFiGPSLocationService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService
{
private static final java.lang.String DESCRIPTOR = "edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService interface,
 * generating a proxy if needed.
 */
public static edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService))) {
return ((edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService)iin);
}
return new edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService.Stub.Proxy(obj);
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
case TRANSACTION_isRunning:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isRunning();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getLocation:
{
data.enforceInterface(DESCRIPTOR);
android.location.Location _result = this.getLocation();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_suggestInterval:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _result = this.suggestInterval(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_registerCallback:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
edu.ucla.cens.wifigpslocation.ILocationChangedCallback _arg1;
_arg1 = edu.ucla.cens.wifigpslocation.ILocationChangedCallback.Stub.asInterface(data.readStrongBinder());
this.registerCallback(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_unregisterCallback:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
edu.ucla.cens.wifigpslocation.ILocationChangedCallback _arg1;
_arg1 = edu.ucla.cens.wifigpslocation.ILocationChangedCallback.Stub.asInterface(data.readStrongBinder());
this.unregisterCallback(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.stop(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_start:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.start(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getWiFiScan:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getWiFiScan();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService
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
     * Returns true if the service has already been started.
     *
     * @return      the state of the service
     */
public boolean isRunning() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isRunning, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Returns the current location. 
     *
     * @return		the last known location
     */
public android.location.Location getLocation() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.location.Location _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLocation, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.location.Location.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
     * Change the GPS sampling interval. It is interpreted as
     * a suggestion. It will return the actual value that was applied.
     * 
     * @param		interval	GPS sampling interval in milliseconds
     * @param       callerName  String name identifying the client
     * @return					actual GPS sampling interval in milliseconds
     */
public int suggestInterval(java.lang.String callerName, int interval) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callerName);
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
     * Registers the locationChanged callback along with the
     * acceleration threshold to be used. When the service notices a
     * change in location based on WiFi signature, it calls 
     * the callback method.
     *
     * @param   callback        the Callback object
     * @param   callerName      String name identifying the client
     */
public void registerCallback(java.lang.String callerName, edu.ucla.cens.wifigpslocation.ILocationChangedCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callerName);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Unregisteres the callback
     * 
     * @param  callback        the callback to be unregistered
     * @param  callerName      String name identifying the client
     */
public void unregisterCallback(java.lang.String callerName, edu.ucla.cens.wifigpslocation.ILocationChangedCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callerName);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Stops the WiFiGPSLocationService if no other client
     * is using the service. Make sure you make this call when you are
     * certain that your application does not need location
     * information.
     * 
     * @param  callerName      String name identifying the client
     */
public void stop(java.lang.String callerName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callerName);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Starts the WiFiGPSLocationService if it has not been started
     * before. 
     * 
     * @param  callerName      String name identifying the client
     */
public void start(java.lang.String callerName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(callerName);
mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/**
     * Returns a String dump of last visible WiFi access points. 
     * The returned String can be interpreted as a JSON object. Each
     * key is the BSSID of a WiFi AP and the corresponding value is 
     * the signal strength in dBm.
     *
     * @return              JSON object containing visible WiFi APs
     */
public java.lang.String getWiFiScan() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getWiFiScan, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_isRunning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getLocation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_suggestInterval = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getWiFiScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
/**
     * Returns true if the service has already been started.
     *
     * @return      the state of the service
     */
public boolean isRunning() throws android.os.RemoteException;
/**
     * Returns the current location. 
     *
     * @return		the last known location
     */
public android.location.Location getLocation() throws android.os.RemoteException;
/**
     * Change the GPS sampling interval. It is interpreted as
     * a suggestion. It will return the actual value that was applied.
     * 
     * @param		interval	GPS sampling interval in milliseconds
     * @param       callerName  String name identifying the client
     * @return					actual GPS sampling interval in milliseconds
     */
public int suggestInterval(java.lang.String callerName, int interval) throws android.os.RemoteException;
/**
     * Registers the locationChanged callback along with the
     * acceleration threshold to be used. When the service notices a
     * change in location based on WiFi signature, it calls 
     * the callback method.
     *
     * @param   callback        the Callback object
     * @param   callerName      String name identifying the client
     */
public void registerCallback(java.lang.String callerName, edu.ucla.cens.wifigpslocation.ILocationChangedCallback callback) throws android.os.RemoteException;
/**
     * Unregisteres the callback
     * 
     * @param  callback        the callback to be unregistered
     * @param  callerName      String name identifying the client
     */
public void unregisterCallback(java.lang.String callerName, edu.ucla.cens.wifigpslocation.ILocationChangedCallback callback) throws android.os.RemoteException;
/**
     * Stops the WiFiGPSLocationService if no other client
     * is using the service. Make sure you make this call when you are
     * certain that your application does not need location
     * information.
     * 
     * @param  callerName      String name identifying the client
     */
public void stop(java.lang.String callerName) throws android.os.RemoteException;
/**
     * Starts the WiFiGPSLocationService if it has not been started
     * before. 
     * 
     * @param  callerName      String name identifying the client
     */
public void start(java.lang.String callerName) throws android.os.RemoteException;
/**
     * Returns a String dump of last visible WiFi access points. 
     * The returned String can be interpreted as a JSON object. Each
     * key is the BSSID of a WiFi AP and the corresponding value is 
     * the signal strength in dBm.
     *
     * @return              JSON object containing visible WiFi APs
     */
public java.lang.String getWiFiScan() throws android.os.RemoteException;
}
