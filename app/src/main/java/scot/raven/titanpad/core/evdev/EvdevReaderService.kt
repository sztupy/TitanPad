package scot.raven.titanpad.core.evdev

class EvdevReaderService : IEvdevReaderService.Stub() {
    init {
        System.loadLibrary("titanpad_rust")
    }

    override fun destroy() {
    }

    override fun init() {
        initLibrary()
    }

    override fun exit() {
        destroy()
    }

    override fun devices(): List<IEvdevDevice.Stub> {
        return findDevices()
    }

    external fun initLibrary()
    external fun findDevices() : List<EvdevDevice>
}