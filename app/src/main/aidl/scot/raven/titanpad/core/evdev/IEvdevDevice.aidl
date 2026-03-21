package scot.raven.titanpad.core.evdev;

import scot.raven.titanpad.core.evdev.IEventCallback;

interface IEvdevDevice {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    String getName() = 1;
    String getPath() = 2;
    String getPhysicalPath() = 3;
    String getBusType() = 4;
    int getVendor() = 5;
    int getProduct() = 6;
    int getVersion() = 7;

    void close() = 10;
    void events(int timeout, IEventCallback callback) = 11;
}