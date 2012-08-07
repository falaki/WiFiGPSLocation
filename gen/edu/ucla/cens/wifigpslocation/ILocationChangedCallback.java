/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/falaki/phd/projects/WiFiGPSLocation/src/edu/ucla/cens/wifigpslocation/ILocationChangedCallback.aidl
 */
package edu.ucla.cens.wifigpslocation;
public interface ILocationChangedCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements edu.ucla.cens.wifigpslocation.ILocationChangedCallback
{
private static final java.lang.String DESCRIPTOR = "edu.ucla.cens.wifigpslocation.ILocationChangedCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an edu.ucla.cens.wifigpslocation.ILocationChangedCallback interface,
 * generating a proxy if needed.
 */
public static edu.ucla.cens.wifigpslocation.ILocationChangedCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof edu.ucla.cens.wifigpslocation.ILocationChangedCallback))) {
return ((edu.ucla.cens.wifigpslocation.ILocationChangedCallback)iin);
}
return new edu.ucla.cens.wifigpslocation.ILocationChangedCallback.Stub.Proxy(obj);
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
case TRANSACTION_locationChanged:
{
data.enforceInterface(DESCRIPTOR);
this.locationChanged();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements edu.ucla.cens.wifigpslocation.ILocationChangedCallback
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
	 * Is called when the service detects that location has changed.
	 *
	 */
public void locationChanged() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_locationChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_locationChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
	 * Is called when the service detects that location has changed.
	 *
	 */
public void locationChanged() throws android.os.RemoteException;
}
