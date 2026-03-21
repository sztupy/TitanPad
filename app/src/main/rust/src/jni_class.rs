use std::time::SystemTime;

use evdev::{Device, InputEvent};
use jni::{
    Env, JValue, bind_java_type, jni_sig, jni_str,
    objects::{JObject, JString},
    sys::{jint, jlong},
};
use nix::poll::PollTimeout;

#[repr(transparent)]
#[derive(Copy, Clone)]
struct DeviceHandle(*const Device);
impl DeviceHandle {
    pub fn new(thing: Device) -> Self {
        let boxed = Box::new(thing);
        DeviceHandle(Box::into_raw(boxed))
    }

    // unsafe fn as_ref(&self) -> &Device {
    //     unsafe { &*self.0 }
    // }

    unsafe fn as_mut_ref(&self) -> &mut Device {
        unsafe { &mut *(self.0 as *mut Device) }
    }

    // Safety: only convert back to Box (to drop) when sure handle is no longer shared.
    pub unsafe fn into_box(self) -> Box<Device> {
        unsafe { Box::from_raw(self.0 as *mut Device) }
    }
}

impl From<DeviceHandle> for jlong {
    fn from(handle: DeviceHandle) -> Self {
        handle.0 as jlong
    }
}

bind_java_type! {
    rust_type = pub InputEventData,
    java_type = scot.raven.titanpad.core.evdev.InputEvent,

    constructors {
        fn new(),
    },

    fields {
        time: jlong,
        event_type: jint,
        event_code: jint,
        event_value: jint,
    },
}

bind_java_type! {
    rust_type = pub DeviceData,
    java_type = scot.raven.titanpad.core.evdev.EvdevDevice,

    constructors {
        fn new(),
    },

    fields {
        name: JString,
        path: JString,
        physical_path: JString,
        bus_type: JString,
        vendor: jint,
        product: jint,
        version: jint,
        internal_device: long,
    },

    native_methods {
        fn native_get_event(timeout: jint, callback: scot.raven.titanpad.core.evdev.IEventCallback),
        fn native_grab(),
        fn native_ungrab(),
        fn native_drop(),
    },
}

#[macro_export]
macro_rules! handle_jni_error {
    ( $x:expr ) => {
        match $x {
            Ok(it) => it,
            Err(err) => {
                return Err(jni::errors::Error::ClassNotFound {
                    name: err.to_string(),
                });
            }
        };
    };
}

impl DeviceDataNativeInterface for DeviceDataAPI {
    type Error = jni::errors::Error;

