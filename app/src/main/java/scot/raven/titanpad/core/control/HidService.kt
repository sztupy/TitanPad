package scot.raven.titanpad.core.control

import android.os.RemoteException
import android.util.Log
import com.android.commands.hid.Device

class HidService : IHidService.Stub() {

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

    val mouse: Device
    val mouseCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00)

    init {
        Log.i(LOG_TAG, "Starting HidService")
        mouse =
            Device(1, "uHidMouse", "1", 1234, 5678, 0x03, hidMouseDescriptor, mouseCode, null, null)
    }

    @Throws(RemoteException::class)
    override fun destroy() {
        Log.i(LOG_TAG,"Stopping HidService")
    }

    @Throws(RemoteException::class)
    override fun exit() {
        destroy()
    }

    @Throws(RemoteException::class)
    override fun setMousePosition(x: Int, y: Int, buttons: Int) {
        Log.d(LOG_TAG, "Mouse set to $x $y $buttons")

        mouseCode[0] = buttons.toByte()
        mouseCode[1] = x.toByte()
        mouseCode[2] = y.toByte()
        mouse.sendReport(mouseCode) //Here we send mouse report data.
    }

    companion object {
        val LOG_TAG = "HidService"
    }
}
