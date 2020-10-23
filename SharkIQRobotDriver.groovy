/**
 *  Shark IQ Robot
 *
 *  Copyright 2020 Chris Stevens
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
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2020-02-13  Chris Stevens  Original Creation
 *    2020-10-16  Chris Stevens  Initial 'Public' Release
 *    2020-10-17  Chris Stevens  Revision for newer AlyaNetworks API endpoints - Support for Multi Devices (Just create Multiple Drivers) - Spoof iOS or Android Devices when making API calls.
 *    2020-10-21  Chris Stevens  Toggle for Debug Logging - Shark States - Some code cleanup
 *
 */

import groovy.json.*
import java.util.regex.*

metadata {
    definition (name: "Shark IQ Robot", namespace: "cstevens", author: "Chris Stevens") {    
        capability "Switch"
        capability "Refresh"
        command "pause"
        command "stop"
        command "grabSharkInfo"
        command "setPowerMode", [[name:"Set Power Mode", type: "ENUM",description: "Set Power Mode", constraints: ["Eco", "Normal", "Max"]]]

        attribute "Battery_Level", "integer"
        attribute "Operating_Mode", "text"
        attribute "Power_Mode", "text"
        attribute "Charging_Status", "text"
        attribute "RSSI", "text"
        attribute "Error_Code","text"
        attribute "Robot_Volume","text"
        attribute "Firmware_Version","text"
    }
 
    preferences {
        input(name: "loginusername", type: "string", title: "Email", description: "Shark Account Email Address", required: true, displayDuringSetup: true)
        input(name: "loginpassword", type: "password", title: "Password", description: "Shark Account Password", required: true, displayDuringSetup: true)
        input(name: "sharkdevicename", type: "string", title: "Device Name", description: "Name you've given your Shark Device within the App", required: true, displayDuringSetup: true)
        input(name: "mobiletype", type: "enum", title: "Mobile Device", description: "Type of Mobile Device your Shark is setup on", required: true, displayDuringSetup: true, options:["Apple iOS", "Android OS"])
        input(name: "refreshEnable", type: "bool", title: "Enable Refresh Interval", description: "If enabling, after you click 'Save Preferences', click the 'Refresh' button to start the schedule.", defaultValue: false)
        input(name: "refreshInterval", type: "integer", title: "Refresh Interval", description: "Number of seconds between State Refreshes", required: true, displayDuringSetup: true, defaultValue: 60)
        input(name: "debugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true)
    }
}

def refresh() {
    logging("d", "Refresh Triggered.")
    grabSharkInfo()
    if (refreshEnable)
    {
        logging("d", "Refresh scheduled in $refreshInterval seconds.")
        runIn("$refreshInterval".toInteger(), refresh)
    }
}
 
def on() {
    runPostDatapointsCmd("SET_Operating_Mode", 2)
    grabSharkInfo()
}
 
def off() {
    def stopresults = runPostDatapointsCmd("SET_Operating_Mode", 3)
    logging("d", "$stopresults")
    grabSharkInfo()
}

def pause() {
    runPostDatapointsCmd("SET_Operating_Mode", 1)
    grabSharkInfo()
}

def stop() {
    runPostDatapointsCmd("SET_Operating_Mode", 0)
    grabSharkInfo()
}

def setPowerMode(String powermode) {
    power_modes = ["Normal", "Eco", "Max"]
    powermodeint = power_modes.indexOf(powermode)
    if (powermodeint >= 0) { runPostDatapointsCmd("SET_Power_Mode", powermodeint) }
    grabSharkInfo()
}

def grabSharkInfo() {
    propertiesResults = runGetPropertiesCmd("names[]=GET_Battery_Capacity&names[]=GET_Operating_Mode&names[]=GET_Power_Mode&names[]=GET_RSSI&names[]=GET_Error_Code&names[]=GET_Robot_Volume_Setting&names[]=OTA_FW_VERSION")
    propertiesResults.each { singleProperty ->
        if (singleProperty.property.name == "GET_Battery_Capacity")
        {
            sendEvent(name: "Battery_Level", value: "$singleProperty.property.value", display: true, displayed: true)
        }
        else if (singleProperty.property.name == "GET_Operating_Mode")
        {
            operating_modes = ["Stopped", "Paused", "Running", "Return to Base"]
            sendEvent(name: "Operating_Mode", value: operating_modes[singleProperty.property.value], display: true, displayed: true)
        }
        else if (singleProperty.property.name == "GET_Power_Mode")
        {
            power_modes = ["Normal", "Eco", "Max"]
            sendEvent(name: "Power_Mode", value: power_modes[singleProperty.property.value], display: true, displayed: true)
        }
        else if (singleProperty.property.name == "GET_RSSI")
        {
            sendEvent(name: "RSSI", value: "$singleProperty.property.value", display: true, displayed: true)
        }
        else if (singleProperty.property.name == "GET_Error_Code")
        {
            error_codes = ["No error", "Side wheel is stuck","Side brush is stuck","Suction motor failed","Brushroll stuck","Side wheel is stuck (2)","Bumper is stuck","Cliff sensor is blocked","Battery power is low","No Dustbin","Fall sensor is blocked","Front wheel is stuck","Switched off","Magnetic strip error","Top bumper is stuck","Wheel encoder error"]
            sendEvent(name: "Error_Code", value: error_codes[singleProperty.property.value], display: true, displayed: true)
        }
        else if (singleProperty.property.name == "GET_Robot_Volume_Setting")
        {
            sendEvent(name: "Robot_Volume", value: "$singleProperty.property.value", display: true, displayed: true)
        }
        else if (singleProperty.property.name == "OTA_FW_VERSION")
        {
            sendEvent(name: "Firmware_Version", value: "$singleProperty.property.value", display: true, displayed: true)
        }
    }
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
        uri: "https://ads-field.aylanetworks.com",
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
        uri: "https://ads-field.aylanetworks.com",
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
            if(response.getStatus() == 200 || response.getStatus() == 201) {
                results = response.data
                logging("d", "Response received from Shark in the postResponseHandler. $response.data")
            }
            else {
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
            if(response.getStatus() == 200 || response.getStatus() == 201) {
                results = response.data
                logging("d", "Response received from Shark in the getResponseHandler. $response.data")
            }
            else {
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
    if (mobiletype == "Apple iOS") {
        app_id = "Shark-iOS-field-id"
        app_secret = "Shark-iOS-field-_wW7SiwgrHN8dpU_ugCattOoDk8"
    }
    else if (mobiletype == "Android OS") {
        app_id = "Shark-Android-field-id"
        app_secret = "Shark-Android-field-Wv43MbdXRM297HUHotqe6lU1n-w"
    }
	def body = """{"user":{"email":"$loginusername","application":{"app_id":"$app_id","app_secret":"$app_secret"},"password":"$loginpassword"}}"""
    
    //log.info body
	def params = [
        uri: "https://ads-field.aylanetworks.com",
		path: "/users/sign_in.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*"],
        body: "$body"
    ]
    try {
        httpPost(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201) {
                logging("d","Response received from Shark in the postResponseHandler. $response.data")
                def accesstokenstring = ("$response.data" =~ /access_token:([A-Za-z0-9]*.*?)/)
                authtoken = accesstokenstring[0][1]
                return response
            }
            else {
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
        uri: "https://ads-field.aylanetworks.com",
		path: "/users/get_user_profile.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
        httpGet(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201) {
                logging("d","Response received from Shark in the postResponseHandler. $response.data")
                def uuidstring = ("$response.data" =~ /uuid:([A-Za-z0-9-]*.*?)/)
                uuid = uuidstring[0][1]
                return response
            }
            else {
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
        uri: "https://ads-field.aylanetworks.com",
		path: "/apiv1/devices.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
        httpGet(params) { response ->
            if(response.getStatus() == 200 || response.getStatus() == 201) {
                logging("d", "Response received from Shark in the postResponseHandler. $response.data")
                def devicedsn = ""
                for (devices in response.data.device ) {
                    if ("$sharkdevicename" == "${devices.product_name}")
                    {   
                        dsnForDevice = "${devices.dsn}"
                    }
                }
                if ("$dsnForDevice" == '')
                {
                    logging("e", "$sharkdevicename did not match any product_name on your account. Please verify your `Device Name`.")
                }
                return response
            }
            else {
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
    else if (status == "w"){ log.warn(description) }
    else if (status == "e"){ log.error(description) }
}
