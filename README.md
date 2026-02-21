<div align="center">
<img src='./app/src/main/ic_launcher-playstore.png' width=100 alt='Application icon'>
</div>

---

# TitanPad: Android Input Remapper

And mainly Trackpad Mouse for Titan 2

![GitHub release (latest by date)](https://img.shields.io/github/v/release/sztupy/TitanPad) ![Android Version](https://img.shields.io/badge/Android-15.0%2B-brightgreen) ![GitHub all releases](https://img.shields.io/github/downloads/sztupy/TitanPad/total) ![License](https://img.shields.io/github/license/sztupy/TitanPad)

<div align="center">
<img src='./docs/gifs/demo.webp' width=450 alt='Application demo'>
</div>

TitanPad is an Accessibility Service allowing you to remap input devices (touchscreen, trackpad, buttons and built-in keyboards) so they do something else, including send key events, move a mouse on the screen or scroll the current contents.

As a main use case it has support for Unihertz Titan 2 allowing you to use the keyboard's capacitive sensor as a trackpad moving a virtual mouse around the screen among others.

## Table of Contents
- [Installation](#installation)
- [Acknowledgment](#acknowledgments)
- [Overview](#overview)
- [Troubleshooting](#troubleshooting)
- [Build](#build)
- [FAQs](#faqs)
- [License](#license)

## Installation
The latest version can be found under [releases](https://github.com/sztupy/TrackPad/releases). You can use GitHub's `Watch > Custom > Releases` option to be notified of new releases.

### Option 1
Install using the standard package installer. Allow the accessibility service using the banner in the application.

### Option 2
Install using adb:
```
>> adb install path/to/apk
>> adb shell settings put secure enabled_accessibility_services scot.raven.titanpad/scot.raven.titanpad.accessibility.AppAccessibilityService
```

### Shizuku
The application also requires you to [install Shizuku](https://github.com/thedjchi/Shizuku/releases) for most features to work.

Note that unless your device is rooted, you will either need to restart the Shizuku service upon reboot, or be under the Wi-Fi coverage of an already trusted network.

## Acknowledgments
- [austinauyeung](https://github.com/austinauyeung) whose [C9 app](https://github.com/austinauyeung/C9) is used as the fork / core for this project. While it has been stripped from a lot of features C9's Cursor Mode implementation is heavily used as the virtual mouse implementation that doesn't require Shizuku
- [palsoftware](https://github.com/palsoftware/) whose [Pastiera keyboard](https://github.com/palsoftware/pastiera) is one of the best keyboard app for Titan 2. This project also uses the shizuku workaround implemented in it to access the keypad events.
- [PeterGSI](https://gitea.angry.im/PeterGSI) whose [Titan 2 TouchPad Daemon](https://gitea.angry.im/PeterGSI/titan2-touchpadd) implementation contains lots of useful bits for proper mouse integration
- [WuDi-ZhanShen](https://github.com/WuDi-ZhanShen) whose [Pure Java UHid implementation](https://github.com/WuDi-ZhanShen/AndroidUHidPureJava) was used to implement the uhid based mouse support 
- The Unihertz Titan 2 community in general!

## Overview

The app accesses the system's input events through Shizuku allowing you to read inputs that are hidden from the main Android system. Afterwards it can translate these events to virtualized input devices, like a mouse, a virtual keyboard or a virtual gamepad.

As a main use case it can read the Unihertz Titan 2's capacitive keyboard and remap it's input as a mouse, joystick or scrollbar. It also has some basic multi-touch functionality based on the fact that the sensor while doesn't support multiple fingers, it does support obtaining the approximate size of your touch, so if you use two fingers close to each other it will return a larger touch point. This information can then be used to differentiate between single finger touches and double finger touches.

## Basic setup

Follow the steps below to get started quickly:

* Install Application
* Install Shizuku [from this repository](https://github.com/thedjchi/Shizuku/releases). Make sure to use this Shizuku fork as the official version does not support MTK phones (like the Titan 2) properly.
* Open Application
* Grant both Accessibility permission and Shizuku permission
* Disable built in Titan 2 features, especially Scroll Assistant
* Enable "Main Config"

This will enable a hardware emulated mouse on the entire Trackpad with tap to click feature

## Supported input methods

### Software based mouse

This will set up a virtual mouse using Android's built in Overlay and Accessibility features, and doesn't rely on emulating a proper hardware.
(Technically this option could work without Shizuku if there would be support for accessing the Trackpad's motion events without it)

Compared to the hardware emulated mouse

Pros: 
* All clicks are converted to taps which have better support in Android apps
* The mouse display can be fully configured including using custom images

Cons:
* No support for hovering over elements
* Decreased performance

### Hardware based mouse

This will set up a kernel based mouse as if you connected one using USB / Bluetooth.

Compared to the software based mouse

Pros:
* Higher performance, doesn't rely on Android features
* Native support for hovering over elements

Cons:
* Not all applications accept mouse clicks, and some features only work with taps

### Software based scroll

This will set up a virtual touchpad using Android's built in Overlay and Accessibility features, and doesn't rely on emulating a proper hardware.

This will map touches on the trackpad as taps on the screen at the same location. Afterwards movement on the trackpad will translate to swipes on the main screen.

The software based scroll is fairly finicky right now and it is preferred to use the hardware based scroll which has the same feature set.

### Hardware based scroll

This will set up a hardware emulated touchpad using kernel features.

This will map touches on the trackpad as taps on the screen at the same location. Afterwards movement on the trackpad will translate to swipes on the main screen.

This is more performant than the software based scroll and has the same features.

### Hardware mouse scroll

This will send scroll events for the hardware mouse set up earlier. The mouse is set up to have smooth scrolling enabled and the events will be sent accordingly. Scroll events will always happen where the hardware mouse cursor is right now (or if it is not enabled, then by default it is considered to be in the middle of the screen)

### Hardware based joystick

This will set up a hardware emulated gamepad using kernel features.

Touching the trackpad will set up the center point for the joystick, then movements up/down/left/right will translate to movements of an analog stick.

Due to how multi touch works on the trackpad it is actually possible to have both of your hands on the trackpad with one using it as a virtual joystick, and the other pressing keys that are mapped to buttons, but the setup is not straightforward.

You basically have two options:
* Use non-capacitive gloves on your hand that you don't want to use as a joystick.
* With two bare hands:
  * First touch the side of the trackpad you want to use as a joystick.
  * Then with your other hand touch the part where you want to use the keyboard buttons (e.g. WSAD or IJKL)
  * Then without letting go of the keyboard hand from the touchpad you can now let go of the joystick side
  * You can touch the joystick side whenever needed, and you can press buttons with your keyboard side
  * If you let go of the keyboard hand from the trackpad you'll need to do the above setup again (release both hands, touch joystick side first, touch keyboard side second then release joystick side, keeping the keyboard side touching)

[Watch this video for a video guide](https://www.youtube.com/watch?v=KmtyBWD3xmQ)

## Additional features

### Func1 & Func2 remap

By default the Func1 and Func2 keys (the two buttons on the left hand side) are not visible in the system, so you cannot use Key Mapper and other tools to remap them. This setting will create a separate hardware keyboard that emits keypress actions whenever Func1 or Func2 is pressed that can be caught by Key Mapper and other apps for any use.

By default the buttons will be mapped to F13 and F14, which although is supported by Key Mapper, is not supported by a lot of other apps. The compatibility mode will map them to F11 and F12 which have better support overall, but might clash with some apps that actually read these events, like virtual desktop apps. 

### Multiple configurations

You can set up multiple distinct configurations and switch between them as you need fit for your usage.

### Activation key

When you set an activation key for a config you can press that key to quickly turn on / off that particular config. When Func1&Func2 remapping is enabled you can also use them as the activator

### Split trackpad

You can split the trackpad into two sides, with both sides having a different config, like one doing scroll, the other mouse emulation.

### Back screen touch

You can also use the back screen's touch sensor for any events, including mouse moves, scrolling or joystick input

## Troubleshooting

### Verifying Shizuku authorization
A green banner on the main page indicates that Shizuku authorization has been granted to TitanPad. TitanPad also requires Shizuku with support for MTK phones. Only the fourth screenshot below will result in a fully working application.

<div align="center">
<img src='./docs/imgs/screenshot_1_no_permissions.png' width=200 alt='Screenshot showing no permissions at all'>
<img src='./docs/imgs/screenshot_2_only_accessibility.png' width=200 alt='Screenshot showing only accessibility permissions being enabled'>
<img src='./docs/imgs/screenshot_3_wrong_shizuku.png' width=200 alt='Screenshot showing both accessibility and shizuku enabled, but with an invalid Shizuku version'>
<img src='./docs/imgs/screenshot_4_ok.png' width=200 alt='Screenshot showing both accessibility and Shizuku enabled, and with the correct version of the latter'>
</div>

**WARNING!** Also note that due to a bug in Shizuku v13.6.0 (the latest official release as of February 2026) it does not work fully on MTK phones. You either have to downgrade to v13.5.4 (the version present on Google Play), or use [thedjchi's Shizuku fork](https://github.com/thedjchi/Shizuku/releases), which also contain other fixes and improvements. 

## Screenshots

<div align="center">
<img src='./docs/imgs/screenshot_example_1.png' width=200 alt='Main menu'>
<img src='./docs/imgs/screenshot_example_2.png' width=200 alt='Input config'>
<img src='./docs/imgs/screenshot_example_3.png' width=200 alt='Tap settings'>
<img src='./docs/imgs/screenshot_example_4.png' width=200 alt='Software emulation'>
</div>

## Build

To build the app you need:

- Android Studio Panda 1 2025.3.1 or later
  - Under SDK Manager install at least:
    - Android SDK 36.1
    - Android NDK 29.0
    - CMake 4.1.2
- Rust
  - Install rust from https://rustup.rs/
  - `rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android`

## FAQs
### Where can I make feature suggestions or report bugs?
You can use the [issues](https://github.com/sztupy/TitanPad/issues) tab for both.

### How else can I contribute?
Please feel free to submit a pull request, create a video walkthrough, or provide anything else you think would be helpful!

### What is Shizuku?
Shizuku allows applications in general to perform actions that require elevated privileges. In TitanPad, it is required to read the trackpad events on the Keyboard on firmware versions where this is not supported natively, as well as to create and manage virtual mouse devices for more native mouse pointer support.

## License
[Apache License Version 2.0](./LICENSE)
