package scot.raven.titanpad.core.control

import scot.raven.titanpad.core.logs.Logger

class InputReaderService : IInputReaderService.Stub() {
    init {
        System.loadLibrary("titanpad_rust")
    }

    override fun destroy() {
    }

    override fun init() {
        initLibrary()
        val result = findDevices()
        result.forEach { Logger.i("DATA: ${it.name} ${it.path} ${it.physicalPath} ${it.busType} ${it.vendor} ${it.product} ${it.version}") }
    }

    override fun exit() {
        destroy()
    }

    external fun initLibrary()
    external fun findDevices() : List<DeviceData>
}