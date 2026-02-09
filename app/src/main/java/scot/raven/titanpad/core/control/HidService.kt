package scot.raven.titanpad.core.control

import android.os.RemoteException
import android.util.Log
import com.android.commands.hid.Device

class HidService : IHidService.Stub() {

    val hidKeyboardDescriptor: ByteArray = byteArrayOf(
        0x05, 0x01,           // Usage Page (Generic Desktop Ctrls)
        0x09, 0x06,           // Usage (Keyboard)
        0xA1.toByte(), 0x01,  // Collection (Application)
        0x05, 0x07,           //   Usage Page (Kbrd/Keypad)
        0x19, 0xE0.toByte(),  //   Usage Minimum (0xE0)
        0x29, 0xE7.toByte(),  //   Usage Maximum (0xE7)
        0x15, 0x00,           //   Logical Minimum (0)
        0x25, 0x01,           //   Logical Maximum (1)
        0x75, 0x01,           //   Report Size (1)
        0x95.toByte(), 0x08,  //   Report Count (8)
        0x81.toByte(), 0x02,  //   Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x01,  //   Report Count (1)
        0x75, 0x08,           //   Report Size (8)
        0x81.toByte(), 0x03,  //   Input (Const,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x06,  //   Report Count (6)
        0x75, 0x08,           //   Report Size (8)
        0x15, 0x00,           //   Logical Minimum (0)
        0x25, 0x6A,           //   Logical Maximum (101)
        0x05, 0x07,           //   Usage Page (Kbrd/Keypad)
        0x19, 0x00,           //   Usage Minimum (0x00)
        0x29, 0x6A,           //   Usage Maximum (0x65)
        0x81.toByte(), 0x00,  //   Input (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),        // End Collection
    )

