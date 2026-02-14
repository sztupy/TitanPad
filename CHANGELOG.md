# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Support for right side trackpad split
- Support for setting up screen areas for the scroll events for all 4 different input areas (left trackpad, main trackpad, right trackpad and back screen)
- Add support to only auto-enable a config if TitanPad is already enabled. Can be used if you want to add an app-specific config but you only want to run it if you have TitanPad (like the Main Config) already enabled
- Add support for backing up and restoring the application config to a json file
- Add support for backing up and restoring a single configuration to a json file. Can be used to duplicate configs or to share configs with others
- Add support for auto-enable config on boot. Useful to counter the effects when killing the app in the app switcher. Also useful during boot if Shizuku can auto-start
- Add support for hardware mouse wheel as an input. This will emulate a mouse with two scroll wheels, both capable of high resolution movements. Options in the menu also include emulating momentum based wheel as well. Wheel would create the scroll effect at the position the hardware mouse is at.

### Changed

- Allow more granular slider (every 5%) for the trackpad split
- Instead of an "auto disable" option you can now also select to enable another config when switching away from an app
- Changed the Disclosure page to be more compliant to Google Play rules
- Made the debug build's icon slightly different so it can be easier separated from production builds

## [0.1.0] - 2026-02-10

### Added

- Initial public version
- Watch the intro video at https://www.youtube.com/watch?v=n3HS-zF6z5E

[unreleased]: https://github.com/sztupy/TitanPad/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sztupy/TitanPad/releases/tag/v0.1.0
