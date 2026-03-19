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
        path: JString,
        physical_path: JString,
        bus_type: JString,
        vendor: jint,
        product: jint,
        version: jint
    },
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

    return Ok(result);
}