    val hidMouseDescriptor: ByteArray = byteArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop Ctrls)
        0x09, 0x02,             // Usage (Mouse)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x09, 0x01,             //   Usage (Pointer)
        0xA1.toByte(), 0x00,    //   Collection (Physical)
        0x05, 0x09,             //     Usage Page (Button)
        0x19, 0x01,             //     Usage Minimum (0x01)
        0x29, 0x05,             //     Usage Maximum (0x05)
        0x15, 0x00,             //     Logical Minimum (0)
        0x25, 0x01,             //     Logical Maximum (1)
        0x95.toByte(), 0x05,    //     Report Count (5)
        0x75, 0x01,             //     Report Size (1)
        0x81.toByte(), 0x02,    //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x95.toByte(), 0x01,    //     Report Count (1)
        0x75, 0x03,             //     Report Size (3)
        0x81.toByte(), 0x01,    //     Input (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05, 0x01,             //     Usage Page (Generic Desktop Ctrls)
        0x09, 0x30,             //     Usage (X)
        0x09, 0x31,             //     Usage (Y)
        0x09, 0x38,             //     Usage (Wheel)
        0x15, 0x81.toByte(),    //     Logical Minimum (-127)
        0x25, 0x7F,             //     Logical Maximum (127)
        0x75, 0x08,             //     Report Size (8)
        0x95.toByte(), 0x03,    //     Report Count (3)
        0x81.toByte(), 0x06,    //     Input (Data,Var,Rel,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),          //   End Collection
        0xC0.toByte(),          // End Collection
    )

    val hidGamePadDescriptor: ByteArray = byteArrayOf(
        0x05, 0x01,           //Usage Page (Generic Desktop Ctrls)
        0x09, 0x05,           //Usage (Game Pad)
        0xA1.toByte(), 0x01,  //Collection (Application)
        0x05, 0x09,           //  Usage Page (Button)
        0x19, 0x01,           //  Usage Minimum (Button 1)
        0x29, 0x10,           //  Usage Maximum (Button 16)
        0x15, 0x00,           //  Logical Minimum (0)
        0x25, 0x01,           //  Logical Maximum (1)
        0x75, 0x01,           //  Report Size (1)
        0x95.toByte(), 0x10,  //  Report Count (16)
        0x81.toByte(), 0x02,  //  Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05, 0x01,           //  Usage Page (Generic Desktop Ctrls)
        0x15, 0x81.toByte(),  //  Logical Minimum (-127)
        0x25, 0x7F,           //  Logical Maximum (127)
        0x09, 0x30,           //  Usage (X)
        0x09, 0x31,           //  Usage (Y)
        0x09, 0x32,           //  Usage (Z)
        0x09, 0x35,           //  Usage (Rz)
        0x75, 0x08,           //  Report Size (8)
        0x95.toByte(), 0x04,  //  Report Count (4)
        0x81.toByte(), 0x02,  //  Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0xC0.toByte(),        //End Col
    )

    val hidTouchScreenDescriptor: ByteArray = byteArrayOf(
        0x05, 0x0D,                 // Usage Page (Digitizer)
        0x09, 0x04,                 // Usage (Touch Screen)
        0xA1.toByte(), 0x01,        // Collection (Application)
        0x09, 0x22,                 //   Usage (Finger)
        0xA1.toByte(), 0x00,        //   Collection (Physical)
        0x09, 0x42,                 //     Usage (Tip Switch)
        0x15, 0x00,                 //     Logical Minimum (0)
        0x25, 0x01,                 //     Logical Maximum (1)
        0x75, 0x01,                 //     Report Size (1)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x09, 0x32,                 //     Usage (In Range)
        0x15, 0x00,                 //     Logical Minimum (0)
        0x25, 0x01,                 //     Logical Maximum (1)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x09, 0x51,                 //     Usage (0x51)
        0x75, 0x05,                 //     Report Size (5)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0x16, 0x00, 0x00,           //     Logical Minimum (0)
        0x26, 0x10, 0x00,           //     Logical Maximum (16)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x09, 0x47,                 //     Usage (0x47)
        0x75, 0x01,                 //     Report Size (1)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0x15, 0x00,                 //     Logical Minimum (0)
        0x25, 0x01,                 //     Logical Maximum (1)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05, 0x01,                 //     Usage Page (Generic Desktop Ctrls)
        0x09, 0x30,                 //     Usage (X)
        0x75, 0x10,                 //     Report Size (16)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0x55, 0x0D,                 //     Unit Exponent (-3)
        0x65, 0x33,                 //     Unit (System: English Linear, Length: Inch)
        0x15, 0x00,                 //     Logical Minimum (0)
        0x26, 0x9F.toByte(), 0x05,  //     Logical Maximum (1439)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x09, 0x31,                 //     Usage (Y)
        0x75, 0x10,                 //     Report Size (16)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0x55, 0x0D,                 //     Unit Exponent (-3)
        0x65, 0x33,                 //     Unit (System: English Linear, Length: Inch)
        0x15, 0x00,                 //     Logical Minimum (0)
        0x26, 0x9F.toByte(), 0x05,  //     Logical Maximum (1439)
        0x81.toByte(), 0x02,        //     Input (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        0x05, 0x0D,                 //     Usage Page (Digitizer)
        0x09, 0x55,                 //     Usage (0x55)
        0x25, 0x08,                 //     Logical Maximum (8)
        0x75, 0x08,                 //     Report Size (8)
        0x95.toByte(), 0x01,        //     Report Count (1)
        0xB1.toByte(), 0x02,        //     Feature (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        0xC0.toByte(),              //   End Collection
        0xC0.toByte(),              // End Collection
    )

    val mouse: Device
    val keyboard: Device
    val gamePad: Device
    val touchScreen: Device

    val mouseCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00)
    val keyboardCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val gamePadCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val touchScreenCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)

    init {
        Log.i(LOG_TAG, "Starting TitanPad HidService")
        mouse = Device(
            1,
            "TitanPadMouse",
            "1",
            1234,
            5678,
            0x03,
            hidMouseDescriptor,
            mouseCode,
            null,
            null
        )

        keyboard = Device(
            2,
            "TitanPadKeyboard",
            "2",
            8765,
            4321,
            0x03,
            hidKeyboardDescriptor,
            keyboardCode,
            null,
            null
        )

        gamePad = Device(
            3,
            "TitanPadGamePad",
            "3",
            2345,
            6789,
            0x03,
            hidGamePadDescriptor,
            gamePadCode,
            null,
            null
        )

        touchScreen = Device(
            4,
            "TitanPadTouchScreen",
            "4",
            6789,
            2345,
            0x03,
            hidTouchScreenDescriptor,
            touchScreenCode,
            null,
            null
        )
    }

    @Throws(RemoteException::class)
    override fun destroy() {
        mouse.close()
        keyboard.close()
        gamePad.close()
        touchScreen.close()
        Log.i(LOG_TAG,"Stopping TitanPad HidService")
    }

    @Throws(RemoteException::class)
    override fun exit() {
        destroy()
    }

    @Throws(RemoteException::class)
    override fun setMousePosition(x: Int, y: Int, buttons: Int) {
        val xClamp = x.coerceIn(-127, 127);
        val yClamp = y.coerceIn(-127, 127);

        mouseCode[0] = buttons.toByte()
        mouseCode[1] = xClamp.toByte()
        mouseCode[2] = yClamp.toByte()
        mouse.sendReport(mouseCode) //Here we send mouse report data.
    }

    override fun keyDown(keyCode: Int) {
        var i=2

        // find the first empty keypress we have
        while (keyboardCode[i] != 0.toByte() && keyboardCode[i] != keyCode.toByte() && i<8)
            i+=1

        // if there's space add this as a new button. Otherwise just ignore
        if (i<8 && keyboardCode[i] == 0.toByte()) {
            keyboardCode[i] = keyCode.toByte()
            keyboard.sendReport(keyboardCode)
        }
    }

    override fun keyUp(keyCode: Int) {
        var i=2

        // find the pressed button
        while (keyboardCode[i] != 0.toByte() && keyboardCode[i] != keyCode.toByte() && i<8)
            i+=1

        // if found, remove by moving everything afterwards forward
        if (i<8 && keyboardCode[i] == keyCode.toByte()) {
            while (i<7) {
                keyboardCode[i] = keyboardCode[i+1]
                i+=1
            }
            keyboardCode[i] = 0.toByte()
            keyboard.sendReport(keyboardCode)
        }
    }

    override fun tapScreen(x: Int, y: Int) {
        touchScreenCode[0] = 1
        touchScreenCode[1] = (x%256).toByte()
        touchScreenCode[2] = (x/256).toByte()
        touchScreenCode[3] = (y%256).toByte()
        touchScreenCode[4] = (y/256).toByte()
        touchScreen.sendReport(touchScreenCode)
    }

    override fun tapRelease() {
        touchScreenCode[0] = 0
        touchScreenCode[1] = 0
        touchScreenCode[2] = 0
        touchScreenCode[3] = 0
        touchScreenCode[4] = 0
        touchScreen.sendReport(touchScreenCode)
    }

    override fun setJoystick(x: Int, y: Int) {
        val xClamp = x.coerceIn(-127, 127);
        val yClamp = y.coerceIn(-127, 127);
        gamePadCode[2] = xClamp.toByte();
        gamePadCode[3] = yClamp.toByte();
        gamePad.sendReport(gamePadCode);
    }

    companion object {
        const val LOG_TAG = "HidService"
    }
}
