/**
 *  Shark IQ Robot v1.0.8
 *
 *  Copyright 2021-2022 Chris Stevens
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  
 *  GitHub link: https://github.com/TheChrisTech/Hubitat-SharkIQRobot
 *
 *  Readme is outlined in README.md
 *  Change History is outlined in CHANGELOG.md
 *
 */

import java.String.*
import groovy.time.*
import java.util.regex.*
import java.text.SimpleDateFormat

metadata {
    definition (name: "Shark IQ Robot", namespace: "cstevens", author: "Chris Stevens", importUrl: "https://raw.githubusercontent.com/TheChrisTech/Hubitat-SharkIQRobot/master/SharkIQRobotDriver.groovy") {
        capability "Switch"
        capability "Refresh"
        capability "Momentary"
        capability "Battery"
        command "locate"
        command "pause"
        command "setPowerMode", [[name:"Set Power Mode to", type: "ENUM",description: "Set Power Mode", constraints: ["Eco", "Normal", "Max"]]]
        command "getRobotInfo", [[name:"Get verbose robot information and push to logs."]]

        attribute "Battery_Level", "integer"
        attribute "Operating_Mode", "string"
        attribute "Power_Mode", "string"
        attribute "Charging_Status", "string"
        attribute "RSSI", "string"
        attribute "Error_Code","string"
        attribute "Robot_Volume","string"
        attribute "Firmware_Version","string"
        attribute "Last_Refreshed","string"
        attribute "Recharging_To_Resume","string"
        attribute "Schedule_Type","string"
    }
 
    preferences {
        input(name: "loginUsername", type: "string", title: "Email", description: "Shark Account Email Address", required: true, displayDuringSetup: true)
        input(name: "loginPassword", type: "password", title: "Password", description: "Shark Account Password", required: true, displayDuringSetup: true)
        input(name: "sharkDeviceName", type: "string", title: "Device Name", description: "Name you've given your Shark Device within the App", required: true, displayDuringSetup: true)
        input(name: "mobileType", type: "enum", title: "Mobile Device", description: "Type of Mobile Device your Shark is setup on", required: true, displayDuringSetup: true, options:["Apple iOS", "Android OS"])
        input(name: "refreshEnable", type: "bool", title: "Scheduled State Refresh", description: "If enabled, after you click 'Save Preferences', click the 'Refresh' button to start the schedule.", defaultValue: false)
        input(name: "refreshInterval", type: "integer", title: "Refresh Interval", description: "Number of minutes between Scheduled State Refreshes. Active only if Scheduled State Refresh is turned on.", required: true, displayDuringSetup: true, defaultValue: 1)
        input(name: "smartRefresh", type: "bool", title: "Smart State Refresh", description: "If enabled, will only refresh when vacuum is running (per interval), then every 5 minutes until Fully Charged. After running, polls less frequently or as scheduled through the Shark App. Takes precedence over Scheduled State Refresh.", required: true, displayDuringSetup: true, defaultValue: true)
        input(name: "scheduledTime", type: "time", title: "Scheduled Run Time from Shark App", description: "Enter the time Shark is scheduled to run through the Shark App, blank to disable. Smart State Refresh must be enabled for this to be triggered.", required: false, displayDuringSetup: true, defaultValue: null)
        input(name: "googleHomeCompat", type: "bool", title: "Google Home Compatibility", description: "If enabled, Operating Mode will either be docked, returning to dock, running or paused.", defaultValue: false)
        input(name: "debugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true)
    }
}

def refresh() {
    logging("d", "Refresh Triggered.")
    grabSharkInfo()
    if (smartRefresh && !refreshEnable) 
    {
        if (operatingMode in ["Paused", "Running", "Returning to Dock", "Recharging to Continue"])
        {
            if(device.currentValue('Schedule_Type') != "Smart Refresh - Active")
            {
                logging("d", "Refresh scheduled in $refreshInterval minutes.")
                schedule("0 */$refreshInterval * * * ? *", refresh)
            }
            eventSender("Schedule_Type", "Smart Refresh - Active", true)
        }
        else if (operatingMode in ["Charging on Dock"])
        {
            if(device.currentValue('Schedule_Type') != "Smart Refresh - Charging")
            {
                logging("d", "Refresh scheduled in 5 minutes.")
                schedule("0 */5 * * * ? *", refresh)
            }  
            eventSender("Schedule_Type", "Smart Refresh - Charging", true)
        }        
        else if (operatingMode in ["Resting on Dock"])
        {
			if (scheduledTime != null)
			{
				String hour = scheduledTime.substring(11,13)
				String minute = scheduledTime.substring(14,16)
				if(device.currentValue('Schedule_Type') != "Smart Scheduled Refresh - Dormant")
				{
					logging("d", "Refresh scheduled for $hour:$minute.")
					schedule("*/30 $minute $hour * * ? *", refresh)
					eventSender("Schedule_Type", "Smart Scheduled Refresh - Dormant", true)
				}
			} 
			else
			{
				if(device.currentValue('Schedule_Type') != "Smart Interval Refresh - Dormant")
				{
					logging("d", "Refresh scheduled in 15 minutes.")
					schedule("0 */15 * * * ? *", refresh)
					eventSender("Schedule_Type", "Smart Interval Refresh - Dormant", true)
				}
			}
        }
        else
        {
            logging("d", "Not scheduling a refresh, because the operatingMode = $operatingMode")
            unschedule()
            eventSender("Schedule_Type", "Unscheduled", true)
        }
    }
    else if (!smartRefresh && refreshEnable)
    {
        if (device.currentValue('Schedule_Type') != "Scheduled Refresh")
        {
            logging("d", "Refresh scheduled in $refreshInterval minutes.")
            schedule("0 */$refreshInterval * * * ? *", refresh)
        }
        eventSender("Schedule_Type", "Scheduled Refresh", true)
    }
    else if (smartRefresh && refreshEnable)
    {
        logging("e", "Not scheduling refresh - Please enable only 1 refresh type (Smart or Scheduled).")
        unschedule()
        eventSender("Schedule_Type", "Unscheduled", true)
    }
    else
    {
        logging("d", "No options chosen for scheduled refresh.")
        unschedule()
        eventSender("Schedule_Type", "Unscheduled", true)
    }

}
def push() {
    grabSharkInfo()
    if (operatingModeValue == 3)
    {
        on()
    }
    else 
    {
        off()
    }
}
 
def on() {
    runPostDatapointsCmd("SET_Operating_Mode", 2)
    eventSender("switch","on",true)
    eventSender("Operating_Mode", "Running", true)
    runIn(10, refresh)
}
 
def off() {
    runPostDatapointsCmd("SET_Operating_Mode", 3)
    eventSender("switch","off",true)
    eventSender("Operating_Mode", "Returning to Dock", true)
    runIn(10, refresh)
}

def pause() {
    runPostDatapointsCmd("SET_Operating_Mode", 0)
    eventSender("switch","off",true)
    eventSender("Operating_Mode", "Paused", true)
    runIn(10, refresh)
}

def setPowerMode(String powermode) {
    power_modes = ["Normal", "Eco", "Max"]
    powermodeint = power_modes.indexOf(powermode)
    if (powermodeint >= 0) { runPostDatapointsCmd("SET_Power_Mode", powermodeint) }
    runIn(10, refresh)
}

def locate() {
    logging("d", "Locate Pushed.")
    runPostDatapointsCmd("SET_Find_Device", 1)
    eventSender("Locate", "Active", false)
    runIn(5, runPostDatapointsCmd("SET_Find_Device", 0))
    runIn(10, refresh)
}

def getRobotInfo() {
    propertiesResults = runGetPropertiesCmd("names[]=GET_Main_PCB_BL_Version&names[]=GET_Main_PCB_HW_Version&names[]=GET_Main_PCB_FW_Version&names[]=GET_Nav_Module_FW_Version&names[]=GET_Nav_Module_App_Version&names[]=GET_SCM_FW_Version")
    propertiesResults.each { singleProperty ->
        if (singleProperty.property.name == "GET_Main_PCB_BL_Version")
        {
            logging("i", "Main_PCB_BL_Version: $singleProperty.property.value")
        }
        else if (singleProperty.property.name == "GET_Main_PCB_HW_Version")
        {
            logging("i", "Main_PCB_HW_Version: $singleProperty.property.value")
        }
        else if (singleProperty.property.name == "GET_Main_PCB_FW_Version")
        {
            logging("i", "Main_PCB_FW_Version: $singleProperty.property.value")
        }
        else if (singleProperty.property.name == "GET_Nav_Module_FW_Version")
        {
            logging("i", "Nav_Module_FW_Version: $singleProperty.property.value")
        }
        else if (singleProperty.property.name == "GET_Nav_Module_App_Version")
        {
            logging("i", "Nav_Module_App_Version: $singleProperty.property.value")
        }
        else if (singleProperty.property.name == "GET_SCM_FW_Version")
        {
            logging("i", "SCM_FW_Version: $singleProperty.property.value")
        }
    }
}

def grabSharkInfo() {
    propertiesResults = runGetPropertiesCmd("names[]=GET_Battery_Capacity&names[]=GET_Recharging_To_Resume&names[]=GET_Charging_Status&names[]=GET_Operating_Mode&names[]=GET_Power_Mode&names[]=GET_RSSI&names[]=GET_Error_Code&names[]=GET_Robot_Volume_Setting&names[]=OTA_FW_VERSION")
    propertiesResults.each { singleProperty ->
        if (singleProperty.property.name == "GET_Battery_Capacity")
        {
            eventSender("Battery_Level", "$singleProperty.property.value", true)
            batteryCapacity = singleProperty.property.value
            eventSender("battery",batteryCapacity.toString(),true)
        }
        else if (singleProperty.property.name == "GET_Recharging_To_Resume")
        {
            recharging_resume = ["False", "True"]
            eventSender("Recharging_To_Resume", recharging_resume[singleProperty.property.value], true)
        }
        else if (singleProperty.property.name == "GET_Charging_Status")
        {
            chargingStatusValue = singleProperty.property.value
        }
        else if (singleProperty.property.name == "GET_Operating_Mode")
        {
            operatingModeValue = singleProperty.property.value
        }
        else if (singleProperty.property.name == "GET_Power_Mode")
        {
            power_modes = ["Normal", "Eco", "Max"]
            eventSender("Power_Mode", power_modes[singleProperty.property.value], true)
        }
        else if (singleProperty.property.name == "GET_RSSI")
        {
            eventSender("RSSI", "$singleProperty.property.value", true)
        }
        else if (singleProperty.property.name == "GET_Error_Code")
        {
            error_codes = ["No error", "Side wheel is stuck","Side brush is stuck","Suction motor failed","Brushroll stuck","Side wheel is stuck (2)","Bumper is stuck","Cliff sensor is blocked","Battery power is low","No Dustbin","Fall sensor is blocked","Front wheel is stuck","Switched off","Magnetic strip error","Top bumper is stuck","Wheel encoder error"]
            eventSender("Error_Code", error_codes[singleProperty.property.value], true)
        }
        else if (singleProperty.property.name == "GET_Robot_Volume_Setting")
        {
            eventSender("Robot_Volume", "$singleProperty.property.value", true)
        }
        else if (singleProperty.property.name == "OTA_FW_VERSION")
        {
            eventSender("Firmware_Version", "$singleProperty.property.value", true)
        }
    }

    // Charging Status
    // chargingStatusValue - 0 = NOT CHARGING, 1 = CHARGING
    charging_status = ["Not Charging", "Charging"]
    if (device.currentValue('Battery_Level') == "100")
    {
        chargingStatusToSend = "Fully Charged" 
    }
    else
    {
        chargingStatusToSend = charging_status[chargingStatusValue]
    }
    eventSender("Charging_Status", chargingStatusToSend, true)

    // Operating Mode 
    // operatingModeValue - 0 = STOPPED, 1 = PAUSED, 2 = ON, 3 = OFF
    operating_modes = ["Stopped", "Paused", "Running", "Returning to Dock"]
    if (device.currentValue('Recharging_To_Resume') == "True" && operatingModeValue.toString() == "3")
    { 
        operatingModeToSend = "Recharging to Continue" 
    }
    else if (device.currentValue('Recharging_To_Resume') != "True" && operatingModeValue.toString() == "3")
    {
        if (device.currentValue('Charging_Status') == "Fully Charged")
        {
            operatingModeToSend = "Resting on Dock" 
        }
        else if (device.currentValue('Charging_Status') == "Charging")
        {
            operatingModeToSend = "Charging on Dock" 
        }
        else {
            operatingModeToSend = "Returning to Dock" 
        }
    }
    else {
        operatingModeToSend = operating_modes[operatingModeValue] 
        if (operatingModeValue.toString() == "2")
        {
            eventSender("switch","on",true)
        }
    }
    if (operatingModeValue != 2)
    {
        eventSender("switch","off",true)
    }
    eventSender("Operating_Mode", operatingModeToSend, true)
    operatingMode = operatingModeToSend

    def date = new Date()
    eventSender("Last_Refreshed", "$date", true)
}

def initialLogin() {
    login()
    getDevices()
    getUserProfile()
}

def runPostDatapointsCmd(String operation, Integer operationValue) {
    initialLogin()
    def localDevicePort = (devicePort==null) ? "80" : devicePort
	def params = [
        uri: "https://ads-field-39a9391a.aylanetworks.com",
		path: "/apiv1/dsns/$dsnForDevice/properties/$operation/datapoints.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
        body: "{\"datapoint\":{\"value\":\"$operationValue\",\"metadata\":{\"userUUID\":\"$uuid\"}}}"
    ]
    performHttpPost(params)
}

def runGetPropertiesCmd(String operation) {
    initialLogin()
    def localDevicePort = (devicePort==null) ? "80" : devicePort
	def params = [
        uri: "https://ads-field-39a9391a.aylanetworks.com",
		path: "/apiv1/dsns/$dsnForDevice/properties.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
        queryString: "$operation".toString()
    ]
    performHttpGet(params)
}

private performHttpPost(params) {
    try {
        httpPost(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201)
            {
                results = response.data
                logging("d", "Response received from Shark in the postResponseHandler. $response.data")
            }
            else
            {
                logging("e", "Shark failed. Shark returned ${response.getStatus()}.")
                logging("e", "Error = ${response.getErrorData()}")
            }
        }
    } 
    catch (e) {
        logging("e", "Error during performHttpPost: $e")
    }
    return results
}

