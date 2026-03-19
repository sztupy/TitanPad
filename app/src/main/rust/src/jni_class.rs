use evdev::Device;
use jni::{Env, bind_java_type, objects::JString};

bind_java_type! {
    rust_type = pub DeviceData,
    java_type = scot.raven.titanpad.core.control.DeviceData,

    constructors {
        fn new(),
    },

    fields {
        name: JString,
    },
}

pub fn create_device_data<'local>(
    env: &mut Env<'local>,
    device: Device,
) -> eyre::Result<DeviceData<'local>> {
    let result = DeviceData::new(env)?;
    let name = device.name().unwrap_or("");
    let name_java = JString::from_str(env, name)?;
    result.set_name(env, name_java)?;
    return Ok(result);
}
