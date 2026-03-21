package scot.raven.titanpad.core.evdev;

import scot.raven.titanpad.core.evdev.IInputEvent;

interface IEventCallback {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void accept(in IInputEvent event) = 1;
}