private performHttpGet(params) {
    try {
        httpGet(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201)
            {
                results = response.data
                logging("d", "Response received from Shark in the getResponseHandler. $response.data")
            }
            else
            {
                logging("e", "Shark failed. Shark returned ${response.getStatus()}.")
                logging("e", "Error = ${response.getErrorData()}")
            }
        }
    } 
    catch (e) {
        logging("e", "Error during performHttpGet: $e")
    }
    return results
}

def login() {
    def localDevicePort = (devicePort==null) ? "80" : devicePort
    def app_id = ""
    def app_secret = ""
    if (mobileType == "Apple iOS")
    {
        app_id = "Shark-iOS-field-id"
        app_secret = "Shark-iOS-field-_wW7SiwgrHN8dpU_ugCattOoDk8"
    }
    else if (mobileType == "Android OS")
    {
        app_id = "Shark-Android-field-id"
        app_secret = "Shark-Android-field-Wv43MbdXRM297HUHotqe6lU1n-w"
    }
	def body = """{"user":{"email":"$loginUsername","application":{"app_id":"$app_id","app_secret":"$app_secret"},"password":"$loginPassword"}}"""
    
    //log.info body
	def params = [
        uri: "https://ads-field-39a9391a.aylanetworks.com",
		path: "/users/sign_in.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*"],
        body: "$body"
    ]
    try {
        httpPost(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201)
            {
                logging("d","Response received from Shark in the postResponseHandler. $response.data")
                def accesstokenstring = ("$response.data" =~ /access_token:([A-Za-z0-9]*.*?)/)
                authtoken = accesstokenstring[0][1]
                return response
            }
            else
            {
                logging("e","Shark failed. Shark returned ${response.getStatus()}.")
                logging("e","Error = ${response.getErrorData()}")
            }
        }
    } catch (e) {
    	logging("e", "Error during login: $e")
	}
}

