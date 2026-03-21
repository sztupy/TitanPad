package scot.raven.titanpad.core.evdev;

interface IInputEvent {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    long getTime() = 1;
    int getEventType() = 2;
    int getEventCode() = 3;
    int getEventValue() = 4;
}