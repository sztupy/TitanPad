package scot.raven.titanpad.core.control;

interface IInputReaderService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void init() = 1;
    void exit() = 2;
}