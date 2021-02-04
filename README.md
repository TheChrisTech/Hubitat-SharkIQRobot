# Hubitat-SharkIQRobot

![GitHub all releases](https://img.shields.io/github/downloads/TheChrisTech/Hubitat-SharkIQRobot/total) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/TheChrisTech/Hubitat-SharkIQRobot) ![GitHub issues](https://img.shields.io/github/issues-raw/TheChrisTech/Hubitat-SharkIQRobot)

This integration was created by manipulating API requests that were captured through Charles Proxy and the iPhone Shark iOS App.

Please send feedback and/or issues to the Hubitat Forum Thread: https://community.hubitat.com/t/shark-iq-robot/34286, or issues to the Issues Section of this Repository.

## Preferences:

| Preference | Description |
| ------------- | ------------- |
| Email | Shark Account Email Address. |
| Password | Shark Account Password. |
| Device Name | Name you've given your Shark Device within the App. |
| Mobile Device | Type of Mobile Device your Shark is setup on (Not super important, but will spoof the api calls as if you're using a particular device) |
| Scheduled State Refresh | If enabled, after you click 'Save Preferences', click the 'Refresh' button to start the schedule. |
| Refresh Interval | Number of minutes between State Refreshes. |
| Smart State Refresh | If enabled, will only refresh when vacuum is running (per interval), then every 5 minutes until Fully Charged. Takes precedence over Scheduled State Refresh. |
| Scheduled Run Time from Shark App | Enter the time Shark is scheduled to run through the Shark App to control dormant smart scheduling, blank to disable and default to 15 minute pings when dormant |
| Google Home Compatibility | Toggle to add a 'status' state |
| Enable Debug Logging | Adds more logging information. |
| Room Cleaning Group 1 | Group 1 that you can enter for specific room cleaning. Enter up to 3 rooms - Comma delimited (eg. 'Basement,Living Room,Bathroom') |
| Room Cleaning Group 2 | Group 2 that you can enter for specific room cleaning. Enter up to 3 rooms - Comma delimited (eg. 'Family Room,Kitchen,Dining Room') |
| Room Cleaning Group 3 | Group 3 that you can enter for specific room cleaning. Enter up to 3 rooms - Comma delimited (eg. 'Guest Bedroom,Foyer,Office') |


<br>

## Current States
*This section outlines what the States on the right-side of Hubitat mean.*

| State  | Description |
| ------------- | ------------- |
| Available_Rooms | This displays all the rooms you have configured on your map. |
| Battery_Level | This displays the Shark's current battery level. |
| Charging_Status | This displays the Shark's current charging status. More information can be found under [Charging Status](#charging-status) section. |
| Error_Code | This displays the meaning to the error code number. More information can be found under [Error Codes](#error-codes) section. |
| Firmware_Version | What firmware your Shark vacuum is currently running. |
| Last_Refreshed | Displays the date/time when the current state information was retrieved. |
| Operating_Mode | This displays the Shark's current battery level. More information can be found under [Operating Modes](#operating-modes) section. |
| Power_Mode | This displays the Shark's current battery level. More information can be found under [Power Modes](#power-modes) section.|
| RSSI | This is the Shark vacuums current wireless (WiFi) signal strength. |
| Recharging_To_Resume | This displays whether the Shark is recharging to continue cleaning. |
| Robot_Volume | This displays the Shark's volume level. |
| switch | This is the current switch state (on/off). |
| status | This displays one of four values: docked, undocked, running, or paused. Must have 'Google Home Compatibility' preference enabled.

<br>

## Charging Status

| Charging Status | Description |
| ------------- | ------------- |
| Charging | The Shark is currently charging. |
| Fully Charged | The Shark is not charging, as it's battery is at 100%. |
| Not Charging | The Shark is currently not charging (As it may be off the dock or running). |

<br>

## Error Codes
*By default, this driver will display the meaning under the Current States section of Hubitat. This state was never fully tested.*

| Error Code | Means |
| ------------- | ------------- |
| 0 | No error |
| 1 | Side wheel is stuck |
| 2 | Side brush is stuck |
| 3 | Suction motor failed |
| 4 | Brushroll stuck |
| 5 | Side wheel is stuck (2) |
| 6 | Bumper is stuck |
| 7 | Cliff sensor is blocked |
| 8 | Battery power is low |
| 9 | No Dustbin |
| 10 | Fall sensor is blocked |
| 11 | Front wheel is stuck |
| 12 | Switched off |
| 13 | Magnetic strip error |
| 14 | Top bumper is stuck |
| 15 | Wheel encoder error |

## Operating Modes:

| Operating Mode  | Description |
| ------------- | ------------- |
| Recharging to Continue | The Shark is not done cleaning, but is recharging because the battery is too low to continue. |
| Stopped | The Shark is currently stopped. |
| Paused | The Shark is currently paused. |
| Running | The Shark is currently running. |
| Returning to Dock | The Shark is currently returning to the dock. |
| Resting on Dock | The Shark is on the dock, but not charging. |
| Charging on Dock | The Shark is on the dock and is charging. |

<br>

## Power Modes
*By default, this driver will display the meaning under the Current States section of Hubitat.*

| Power Mode | Means |
| ------------- | ------------- |
| 0 | Normal |
| 1 | Eco |
| 2 | Max |

## Schedule Types
| Schedule Type  | Description |
| ------------- | ------------- |
| Smart Refresh - Active | The Shark is actively cleaning, paused, or docking. Refreshes will occur every minute by default or as set in Refresh Interval parameter. |
| Smart Refresh - Charging | The Shark is charging on the dock. Refreshes will occur every 5 minutes until fully charged. |
| Smart Scheduled Refresh - Dormant | The Shark is fully charged resting on the dock. Refreshes have been scheduled according to the time supplied in Scheduled Run Time from Shark App parameter. |
| Smart Interval Refresh - Dormant | The Shark is fully charged resting on the dock. Refreshes have been scheduled every 15 minutes. |
| Unscheduled | No schedule has been set for refreshing the status of your Shark. Modify your preferences to enable scheduled refreshes. |
| Scheduled Refresh | The Shark's status will be refreshed on a recurring interval as set in the Refresh Interval parameter. |

## Dashboard Tiles

### Start and Stop
This driver does support starting and stopping via a dashboard tile. To do so:
1. Click "+" to add a new tile.
2. Select your Shark device.
3. Choose the template "Button".

### Current States
You can also pull current states to a dashboard tile as well. To do so:
1. Click "+" to add a new tile.
2. Select your Shark device.
3. Choose the template "Attribute".
4. Select the attribute from the dropdown.

## Schedules Explained
There's a few methods of scheduling (and refreshing) within this driver. Explanations for the state itself can be found under [Schedule_Types](#schedule-types).
1. Smart State Refresh - If enabled, the driver will only refresh when vacuum is running (at the `Refresh Inverval`). After cleaning is complete, it will then refresh every 5 minutes until the Shark Vacuum is Fully Charged. 
2. Scheduled State Refresh - If enabled, the driver will constantly refresh using the `Refresh Interval` value. An initial click on the 'Refresh' button is needed to start this schedule. Use this method
3. Smart State Refresh + Scheduled Run through the Shark App - If you want to schedule your Shark Vacuum runs through the Sharp App, you must have both `Smart State Refresh` enabled and have `Scheduled Run Time from Shark App` populated. 

# Donations
To support this project, you can make a donation to its current maintainer:

[![paypal](https://github.com/Ximi1970/Donate/blob/master/paypal_btn_donateCC_LG_1.gif?raw=true)](https://paypal.me/TheChrisTech)
[![bitcoin-black](https://github.com/Ximi1970/Donate/blob/master/bitcoin-donate-black.png?raw=true)](https://commerce.coinbase.com/checkout/a04089a2-8773-492d-bb77-8524666c2401)