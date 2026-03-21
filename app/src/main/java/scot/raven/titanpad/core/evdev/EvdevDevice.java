package scot.raven.titanpad.core.evdev;

import android.os.IBinder;
import android.os.RemoteException;

import java.lang.AutoCloseable;

public class EvdevDevice extends IEvdevDevice.Stub implements AutoCloseable {
    private String name = null;
    private String path = null;
    private String physicalPath = null;
    private String busType = null;
    private int vendor = 0;
    private int product = 0;
    private int version = 0;
    private long internalDevice = 0;

    @Override
    public void close() {
        synchronized (this) {
            if (internalDevice != 0L) {
                nativeDrop();
                internalDevice = 0;
            }
        }
    }

    @Override
    public void events(int timeout, IEventCallback callback) {
        nativeGetEvent(timeout, callback);
    }

    public native void nativeGetEvent(int timeout, IEventCallback callback);
    public native void nativeGrab();
    public native void nativeUngrab();
    public native void nativeDrop();

    @Override
    public void destroy() {}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getPhysicalPath() {
        return physicalPath;
    }

    @Override
    public String getBusType() {
        return busType;
    }

    @Override
    public int getVendor() {
        return vendor;
    }

    @Override
    public int getProduct() {
        return product;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPhysicalPath(String physicalPath) {
        this.physicalPath = physicalPath;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public void setVendor(int vendor) {
        this.vendor = vendor;
    }

    public void setProduct(int product) {
        this.product = product;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getInternalDevice() {
        return internalDevice;
    }

    public void setInternalDevice(long internalDevice) {
        this.internalDevice = internalDevice;
    }

    @Override
    public String toString() {
        return "EvdevDevice{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", physicalPath='" + physicalPath + '\'' +
                ", busType='" + busType + '\'' +
                ", vendor=" + vendor +
                ", product=" + product +
                ", version=" + version +
                ", internalDevice=" + internalDevice +
                '}';
    }
}