use jni::sys::jstring;
use jni::{JNIEnv, objects::JObject};

#[unsafe(no_mangle)]
pub extern "C" fn Java_scot_raven_titanpad_MainActivity_callRustCode(
    env: JNIEnv,
    _: JObject,
) -> jstring {
    let message = "Hello from Rust";
    let java_string = env
        .new_string(message)
        .expect("Couldn't create java string!");
    java_string.into_raw()
}