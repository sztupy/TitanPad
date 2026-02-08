package scot.raven.titanpad.core.control;

interface IHidService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    void setMousePosition(int x, int y, int buttons) = 2;

    void keyDown(int keyCode) = 3;
    void keyUp(int keyCode) = 4;
}