use std::os::unix::fs::FileTypeExt;

use evdev::Device;

pub(crate) fn find_devices() -> eyre::Result<Vec<(String, Device)>> {
    let result = find_touchpad_and_keyboard_dev()?;

    Ok(result)
}

fn find_touchpad_and_keyboard_dev() -> eyre::Result<Vec<(String, Device)>> {
    let mut result: Vec<(String, Device)> = Vec::new();

    for ent in std::fs::read_dir("/dev/input")? {
        let Ok(ent) = ent else {
            continue;
        };

        let Ok(file_type) = ent.file_type() else {
            continue;
        };

        if !file_type.is_char_device() {
            continue;
        }

        let Ok(filename) = ent.file_name().into_string() else {
            continue;
        };

        if !filename.starts_with("event") {
            continue;
        }

        info!("Checking device /dev/input/{filename}");

        let Ok(dev) = Device::open(ent.path()) else {
            warn!("Unable to open device /dev/input/{filename}, skipping");
            continue;
        };

        let path_data = ent
            .path()
            .into_os_string()
            .into_string()
            .unwrap_or_default();
        result.push((path_data, dev));
    }

    Ok(result)
}
