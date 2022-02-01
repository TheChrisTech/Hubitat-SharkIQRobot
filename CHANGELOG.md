# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.8]

### Changed

- API endpoint changed on Sharks end, which caused this integration to start failing. Updated that within the code.

## [1.0.7]

## [1.0.6]

### Added

- Added 'importUrl' to metadata definition for easier driver updates. Just click Import on the driver and it'll grab the latest (But will overwrite any manual changes to the driver you have made, so be careful!).
- Scheduled Run Time from Shark App (Preference) - Use this setting with Smart State Refresh Enabled to allow refreshes to lie dormant.  This setting should be set to the same time the Shark is scheduled to run through the Shark App.  If disabled, dormant refreshes default to 15 minutes. 
- Schedule_Type (Attribute) - This Attribute shows the current type of refresh schedule your Shark device is following.

### Changed

- Refresh Interval Change - This preference was changed from seconds to minutes. (eg. a value of '60' now means 1 hour, not 1 minute as it did previously)
- State Detection Improvements - Fixed a bug where if Recharging to Resume was null, the robot would always show Returning to Dock.
- Scheduling done by cron instead of RunIn - Since cron scheduling survives a hub reboot, it is a more hands-off way of managing scheduling for your robot vacuum.  Once set, the schedule will faithfully run allowing any rules set up to auto run without intervention.
- Updated 'status' state - Actually make it display one of four values: running, paused, returning to dock (previously undocked), or docked.

## [1.0.5]

### Added

- Google Home Compatibility (Preference) - Toggle to add a 'status' state. This state displays one of four values: docked, returning to base, running, or paused. This has not been fully tested, so may have some issues (I don't use Google Home).
- Additional Scheduling Log Messages - If you have both configured or none configured, an error or debug message (accordingly) will be displayed in the logs.

### Changed

- SendEvent Improvements - Created Helper method  more consistent coding.
- Updated 'switch' state - Simple 'on' or 'off' values to determine the state of your vacuum - Now visible and triggered more frequently for during button presses and updates.

## [1.0.4]

### Changed

- Fixed Driver Versioning

## [1.0.3]

### Changed

- Fixed issue with HTTP Posts (caused broken driver - Do not use 1.0.2)

## [1.0.2]

### Added

- Get Robot Info (button) - Allows this to log out Specific Firmware and Hardware revisions to the logs (as 'info' level entries). This is to help troubleshoot differences some folks are seeing with behavior with this driver.
- Info logging - Allows 'info' entries to be logged.
- Locate (button) - This allows you to locate your Vacuum in your house. Runs for 5 seconds, then stops.

### Changed

- Logic for 'Fully Charged' - Device will be marked 'Fully Charged' if battery level is at 100%, regardless of charge state.

## [1.0.1]

### Changed

- Fix Readme issues

## [1.0.0]

### Added

- New Formatting to README.md
- CHANGELOG.md created
- Added Momentary Capability (for Dashboard support)
- Added Dashboard Tile steps in the README.md

### Changed

- Added additional comments to keep track of what Shark API responses mean.

## [0.6.0] - 2020-10-23

### Added

- "Last_Refreshed" State
- "Recharging_To_Resume" State
- "Smart State Refresh" Button
- Additional "Operating_Mode" State values
- Slight Delay when getting Refresh After Triggering Button

### Changed

- Fixed Pause Button

### Removed

- Stop Button

## [0.5.0] - 2020-10-22

### Added

- Add Refresh 
- Re-add Switch 
- Optimize State API Calls

## [0.4.0] - 2020-10-21

### Added

- Toggle for Debug Logging 
- Shark States

### Changed

- Performed some code cleanup.

## [0.3.0] - 2020-10-17

### Added

- Revision for newer AlyaNetworks API endpoints
- Support for Multi Devices (Just create Multiple Drivers)
- Spoof iOS or Android Devices when making API calls.

## [0.2.0] - 2020-10-16

### Added

- Initial 'Public' Release

## [0.1.0] - 2020-02-13

### Added

- Original Creation of Github Repo.