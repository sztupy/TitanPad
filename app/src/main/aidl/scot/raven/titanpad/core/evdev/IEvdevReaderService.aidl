package scot.raven.titanpad.core.evdev;

interface IEvdevReaderService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void init() = 1;
    void exit() = 2;
    List devices() = 3;
}