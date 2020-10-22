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
        command "start"
        command "stop"
        command "pause"
        command "returnToBase"
        command "grabSharkInfo"

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
        input(name: "loginusername", type: "string", title:"Email", description: "Shark Account Email Address", required: true, displayDuringSetup: true)
        input(name: "loginpassword", type: "password", title:"Password", description: "Shark Account Password", required: true, displayDuringSetup: true)
        input(name: "sharkdevicename", type: "string", title:"Device Name", description: "Name you've given your Shark Device within the App", required: true, displayDuringSetup: true)
        input(name: "mobiletype", type: "enum", title:"Mobile Device", description: "Type of Mobile Device your Shark is setup on", required: true, displayDuringSetup: true, options:["Apple iOS", "Android OS"])
        input(name: "debugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true)
    }
}
 
def start() {
    runPostDatapointsCmd("SET_Operating_Mode", 2)
}
 
def stop() {
    def stopresults = runPostDatapointsCmd("SET_Operating_Mode", 0)
    logging("d", "$stopresults")
}

def pause() {
    runPostDatapointsCmd("SET_Operating_Mode", 1)
}

def returnToBase() {
    runPostDatapointsCmd("SET_Operating_Mode", 3)
}

def grabSharkInfo() {
    battery = runGetPropertiesCmd("GET_Battery_Capacity").property.value[0]
    sendEvent(name: "Battery_Level", value: "$battery", isStateChange: true, display: true, displayed: true)
    operatingmode = runGetPropertiesCmd("GET_Operating_Mode").property.value[0]
    sendEvent(name: "Operating_Mode", value: "$operatingmode", isStateChange: true, display: true, displayed: true)
    powermode = runGetPropertiesCmd("GET_Power_Mode").property.value[0]
    sendEvent(name: "Power_Mode", value: "$powermode", isStateChange: true, display: true, displayed: true)
    rssi = runGetPropertiesCmd("GET_RSSI").property.value[0]
    sendEvent(name: "RSSI", value: "$rssi", isStateChange: true, display: true, displayed: true)
    errorcode = runGetPropertiesCmd("GET_Error_Code").property.value[0]
    sendEvent(name: "Error_Code", value: "$errorcode", isStateChange: true, display: true, displayed: true)
    volume = runGetPropertiesCmd("GET_Robot_Volume_Setting").property.value[0]
    sendEvent(name: "Robot_Volume", value: "$volume", isStateChange: true, display: true, displayed: true)
    fw = runGetPropertiesCmd("OTA_FW_VERSION").property.value[0]
    sendEvent(name: "Firmware_Version", value: "$fw", isStateChange: true, display: true, displayed: true)

    // GET_Error_Code
    // GET_Robot_Volume_Setting
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
        queryString: "names[]=$operation".toString()
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