def getUserProfile() {
	def params = [
        uri: "https://ads-field-39a9391a.aylanetworks.com",
		path: "/users/get_user_profile.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
        httpGet(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201)
            {
                logging("d","Response received from Shark in the postResponseHandler. $response.data")
                def uuidstring = ("$response.data" =~ /uuid:([A-Za-z0-9-]*.*?)/)
                uuid = uuidstring[0][1]
                return response
            }
            else
            {
                logging("e", "Shark failed. Shark returned ${response.getStatus()}.")
                logging("e", "Error = ${response.getErrorData()}")
            }
        }
    } catch (e) {
    	logging("e", "Error during getUserProfile: $e")
	}

}

def getDevices() {
	def params = [
        uri: "https://ads-field-39a9391a.aylanetworks.com",
		path: "/apiv1/devices.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
        httpGet(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201)
            {
                logging("d", "Response received from Shark in the postResponseHandler. $response.data")
                def devicedsn = ""
                for (devices in response.data.device )
                {
                    if ("$sharkDeviceName" == "${devices.product_name}")
                    {   
                        dsnForDevice = "${devices.dsn}"
                    }
                }
                if ("$dsnForDevice" == '')
                {
                    logging("e", "$sharkDeviceName did not match any product_name on your account. Please verify your `Device Name`.")
                }
                return response
            }
            else
            {
                logging("e", "Shark failed. Shark returned ${response.getStatus()}.")
                logging("e", "Error = ${response.getErrorData()}")
            }
        }
    } catch (e) {
    	logging("e", "Error during getDevices: $e")
	}

}

/********************************************
*** HELPER METHODS
********************************************/

def logging(String status, String description) {
    if (debugEnable && status == "d"){ log.debug(description) }
    else if (status == "i"){ log.info(description) }
    else if (status == "w"){ log.warn(description) }
    else if (status == "e"){ log.error(description) }
}

def eventSender(String name, String value, Boolean display)
{
    if (googleHomeCompat)
    {
        if (name == "Operating_Mode")
        {
            sendEvent(name: "$name", value: "$value", display: "$display", displayed: "$display")
            name = "status"
            if (value == "Charging on Dock" || value == "Resting on Dock" || value == "Recharging to Continue")
            {
                value = "docked"
                eventSender("switch","off",true)
            }
            else if (value == "Returning to Dock" || value == "Stopped")
            {
               value = "returning to dock" 
            }
            else if (value == "Paused")
            {
               value = "paused"  
            }
            else if (value == "Running")
            {
               value = "running"  
            }
            value = value.toLowerCase()
        }
    }
    sendEvent(name: "$name", value: "$value", display: "$display", displayed: "$display")
}