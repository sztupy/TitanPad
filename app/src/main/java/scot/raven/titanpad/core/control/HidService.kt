package scot.raven.titanpad.core.control

import android.os.RemoteException
import android.util.Log
import android.util.SparseArray
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

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
        0x05, 0x01,          // USAGE_PAGE (Generic Desktop)
        0x09, 0x02,          // USAGE (Mouse)
        0xa1.toByte(), 0x01, // COLLECTION (Application)
        0x09, 0x02,          //   USAGE (Mouse)
        0xa1.toByte(), 0x02, //   COLLECTION (Logical)
        0x09, 0x01,          //     USAGE (Pointer)
        0xa1.toByte(), 0x00, //     COLLECTION (Physical)
        // ------------------------------  Buttons
        0x05, 0x09,          //       USAGE_PAGE (Button)
        0x19, 0x01,          //       USAGE_MINIMUM (Button 1)
        0x29, 0x05,          //       USAGE_MAXIMUM (Button 5)
        0x15, 0x00,          //       LOGICAL_MINIMUM (0)
        0x25, 0x01,          //       LOGICAL_MAXIMUM (1)
        0x75, 0x01,          //       REPORT_SIZE (1)
        0x95.toByte(), 0x05, //       REPORT_COUNT (5 Buttons)
        0x81.toByte(), 0x02, //       INPUT (Data,Var,Abs)
        // ------------------------------  Padding
        0x75, 0x03,          //       REPORT_SIZE (8-5buttons 3)
        0x95.toByte(), 0x01, //       REPORT_COUNT (1)
        0x81.toByte(), 0x03, //       INPUT (Cnst,Var,Abs)
        // ------------------------------  X,Y position
        0x05, 0x01,          //       USAGE_PAGE (Generic Desktop)
        0x09, 0x30,          //       USAGE (X)
        0x09, 0x31,          //       USAGE (Y)
        0x15, 0x81.toByte(), //       LOGICAL_MINIMUM (-127)
        0x25, 0x7f,          //       LOGICAL_MAXIMUM (127)
        0x75, 0x08,          //       REPORT_SIZE (8)
        0x95.toByte(), 0x02, //       REPORT_COUNT (2)
        0x81.toByte(), 0x06, //       INPUT (Data,Var,Rel)
        0xa1.toByte(), 0x02, //       COLLECTION (Logical)
        // ------------------------------  Vertical wheel res multiplier
        0x09, 0x48,          //         USAGE (Resolution Multiplier)
        0x15, 0x00,          //         LOGICAL_MINIMUM (0)
        0x25, 0x01,          //         LOGICAL_MAXIMUM (1)
        0x35, 0x78,          //         PHYSICAL_MINIMUM (120)
        0x45, 0x78,          //         PHYSICAL_MAXIMUM (120) - hardcode resolution to the minimum available
        0x75, 0x02,          //         REPORT_SIZE (2)
        0x95.toByte(), 0x01, //         REPORT_COUNT (1)
        0xa4.toByte(),       //         PUSH
        0xb1.toByte(), 0x02, //         FEATURE (Data,Var,Abs)
        // ------------------------------  Vertical wheel
        0x09, 0x38,          //         USAGE (Wheel)
        0x15, 0x81.toByte(), //         LOGICAL_MINIMUM (-127)
        0x25, 0x7f,          //         LOGICAL_MAXIMUM (127)
        0x35, 0x00,          //         PHYSICAL_MINIMUM (0)        - reset physical
        0x45, 0x00,          //         PHYSICAL_MAXIMUM (0)
        0x75, 0x08,          //         REPORT_SIZE (8)
        0x81.toByte(), 0x06, //         INPUT (Data,Var,Rel)
        0xc0.toByte(),       //       END_COLLECTION
        0xa1.toByte(), 0x02, //       COLLECTION (Logical)
        // ------------------------------  Horizontal wheel res multiplier
        0x09, 0x48,          //         USAGE (Resolution Multiplier)
        0xb4.toByte(),       //         POP
        0xb1.toByte(), 0x02, //         FEATURE (Data,Var,Abs)
        // ------------------------------  Padding for Feature report
        0x35, 0x00,          //         PHYSICAL_MINIMUM (0)        - reset physical
        0x45, 0x00,          //         PHYSICAL_MAXIMUM (0)
        0x75, 0x04,          //         REPORT_SIZE (4)
        0xb1.toByte(), 0x03, //         FEATURE (Cnst,Var,Abs)
        // ------------------------------  Horizontal wheel
        0x05, 0x0c,          //         USAGE_PAGE (Consumer Devices)
        0x0a, 0x38, 0x02,    //         USAGE (AC Pan)
        0x15, 0x81.toByte(), //         LOGICAL_MINIMUM (-127)
        0x25, 0x7f,          //         LOGICAL_MAXIMUM (127)
        0x75, 0x08,          //         REPORT_SIZE (8)
        0x81.toByte(), 0x06, //         INPUT (Data,Var,Rel)
        0xc0.toByte(),       //       END_COLLECTION
        0xc0.toByte(),       //     END_COLLECTION
        0xc0.toByte(),       //   END_COLLECTION
        0xc0.toByte()        // END_COLLECTION
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

    val mouseCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)

    val mouseFeatureReportData: ByteArray = byteArrayOf(0xff.toByte())

    val keyboardCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    val gamePadCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    val touchScreenCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)

    val touchScreenReportData: ByteArray = byteArrayOf(0x00)

    init {
        Log.i(LOG_TAG, "Starting TitanPad HidService")

        val mouseFeatureReport = SparseArray<ByteArray>()
        mouseFeatureReport.append(0, mouseFeatureReportData)

        val touchFeatureReport = SparseArray<ByteArray>()
        touchFeatureReport.append(0, touchScreenReportData)

        mouse = Device(
            1,
            "TitanPadMouse",
            "1",
            1234,
            5678,
            0x03,
            hidMouseDescriptor,
            mouseCode,
            mouseFeatureReport,
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
            touchFeatureReport,
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
    override fun setMousePosition(x: Int, y: Int, buttonDown: Int, buttonUp: Int, scroll: Int, hScroll: Int) {
        val xClamp = x.coerceIn(-127, 127)
        val yClamp = y.coerceIn(-127, 127)

        mouseCode[0] = mouseCode[0].or(buttonDown.toByte()).and(buttonUp.toByte().inv())
        mouseCode[1] = xClamp.toByte()
        mouseCode[2] = yClamp.toByte()
        mouseCode[3] = scroll.toByte()
        mouseCode[4] = hScroll.toByte()
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
        val xClamp = x.coerceIn(0, 1440)
        val yClamp = y.coerceIn(0, 1440)

        touchScreenCode[0] = 1
        touchScreenCode[1] = (xClamp%256).toByte()
        touchScreenCode[2] = (xClamp/256).toByte()
        touchScreenCode[3] = (yClamp%256).toByte()
        touchScreenCode[4] = (yClamp/256).toByte()
        touchScreen.sendReport(touchScreenCode)
    }

    override fun tapRelease() {
        touchScreenCode[0] = 0
        touchScreen.sendReport(touchScreenCode)
    }

    override fun setJoystick(x: Int, y: Int) {
        val xClamp = x.coerceIn(-127, 127)
        val yClamp = y.coerceIn(-127, 127)
        gamePadCode[2] = xClamp.toByte()
        gamePadCode[3] = yClamp.toByte()
        gamePad.sendReport(gamePadCode)
    }

    companion object {
        const val LOG_TAG = "HidService"
    }
}