    fn native_grab<'local>(
        env: &mut jni::Env<'local>,
        this: DeviceData<'local>,
    ) -> ::std::result::Result<(), jni::errors::Error> {
        let ptr = this.internal_device(env)?;
        if ptr != 0 {
            let device_handle = DeviceHandle(ptr as *const Device);
            let device = unsafe { device_handle.as_mut_ref() };
            info!("Grabbing Device: {}", device.name().unwrap_or_default());
            handle_jni_error!(device.grab());
        }
        Ok(())
    }

    fn native_ungrab<'local>(
        env: &mut jni::Env<'local>,
        this: DeviceData<'local>,
    ) -> ::std::result::Result<(), jni::errors::Error> {
        let ptr = this.internal_device(env)?;
        if ptr != 0 {
            let device_handle = DeviceHandle(ptr as *const Device);
            let device = unsafe { device_handle.as_mut_ref() };
            info!("UnGrabbing Device: {}", device.name().unwrap_or_default());
            handle_jni_error!(device.ungrab());
        }
        Ok(())
    }

    fn native_drop<'local>(
        env: &mut jni::Env<'local>,
        this: DeviceData<'local>,
    ) -> ::std::result::Result<(), jni::errors::Error> {
        let ptr = this.internal_device(env)?;
        if ptr != 0 {
            let device_handle = DeviceHandle(ptr as *const Device);
            info!("Closing device handle {}", ptr);
            unsafe { drop(device_handle.into_box()) }
        }
        Ok(())
    }

    fn native_get_event<'local>(
        env: &mut jni::Env<'local>,
        this: DeviceData<'local>,
        timeout: jint,
        callback: JObject,
    ) -> ::std::result::Result<(), jni::errors::Error> {
        use nix::sys::epoll;

        let ptr = this.internal_device(env)?;
        let device_handle = DeviceHandle(ptr as *const Device);
        let device = unsafe { device_handle.as_mut_ref() };
        let Ok(epoll) = epoll::Epoll::new(epoll::EpollCreateFlags::EPOLL_CLOEXEC) else {
            return Err(jni::errors::Error::Instantiation);
        };
        let event = epoll::EpollEvent::new(epoll::EpollFlags::EPOLLIN, 0);
        handle_jni_error!(epoll.add(&device, event));
        // We don't care about these, but the kernel wants to fill them.
        let mut events = [epoll::EpollEvent::empty(); 2];
        loop {
            match device.fetch_events() {
                Ok(iterator) => {
                    for ev in iterator {
                        let Ok(event_data) = create_event_data(env, ev) else {
                            return Err(jni::errors::Error::Instantiation);
                        };

                        handle_jni_error!(env.call_method(
                            &callback,
                            jni_str!("accept"),
                            jni_sig!((t: scot.raven.titanpad.core.evdev.IInputEvent) -> void),
                            &[JValue::Object(&event_data)],
                        ));
                    }
                }
                Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                    let Ok(poll_timeout) = PollTimeout::try_from(timeout) else {
                        return Err(jni::errors::Error::Instantiation);
                    };
                    let Ok(count) = epoll.wait(&mut events, poll_timeout) else {
                        return Err(jni::errors::Error::Instantiation);
                    };

                    // timeout occurred, we go back to Java so it can decide to continue or exit
                    if count == 0 {
                        return Ok(());
                    }
                }
                Err(e) => {
                    eprintln!("{e}");
                    break;
                }
            }
        }
        Ok(())
    }
}

pub fn create_device_data<'local>(
    env: &mut Env<'local>,
    device: Device,
    path: String,
) -> eyre::Result<DeviceData<'local>> {
    let result = DeviceData::new(env)?;
    let input_id = device.input_id();

    let path_java = JString::from_str(env, path)?;
    result.set_path(env, path_java)?;

    let name = device.name().unwrap_or("");
    let name_java = JString::from_str(env, name)?;
    result.set_name(env, name_java)?;

    let physical_path = device.physical_path().unwrap_or("");
    let physical_path_java = JString::from_str(env, physical_path)?;
    result.set_physical_path(env, physical_path_java)?;

    let bus_type = input_id.bus_type().to_string();
    let bus_type_java = JString::from_str(env, bus_type)?;
    result.set_bus_type(env, bus_type_java)?;

    result.set_vendor(env, i32::from(input_id.vendor()))?;
    result.set_product(env, i32::from(input_id.product()))?;
    result.set_version(env, i32::from(input_id.version()))?;

    device.set_nonblocking(true)?;
    result.set_internal_device(env, DeviceHandle::new(device).into())?;

    return Ok(result);
}

pub fn create_event_data<'local>(
    env: &mut Env<'local>,
    input_event: InputEvent,
) -> eyre::Result<InputEventData<'local>> {
    let result = InputEventData::new(env)?;

    result.set_event_code(env, i32::from(input_event.code()))?;
    result.set_event_type(env, i32::from(input_event.event_type().0))?;
    result.set_event_value(env, input_event.value())?;

    let time = input_event
        .timestamp()
        .duration_since(SystemTime::UNIX_EPOCH)?;

    result.set_time(env, time.as_millis().try_into().unwrap_or(0))?;

    return Ok(result);
}
