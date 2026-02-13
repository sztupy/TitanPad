# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Support for right side trackpad split
- Support for setting up screen areas for the scroll events for all 4 different input areas (left trackpad, main trackpad, right trackpad and back screen)
- Add support to only auto-enable a config if TitanPad is already enabled. Can be used if you want to add an app-specific config but you only want to run it if you have TitanPad (like the default config) already enabled
- Add support for backing up and restoring the application config to a json file

### Changed

- Allow more granular slider (every 5%) for the trackpad split
- Instead of an "auto disable" option you can now also select to enable another config when switching away from an app 

## [0.1.0] - 2026-02-10

### Added

- Initial public version
- Watch the intro video at https://www.youtube.com/watch?v=n3HS-zF6z5E

[unreleased]: https://github.com/sztupy/TitanPad/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sztupy/TitanPad/releases/tag/v0.1.0