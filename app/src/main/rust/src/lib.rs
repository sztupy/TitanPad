#[macro_use]
extern crate log;
extern crate android_logger;

use android_logger::Config;
use jni::{EnvUnowned, JValue, jni_sig, jni_str};
use log::LevelFilter;

use jni::{Env, objects::JObject};

use crate::jni_class::create_device_data;

mod find_devices;
mod jni_class;

#[unsafe(no_mangle)]
pub extern "C" fn Java_scot_raven_titanpad_core_evdev_EvdevReaderService_initLibrary<'caller>(
    mut _unowned_env: EnvUnowned<'caller>,
    _this: JObject<'caller>,
) {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("TitanPadRust"),
    );

    debug!("Rust Library initialized");
}

fn find_devices_impl<'local>(env: &mut Env<'local>) -> eyre::Result<JObject<'local>> {
    let device_list = find_devices::find_devices()?;

    let cls_array_list = env.find_class(jni_str!("java/util/ArrayList"))?;
    let items_list = env.new_object(cls_array_list, jni_sig!("()V"), &[])?;

    for device in device_list {
        let device_jni = create_device_data(env, device.1, device.0)?;

        env.call_method(
            &items_list,
            jni_str!("add"),
            jni_sig!("(Ljava/lang/Object;)Z"),
            &[JValue::Object(&device_jni)],
        )?;
    }

    Ok(items_list)
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_scot_raven_titanpad_core_evdev_EvdevReaderService_findDevices<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _this: JObject<'caller>,
) -> JObject<'caller> {
    let outcome = unowned_env.with_env(|env| -> Result<_, jni::errors::Error> {
        match find_devices_impl(env) {
            Ok(result) => return Ok(result),
            Err(error) => {
                error!("Find Devices Error: {}", error.to_string());
                return Err(jni::errors::Error::JavaException);
            }
        }
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
